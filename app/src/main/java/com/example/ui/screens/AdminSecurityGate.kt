package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.firebase.AdminSecurityHelper
import com.example.localization.ZeluStrings
import com.example.model.AdminSecurityConfig
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun AdminSecurityGate(
    strings: ZeluStrings,
    isAdmin: Boolean,
    adminViewModel: AdminViewModel?,
    initialResetToken: String,
    onClearResetToken: () -> Unit,
    onBack: () -> Unit,
    content: @Composable (AdminViewModel, AdminSecurityConfig) -> Unit
) {
    if (!isAdmin || adminViewModel == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(10.dp))
            Text(strings.adminAccessDenied, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Route #admin444 requires an authorized administrator Google account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(onClick = onBack) { Text("Back to App") }
        }
        return
    }

    val securityState by adminViewModel.securityConfig.collectAsStateWithLifecycle()
    var sessionUnlocked by rememberSaveable { mutableStateOf(false) }

    when (val state = securityState) {
        is UiState.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Verifying admin vault...", style = MaterialTheme.typography.bodySmall)
            }
        }

        is UiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Exit Admin") }
            }
        }

        is UiState.Success -> {
            val config = state.data
            if (config == null || !config.passwordConfigured) {
                AdminFirstTimeSetupCard(
                    adminViewModel = adminViewModel,
                    initialRecoveryEmail = config?.recoveryEmail?.ifBlank { "ranamaaz064225@gmail.com" } ?: "ranamaaz064225@gmail.com",
                    onSetupComplete = { sessionUnlocked = true },
                    onBack = onBack
                )
            } else if (!sessionUnlocked || initialResetToken.isNotBlank()) {
                AdminUnlockCard(
                    config = config,
                    adminViewModel = adminViewModel,
                    initialResetToken = initialResetToken,
                    onClearResetToken = onClearResetToken,
                    onUnlocked = { sessionUnlocked = true },
                    onBack = onBack
                )
            } else {
                content(adminViewModel, config)
            }
        }
    }
}

