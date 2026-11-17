package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.MathAiRepository
import com.example.data.ZeluFirestoreRepository
import com.example.localization.AppLanguage
import com.example.model.CurriculumItem
import com.example.model.EducationCatalog
import com.example.model.PaymentMethod
import com.example.model.PracticeAttempt
import com.example.model.PracticeMcq
import com.example.model.ScanRecord
import com.example.model.SolvedQuestion
import com.example.model.SubscriptionPlan
import com.example.model.SupportTicket
import com.example.model.SystemSettings
import com.example.model.UserNotification
import com.example.model.UserProfile
import com.google.firebase.auth.FirebaseUser
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

private const val TAG = "ZeluMainVM"

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface SolverUiState {
    data object Idle : SolverUiState
    data object Solving : SolverUiState
    data class Solved(val question: SolvedQuestion, val uncertaintyNote: String = "") : SolverUiState
    data class UnclearImage(val message: String) : SolverUiState
    data class Error(val message: String) : SolverUiState
}

sealed interface FollowUpUiState {
    data object Idle : FollowUpUiState
    data object Streaming : FollowUpUiState
    data class Ready(val answer: String, val stepIndex: Int?) : FollowUpUiState
    data class Error(val message: String) : FollowUpUiState
}

sealed interface PracticeUiState {
    data object Idle : PracticeUiState
    data object Generating : PracticeUiState
    data class Active(
        val subject: String,
        val board: String,
        val studentClass: String,
        val questions: List<PracticeMcq>,
        val selectedIndices: Map<Int, Int> = emptyMap(),
        val submittedScore: Int? = null
    ) : PracticeUiState
    data class Error(val message: String) : PracticeUiState
}

