package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import coil.compose.AsyncImage
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.model.SolvedQuestion
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepNavy900
import com.example.ui.theme.ElectricBlue
import com.example.viewmodel.FollowUpUiState
import com.example.voice.VoicePersona
import com.example.voice.VoiceTeacherManager
import java.util.Locale

@Composable
fun VoiceTeacherScreen(
    strings: ZeluStrings,
    language: AppLanguage,
    activeQuestion: SolvedQuestion?,
    voiceManager: VoiceTeacherManager,
    followUpState: FollowUpUiState,
    onAskFollowUp: (SolvedQuestion, Int?, String) -> Unit
) {
    val context = LocalContext.current
    val voiceState by voiceManager.state.collectAsState()
    var voiceMenuExpanded by remember { mutableStateOf(false) }
    var micError by remember { mutableStateOf<String?>(null) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && activeQuestion != null) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (spoken.isNotBlank()) {
                val activeStep = voiceState.activeStepIndex.takeIf { it in activeQuestion.steps.indices }
                onAskFollowUp(activeQuestion, activeStep, spoken)
            }
        }
    }

    fun startMicFollowUp() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                if (language == AppLanguage.URDU) "ur-PK" else Locale.US.toLanguageTag()
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, strings.askByMic)
        }
        try {
            speechLauncher.launch(intent)
            micError = null
        } catch (_: Exception) {
            micError = "Speech recognition unavailable on this device."
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startMicFollowUp()
        else micError = strings.micPermissionRequired
    }

    val spokenScript = remember(activeQuestion, strings) {
        if (activeQuestion == null) emptyList()
        else buildList {
            add("${strings.step1Identification}: ${activeQuestion.identification.ifBlank { activeQuestion.questionText }}")
            if (activeQuestion.relevantFormula.isNotBlank()) {
                add("${strings.step3Formula}: ${activeQuestion.relevantFormula}")
            }
            activeQuestion.steps.forEachIndexed { i, step ->
                add("Step ${i + 1}: $step")
            }
            add("${strings.step7FinalAnswer}: ${activeQuestion.finalAnswer}")
            if (activeQuestion.whyItWorks.isNotBlank()) {
                add("${strings.step9WhyItWorks}: ${activeQuestion.whyItWorks}")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 760.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Artwork Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
            ) {
                AsyncImage(
                    model = R.drawable.img_voice_teacher,
                    contentDescription = strings.voiceTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(DeepNavy900.copy(alpha = 0.90f), DeepNavy900.copy(alpha = 0.45f))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(strings.voiceTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = if (voiceState.isSpeaking) {
                                "Speaking step ${voiceState.activeStepIndex + 1}..."
                            } else {
                                if (voiceState.persona == VoicePersona.MADAM) strings.voicePersonaMadam else strings.voicePersonaTeacher
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }

            // Persona & Tone Card (always accessible so user can set Madam / Teacher voice anytime)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(strings.voicePersonaLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = voiceState.persona == VoicePersona.MADAM,
                            onClick = {
                                voiceManager.setPersona(VoicePersona.MADAM, preferUrdu = language == AppLanguage.URDU)
                            },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                            modifier = Modifier.testTag("persona_madam_button")
                        ) {
                            Text(strings.voicePersonaMadam, maxLines = 1)
                        }
                        SegmentedButton(
                            selected = voiceState.persona == VoicePersona.TEACHER,
                            onClick = {
                                voiceManager.setPersona(VoicePersona.TEACHER, preferUrdu = language == AppLanguage.URDU)
                            },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                            modifier = Modifier.testTag("persona_teacher_button")
                        ) {
                            Text(strings.voicePersonaTeacher, maxLines = 1)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.voicePitchLabel, style = MaterialTheme.typography.labelMedium)
                        Text("${String.format(Locale.US, "%.2f", voiceState.pitch)}x", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = voiceState.pitch,
                        onValueChange = { voiceManager.setPitch(it) },
                        valueRange = 0.70f..1.40f,
                        modifier = Modifier.testTag("voice_pitch_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.speechSpeed, style = MaterialTheme.typography.labelMedium)
                        Text("${String.format(Locale.US, "%.2f", voiceState.speechRate)}x", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = voiceState.speechRate,
                        onValueChange = { voiceManager.setSpeechRate(it) },
                        valueRange = 0.6f..1.75f,
                        modifier = Modifier.testTag("voice_speed_slider")
                    )

                    if (voiceState.availableVoices.isNotEmpty()) {
                        Box {
                            OutlinedButton(
                                onClick = { voiceMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth().testTag("voice_select_dropdown")
                            ) {
                                Text("${strings.selectVoice}: ${voiceState.selectedVoiceName ?: "Auto (ur-PK / en-PK)"}")
                            }
                            DropdownMenu(
                                expanded = voiceMenuExpanded,
                                onDismissRequest = { voiceMenuExpanded = false }
                            ) {
                                voiceState.availableVoices.forEach { v ->
                                    val localeTag = "${v.locale.language}-${v.locale.country}".trimEnd('-')
                                    DropdownMenuItem(
                                        text = { Text("${v.locale.displayName} [$localeTag] • ${v.name}") },
                                        onClick = {
                                            voiceManager.selectVoice(v)
                                            voiceMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Honest PK Urdu TTS support notice + one-tap install button if missing
            if (language == AppLanguage.URDU && voiceState.isReady && !voiceState.isPkUrduInstalled) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.urduTtsUnavailableWarning, style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.installPkUrduVoice)
                        }
                    }
                }
            }

            if (activeQuestion == null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.GraphicEq, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(strings.noSolutionSelectedForVoice, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                // Playback Controls Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = activeQuestion.questionText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (voiceState.isPaused) {
                                        voiceManager.resume(preferUrdu = language == AppLanguage.URDU)
                                    } else {
                                        voiceManager.speakSteps(spokenScript, 0, preferUrdu = language == AppLanguage.URDU)
                                    }
                                },
                                enabled = voiceState.isReady && !voiceState.isSpeaking,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                modifier = Modifier.weight(1f).testTag("voice_play_button")
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (voiceState.isPaused) strings.resume else strings.play)
                            }
                            OutlinedButton(
                                onClick = { voiceManager.pause() },
                                enabled = voiceState.isSpeaking,
                                modifier = Modifier.weight(1f).testTag("voice_pause_button")
                            ) {
                                Icon(Icons.Filled.Pause, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.pause)
                            }
                            OutlinedButton(
                                onClick = { voiceManager.stop() },
                                enabled = voiceState.isSpeaking || voiceState.isPaused,
                                modifier = Modifier.weight(1f).testTag("voice_stop_button")
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.stop)
                            }
                        }

                        // Mic Follow-up
                        OutlinedButton(
                            onClick = {
                                val hasMic = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasMic) startMicFollowUp()
                                else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("voice_mic_followup_button")
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.askByMic)
                        }

                        if (micError != null) {
                            Text(micError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Follow-up AI response
                when (followUpState) {
                    is FollowUpUiState.Ready -> {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Text(followUpState.answer, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    is FollowUpUiState.Error -> {
                        Text(followUpState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    else -> Unit
                }

                // Step-by-step highlighted script
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    spokenScript.forEachIndexed { idx, line ->
                        val active = idx == voiceState.activeStepIndex
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (active) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (active) Modifier.border(2.dp, ElectricBlue, RoundedCornerShape(14.dp))
                                    else Modifier
                                )
                                .clickable {
                                    voiceManager.speakSteps(spokenScript, startIndex = idx, preferUrdu = language == AppLanguage.URDU)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (active) {
                                    Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
