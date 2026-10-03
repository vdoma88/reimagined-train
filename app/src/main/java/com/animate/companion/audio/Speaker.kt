package com.animate.companion.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.animate.companion.model.Gender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import kotlin.random.Random

/** How a character sounds when reading replies aloud. */
data class SpeechVoice(val pitch: Float, val rate: Float)

enum class SpeakerStatus { INITIALIZING, READY, NO_RUSSIAN_VOICE, UNAVAILABLE }

/**
 * Reads replies aloud with the device's offline text-to-speech engine.
 * Each character gets its own pitch and pace; [speakingId] drives lip-sync and the 🔊 buttons.
 */
class Speaker(context: Context) {
    private val appContext = context.applicationContext
    private val _status = MutableStateFlow(SpeakerStatus.INITIALIZING)
    val status: StateFlow<SpeakerStatus> = _status

    /** Id of the utterance being spoken, or null when silent. */
    private val _speakingId = MutableStateFlow<String?>(null)
    val speakingId: StateFlow<String?> = _speakingId

    var onSpeakingChanged: ((Boolean) -> Unit)? = null

    private val tts: TextToSpeech = TextToSpeech(appContext) { code ->
        _status.value = if (code == TextToSpeech.SUCCESS) configureLanguage() else SpeakerStatus.UNAVAILABLE
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                _speakingId.value = utteranceId
                onSpeakingChanged?.invoke(true)
            }

            override fun onDone(utteranceId: String) = finished(utteranceId)

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) = finished(utteranceId)

            override fun onStop(utteranceId: String, interrupted: Boolean) = finished(utteranceId)
        })
    }

    private fun finished(id: String) {
        if (_speakingId.value == id) {
            _speakingId.value = null
            onSpeakingChanged?.invoke(false)
        }
    }

    private fun configureLanguage(): SpeakerStatus {
        val result = tts.setLanguage(Locale("ru", "RU"))
        return if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            SpeakerStatus.NO_RUSSIAN_VOICE
        } else {
            SpeakerStatus.READY
        }
    }

    /** Re-checks the language after the user installed voice data. */
    fun refresh() {
        if (_status.value != SpeakerStatus.UNAVAILABLE) _status.value = configureLanguage()
    }

    /** Speaks [text] (already cleaned) and returns false when TTS cannot be used right now. */
    fun speak(text: String, voice: SpeechVoice, id: String): Boolean {
        if (_status.value != SpeakerStatus.READY || text.isBlank()) return false
        tts.setPitch(voice.pitch)
        tts.setSpeechRate(voice.rate)
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f) }
        return tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, id) == TextToSpeech.SUCCESS
    }

    fun stop() {
        tts.stop()
        _speakingId.value?.let { finished(it) }
    }

    /** Opens the system screen that downloads voice data (for phones without a Russian voice). */
    fun installVoiceIntent(): Intent =
        Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

object SpeechText {
    private val action = Regex("""\*[^*\n]*\*""")
    private val emoTag = Regex("""\[\s*emo[^\]]*]""", RegexOption.IGNORE_CASE)
    private val url = Regex("""https?://\S+""")
    // Emoji, pictographs, dingbats, variation selectors and joiners.
    private val emoji = Regex("""[\x{1F000}-\x{1FAFF}\x{2600}-\x{27BF}\x{2B00}-\x{2BFF}\x{FE0F}\x{200D}\x{1F1E6}-\x{1F1FF}]""")
    // A lone letter + hyphen before a word starting with the same letter: "Б-бака", "н-ну".
    private val stutter = Regex("""(?<!\p{L})(\p{L})-(\p{L})""")
    private val tildes = Regex("""~+""")
    private val spaces = Regex("""\s+""")

    /** Turns a chat reply into something pleasant to hear: no *actions*, emoji, tags or links. */
    fun clean(text: String): String = text
        .let { emoTag.replace(it, " ") }
        .let { action.replace(it, " ") }
        .let { url.replace(it, " ") }
        .let { emoji.replace(it, "") }
        .let { t ->
            // "Б-бака" → "Бака": keep the first letter, drop the hyphen and the repeat.
            stutter.replace(t) { m ->
                if (m.groupValues[1].equals(m.groupValues[2], ignoreCase = true)) m.groupValues[1] else m.value
            }
        }
        .let { tildes.replace(it, "!") }
        .replace("…", "... ")
        .let { spaces.replace(it, " ") }
        .trim()
        .take(1200)

    /** Per-character voice: gender and archetype set the base, the seed makes each one unique. */
    fun voiceFor(gender: Gender, archetypeId: String, archetypePitch: Float, seed: Int, userRate: Float): SpeechVoice {
        val jitter = Random(seed xor 0x5EED).nextFloat() * 0.16f - 0.08f
        val basePitch = when (gender) {
            Gender.FEMALE -> 1.35f
            Gender.MALE -> 0.88f
            Gender.NEUTRAL -> 1.1f
        }
        val pace = when (archetypeId) {
            "genki" -> 1.12f
            "lazy" -> 0.88f
            "kuudere", "onee" -> 0.95f
            "dandere" -> 0.92f
            else -> 1f
        }
        return SpeechVoice(
            pitch = (basePitch * archetypePitch + jitter).coerceIn(0.6f, 1.8f),
            rate = (pace * userRate).coerceIn(0.6f, 1.6f),
        )
    }
}
