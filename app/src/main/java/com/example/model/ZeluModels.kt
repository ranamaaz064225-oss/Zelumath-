package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val preferredLanguage: String = "en",
    val country: String = "Pakistan",
    val board: String = "Punjab Board",
    val studentClass: String = "Class 10",
    val planTier: String = "FREE",
    val status: String = "active",
    val dailyQuestionsUsed: Long = 0L,
    val dailyScansUsed: Long = 0L,
    val totalSolved: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "displayName" to displayName.ifBlank { "Student" }.take(120),
            "email" to email.take(200),
            "photoUrl" to photoUrl.take(600),
            "preferredLanguage" to if (preferredLanguage == "ur") "ur" else "en",
            "country" to country.ifBlank { "Pakistan" }.take(80),
            "board" to board.ifBlank { "Punjab Board" }.take(80),
            "studentClass" to studentClass.ifBlank { "Class 10" }.take(80),
            "planTier" to "FREE",
            "status" to "active",
            "dailyQuestionsUsed" to dailyQuestionsUsed.coerceAtLeast(0L),
            "dailyScansUsed" to dailyScansUsed.coerceAtLeast(0L),
            "totalSolved" to totalSolved.coerceAtLeast(0L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@Keep
@IgnoreExtraProperties
data class SolvedQuestion(
    @DocumentId val id: String = "",
    val userId: String = "",
    val questionText: String = "",
    val detectedExpression: String = "",
    val inputMode: String = "text",
    val language: String = "en",
    val difficultyLevel: String = "detailed",
    val subject: String = "Mathematics",
    val verificationStatus: String = "",
    val identification: String = "",
    val givenInfo: String = "",
    val relevantFormula: String = "",
    val formulaExplanation: String = "",
    val steps: List<String> = emptyList(),
    val calculations: String = "",
    val finalAnswer: String = "",
    val verification: String = "",
    val whyItWorks: String = "",
    val practiceQuestion: String = "",
    val bookmarked: Boolean = false,
    val saved: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val safeMode = when (inputMode) {
            "camera", "voice", "whiteboard" -> inputMode
            else -> "text"
        }
        val safeLang = if (language == "ur") "ur" else "en"
        val safeDiff = if (difficultyLevel == "easy") "easy" else "detailed"
        return mapOf(
            "userId" to userId,
            "questionText" to questionText.ifBlank { "Math Problem" }.take(4000),
            "detectedExpression" to detectedExpression.take(1000),
            "inputMode" to safeMode,
            "language" to safeLang,
            "difficultyLevel" to safeDiff,
            "subject" to subject.ifBlank { "Mathematics" }.take(100),
            "verificationStatus" to verificationStatus.take(200),
            "identification" to identification.take(2000),
            "givenInfo" to givenInfo.take(2000),
            "relevantFormula" to relevantFormula.take(1000),
            "formulaExplanation" to formulaExplanation.take(2000),
            "steps" to steps.take(25),
            "calculations" to calculations.take(4000),
            "finalAnswer" to finalAnswer.ifBlank { "See steps" }.take(2000),
            "verification" to verification.take(2000),
            "whyItWorks" to whyItWorks.take(2000),
            "practiceQuestion" to practiceQuestion.take(2000),
            "bookmarked" to bookmarked,
            "saved" to saved,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@Keep
@IgnoreExtraProperties
data class ScanRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val detectedText: String = "",
    val clarityStatus: String = "clear",
    val subject: String = "Mathematics",
    val questionId: String = "",
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "detectedText" to detectedText.ifBlank { "Image scan" }.take(4000),
            "clarityStatus" to if (clarityStatus == "unclear") "unclear" else "clear",
            "subject" to subject.ifBlank { "Mathematics" }.take(100),
            "questionId" to questionId.take(128),
            "createdAt" to FieldValue.serverTimestamp()
        )
    }
}