@Composable
private fun AdminFirstTimeSetupCard(
    adminViewModel: AdminViewModel,
    initialRecoveryEmail: String,
    onSetupComplete: () -> Unit,
    onBack: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var recoveryEmail by remember { mutableStateOf(initialRecoveryEmail) }
    var showPass by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.widthIn(max = 480.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set Up Admin Vault (#admin444)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Create a dedicated password for the #admin444 portal. You can also enable 2FA Authenticator protection inside Security.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMsg = null },
                    label = { Text("New Admin Password (min 6 chars)") },
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(if (showPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = "Toggle password visibility")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("admin_setup_password")
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMsg = null },
                    label = { Text("Confirm Password") },
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = recoveryEmail,
                    onValueChange = { recoveryEmail = it },
                    label = { Text("Recovery Email for Forgot Password Link") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(errorMsg.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (password.length < 6) {
                                errorMsg = "Password must be at least 6 characters."
                            } else if (password != confirmPassword) {
                                errorMsg = "Passwords do not match."
                            } else {
                                adminViewModel.saveNewAdminPassword(
                                    currentConfig = null,
                                    currentPassword = null,
                                    newPassword = password,
                                    recoveryEmail = recoveryEmail,
                                    requireCurrentCheck = false
                                ) { ok, msg ->
                                    if (ok) onSetupComplete() else errorMsg = msg
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("admin_setup_submit")
                    ) { Text("Create & Enter") }
                }
            }
        }
    }
}

@Composable
private fun AdminUnlockCard(
    config: AdminSecurityConfig,
    adminViewModel: AdminViewModel,
    initialResetToken: String,
    onClearResetToken: () -> Unit,
    onUnlocked: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var totpCode by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var forgotMode by remember(initialResetToken) { mutableStateOf(initialResetToken.isNotBlank()) }
    var emailInput by remember(config.recoveryEmail) { mutableStateOf(config.recoveryEmail.ifBlank { "ranamaaz064225@gmail.com" }) }
    var tokenInput by remember(initialResetToken) { mutableStateOf(initialResetToken) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var disable2FaOnReset by remember { mutableStateOf(false) }
    var feedbackMsg by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.widthIn(max = 480.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (forgotMode) "Reset Admin Password via Email Link" else "Admin Portal Lock (domain/#admin444)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!forgotMode) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; feedbackMsg = null },
                        label = { Text("Admin Password") },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = "Toggle visibility")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("admin_login_password")
                    )

                    if (config.twoFactorEnabled) {
                        OutlinedTextField(
                            value = totpCode,
                            onValueChange = { totpCode = it.filter { ch -> ch.isDigit() }.take(6); feedbackMsg = null },
                            label = { Text("2FA 6-Digit Authenticator Code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("admin_login_2fa")
                        )
                    }

                    if (feedbackMsg != null) {
                        Surface(
                            color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(feedbackMsg.orEmpty(), modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Exit") }
                        Button(
                            onClick = {
                                val passOk = AdminSecurityHelper.verifyPassword(password, config.passwordSalt, config.passwordHash)
                                if (!passOk) {
                                    isError = true
                                    feedbackMsg = "Incorrect admin password."
                                    return@Button
                                }
                                if (config.twoFactorEnabled) {
                                    val totpOk = AdminSecurityHelper.verifyTotpCode(config.totpSecret, totpCode)
                                    if (!totpOk) {
                                        isError = true
                                        feedbackMsg = "Invalid or expired 2FA code. Check your Authenticator app."
                                        return@Button
                                    }
                                }
                                onUnlocked()
                            },
                            modifier = Modifier.weight(1f).testTag("admin_unlock_button")
                        ) { Text("Unlock") }
                    }

                    TextButton(
                        onClick = {
                            forgotMode = true
                            feedbackMsg = null
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Filled.Email, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Forgot Password? Send Reset Link")
                    }
                } else {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Admin Email Address") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = {
                            adminViewModel.issuePasswordResetToken(
                                currentConfig = config,
                                emailInput = emailInput,
                                onTokenIssued = { token, targetEmail ->
                                    val webLink = "https://ais-pre-uw6q3wpj3egqlczdzl4g5m-471535351601.asia-southeast1.run.app/#admin444?reset=$token"
                                    val appLink = "zelu://app/#admin444?reset=$token"
                                    val mailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$targetEmail")).apply {
                                        putExtra(Intent.EXTRA_SUBJECT, "ZELU Math AI Admin Password Reset Link")
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Click your ZELU Admin reset link (expires in 15 minutes):\n\nWeb: $webLink\nApp: $appLink\n\nReset Code: $token"
                                        )
                                    }
                                    runCatching { context.startActivity(mailIntent) }
                                    runCatching { Firebase.auth.sendPasswordResetEmail(targetEmail) }
                                    isError = false
                                    feedbackMsg = "Reset link prepared for $targetEmail! Paste or open the link token below to set a new password."
                                },
                                onError = { err ->
                                    isError = true
                                    feedbackMsg = err
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Email, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Reset Link to Email")
                    }

                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it; feedbackMsg = null },
                        label = { Text("Reset Token (from #admin444?reset=... link)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; feedbackMsg = null },
                        label = { Text("New Admin Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; feedbackMsg = null },
                        label = { Text("Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = disable2FaOnReset, onCheckedChange = { disable2FaOnReset = it })
                        Text("Also turn off 2FA during recovery", style = MaterialTheme.typography.bodySmall)
                    }

                    if (feedbackMsg != null) {
                        Surface(
                            color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(feedbackMsg.orEmpty(), modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                onClearResetToken()
                                forgotMode = false
                                feedbackMsg = null
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Back to Login") }
                        Button(
                            onClick = {
                                if (newPassword != confirmPassword) {
                                    isError = true
                                    feedbackMsg = "Passwords do not match."
                                } else {
                                    adminViewModel.completePasswordResetWithToken(
                                        currentConfig = config,
                                        token = tokenInput,
                                        newPassword = newPassword,
                                        disableTwoFactor = disable2FaOnReset
                                    ) { ok, msg ->
                                        isError = !ok
                                        feedbackMsg = msg
                                        if (ok) {
                                            onClearResetToken()
                                            onUnlocked()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Reset & Unlock") }
                    }
                }
            }
        }
    }
}
