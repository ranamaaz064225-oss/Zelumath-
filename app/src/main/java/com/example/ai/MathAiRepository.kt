package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.localization.AppLanguage
import com.example.math.MathEvalResult
import com.example.math.MathEvaluator
import com.example.model.PracticeMcq
import com.example.model.SolvedQuestion
import com.example.model.TeacherAiResult
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "MathAiRepository"
private const val DEFAULT_MODEL = "gemini-3.8-flash"
private const val FAST_FALLBACK_MODEL = "gemini-3.5-flash-lite"

class MathAiRepository {

    private val requestOptions = RequestOptions(90_000L)

    private val teacherSchema = Schema.obj(
        properties = mapOf(
            "isImageClear" to Schema.boolean(description = "True if text/image is clear enough to read accurately; false if blurry, cut off, or unreadable."),
            "clarityMessage" to Schema.string(description = "If isImageClear is false, polite instruction asking student to retake the photo clearly."),
            "subject" to Schema.string(description = "Math branch e.g. Algebra, Calculus, Geometry, Trigonometry, Basic Arithmetic, Statistics, Probability, Linear Algebra."),
            "detectedExpression" to Schema.string(description = "Normalized math expression or equation extracted from the question."),
            "numericCheckExpression" to Schema.string(description = "Pure arithmetic expression or equation with variable(s) like '2*x + 4 = 10' or '(3+5)*2' suitable for deterministic numeric verification, or empty string if conceptual/word-only."),
            "identification" to Schema.string(description = "1. Question identification"),
            "givenInfo" to Schema.string(description = "2. Given information"),
            "relevantFormula" to Schema.string(description = "3. Relevant formula or theorem"),
            "formulaExplanation" to Schema.string(description = "4. Clear explanation of why/how the formula applies"),
            "steps" to Schema.array(
                items = Schema.string(),
                description = "5. Ordered step-by-step teacher explanations"
            ),
            "calculations" to Schema.string(description = "6. Clean mathematical calculations summary"),
            "finalAnswer" to Schema.string(description = "7. Final answer clearly stated"),
            "verification" to Schema.string(description = "8. Verification / substitution check of the result"),
            "whyItWorks" to Schema.string(description = "9. Simple conceptual explanation of why the method works"),
            "practiceQuestion" to Schema.string(description = "10. One similar practice question for the student to try"),
            "uncertaintyNote" to Schema.string(description = "Empty if confident, or explicit explanation of any ambiguity/uncertainty.")
        ),
        optionalProperties = listOf("clarityMessage", "numericCheckExpression", "uncertaintyNote")
    )

