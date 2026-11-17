package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import coil.compose.AsyncImage
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.localization.AppLanguage
import com.example.localization.ZeluStrings
import com.example.model.EducationCatalog
import com.example.model.SolvedQuestion
import com.example.model.SubscriptionPlan
import com.example.model.SystemSettings
import com.example.ui.components.MathSolutionCard
import com.example.ui.components.rememberDecodedBase64Bitmap
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepNavy900
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.FollowUpUiState
import com.example.viewmodel.SolverUiState
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeSolverScreen(
    strings: ZeluStrings,
    language: AppLanguage,
    solverState: SolverUiState,
    followUpState: FollowUpUiState,
    plans: List<SubscriptionPlan>,
    systemSettings: SystemSettings?,
    prefillQuestion: String,
    onPrefillConsumed: () -> Unit,
    onSolveText: (question: String, difficulty: String) -> Unit,
    onOpenCamera: () -> Unit,
    onImagePickedForSolver: (Uri) -> Unit,
    onToggleBookmark: (SolvedQuestion) -> Unit,
    onListenVoice: (SolvedQuestion) -> Unit,
    onAskFollowUp: (SolvedQuestion, Int?, String) -> Unit,
    onSelectSubjectOrClass: (subject: String?, studentClass: String?) -> Unit,
    onOpenPlansTab: () -> Unit,
    onOpenRoutePrompt: () -> Unit
) {
    val context = LocalContext.current
    var questionInput by rememberSaveable { mutableStateOf("") }
    if (prefillQuestion.isNotBlank()) {
        questionInput = prefillQuestion
        onPrefillConsumed()
    }
    var difficultyLevel by rememberSaveable { mutableStateOf("detailed") }
    var voiceFeedbackError by remember { mutableStateOf<String?>(null) }
    var expandedFaqIndex by rememberSaveable { mutableStateOf<Int?>(0) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onImagePickedForSolver(uri)
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull().orEmpty()
            if (spoken.isNotBlank()) {
                questionInput = spoken
                voiceFeedbackError = null
            }
        }
    }

    fun launchSpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                if (language == AppLanguage.URDU) "ur-PK" else Locale.US.toLanguageTag()
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, strings.voiceQuestion)
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            voiceFeedbackError = "Speech recognition service is unavailable on this device."
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchSpeechRecognizer()
        } else {
            voiceFeedbackError = strings.micPermissionRequired
        }
    }

    val mathSymbols = listOf("√", "π", "^", "÷", "×", "²", "(", ")", "=", "∫", "∑", "θ")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 760.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Premium Hero Card (uses custom admin-uploaded banner if set)
            val customBannerBitmap = rememberDecodedBase64Bitmap(systemSettings?.bannerBase64.orEmpty())
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
            ) {
                if (customBannerBitmap != null) {
                    Image(
                        bitmap = customBannerBitmap,
                        contentDescription = strings.heroTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = R.drawable.img_hero_math,
                        contentDescription = strings.heroTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    DeepNavy900.copy(alpha = 0.92f),
                                    DeepNavy900.copy(alpha = 0.55f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(18.dp)
                ) {
                    Text(
                        text = systemSettings?.seoTitle?.ifBlank { strings.heroTitle } ?: strings.heroTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = systemSettings?.seoDescription?.ifBlank { strings.heroSubtitle } ?: strings.heroSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }

            // Main AI Math Solver Input Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = questionInput,
                        onValueChange = { questionInput = it },
                        placeholder = { Text(strings.questionPlaceholder) },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("math_question_input")
                    )

                    // Quick math symbols bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        mathSymbols.forEach { sym ->
                            FilterChip(
                                selected = false,
                                onClick = { questionInput += sym },
                                label = {
                                    Text(
                                        text = sym,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                modifier = Modifier.testTag("symbol_chip_$sym")
                            )
                        }
                    }

                    // Easy vs Detailed toggle
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = difficultyLevel == "easy",
                            onClick = { difficultyLevel = "easy" },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            modifier = Modifier.testTag("difficulty_easy_button")
                        ) {
                            Text(strings.easyMode, maxLines = 1)
                        }
                        SegmentedButton(
                            selected = difficultyLevel == "detailed",
                            onClick = { difficultyLevel = "detailed" },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            modifier = Modifier.testTag("difficulty_detailed_button")
                        ) {
                            Text(strings.detailedMode, maxLines = 1)
                        }
                    }

                    // Primary Solve button
                    Button(
                        onClick = { onSolveText(questionInput, difficultyLevel) },
                        enabled = solverState !is SolverUiState.Solving,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("solve_math_button")
                    ) {
                        if (solverState is SolverUiState.Solving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(strings.solving)
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.solveNow, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Multimodal Input Bar: Upload Image, Open Camera, Voice Question
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_upload_image_button")
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.uploadImage, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onOpenCamera,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_open_camera_button")
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.openCamera, maxLines = 1)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val hasMic = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasMic) launchSpeechRecognizer()
                            else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_voice_question_button")
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.voiceQuestion)
                    }

                    if (voiceFeedbackError != null) {
                        Text(
                            text = voiceFeedbackError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Active Solution / Error / Unclear Image Output
            when (solverState) {
                is SolverUiState.Solved -> {
                    MathSolutionCard(
                        question = solverState.question,
                        strings = strings,
                        uncertaintyNote = solverState.uncertaintyNote,
                        followUpState = followUpState,
                        onToggleBookmark = { onToggleBookmark(solverState.question) },
                        onListenVoice = { onListenVoice(solverState.question) },
                        onAskFollowUp = { stepIdx, text ->
                            onAskFollowUp(solverState.question, stepIdx, text)
                        }
                    )
                }
                is SolverUiState.UnclearImage -> {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.WarningAmber, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.unclearImageTitle, fontWeight = FontWeight.Bold)
                                Text(solverState.message, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                is SolverUiState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${strings.errorPrefix}: ${solverState.message}",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                else -> Unit
            }

            // How It Works Section
            Text(
                text = strings.howItWorksTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                strings.howItWorksSteps.forEach { (title, desc) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Supported Subjects & Class Selection
            Text(
                text = strings.supportedSubjectsTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EducationCatalog.subjectsList.forEach { subject ->
                    AssistChip(
                        onClick = { onSelectSubjectOrClass(subject, null) },
                        label = { Text(subject) }
                    )
                }
            }

            Text(
                text = strings.classSelectionTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EducationCatalog.classesList.forEach { cls ->
                    AssistChip(
                        onClick = { onSelectSubjectOrClass(null, cls) },
                        label = { Text(cls) }
                    )
                }
            }

            // Why Choose ZELU Math AI
            Text(
                text = strings.whyChooseTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                strings.whyChoosePoints.forEach { (title, desc) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Pricing Preview
            Text(
                text = strings.pricingTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            val displayPlans = plans.ifEmpty { EducationCatalog.defaultPlans }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                displayPlans.forEach { plan ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPlansTab() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${plan.name} (${plan.tier})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "${plan.dailyQuestionLimit} Q/day • ${plan.dailyScanLimit} Scans/day",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (plan.monthlyPriceUsd <= 0.0) "Free" else "$${plan.monthlyPriceUsd}/mo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Genuine Student Reviews Empty State (never fabricated)
            Text(
                text = strings.testimonialsTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = strings.testimonialsEmptyNotice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }

            // FAQ Accordion
            Text(
                text = strings.faqTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                strings.faqs.forEachIndexed { idx, (q, a) ->
                    val expanded = expandedFaqIndex == idx
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedFaqIndex = if (expanded) null else idx }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(q, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null
                                )
                            }
                            AnimatedVisibility(visible = expanded) {
                                Text(
                                    text = a,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Optional Sponsored / Ad Placement managed via Admin
            if (systemSettings?.adsEnabled == true && systemSettings.adBannerText.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = systemSettings.adBannerText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // Rich Footer with Social Media Links & Custom Admin Footer Text
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val socials = buildList {
                        systemSettings?.socialFacebook?.takeIf { it.isNotBlank() }?.let { add("Facebook" to it) }
                        systemSettings?.socialYoutube?.takeIf { it.isNotBlank() }?.let { add("YouTube" to it) }
                        systemSettings?.socialInstagram?.takeIf { it.isNotBlank() }?.let { add("Instagram" to it) }
                        systemSettings?.socialWhatsapp?.takeIf { it.isNotBlank() }?.let { add("WhatsApp" to it) }
                        systemSettings?.socialEmail?.takeIf { it.isNotBlank() }?.let { add("Email" to "mailto:$it") }
                    }
                    if (socials.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            socials.forEach { (label, url) ->
                                AssistChip(
                                    onClick = {
                                        val normalized = if (url.startsWith("http") || url.startsWith("mailto:") || url.startsWith("tel:")) {
                                            url
                                        } else {
                                            "https://$url"
                                        }
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)))
                                        }
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = systemSettings?.footerText?.ifBlank { "${strings.ownedBy} • ${strings.footerCopyright}" }
                                ?: "${strings.ownedBy} • ${strings.footerCopyright}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "#",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clickable { onOpenRoutePrompt() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("footer_route_trigger")
                        )
                    }
                }
            }
        }
    }
}