class ZeluMainViewModel(
    private val repository: ZeluFirestoreRepository,
    private val aiRepository: MathAiRepository,
    private val currentUserId: String
) : ViewModel() {

    val userProfile: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .asUiState("Failed to load user profile")

    val solvedQuestions: StateFlow<UiState<List<SolvedQuestion>>> = repository.observeQuestions(currentUserId)
        .asUiState("Failed to load solved questions")

    val scanHistory: StateFlow<UiState<List<ScanRecord>>> = repository.observeScanHistory(currentUserId)
        .asUiState("Failed to load scan history")

    val practiceAttempts: StateFlow<UiState<List<PracticeAttempt>>> = repository.observePracticeAttempts(currentUserId)
        .asUiState("Failed to load practice attempts")

    val notifications: StateFlow<UiState<List<UserNotification>>> = repository.observeNotifications(currentUserId)
        .asUiState("Failed to load notifications")

    val userTickets: StateFlow<UiState<List<SupportTicket>>> = repository.observeUserTickets(currentUserId)
        .asUiState("Failed to load support tickets")

    val publishedCurriculum: StateFlow<UiState<List<CurriculumItem>>> = repository.observePublishedCurriculum()
        .asUiState("Failed to load curriculum")

    val activePlans: StateFlow<UiState<List<SubscriptionPlan>>> = repository.observeActivePlans()
        .asUiState("Failed to load plans")

    val paymentMethods: StateFlow<UiState<List<PaymentMethod>>> = repository.observeEnabledPaymentMethods()
        .asUiState("Failed to load payment methods")

    val systemSettings: StateFlow<UiState<SystemSettings?>> = repository.observeSystemSettings()
        .asUiState("Failed to load settings")

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _solverState = MutableStateFlow<SolverUiState>(SolverUiState.Idle)
    val solverState: StateFlow<SolverUiState> = _solverState.asStateFlow()

    private val _followUpState = MutableStateFlow<FollowUpUiState>(FollowUpUiState.Idle)
    val followUpState: StateFlow<FollowUpUiState> = _followUpState.asStateFlow()

    private val _practiceState = MutableStateFlow<PracticeUiState>(PracticeUiState.Idle)
    val practiceState: StateFlow<PracticeUiState> = _practiceState.asStateFlow()

    private val _activeVoiceQuestion = MutableStateFlow<SolvedQuestion?>(null)
    val activeVoiceQuestion: StateFlow<SolvedQuestion?> = _activeVoiceQuestion.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun syncUserSession(user: FirebaseUser, preferredLanguage: AppLanguage) {
        viewModelScope.launch {
            repository.ensureUserProfile(user, preferredLanguage.code)
            _isAdmin.value = repository.checkIsAdmin(user)
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun setVoiceQuestion(question: SolvedQuestion) {
        _activeVoiceQuestion.value = question
    }

    fun solveTextQuestion(
        questionText: String,
        language: AppLanguage,
        difficultyLevel: String,
        inputMode: String = "text"
    ) {
        val trimmed = questionText.trim()
        if (trimmed.isEmpty()) {
            _solverState.value = SolverUiState.Error(
                if (language == AppLanguage.URDU) "براہ کرم ریاضی کا سوال درج کریں۔"
                else "Please enter a mathematics question."
            )
            return
        }
        val limitMsg = checkLimitOrSuspension(isScan = false, language = language)
        if (limitMsg != null) {
            _solverState.value = SolverUiState.Error(limitMsg)
            return
        }

        val activeModel = (systemSettings.value as? UiState.Success)?.data?.aiModel ?: "gemini-3.8-flash"

        viewModelScope.launch {
            _solverState.value = SolverUiState.Solving
            _followUpState.value = FollowUpUiState.Idle
            val result = aiRepository.solveTextQuestion(
                question = trimmed,
                language = language,
                difficultyLevel = difficultyLevel,
                modelName = activeModel
            )
            result.fold(
                onSuccess = { (ai, verificationBadge) ->
                    val question = SolvedQuestion(
                        userId = currentUserId,
                        questionText = trimmed,
                        detectedExpression = ai.detectedExpression,
                        inputMode = inputMode,
                        language = language.code,
                        difficultyLevel = difficultyLevel,
                        subject = ai.subject,
                        verificationStatus = verificationBadge,
                        identification = ai.identification,
                        givenInfo = ai.givenInfo,
                        relevantFormula = ai.relevantFormula,
                        formulaExplanation = ai.formulaExplanation,
                        steps = ai.steps,
                        calculations = ai.calculations,
                        finalAnswer = ai.finalAnswer,
                        verification = ai.verification,
                        whyItWorks = ai.whyItWorks,
                        practiceQuestion = ai.practiceQuestion,
                        bookmarked = false,
                        saved = true
                    )
                    val savedId = repository.saveSolvedQuestion(currentUserId, question, incrementScan = false)
                        .getOrNull().orEmpty()
                    val finalModel = question.copy(id = savedId)
                    _activeVoiceQuestion.value = finalModel
                    _solverState.value = SolverUiState.Solved(finalModel, ai.uncertaintyNote)
                },
                onFailure = { error ->
                    _solverState.value = SolverUiState.Error(
                        error.localizedMessage ?: "Failed to solve question."
                    )
                }
            )
        }
    }

    fun solveImageQuestion(
        bitmap: Bitmap,
        extraHint: String,
        language: AppLanguage,
        difficultyLevel: String,
        inputMode: String = "camera"
    ) {
        val limitMsg = checkLimitOrSuspension(isScan = true, language = language)
        if (limitMsg != null) {
            _solverState.value = SolverUiState.Error(limitMsg)
            return
        }
        val activeModel = (systemSettings.value as? UiState.Success)?.data?.aiModel ?: "gemini-3.8-flash"

        viewModelScope.launch {
            _solverState.value = SolverUiState.Solving
            _followUpState.value = FollowUpUiState.Idle
            val result = aiRepository.solveImageQuestion(
                bitmap = bitmap,
                extraHint = extraHint,
                language = language,
                difficultyLevel = difficultyLevel,
                modelName = activeModel
            )
            result.fold(
                onSuccess = { (ai, verificationBadge) ->
                    if (!ai.isImageClear) {
                        val msg = ai.clarityMessage.ifBlank {
                            if (language == AppLanguage.URDU) {
                                "تصویر واضح نہیں ہے۔ براہ کرم بہتر روشنی میں دوبارہ تصویر لیں۔"
                            } else {
                                "The math problem in this image is unclear. Please retake the photo in good lighting."
                            }
                        }
                        repository.saveScanRecord(
                            userId = currentUserId,
                            detectedText = msg,
                            clarityStatus = "unclear",
                            subject = ai.subject
                        )
                        _solverState.value = SolverUiState.UnclearImage(msg)
                    } else {
                        val qText = ai.identification.ifBlank { ai.detectedExpression.ifBlank { "Image math scan" } }
                        val question = SolvedQuestion(
                            userId = currentUserId,
                            questionText = qText,
                            detectedExpression = ai.detectedExpression,
                            inputMode = inputMode,
                            language = language.code,
                            difficultyLevel = difficultyLevel,
                            subject = ai.subject,
                            verificationStatus = verificationBadge,
                            identification = ai.identification,
                            givenInfo = ai.givenInfo,
                            relevantFormula = ai.relevantFormula,
                            formulaExplanation = ai.formulaExplanation,
                            steps = ai.steps,
                            calculations = ai.calculations,
                            finalAnswer = ai.finalAnswer,
                            verification = ai.verification,
                            whyItWorks = ai.whyItWorks,
                            practiceQuestion = ai.practiceQuestion,
                            bookmarked = false,
                            saved = true
                        )
                        val savedId = repository.saveSolvedQuestion(
                            currentUserId,
                            question,
                            incrementScan = inputMode == "camera"
                        ).getOrNull().orEmpty()
                        if (inputMode == "camera") {
                            repository.saveScanRecord(
                                userId = currentUserId,
                                detectedText = ai.detectedExpression.ifBlank { qText },
                                clarityStatus = "clear",
                                subject = ai.subject,
                                questionId = savedId
                            )
                        }
                        val finalModel = question.copy(id = savedId)
                        _activeVoiceQuestion.value = finalModel
                        _solverState.value = SolverUiState.Solved(finalModel, ai.uncertaintyNote)
                    }
                },
                onFailure = { error ->
                    _solverState.value = SolverUiState.Error(
                        error.localizedMessage ?: "Failed to analyze image."
                    )
                }
            )
        }
    }

    fun askFollowUp(
        question: SolvedQuestion,
        stepIndex: Int?,
        followUpPrompt: String,
        language: AppLanguage
    ) {
        if (followUpPrompt.isBlank()) return
        viewModelScope.launch {
            _followUpState.value = FollowUpUiState.Streaming
            try {
                aiRepository.askFollowUpStream(question, stepIndex, followUpPrompt, language)
                    .collect { partial ->
                        _followUpState.value = FollowUpUiState.Ready(partial, stepIndex)
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Follow-up error", e)
                _followUpState.value = FollowUpUiState.Error(
                    e.localizedMessage ?: "Failed to answer follow-up question."
                )
            }
        }
    }

    fun toggleBookmark(question: SolvedQuestion) {
        if (question.id.isBlank()) return
        viewModelScope.launch {
            val res = repository.toggleQuestionBookmark(currentUserId, question.id, !question.bookmarked)
            if (res.isSuccess) {
                val updated = question.copy(bookmarked = !question.bookmarked)
                val current = _solverState.value
                if (current is SolverUiState.Solved && current.question.id == question.id) {
                    _solverState.value = current.copy(question = updated)
                }
                if (_activeVoiceQuestion.value?.id == question.id) {
                    _activeVoiceQuestion.value = updated
                }
            } else {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Bookmark update failed"
            }
        }
    }

    fun deleteQuestion(questionId: String) {
        viewModelScope.launch {
            val res = repository.deleteSolvedQuestion(currentUserId, questionId)
            if (res.isFailure) {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Delete failed"
            }
        }
    }

    fun deleteScan(scanId: String) {
        viewModelScope.launch {
            val res = repository.deleteScanRecord(currentUserId, scanId)
            if (res.isFailure) {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Delete scan failed"
            }
        }
    }

    fun startPracticeTest(board: String, studentClass: String, subject: String, language: AppLanguage) {
        viewModelScope.launch {
            _practiceState.value = PracticeUiState.Generating
            val res = aiRepository.generatePracticeTest(board, studentClass, subject, language)
            res.fold(
                onSuccess = { mcqs ->
                    _practiceState.value = PracticeUiState.Active(
                        subject = subject,
                        board = board,
                        studentClass = studentClass,
                        questions = mcqs
                    )
                },
                onFailure = { error ->
                    _practiceState.value = PracticeUiState.Error(
                        error.localizedMessage ?: "Failed to generate practice test."
                    )
                }
            )
        }
    }

    fun selectPracticeOption(questionIndex: Int, optionIndex: Int) {
        val current = _practiceState.value as? PracticeUiState.Active ?: return
        if (current.submittedScore != null) return
        _practiceState.value = current.copy(
            selectedIndices = current.selectedIndices + (questionIndex to optionIndex)
        )
    }

    fun submitPracticeTest() {
        val current = _practiceState.value as? PracticeUiState.Active ?: return
        if (current.questions.isEmpty()) return
        val correct = current.questions.indices.count { idx ->
            current.selectedIndices[idx] == current.questions[idx].correctIndex
        }
        val pct = (correct * 100) / current.questions.size
        _practiceState.value = current.copy(submittedScore = pct)

        viewModelScope.launch {
            repository.savePracticeAttempt(
                currentUserId,
                PracticeAttempt(
                    userId = currentUserId,
                    subject = current.subject,
                    topic = "${current.board} • ${current.studentClass}",
                    board = current.board,
                    studentClass = current.studentClass,
                    totalQuestions = current.questions.size.toLong(),
                    correctCount = correct.toLong(),
                    scorePercent = pct.toLong()
                )
            )
        }
    }

    fun resetPracticeTest() {
        _practiceState.value = PracticeUiState.Idle
    }

    fun updateProfile(
        displayName: String,
        preferredLanguage: AppLanguage,
        country: String,
        board: String,
        studentClass: String
    ) {
        viewModelScope.launch {
            val res = repository.updateUserProfilePreferences(
                userId = currentUserId,
                displayName = displayName,
                preferredLanguage = preferredLanguage.code,
                country = country,
                board = board,
                studentClass = studentClass
            )
            _actionMessage.value = if (res.isSuccess) {
                if (preferredLanguage == AppLanguage.URDU) "پروفائل محفوظ ہو گئی۔" else "Profile updated."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to update profile."
            }
        }
    }

    fun submitSupportTicket(email: String, subject: String, message: String, category: String) {
        viewModelScope.launch {
            val res = repository.submitSupportTicket(currentUserId, email, subject, message, category)
            _actionMessage.value = if (res.isSuccess) {
                "Support ticket submitted."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to submit ticket."
            }
        }
    }

    private fun checkLimitOrSuspension(isScan: Boolean, language: AppLanguage): String? {
        val profile = (userProfile.value as? UiState.Success)?.data ?: return null
        if (profile.status == "suspended") {
            return if (language == AppLanguage.URDU) {
                "آپ کا اکاؤنٹ عارضی طور پر معطل ہے۔ سپورٹ سے رابطہ کریں۔"
            } else {
                "Your account is currently suspended. Please contact support."
            }
        }
        val plans = (activePlans.value as? UiState.Success)?.data.orEmpty()
            .ifEmpty { EducationCatalog.defaultPlans }
        val userPlan = plans.firstOrNull { it.tier == profile.planTier } ?: EducationCatalog.defaultPlans.first()
        if (!isScan && profile.dailyQuestionsUsed >= userPlan.dailyQuestionLimit) {
            return if (language == AppLanguage.URDU) {
                "آپ کی یومیہ سوالات کی حد (${userPlan.dailyQuestionLimit}) مکمل ہو گئی ہے۔"
            } else {
                "Daily question limit (${userPlan.dailyQuestionLimit}) reached for your ${userPlan.name} plan."
            }
        }
        if (isScan && profile.dailyScansUsed >= userPlan.dailyScanLimit) {
            return if (language == AppLanguage.URDU) {
                "آپ کی یومیہ کیمرہ اسکین حد (${userPlan.dailyScanLimit}) مکمل ہو گئی ہے۔"
            } else {
                "Daily camera scan limit (${userPlan.dailyScanLimit}) reached for your ${userPlan.name} plan."
            }
        }
        return null
    }

    private fun <T> Flow<T>.asUiState(fallbackError: String): StateFlow<UiState<T>> {
        return this
            .map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, fallbackError, rootError)
                emit(UiState.Error(rootError.message ?: fallbackError))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )
    }
}