@Keep
@IgnoreExtraProperties
data class PracticeAttempt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subject: String = "Mathematics",
    val topic: String = "",
    val board: String = "Punjab Board",
    val studentClass: String = "Class 10",
    val totalQuestions: Long = 5L,
    val correctCount: Long = 0L,
    val scorePercent: Long = 0L,
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val total = totalQuestions.coerceIn(1L, 100L)
        val correct = correctCount.coerceIn(0L, total)
        val pct = ((correct * 100L) / total).coerceIn(0L, 100L)
        return mapOf(
            "userId" to userId,
            "subject" to subject.ifBlank { "Mathematics" }.take(100),
            "topic" to topic.ifBlank { "General Practice" }.take(200),
            "board" to board.take(100),
            "studentClass" to studentClass.take(80),
            "totalQuestions" to total,
            "correctCount" to correct,
            "scorePercent" to pct,
            "createdAt" to FieldValue.serverTimestamp()
        )
    }
}

@Keep
@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val read: Boolean = false,
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val subject: String = "",
    val message: String = "",
    val category: String = "General",
    val status: String = "open",
    val adminReply: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "userEmail" to userEmail.take(200),
            "subject" to subject.trim().take(200),
            "message" to message.trim().take(4000),
            "category" to category.ifBlank { "General" }.take(80),
            "status" to "open",
            "adminReply" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@Keep
