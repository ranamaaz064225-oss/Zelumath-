package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ZeluFirestoreRepository
import com.example.firebase.AdminSecurityHelper
import com.example.model.AdminAuditLog
import com.example.model.AdminSecurityConfig
import com.example.model.CurriculumItem
import com.example.model.PaymentMethod
import com.example.model.SubscriptionPlan
import com.example.model.SupportTicket
import com.example.model.SystemSettings
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val ADMIN_TAG = "AdminVM"

class AdminViewModel(
    private val repository: ZeluFirestoreRepository
) : ViewModel() {

    val users: StateFlow<UiState<List<UserProfile>>> = repository.observeAllUsersAdmin()
        .asAdminUiState("Failed to load users")

    val curriculum: StateFlow<UiState<List<CurriculumItem>>> = repository.observeAllCurriculumAdmin()
        .asAdminUiState("Failed to load curriculum")

    val plans: StateFlow<UiState<List<SubscriptionPlan>>> = repository.observeAllPlansAdmin()
        .asAdminUiState("Failed to load plans")

    val paymentMethods: StateFlow<UiState<List<PaymentMethod>>> = repository.observeAllPaymentMethodsAdmin()
        .asAdminUiState("Failed to load payment methods")

    val tickets: StateFlow<UiState<List<SupportTicket>>> = repository.observeAllTicketsAdmin()
        .asAdminUiState("Failed to load tickets")

    val auditLogs: StateFlow<UiState<List<AdminAuditLog>>> = repository.observeAdminLogs()
        .asAdminUiState("Failed to load audit logs")

    val systemSettings: StateFlow<UiState<SystemSettings?>> = repository.observeSystemSettings()
        .asAdminUiState("Failed to load settings")

    val securityConfig: StateFlow<UiState<AdminSecurityConfig?>> = repository.observeAdminSecurityConfig()
        .asAdminUiState("Failed to load security configuration")

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun setStatusMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun seedStarterCatalog() {
        viewModelScope.launch {
            val res = repository.adminSeedStarterCatalog()
            _statusMessage.value = if (res.isSuccess) {
                "Starter curriculum, plans, payment methods, and settings initialized in Firestore."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to initialize catalog."
            }
        }
    }

    fun updateUserStatusAndPlan(userId: String, status: String, planTier: String) {
        viewModelScope.launch {
            val res = repository.adminUpdateUserStatusAndPlan(userId, status, planTier)
            _statusMessage.value = if (res.isSuccess) {
                "Updated user account."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to update user."
            }
        }
    }

    fun addCurriculumItem(item: CurriculumItem) {
        viewModelScope.launch {
            val res = repository.adminAddCurriculumItem(item)
            _statusMessage.value = if (res.isSuccess) {
                "Curriculum question published."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to add curriculum item."
            }
        }
    }

    fun toggleCurriculumPublish(itemId: String, published: Boolean) {
        viewModelScope.launch {
            val res = repository.adminToggleCurriculumPublish(itemId, published)
            if (res.isFailure) {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Toggle failed."
            }
        }
    }

    fun deleteCurriculumItem(itemId: String) {
        viewModelScope.launch {
            val res = repository.adminDeleteCurriculumItem(itemId)
            _statusMessage.value = if (res.isSuccess) {
                "Curriculum item deleted."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Delete failed."
            }
        }
    }

    fun savePlan(plan: SubscriptionPlan) {
        viewModelScope.launch {
            val res = repository.adminSavePlan(plan)
            _statusMessage.value = if (res.isSuccess) {
                "Saved ${plan.name} plan."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to save plan."
            }
        }
    }

    fun savePaymentMethod(method: PaymentMethod) {
        viewModelScope.launch {
            val res = repository.adminSavePaymentMethod(method)
            _statusMessage.value = if (res.isSuccess) {
                "Saved payment method: ${method.name}."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to save payment method."
            }
        }
    }

    fun deletePaymentMethod(methodId: String) {
        viewModelScope.launch {
            val res = repository.adminDeletePaymentMethod(methodId)
            _statusMessage.value = if (res.isSuccess) {
                "Payment method deleted."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to delete payment method."
            }
        }
    }

    fun saveSystemSettings(settings: SystemSettings) {
        viewModelScope.launch {
            val res = repository.adminSaveSystemSettings(settings)
            _statusMessage.value = if (res.isSuccess) {
                "Website, branding & platform settings saved live."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to save settings."
            }
        }
    }

    fun saveNewAdminPassword(
        currentConfig: AdminSecurityConfig?,
        currentPassword: String?,
        newPassword: String,
        recoveryEmail: String,
        requireCurrentCheck: Boolean,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (newPassword.length < 6) {
            val err = "Admin password must be at least 6 characters."
            _statusMessage.value = err
            onComplete(false, err)
            return
        }
        if (requireCurrentCheck && currentConfig?.passwordConfigured == true) {
            val valid = AdminSecurityHelper.verifyPassword(
                password = currentPassword.orEmpty(),
                salt = currentConfig.passwordSalt,
                expectedHash = currentConfig.passwordHash
            )
            if (!valid) {
                val err = "Current admin password is incorrect."
                _statusMessage.value = err
                onComplete(false, err)
                return
            }
        }

        val salt = AdminSecurityHelper.generateSalt()
        val hash = AdminSecurityHelper.hashPassword(newPassword, salt)
        val base = currentConfig ?: AdminSecurityConfig(id = "config")
        val updated = base.copy(
            passwordSalt = salt,
            passwordHash = hash,
            passwordConfigured = true,
            recoveryEmail = recoveryEmail.ifBlank { base.recoveryEmail }.ifBlank { "ranamaaz064225@gmail.com" },
            resetTokenHash = "",
            resetExpiresAt = 0L
        )
        viewModelScope.launch {
            val res = repository.adminSaveSecurityConfig(updated, "SET_ADMIN_PASSWORD")
            val msg = if (res.isSuccess) "Admin password updated." else (res.exceptionOrNull()?.localizedMessage ?: "Failed to save password.")
            _statusMessage.value = msg
            onComplete(res.isSuccess, msg)
        }
    }

    fun setTwoFactorEnabled(
        currentConfig: AdminSecurityConfig?,
        enabled: Boolean,
        regenerateSecret: Boolean = false
    ) {
        val base = currentConfig ?: AdminSecurityConfig(id = "config")
        val secret = if (regenerateSecret || base.totpSecret.isBlank()) {
            AdminSecurityHelper.generateBase32Secret()
        } else {
            base.totpSecret
        }
        val updated = base.copy(
            twoFactorEnabled = enabled,
            totpSecret = secret
        )
        viewModelScope.launch {
            val res = repository.adminSaveSecurityConfig(updated, if (enabled) "ENABLE_2FA" else "DISABLE_2FA")
            _statusMessage.value = if (res.isSuccess) {
                if (enabled) "2FA enabled. Add the TOTP secret to your Authenticator app." else "2FA disabled."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to update 2FA."
            }
        }
    }

    fun updateRecoveryEmail(currentConfig: AdminSecurityConfig?, email: String) {
        val clean = email.trim()
        if (!clean.contains("@")) {
            _statusMessage.value = "Please enter a valid recovery email address."
            return
        }
        val base = currentConfig ?: AdminSecurityConfig(id = "config")
        viewModelScope.launch {
            val res = repository.adminSaveSecurityConfig(base.copy(recoveryEmail = clean), "UPDATE_RECOVERY_EMAIL")
            _statusMessage.value = if (res.isSuccess) "Recovery email updated to $clean." else (res.exceptionOrNull()?.localizedMessage ?: "Failed to update email.")
        }
    }

    fun issuePasswordResetToken(
        currentConfig: AdminSecurityConfig?,
        emailInput: String,
        onTokenIssued: (resetToken: String, targetEmail: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val base = currentConfig ?: AdminSecurityConfig(id = "config")
        val targetEmail = emailInput.trim().ifBlank { base.recoveryEmail }.ifBlank { "ranamaaz064225@gmail.com" }
        val token = AdminSecurityHelper.generateResetToken()
        val tokenHash = AdminSecurityHelper.hashToken(token)
        val expiresAt = System.currentTimeMillis() + 15 * 60_000L

        viewModelScope.launch {
            val res = repository.adminSaveSecurityConfig(
                base.copy(
                    recoveryEmail = targetEmail,
                    resetTokenHash = tokenHash,
                    resetExpiresAt = expiresAt
                ),
                "ISSUE_PASSWORD_RESET"
            )
            if (res.isSuccess) {
                onTokenIssued(token, targetEmail)
            } else {
                onError(res.exceptionOrNull()?.localizedMessage ?: "Could not generate reset token.")
            }
        }
    }

    fun completePasswordResetWithToken(
        currentConfig: AdminSecurityConfig?,
        token: String,
        newPassword: String,
        disableTwoFactor: Boolean,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (currentConfig == null || !AdminSecurityHelper.verifyResetToken(
                token = token,
                expectedHash = currentConfig.resetTokenHash,
                expiresAtMillis = currentConfig.resetExpiresAt
            )
        ) {
            onComplete(false, "Invalid or expired reset link/token.")
            return
        }
        if (newPassword.length < 6) {
            onComplete(false, "New password must be at least 6 characters.")
            return
        }
        val salt = AdminSecurityHelper.generateSalt()
        val hash = AdminSecurityHelper.hashPassword(newPassword, salt)
        viewModelScope.launch {
            val res = repository.adminSaveSecurityConfig(
                currentConfig.copy(
                    passwordSalt = salt,
                    passwordHash = hash,
                    passwordConfigured = true,
                    twoFactorEnabled = if (disableTwoFactor) false else currentConfig.twoFactorEnabled,
                    resetTokenHash = "",
                    resetExpiresAt = 0L
                ),
                "COMPLETE_PASSWORD_RESET"
            )
            val msg = if (res.isSuccess) "Admin password reset successfully." else (res.exceptionOrNull()?.localizedMessage ?: "Reset failed.")
            onComplete(res.isSuccess, msg)
        }
    }

    fun updateSupportTicket(ticketId: String, status: String, reply: String) {
        viewModelScope.launch {
            val res = repository.adminUpdateSupportTicket(ticketId, status, reply)
            _statusMessage.value = if (res.isSuccess) {
                "Support ticket updated."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to update ticket."
            }
        }
    }

    private fun <T> Flow<T>.asAdminUiState(fallbackError: String): StateFlow<UiState<T>> {
        return this
            .map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(ADMIN_TAG, fallbackError, rootError)
                emit(UiState.Error(rootError.message ?: fallbackError))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )
    }
}
