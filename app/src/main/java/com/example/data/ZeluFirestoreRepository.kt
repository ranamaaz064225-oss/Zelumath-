package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.firebase.isBootstrappedAdminEmail
import com.example.model.AdminAuditLog
import com.example.model.AdminSecurityConfig
import com.example.model.CurriculumItem
import com.example.model.EducationCatalog
import com.example.model.PaymentMethod
import com.example.model.PracticeAttempt
import com.example.model.ScanRecord
import com.example.model.SolvedQuestion
import com.example.model.SubscriptionPlan
import com.example.model.SupportTicket
import com.example.model.SystemSettings
import com.example.model.UserNotification
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ZeluFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun ensureUserProfile(user: FirebaseUser, preferredLangCode: String): Result<Unit> {
        val path = "users/${user.uid}"
        return try {
            val uid = requireUserId()
            check(uid == user.uid) { "User ID mismatch" }
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val profile = UserProfile(
                    userId = uid,
                    displayName = user.displayName?.takeIf { it.isNotBlank() } ?: "ZELU Student",
                    email = user.email.orEmpty(),
                    photoUrl = user.photoUrl?.toString().orEmpty(),
                    preferredLanguage = preferredLangCode,
                    country = "Pakistan",
                    board = "Punjab Board",
                    studentClass = "Class 10",
                    planTier = "FREE",
                    status = "active"
                )
                docRef.set(profile.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun checkIsAdmin(user: FirebaseUser?): Boolean {
        if (user == null) return false
        if (isBootstrappedAdminEmail(user)) return true
        return try {
            db.collection("admins").document(user.uid).get().await().exists()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    fun observeQuestions(userId: String): Flow<List<SolvedQuestion>> = flow {
        val path = "users/$userId/questions"
        emitAll(
            db.collection("users").document(userId).collection("questions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SolvedQuestion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeScanHistory(userId: String): Flow<List<ScanRecord>> = flow {
        val path = "users/$userId/scanHistory"
        emitAll(
            db.collection("users").document(userId).collection("scanHistory")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ScanRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observePracticeAttempts(userId: String): Flow<List<PracticeAttempt>> = flow {
        val path = "users/$userId/practiceAttempts"
        emitAll(
            db.collection("users").document(userId).collection("practiceAttempts")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PracticeAttempt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeNotifications(userId: String): Flow<List<UserNotification>> = flow {
        val path = "users/$userId/notifications"
        emitAll(
            db.collection("users").document(userId).collection("notifications")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeUserTickets(userId: String): Flow<List<SupportTicket>> = flow {
        val path = "supportTickets"
        emitAll(
            db.collection("supportTickets")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observePublishedCurriculum(): Flow<List<CurriculumItem>> = flow {
        val uid = requireUserId()
        val path = "curriculum"
        emitAll(
            db.collection("curriculum")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CurriculumItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { "${it.board}-${it.studentClass}-${it.subject}" }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeActivePlans(): Flow<List<SubscriptionPlan>> = flow {
        val uid = requireUserId()
        val path = "plans"
        emitAll(
            db.collection("plans")
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SubscriptionPlan::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.monthlyPriceUsd }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeEnabledPaymentMethods(): Flow<List<PaymentMethod>> = flow {
        val uid = requireUserId()
        val path = "paymentMethods"
        emitAll(
            db.collection("paymentMethods")
                .whereEqualTo("enabled", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PaymentMethod::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeSystemSettings(): Flow<SystemSettings?> = flow {
        val uid = requireUserId()
        val path = "systemSettings/global"
        emitAll(
            db.collection("systemSettings")
                .whereEqualTo("publicRead", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SystemSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .firstOrNull()
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun saveSolvedQuestion(
        userId: String,
        question: SolvedQuestion,
        incrementScan: Boolean = false
    ): Result<String> {
        var docPath = "users/$userId/questions"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val docRef = db.collection("users").document(uid).collection("questions").document()
            docPath = docRef.path
            docRef.set(question.copy(userId = uid).toCreateMap()).await()

            val userUpdates = mutableMapOf<String, Any>(
                "dailyQuestionsUsed" to FieldValue.increment(1L),
                "totalSolved" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (incrementScan) {
                userUpdates["dailyScansUsed"] = FieldValue.increment(1L)
            }
            db.collection("users").document(uid).update(userUpdates).await()

            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveScanRecord(
        userId: String,
        detectedText: String,
        clarityStatus: String,
        subject: String,
        questionId: String = ""
    ): Result<Unit> {
        var docPath = "users/$userId/scanHistory"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val docRef = db.collection("users").document(uid).collection("scanHistory").document()
            docPath = docRef.path
            val record = ScanRecord(
                userId = uid,
                detectedText = detectedText,
                clarityStatus = clarityStatus,
                subject = subject,
                questionId = questionId
            )
            docRef.set(record.toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleQuestionBookmark(userId: String, questionId: String, bookmarked: Boolean): Result<Unit> {
        val docPath = "users/$userId/questions/$questionId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.document(docPath).update(
                mapOf(
                    "bookmarked" to bookmarked,
                    "saved" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSolvedQuestion(userId: String, questionId: String): Result<Unit> {
        val docPath = "users/$userId/questions/$questionId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.document(docPath).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteScanRecord(userId: String, scanId: String): Result<Unit> {
        val docPath = "users/$userId/scanHistory/$scanId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.document(docPath).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun savePracticeAttempt(userId: String, attempt: PracticeAttempt): Result<Unit> {
        var docPath = "users/$userId/practiceAttempts"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("practiceAttempts").document()
            docPath = docRef.path
            docRef.set(attempt.copy(userId = uid).toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfilePreferences(
        userId: String,
        displayName: String,
        preferredLanguage: String,
        country: String,
        board: String,
        studentClass: String
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(displayName.trim().isNotEmpty()) { "Display name cannot be empty" }
            db.document(docPath).update(
                mapOf(
                    "displayName" to displayName.trim().take(120),
                    "preferredLanguage" to if (preferredLanguage == "ur") "ur" else "en",
                    "country" to country.ifBlank { "Pakistan" }.take(80),
                    "board" to board.ifBlank { "Punjab Board" }.take(80),
                    "studentClass" to studentClass.ifBlank { "Class 10" }.take(80),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun submitSupportTicket(
        userId: String,
        userEmail: String,
        subject: String,
        message: String,
        category: String = "General"
    ): Result<Unit> {
        var docPath = "supportTickets"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(subject.trim().length >= 2) { "Subject must be at least 2 characters" }
            require(message.trim().length >= 5) { "Message must be at least 5 characters" }
            val docRef = db.collection("supportTickets").document()
            docPath = docRef.path
            val ticket = SupportTicket(
                userId = uid,
                userEmail = userEmail,
                subject = subject,
                message = message,
                category = category
            )
            docRef.set(ticket.toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    // --- ADMIN OPERATIONS ---

    fun observeAllUsersAdmin(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        val path = "users"
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeAllCurriculumAdmin(): Flow<List<CurriculumItem>> = flow {
        val uid = requireUserId()
        val path = "curriculum"
        emitAll(
            db.collection("curriculum")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CurriculumItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeAllPlansAdmin(): Flow<List<SubscriptionPlan>> = flow {
        val uid = requireUserId()
        val path = "plans"
        emitAll(
            db.collection("plans")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SubscriptionPlan::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.monthlyPriceUsd }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeAllTicketsAdmin(): Flow<List<SupportTicket>> = flow {
        val uid = requireUserId()
        val path = "supportTickets"
        emitAll(
            db.collection("supportTickets")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeAdminLogs(): Flow<List<AdminAuditLog>> = flow {
        val uid = requireUserId()
        val path = "adminLogs"
        emitAll(
            db.collection("adminLogs")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AdminAuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun adminUpdateUserStatusAndPlan(
        targetUserId: String,
        newStatus: String,
        newPlanTier: String
    ): Result<Unit> {
        val docPath = "users/$targetUserId"
        return try {
            requireUserId()
            db.document(docPath).update(
                mapOf(
                    "status" to if (newStatus == "suspended") "suspended" else "active",
                    "planTier" to when (newPlanTier) {
                        "PREMIUM", "PRO" -> newPlanTier
                        else -> "FREE"
                    },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAdminLog("UPDATE_USER", docPath, "Set status=$newStatus, planTier=$newPlanTier")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun adminAddCurriculumItem(item: CurriculumItem): Result<Unit> {
        var docPath = "curriculum"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("curriculum").document()
            docPath = docRef.path
            docRef.set(item.toCreateMap(uid)).await()
            writeAdminLog("ADD_CURRICULUM", docPath, "${item.board} / ${item.studentClass} / ${item.subject}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun adminToggleCurriculumPublish(itemId: String, published: Boolean): Result<Unit> {
        val docPath = "curriculum/$itemId"
        return try {
            requireUserId()
            db.document(docPath).update(
                mapOf("published" to published, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            writeAdminLog("TOGGLE_CURRICULUM", docPath, "published=$published")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun adminDeleteCurriculumItem(itemId: String): Result<Unit> {
        val docPath = "curriculum/$itemId"
        return try {
            requireUserId()
            db.document(docPath).delete().await()
            writeAdminLog("DELETE_CURRICULUM", docPath, "Deleted curriculum item $itemId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun adminSavePlan(plan: SubscriptionPlan): Result<Unit> {
        val docId = plan.id.ifBlank { "plan_${plan.tier.lowercase()}" }
        val docPath = "plans/$docId"
        return try {
            requireUserId()
            db.document(docPath).set(plan.toWriteMap()).await()
            writeAdminLog("SAVE_PLAN", docPath, "Updated ${plan.tier} limit=${plan.dailyQuestionLimit}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun adminSaveSystemSettings(settings: SystemSettings): Result<Unit> {
        val docPath = "systemSettings/global"
        return try {
            requireUserId()
            db.document(docPath).set(settings.toWriteMap()).await()
            writeAdminLog("SAVE_SETTINGS", docPath, "model=${settings.aiModel}, ads=${settings.adsEnabled}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateSupportTicket(ticketId: String, status: String, reply: String): Result<Unit> {
        val docPath = "supportTickets/$ticketId"
        return try {
            requireUserId()
            val safeStatus = when (status) {
                "in_progress", "resolved" -> status
                else -> "open"
            }
            db.document(docPath).update(
                mapOf(
                    "status" to safeStatus,
                    "adminReply" to reply.trim().take(2000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAdminLog("UPDATE_TICKET", docPath, "status=$safeStatus")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeAllPaymentMethodsAdmin(): Flow<List<PaymentMethod>> = flow {
        val uid = requireUserId()
        val path = "paymentMethods"
        emitAll(
            db.collection("paymentMethods")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PaymentMethod::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeAdminSecurityConfig(): Flow<AdminSecurityConfig?> = flow {
        val uid = requireUserId()
        val path = "adminSecurity/config"
        emitAll(
            db.collection("adminSecurity").document("config")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(AdminSecurityConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    suspend fun adminSavePaymentMethod(method: PaymentMethod): Result<Unit> {
        var docPath = "paymentMethods"
        return try {
            requireUserId()
            val docRef = if (method.id.isNotBlank()) {
                db.collection("paymentMethods").document(method.id)
            } else {
                db.collection("paymentMethods").document()
            }
            docPath = docRef.path
            docRef.set(method.toWriteMap()).await()
            writeAdminLog("SAVE_PAYMENT_METHOD", docPath, "${method.name} (${method.providerType}) enabled=${method.enabled}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun adminDeletePaymentMethod(methodId: String): Result<Unit> {
        val docPath = "paymentMethods/$methodId"
        return try {
            requireUserId()
            require(methodId.isNotBlank()) { "Payment method ID must not be blank" }
            db.document(docPath).delete().await()
            writeAdminLog("DELETE_PAYMENT_METHOD", docPath, "Deleted payment method $methodId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun adminSaveSecurityConfig(config: AdminSecurityConfig, actionLabel: String = "UPDATE_ADMIN_SECURITY"): Result<Unit> {
        val docPath = "adminSecurity/config"
        return try {
            requireUserId()
            db.document(docPath).set(config.toWriteMap()).await()
            writeAdminLog(actionLabel, docPath, "2FA=${config.twoFactorEnabled}, passwordConfigured=${config.passwordConfigured}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun adminSeedStarterCatalog(): Result<Unit> {
        return try {
            val uid = requireUserId()
            EducationCatalog.defaultPlans.forEach { plan ->
                db.collection("plans").document(plan.id).set(plan.toWriteMap()).await()
            }
            EducationCatalog.starterCurriculum(uid).forEach { (id, item) ->
                db.collection("curriculum").document(id).set(item.toCreateMap(uid)).await()
            }
            val starterPayments = listOf(
                "pm_jazzcash" to PaymentMethod(
                    id = "pm_jazzcash",
                    name = "JazzCash",
                    providerType = "Mobile Wallet",
                    accountTitle = "ZELU Math AI",
                    accountNumber = "0300-0000000",
                    instructions = "Update with your official JazzCash number in Admin -> Plans & Payments.",
                    enabled = true
                ),
                "pm_easypaisa" to PaymentMethod(
                    id = "pm_easypaisa",
                    name = "EasyPaisa",
                    providerType = "Mobile Wallet",
                    accountTitle = "ZELU Math AI",
                    accountNumber = "0345-0000000",
                    instructions = "Update with your official EasyPaisa number in Admin -> Plans & Payments.",
                    enabled = true
                )
            )
            starterPayments.forEach { (id, pm) ->
                db.collection("paymentMethods").document(id).set(pm.toWriteMap()).await()
            }
            db.collection("systemSettings").document("global")
                .set(SystemSettings(id = "global").toWriteMap()).await()
            writeAdminLog("SEED_CATALOG", "curriculum+plans+paymentMethods+systemSettings", "Initialized starter ZELU Math AI catalog")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "seed")
            Result.failure(e)
        }
    }

    private suspend fun writeAdminLog(action: String, targetPath: String, details: String) {
        val user = auth.currentUser ?: return
        val logMap = mapOf(
            "adminUid" to user.uid,
            "adminEmail" to (user.email ?: "admin"),
            "action" to action.take(120),
            "targetPath" to targetPath.take(300),
            "details" to details.take(1000),
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("adminLogs").document().set(logMap).await()
    }

    private fun handleFlowError(
        error: Throwable,
        op: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val ex = firestoreError ?: (error as? Exception)
        if (ex != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(ex, op, path)
        }
    }
}