@IgnoreExtraProperties
data class CurriculumItem(
    @DocumentId val id: String = "",
    val country: String = "Pakistan",
    val board: String = "Punjab Board",
    val studentClass: String = "Class 10",
    val subject: String = "Algebra",
    val bookTitle: String = "",
    val chapterTitle: String = "",
    val exerciseTitle: String = "",
    val questionPrompt: String = "",
    val sourceNote: String = "ZELU Open Curriculum",
    val published: Boolean = true,
    val createdByUid: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(adminUid: String): Map<String, Any> {
        return mapOf(
            "country" to country.ifBlank { "Pakistan" }.take(80),
            "board" to board.ifBlank { "Punjab Board" }.take(100),
            "studentClass" to studentClass.ifBlank { "Class 10" }.take(80),
            "subject" to subject.ifBlank { "Mathematics" }.take(100),
            "bookTitle" to bookTitle.ifBlank { "Open Mathematics" }.take(200),
            "chapterTitle" to chapterTitle.ifBlank { "Chapter 1" }.take(200),
            "exerciseTitle" to exerciseTitle.ifBlank { "Practice Set" }.take(200),
            "questionPrompt" to questionPrompt.ifBlank { "Solve x + 2 = 5" }.take(3000),
            "sourceNote" to sourceNote.ifBlank { "ZELU Open Curriculum" }.take(400),
            "published" to published,
            "createdByUid" to adminUid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@Keep
@IgnoreExtraProperties
data class SubscriptionPlan(
    @DocumentId val id: String = "",
    val name: String = "Free",
    val tier: String = "FREE",
    val monthlyPriceUsd: Double = 0.0,
    val monthlyPricePkr: Long = 0L,
    val dailyQuestionLimit: Long = 10L,
    val dailyScanLimit: Long = 5L,
    val voiceTeacherEnabled: Boolean = true,
    val whiteboardAiEnabled: Boolean = true,
    val features: List<String> = emptyList(),
    val active: Boolean = true,
    val updatedAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> {
        val safeTier = when (tier) {
            "PREMIUM", "PRO" -> tier
            else -> "FREE"
        }
        return mapOf(
            "name" to name.ifBlank { safeTier }.take(80),
            "tier" to safeTier,
            "monthlyPriceUsd" to monthlyPriceUsd.coerceIn(0.0, 10000.0),
            "monthlyPricePkr" to monthlyPricePkr.coerceIn(0L, 5000000L),
            "dailyQuestionLimit" to dailyQuestionLimit.coerceIn(1L, 100000L),
            "dailyScanLimit" to dailyScanLimit.coerceIn(1L, 100000L),
            "voiceTeacherEnabled" to voiceTeacherEnabled,
            "whiteboardAiEnabled" to whiteboardAiEnabled,
            "features" to features.take(20),
            "active" to active,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@Keep
@IgnoreExtraProperties
data class PaymentMethod(
    @DocumentId val id: String = "",
    val name: String = "",
    val providerType: String = "Mobile Wallet",
    val accountTitle: String = "",
    val accountNumber: String = "",
    val instructions: String = "",
    val enabled: Boolean = true,
    val updatedAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> {
        return mapOf(
            "name" to name.trim().ifBlank { "Payment Method" }.take(100),
            "providerType" to providerType.trim().ifBlank { "Custom" }.take(80),
            "accountTitle" to accountTitle.trim().take(120),
            "accountNumber" to accountNumber.trim().ifBlank { "N/A" }.take(120),
            "instructions" to instructions.trim().take(1000),
            "enabled" to enabled,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@Keep
@IgnoreExtraProperties
data class SystemSettings(
    @DocumentId val id: String = "",
    val aiModel: String = "gemini-3.8-flash",
    val dailyFreeLimit: Long = 10L,
    val defaultVoiceLang: String = "en",
    val defaultSpeechRate: Double = 0.96,
    val defaultVoicePersona: String = "madam",
    val defaultVoicePitch: Double = 1.04,
    val announcementBannerEn: String = "",
    val announcementBannerUr: String = "",
    val adsEnabled: Boolean = false,
    val publicRead: Boolean = true,
    val siteTitle: String = "ZELU Math AI",
    val siteTagline: String = "Your Personal AI Math Teacher",
    val seoTitle: String = "ZELU Math AI — Global AI Mathematics Tutor",
    val seoDescription: String = "Step-by-step bilingual English & Urdu AI math solver, camera scanner, voice teacher, and whiteboard.",
    val seoKeywords: String = "ZELU Math AI, Urdu math solver, step by step math, board exam practice",
    val themeAccentHex: String = "#2563EB",
    val defaultThemeMode: String = "system",
    val headerNotice: String = "",
    val footerText: String = "Powered by ZELU • © 2026 ZELU Math AI",
    val adBannerText: String = "",
    val customHeaderScript: String = "",
    val customFooterScript: String = "",
    val socialFacebook: String = "",
    val socialYoutube: String = "",
    val socialInstagram: String = "",
    val socialWhatsapp: String = "",
    val socialEmail: String = "support@zelumath.ai",
    val logoBase64: String = "",
    val bannerBase64: String = "",
    val faviconBase64: String = "",
    val updatedAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> {
        val safePersona = if (defaultVoicePersona == "teacher") "teacher" else "madam"
        val safeThemeMode = when (defaultThemeMode) {
            "light", "dark" -> defaultThemeMode
            else -> "system"
        }
        return mapOf(
            "aiModel" to aiModel.ifBlank { "gemini-3.8-flash" }.take(100),
            "dailyFreeLimit" to dailyFreeLimit.coerceIn(1L, 100000L),
            "defaultVoiceLang" to if (defaultVoiceLang == "ur") "ur" else "en",
            "defaultSpeechRate" to defaultSpeechRate.coerceIn(0.5, 2.0),
            "defaultVoicePersona" to safePersona,
            "defaultVoicePitch" to defaultVoicePitch.coerceIn(0.6, 1.5),
            "announcementBannerEn" to announcementBannerEn.take(500),
            "announcementBannerUr" to announcementBannerUr.take(500),
            "adsEnabled" to adsEnabled,
            "publicRead" to true,
            "siteTitle" to siteTitle.ifBlank { "ZELU Math AI" }.take(120),
            "siteTagline" to siteTagline.take(200),
            "seoTitle" to seoTitle.take(200),
            "seoDescription" to seoDescription.take(500),
            "seoKeywords" to seoKeywords.take(400),
            "themeAccentHex" to themeAccentHex.ifBlank { "#2563EB" }.take(20),
            "defaultThemeMode" to safeThemeMode,
            "headerNotice" to headerNotice.take(300),
            "footerText" to footerText.take(400),
            "adBannerText" to adBannerText.take(1000),
            "customHeaderScript" to customHeaderScript.take(4000),
            "customFooterScript" to customFooterScript.take(4000),
            "socialFacebook" to socialFacebook.take(300),
            "socialYoutube" to socialYoutube.take(300),
            "socialInstagram" to socialInstagram.take(300),
            "socialWhatsapp" to socialWhatsapp.take(300),
            "socialEmail" to socialEmail.take(200),
            "logoBase64" to logoBase64.take(150000),
            "bannerBase64" to bannerBase64.take(250000),
            "faviconBase64" to faviconBase64.take(80000),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@Keep
@IgnoreExtraProperties
data class AdminSecurityConfig(
    @DocumentId val id: String = "",
    val passwordSalt: String = "",
    val passwordHash: String = "",
    val passwordConfigured: Boolean = false,
    val twoFactorEnabled: Boolean = false,
    val totpSecret: String = "",
    val recoveryEmail: String = "ranamaaz064225@gmail.com",
    val resetTokenHash: String = "",
    val resetExpiresAt: Long = 0L,
    val updatedAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> {
        return mapOf(
            "passwordSalt" to passwordSalt.take(100),
            "passwordHash" to passwordHash.take(200),
            "passwordConfigured" to passwordConfigured,
            "twoFactorEnabled" to twoFactorEnabled,
            "totpSecret" to totpSecret.take(128),
            "recoveryEmail" to recoveryEmail.take(200),
            "resetTokenHash" to resetTokenHash.take(200),
            "resetExpiresAt" to resetExpiresAt.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@Keep
@IgnoreExtraProperties
data class AdminAuditLog(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val action: String = "",
    val targetPath: String = "",
    val details: String = "",
    val createdAt: Timestamp? = null
)

data class PracticeMcq(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class TeacherAiResult(
    val isImageClear: Boolean = true,
    val clarityMessage: String = "",
    val subject: String = "Mathematics",
    val detectedExpression: String = "",
    val numericCheckExpression: String = "",
    val identification: String = "",
    val givenInfo: String = "",
    val relevantFormula: String = "",
    val formulaExplanation: String = "",
    val steps: List<String> = emptyList(),
    val calculations: String = "",
    val finalAnswer: String = "",
    val verification: String = "",
    val whyItWorks: String = "",
    val practiceQuestion: String = "",
    val uncertaintyNote: String = ""
)

object EducationCatalog {
    val countries = listOf("Pakistan", "International")

    val boardsByCountry = mapOf(
        "Pakistan" to listOf(
            "Punjab Board",
            "Federal Board",
            "Sindh Board",
            "KPK Board",
            "Balochistan Board",
            "Cambridge O Level",
            "Cambridge A Level"
        ),
        "International" to listOf(
            "Cambridge O Level",
            "Cambridge A Level",
            "IB / International",
            "University Standard"
        )
    )

    val classesList = listOf(
        "Class 1", "Class 2", "Class 3", "Class 4", "Class 5",
        "Class 6", "Class 7", "Class 8", "Class 9", "Class 10",
        "Class 11", "Class 12", "O Level", "A Level", "University Mathematics"
    )

    val subjectsList = listOf(
        "Mathematics",
        "Basic Arithmetic",
        "Algebra",
        "Geometry",
        "Trigonometry",
        "Calculus",
        "Statistics",
        "Probability",
        "Linear Algebra"
    )

    val defaultPlans = listOf(
        SubscriptionPlan(
            id = "plan_free",
            name = "Free",
            tier = "FREE",
            monthlyPriceUsd = 0.0,
            monthlyPricePkr = 0L,
            dailyQuestionLimit = 10L,
            dailyScanLimit = 5L,
            voiceTeacherEnabled = true,
            whiteboardAiEnabled = true,
            features = listOf(
                "10 daily AI math questions",
                "5 daily camera/photo scans",
                "10-step Teacher Mode (Easy & Detailed)",
                "English & Urdu bilingual UI"
            ),
            active = true
        ),
        SubscriptionPlan(
            id = "plan_premium",
            name = "Premium",
            tier = "PREMIUM",
            monthlyPriceUsd = 6.99,
            monthlyPricePkr = 1950L,
            dailyQuestionLimit = 100L,
            dailyScanLimit = 50L,
            voiceTeacherEnabled = true,
            whiteboardAiEnabled = true,
            features = listOf(
                "100 daily AI math questions",
                "50 daily camera & handwritten scans",
                "Unlimited Voice Teacher & Whiteboard AI",
                "Saved solutions & board practice tests"
            ),
            active = true
        ),
        SubscriptionPlan(
            id = "plan_pro",
            name = "Pro",
            tier = "PRO",
            monthlyPriceUsd = 14.99,
            monthlyPricePkr = 4200L,
            dailyQuestionLimit = 500L,
            dailyScanLimit = 250L,
            voiceTeacherEnabled = true,
            whiteboardAiEnabled = true,
            features = listOf(
                "500 daily AI questions & 250 scans",
                "Priority advanced math reasoning",
                "Full O/A Level & University support",
                "Unlimited practice test generation"
            ),
            active = true
        )
    )

    fun starterCurriculum(adminUid: String): List<Pair<String, CurriculumItem>> = listOf(
        "cur_punjab_10_quad" to CurriculumItem(
            id = "cur_punjab_10_quad",
            country = "Pakistan",
            board = "Punjab Board",
            studentClass = "Class 10",
            subject = "Algebra",
            bookTitle = "ZELU Open Algebra — Secondary",
            chapterTitle = "Quadratic Equations",
            exerciseTitle = "Practice Set 1",
            questionPrompt = "Solve the quadratic equation by factorization: x^2 - 7x + 12 = 0",
            sourceNote = "ZELU Open Curriculum (Public Domain Algebra Problem)",
            published = true,
            createdByUid = adminUid
        ),
        "cur_federal_9_linear" to CurriculumItem(
            id = "cur_federal_9_linear",
            country = "Pakistan",
            board = "Federal Board",
            studentClass = "Class 9",
            subject = "Algebra",
            bookTitle = "ZELU Open Foundations — Matric",
            chapterTitle = "Linear Equations & Inequalities",
            exerciseTitle = "Practice Set 1",
            questionPrompt = "Solve for x and verify your answer: 3(2x - 4) = 18",
            sourceNote = "ZELU Open Curriculum (Public Domain Algebra Problem)",
            published = true,
            createdByUid = adminUid
        ),
        "cur_olevel_trig" to CurriculumItem(
            id = "cur_olevel_trig",
            country = "Pakistan",
            board = "Cambridge O Level",
            studentClass = "O Level",
            subject = "Trigonometry",
            bookTitle = "ZELU Open Trigonometry Guide",
            chapterTitle = "Right-Angled Triangles",
            exerciseTitle = "Trigonometric Ratios",
            questionPrompt = "In a right-angled triangle, the hypotenuse is 13 cm and one leg is 5 cm. Find the third side and sin(theta) opposite to the 5 cm side.",
            sourceNote = "ZELU Open Curriculum (Standard Geometry & Trigonometry)",
            published = true,
            createdByUid = adminUid
        ),
        "cur_alevel_calc" to CurriculumItem(
            id = "cur_alevel_calc",
            country = "Pakistan",
            board = "Cambridge A Level",
            studentClass = "A Level",
            subject = "Calculus",
            bookTitle = "ZELU Open Pure Mathematics",
            chapterTitle = "Differentiation & Tangent Lines",
            exerciseTitle = "Derivatives Practice",
            questionPrompt = "Find the derivative dy/dx of y = 3x^3 - 4x^2 + 5x - 7 and evaluate the gradient at x = 2.",
            sourceNote = "ZELU Open Curriculum (Standard Calculus)",
            published = true,
            createdByUid = adminUid
        ),
        "cur_sindh_prob" to CurriculumItem(
            id = "cur_sindh_prob",
            country = "Pakistan",
            board = "Sindh Board",
            studentClass = "Class 11",
            subject = "Probability",
            bookTitle = "ZELU Open Combinatorics & Probability",
            chapterTitle = "Counting Principles",
            exerciseTitle = "Probability Basics",
            questionPrompt = "Two fair six-sided dice are rolled together. What is the probability that the sum of the numbers is 8?",
            sourceNote = "ZELU Open Curriculum (Public Domain Probability)",
            published = true,
            createdByUid = adminUid
        ),
        "cur_uni_linalg" to CurriculumItem(
            id = "cur_uni_linalg",
            country = "International",
            board = "University Standard",
            studentClass = "University Mathematics",
            subject = "Linear Algebra",
            bookTitle = "ZELU Open Linear Algebra",
            chapterTitle = "2x2 Matrices & Determinants",
            exerciseTitle = "Determinants & Inverses",
            questionPrompt = "Find the determinant and inverse of the 2x2 matrix A = [[4, 7], [2, 6]].",
            sourceNote = "ZELU Open Curriculum (Standard Linear Algebra)",
            published = true,
            createdByUid = adminUid
        )
    )
}
