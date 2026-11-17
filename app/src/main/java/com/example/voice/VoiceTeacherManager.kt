package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoicePersona(val id: String) {
    MADAM("madam"),
    TEACHER("teacher");

    companion object {
        fun fromId(raw: String): VoicePersona = if (raw.equals("teacher", true)) TEACHER else MADAM
    }
}

data class VoiceUiState(
    val isReady: Boolean = false,
    val isSpeaking: Boolean = false,
    val isPaused: Boolean = false,
    val activeStepIndex: Int = -1,
    val speechRate: Float = 0.96f,
    val pitch: Float = 1.04f,
    val persona: VoicePersona = VoicePersona.MADAM,
    val isUrduSupported: Boolean = false,
    val isPkUrduInstalled: Boolean = false,
    val availableVoices: List<Voice> = emptyList(),
    val selectedVoiceName: String? = null,
    val statusNote: String = ""
)

class VoiceTeacherManager(context: Context) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var queuedSteps: List<String> = emptyList()
    private var currentLocale: Locale = Locale.forLanguageTag("ur-PK")
    private var userManuallyPickedVoice = false
    private var appliedAdminDefaults = false

    private val _state = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts ?: return@TextToSpeech
                val pkUrduLocale = Locale.forLanguageTag("ur-PK")
                val genericUrduLocale = Locale.forLanguageTag("ur")
                val pkCheck = engine.isLanguageAvailable(pkUrduLocale)
                val genCheck = engine.isLanguageAvailable(genericUrduLocale)
                val pkInstalled = pkCheck >= TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                    engine.voices?.any { it.locale.language == "ur" && it.locale.country.equals("PK", true) } == true
                val urduOk = pkCheck >= TextToSpeech.LANG_AVAILABLE || genCheck >= TextToSpeech.LANG_AVAILABLE

                val voices = (engine.voices ?: emptySet())
                    .filter { it.locale.language in listOf("ur", "en") }
                    .sortedWith(
                        compareByDescending<Voice> { it.locale.language == "ur" && it.locale.country.equals("PK", true) }
                            .thenByDescending { it.locale.language == "ur" }
                            .thenByDescending { !it.isNetworkConnectionRequired }
                            .thenByDescending { it.quality }
                            .thenBy { it.name }
                    )
                    .take(30)

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        val idx = utteranceId?.removePrefix("step_")?.toIntOrNull() ?: -1
                        _state.value = _state.value.copy(
                            isSpeaking = true,
                            isPaused = false,
                            activeStepIndex = idx
                        )
                    }

                    override fun onDone(utteranceId: String?) {
                        val idx = utteranceId?.removePrefix("step_")?.toIntOrNull() ?: -1
                        if (idx >= queuedSteps.lastIndex) {
                            _state.value = _state.value.copy(
                                isSpeaking = false,
                                isPaused = false,
                                activeStepIndex = -1
                            )
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _state.value = _state.value.copy(
                            isSpeaking = false,
                            isPaused = false
                        )
                    }
                })

                _state.value = _state.value.copy(
                    isReady = true,
                    isUrduSupported = urduOk,
                    isPkUrduInstalled = pkInstalled,
                    availableVoices = voices,
                    selectedVoiceName = engine.defaultVoice?.name
                )
                applyPersonaVoice(
                    persona = _state.value.persona,
                    preferUrdu = false,
                    overrideManual = false
                )
            } else {
                _state.value = _state.value.copy(
                    isReady = false,
                    statusNote = "TextToSpeech engine unavailable on this device."
                )
            }
        }
    }

    fun syncAdminDefaults(personaId: String, pitch: Float, speechRate: Float, preferUrdu: Boolean) {
        if (appliedAdminDefaults) return
        appliedAdminDefaults = true
        val targetPersona = VoicePersona.fromId(personaId)
        val clampedPitch = pitch.coerceIn(0.70f, 1.40f)
        val clampedRate = speechRate.coerceIn(0.60f, 1.75f)
        _state.value = _state.value.copy(
            persona = targetPersona,
            pitch = clampedPitch,
            speechRate = clampedRate
        )
        tts?.setPitch(clampedPitch)
        tts?.setSpeechRate(clampedRate)
        if (_state.value.isReady && !userManuallyPickedVoice) {
            applyPersonaVoice(targetPersona, preferUrdu = preferUrdu, overrideManual = false)
        }
    }

    fun setPersona(persona: VoicePersona, preferUrdu: Boolean) {
        userManuallyPickedVoice = false
        val naturalPitch = if (persona == VoicePersona.MADAM) 1.06f else 0.88f
        _state.value = _state.value.copy(
            persona = persona,
            pitch = naturalPitch
        )
        tts?.setPitch(naturalPitch)
        applyPersonaVoice(persona, preferUrdu = preferUrdu, overrideManual = true)
    }

    fun setPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.70f, 1.40f)
        tts?.setPitch(clamped)
        _state.value = _state.value.copy(pitch = clamped)
    }

    fun speakSteps(steps: List<String>, startIndex: Int = 0, preferUrdu: Boolean) {
        val engine = tts ?: return
        if (!_state.value.isReady || steps.isEmpty()) return
        queuedSteps = steps

        val targetLocale = when {
            preferUrdu && _state.value.isPkUrduInstalled -> Locale.forLanguageTag("ur-PK")
            preferUrdu && _state.value.isUrduSupported -> Locale.forLanguageTag("ur")
            else -> {
                val enPkOk = engine.isLanguageAvailable(Locale.forLanguageTag("en-PK")) >= TextToSpeech.LANG_AVAILABLE
                if (enPkOk) Locale.forLanguageTag("en-PK") else Locale.US
            }
        }
        currentLocale = targetLocale
        engine.language = targetLocale
        if (!userManuallyPickedVoice) {
            applyPersonaVoice(_state.value.persona, preferUrdu = preferUrdu, overrideManual = false)
        }
        engine.setSpeechRate(_state.value.speechRate)
        engine.setPitch(_state.value.pitch)

        val safeStart = startIndex.coerceIn(0, steps.lastIndex)
        engine.stop()
        for (i in safeStart until steps.size) {
            val queueMode = if (i == safeStart) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val spokenText = normalizeMathForSpeech(
                raw = steps[i],
                preferUrdu = preferUrdu,
                persona = _state.value.persona,
                isIntroLine = (i == 0 && safeStart == 0)
            )
            engine.speak(spokenText, queueMode, null, "step_$i")
        }
        _state.value = _state.value.copy(
            isSpeaking = true,
            isPaused = false,
            activeStepIndex = safeStart
        )
    }

    fun pause() {
        tts?.stop()
        _state.value = _state.value.copy(
            isSpeaking = false,
            isPaused = true
        )
    }

    fun resume(preferUrdu: Boolean) {
        if (queuedSteps.isEmpty()) return
        val resumeIdx = _state.value.activeStepIndex.coerceAtLeast(0)
        speakSteps(queuedSteps, startIndex = resumeIdx, preferUrdu = preferUrdu)
    }

    fun stop() {
        tts?.stop()
        _state.value = _state.value.copy(
            isSpeaking = false,
            isPaused = false,
            activeStepIndex = -1
        )
    }

    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.6f, 1.75f)
        tts?.setSpeechRate(clamped)
        _state.value = _state.value.copy(speechRate = clamped)
    }

    fun selectVoice(voice: Voice) {
        userManuallyPickedVoice = true
        tts?.voice = voice
        _state.value = _state.value.copy(selectedVoiceName = voice.name)
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun applyPersonaVoice(persona: VoicePersona, preferUrdu: Boolean, overrideManual: Boolean) {
        val engine = tts ?: return
        if (userManuallyPickedVoice && !overrideManual) return
        val allVoices = engine.voices ?: return

        val candidatePool = if (preferUrdu) {
            allVoices.filter { it.locale.language == "ur" }
        } else {
            allVoices.filter { it.locale.language == "en" }
        }
        if (candidatePool.isEmpty()) return

        val femaleHints = listOf("female", "woman", "sfb", "lfb", "urfb", "f00", "f01", "tpf", "nfp", "sfg", "iom")
        val maleHints = listOf("male", "man", "urm", "m00", "m01", "tpm", "iol", "sfg_m", "iob")
        val hints = if (persona == VoicePersona.MADAM) femaleHints else maleHints

        val bestVoice = candidatePool.maxByOrNull { v ->
            val lowerName = v.name.lowercase()
            var score = 0
            if (preferUrdu && v.locale.country.equals("PK", true)) score += 100
            if (!preferUrdu && v.locale.country in listOf("PK", "GB", "US")) score += 40
            if (!v.isNetworkConnectionRequired) score += 20
            if (v.quality >= Voice.QUALITY_HIGH) score += 15
            if (hints.any { lowerName.contains(it) }) score += 60
            score
        }

        if (bestVoice != null) {
            engine.voice = bestVoice
            _state.value = _state.value.copy(selectedVoiceName = bestVoice.name)
        }
    }

    private fun normalizeMathForSpeech(
        raw: String,
        preferUrdu: Boolean,
        persona: VoicePersona,
        isIntroLine: Boolean
    ): String {
        var text = raw.trim()
        if (preferUrdu) {
            text = text
                .replace("^2", " مربع ")
                .replace("²", " مربع ")
                .replace("^3", " مکعب ")
                .replace("³", " مکعب ")
                .replace("√", " جذر ")
                .replace("π", " پائی ")
                .replace("θ", " تھیٹا ")
                .replace("=", " برابر ہے ")
                .replace("+", " جمع ")
                .replace("×", " ضرب ")
                .replace("÷", " تقسیم ")
                .replace("±", " جمع یا منفی ")
                .replace("≤", " چھوٹا یا برابر ہے ")
                .replace("≥", " بڑا یا برابر ہے ")
                .replace("≠", " برابر نہیں ہے ")
            val greeting = if (isIntroLine) {
                if (persona == VoicePersona.MADAM) {
                    "السلام علیکم! میں میڈم زیلُو ہوں۔ آئیے یہ سوال آسان انداز میں سمجھتے ہیں۔ "
                } else {
                    "السلام علیکم! میں سر زیلُو ہوں۔ آئیے یہ سوال آسان انداز میں سمجھتے ہیں۔ "
                }
            } else ""
            return "$greeting$text"
        } else {
            text = text
                .replace("^2", " squared ")
                .replace("²", " squared ")
                .replace("^3", " cubed ")
                .replace("³", " cubed ")
                .replace("√", " square root of ")
                .replace("π", " pi ")
                .replace("θ", " theta ")
                .replace("=", " equals ")
                .replace("+", " plus ")
                .replace("×", " times ")
                .replace("÷", " divided by ")
                .replace("±", " plus or minus ")
            val greeting = if (isIntroLine) {
                if (persona == VoicePersona.MADAM) {
                    "Hello student, I am Madam Zelu. Let's walk through this problem patiently. "
                } else {
                    "Hello student, I am Sir Zelu. Let's walk through this problem patiently. "
                }
            } else ""
            return "$greeting$text"
        }
    }
}