    private val practiceSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "question" to Schema.string(),
                        "options" to Schema.array(items = Schema.string()),
                        "correctIndex" to Schema.integer(),
                        "explanation" to Schema.string()
                    )
                ),
                description = "List of 5 multiple-choice math questions"
            )
        )
    )

    private fun buildTeacherModel(modelName: String, difficultyLevel: String): GenerativeModel {
        val resolvedName = modelName.ifBlank { DEFAULT_MODEL }
        val level = if (difficultyLevel == "easy") ThinkingLevel.MINIMAL else ThinkingLevel.LOW
        return Firebase.ai.generativeModel(
            modelName = resolvedName,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = teacherSchema
                thinkingConfig = thinkingConfig {
                    thinkingLevel = level
                }
            },
            requestOptions = requestOptions
        )
    }

    private val practiceModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = practiceSchema
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.LOW
                }
            },
            requestOptions = requestOptions
        )
    }

    private val practiceFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FAST_FALLBACK_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = practiceSchema
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.MINIMAL
                }
            },
            requestOptions = requestOptions
        )
    }

    private val followUpModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.LOW
                }
            },
            requestOptions = requestOptions
        )
    }

    suspend fun solveTextQuestion(
        question: String,
        language: AppLanguage,
        difficultyLevel: String,
        modelName: String = DEFAULT_MODEL
    ): Result<Pair<TeacherAiResult, String>> = withContext(Dispatchers.IO) {
        try {
            val langInstruction = if (language == AppLanguage.URDU) {
                "Respond ENTIRELY in natural, clear Urdu (اردو) using clean mathematical symbols. Do not mix English sentences."
            } else {
                "Respond ENTIRELY in clear, patient English."
            }
            val levelInstruction = if (difficultyLevel == "easy") {
                "Use Easy Explanation mode: simple everyday language, friendly tone, concise steps."
            } else {
                "Use Detailed Explanation mode: clear teacher-level justification for each step while keeping each field concise."
            }

            val prompt = """
                You are ZELU Math AI, a patient, accurate mathematics teacher.
                $langInstruction
                $levelInstruction
                Never fabricate calculations or guesses. If anything in the question is ambiguous, explain the uncertainty clearly in uncertaintyNote.
                Provide all 10 Teacher Mode fields accurately and concisely.
                For numericCheckExpression, provide a pure ASCII math expression or single '=' equation (using *, /, +, -, ^, sqrt, sin, cos, tan, pi) if applicable, otherwise "".
                Student question:
                $question
            """.trimIndent()

            val rawJson = streamTeacherJsonWithFallback(
                primaryModelName = modelName.ifBlank { DEFAULT_MODEL },
                difficultyLevel = difficultyLevel,
                promptContent = content { text(prompt) }
            )
            val parsed = parseTeacherJson(rawJson)
            val verificationBadge = computeDeterministicVerification(parsed, language)
            Result.success(parsed to verificationBadge)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Text solve timed out", e)
            Result.failure(Exception("AI request timed out. Please check your internet connection and try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Text solve failed", e)
            Result.failure(e)
        }
    }

    suspend fun solveImageQuestion(
        bitmap: Bitmap,
        extraHint: String,
        language: AppLanguage,
        difficultyLevel: String,
        modelName: String = DEFAULT_MODEL
    ): Result<Pair<TeacherAiResult, String>> = withContext(Dispatchers.IO) {
        try {
            val scaled = scaleBitmapIfNeeded(bitmap, maxDim = 1280)
            val langInstruction = if (language == AppLanguage.URDU) {
                "Respond ENTIRELY in natural, clear Urdu (اردو)."
            } else {
                "Respond ENTIRELY in clear, patient English."
            }
            val levelInstruction = if (difficultyLevel == "easy") {
                "Use Easy Explanation mode with concise steps."
            } else {
                "Use Detailed Explanation mode with clear concise justification."
            }

            val prompt = """
                You are ZELU Math AI, a patient mathematics teacher analyzing a student's math photo or whiteboard drawing.
                $langInstruction
                $levelInstruction
                CRITICAL: If the image is blurry, too dark, cropped off, or unreadable, set isImageClear = false and write a polite message in clarityMessage asking the student to retake the photo instead of inventing a question.
                Identify symbols, fractions, powers, roots, geometry diagrams, graphs, word problems, and handwritten equations carefully.
                ${if (extraHint.isNotBlank()) "Student note: $extraHint" else ""}
            """.trimIndent()

            val rawJson = streamTeacherJsonWithFallback(
                primaryModelName = modelName.ifBlank { DEFAULT_MODEL },
                difficultyLevel = difficultyLevel,
                promptContent = content {
                    image(scaled)
                    text(prompt)
                }
            )
            val parsed = parseTeacherJson(rawJson)
            val verificationBadge = if (parsed.isImageClear) {
                computeDeterministicVerification(parsed, language)
            } else ""
            Result.success(parsed to verificationBadge)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image solve timed out", e)
            Result.failure(Exception("Image analysis timed out. Please check your connection and try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image solve failed", e)
            Result.failure(e)
        }
    }

    suspend fun generatePracticeTest(
        board: String,
        studentClass: String,
        subject: String,
        language: AppLanguage
    ): Result<List<PracticeMcq>> = withContext(Dispatchers.IO) {
        try {
            val langRule = if (language == AppLanguage.URDU) {
                "Write all questions, options, and explanations in natural, easy-to-understand Pakistani Urdu (اردو)."
            } else {
                "Write all questions, options, and explanations in English."
            }
            val prompt = """
                Create 5 original, mathematically verified multiple-choice practice questions for:
                Board/Curriculum: $board
                Class/Level: $studentClass
                Subject: $subject
                $langRule
                Each question must have exactly 4 distinct options and correctIndex in 0..3.
                Do not copy copyrighted textbooks; write original educational practice problems.
            """.trimIndent()

            val rawJson = try {
                collectStreamText(practiceModel, content { text(prompt) }, timeoutMs = 45_000L)
            } catch (timeout: TimeoutCancellationException) {
                Log.w(TAG, "Primary model practice test timed out; retrying with $FAST_FALLBACK_MODEL", timeout)
                collectStreamText(practiceFallbackModel, content { text(prompt) }, timeoutMs = 35_000L)
            }

            val json = JSONObject(rawJson)
            val arr = json.getJSONArray("questions")
            val list = (0 until arr.length()).map { idx ->
                val obj = arr.getJSONObject(idx)
                val optsArr = obj.getJSONArray("options")
                val opts = (0 until optsArr.length()).map { o -> optsArr.getString(o) }
                PracticeMcq(
                    question = obj.getString("question"),
                    options = opts,
                    correctIndex = obj.optInt("correctIndex", 0).coerceIn(0, (opts.size - 1).coerceAtLeast(0)),
                    explanation = obj.optString("explanation", "")
                )
            }
            Result.success(list)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Practice test generation timed out", e)
            Result.failure(Exception("Practice test generation timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Practice test generation failed", e)
            Result.failure(e)
        }
    }

    fun askFollowUpStream(
        questionContext: SolvedQuestion,
        stepIndex: Int?,
        followUpQuestion: String,
        language: AppLanguage
    ): Flow<String> = flow {
        val langRule = if (language == AppLanguage.URDU) {
            "Answer clearly in Urdu (اردو)."
        } else {
            "Answer clearly in English."
        }
        val stepContext = if (stepIndex != null && stepIndex in questionContext.steps.indices) {
            "The student is specifically asking about Step ${stepIndex + 1}: ${questionContext.steps[stepIndex]}"
        } else {
            "General follow-up on the solution."
        }

        val chat = followUpModel.startChat(
            history = listOf(
                content("user") { text("Original problem: ${questionContext.questionText}") },
                content("model") {
                    text(
                        "Formula: ${questionContext.relevantFormula}\nSteps: ${questionContext.steps.joinToString("\n")}\nFinal Answer: ${questionContext.finalAnswer}"
                    )
                }
            )
        )
        val prompt = """
            $langRule
            $stepContext
            Student follow-up: $followUpQuestion
            Respond patiently like a math tutor in 2-5 short paragraphs.
        """.trimIndent()

        val acc = StringBuilder()
        withTimeout(60_000L) {
            chat.sendMessageStream(prompt).collect { chunk ->
                acc.append(chunk.text.orEmpty())
                emit(acc.toString())
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun streamTeacherJsonWithFallback(
        primaryModelName: String,
        difficultyLevel: String,
        promptContent: Content
    ): String {
        val primaryModel = buildTeacherModel(primaryModelName, difficultyLevel)
        return try {
            collectStreamText(primaryModel, promptContent, timeoutMs = 45_000L)
        } catch (timeout: TimeoutCancellationException) {
            if (primaryModelName == FAST_FALLBACK_MODEL) throw timeout
            Log.w(TAG, "$primaryModelName timed out after 45s; retrying with $FAST_FALLBACK_MODEL", timeout)
            val fallbackModel = buildTeacherModel(FAST_FALLBACK_MODEL, "easy")
            collectStreamText(fallbackModel, promptContent, timeoutMs = 40_000L)
        }
    }

    private suspend fun collectStreamText(
        model: GenerativeModel,
        promptContent: Content,
        timeoutMs: Long
    ): String = withTimeout(timeoutMs) {
        val acc = StringBuilder()
        model.generateContentStream(promptContent).collect { chunk ->
            chunk.text?.let { acc.append(it) }
        }
        val result = acc.toString().trim()
        if (result.isEmpty()) {
            throw IllegalStateException("AI returned an empty response.")
        }
        result
    }

    private fun parseTeacherJson(jsonString: String): TeacherAiResult {
        val json = JSONObject(jsonString)
        val stepsArr = json.optJSONArray("steps")
        val stepsList = if (stepsArr != null) {
            (0 until stepsArr.length()).map { stepsArr.getString(it) }
        } else emptyList()

        return TeacherAiResult(
            isImageClear = json.optBoolean("isImageClear", true),
            clarityMessage = json.optString("clarityMessage", ""),
            subject = json.optString("subject", "Mathematics"),
            detectedExpression = json.optString("detectedExpression", ""),
            numericCheckExpression = json.optString("numericCheckExpression", ""),
            identification = json.optString("identification", ""),
            givenInfo = json.optString("givenInfo", ""),
            relevantFormula = json.optString("relevantFormula", ""),
            formulaExplanation = json.optString("formulaExplanation", ""),
            steps = stepsList,
            calculations = json.optString("calculations", ""),
            finalAnswer = json.optString("finalAnswer", ""),
            verification = json.optString("verification", ""),
            whyItWorks = json.optString("whyItWorks", ""),
            practiceQuestion = json.optString("practiceQuestion", ""),
            uncertaintyNote = json.optString("uncertaintyNote", "")
        )
    }

    private fun computeDeterministicVerification(aiResult: TeacherAiResult, language: AppLanguage): String {
        val expr = aiResult.numericCheckExpression.ifBlank { aiResult.detectedExpression }
        if (expr.isBlank()) return ""
        val assignments = MathEvaluator.parseAssignments(aiResult.finalAnswer)
        return when (val check = MathEvaluator.verifyExpressionOrEquation(expr, assignments)) {
            is MathEvalResult.Numeric -> {
                val formatted = MathEvaluator.formatNumber(check.value)
                if (language == AppLanguage.URDU) "تصدیق شدہ حساب: $formatted"
                else "Deterministically Verified: = $formatted"
            }
            is MathEvalResult.EquationCheck -> {
                if (check.isBalanced) {
                    val lhs = MathEvaluator.formatNumber(check.lhsValue)
                    if (language == AppLanguage.URDU) "مساوات تصدیق شدہ (دونوں اطراف = $lhs)"
                    else "Deterministically Verified (LHS = RHS = $lhs)"
                } else ""
            }
            is MathEvalResult.Invalid -> ""
        }
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
        val newW = (w * ratio).toInt().coerceAtLeast(1)
        val newH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}
