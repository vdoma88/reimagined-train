package com.animate.companion.audio

import com.animate.companion.data.AppSettings
import com.animate.companion.data.CharacterEntity
import com.animate.companion.model.Emotion
import com.animate.companion.model.PersonaPresets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

enum class SfxType { SEND, RECEIVE, TAP, SPARKLE, DICE }

/** Single entry point for music, UI sounds and character voices. */
class SoundDirector(private val scope: CoroutineScope) {
    private val music = LofiPlayer(scope)
    @Volatile private var settings = AppSettings()
    @Volatile private var foreground = true

    fun apply(s: AppSettings) {
        val old = settings
        settings = s
        loaded = true
        if (!foreground) return
        when {
            !s.musicEnabled -> if (musicStarted) {
                music.stop()
                musicStarted = false
            }
            !musicStarted || old.musicTrack != s.musicTrack -> {
                music.play(s.musicTrack, s.musicVolume)
                musicStarted = true
            }
            old.musicVolume != s.musicVolume -> music.setVolume(s.musicVolume)
        }
    }

    private var musicStarted = false
    @Volatile private var loaded = false

    fun onForeground() {
        foreground = true
        if (loaded && settings.musicEnabled) {
            music.play(settings.musicTrack, settings.musicVolume)
            musicStarted = true
        }
    }

    fun onBackground() {
        foreground = false
        music.pause()
    }

    fun sfx(type: SfxType) {
        if (!settings.sfxEnabled) return
        scope.launch(Dispatchers.Default) {
            val pcm = when (type) {
                SfxType.SEND -> Sfx.send
                SfxType.RECEIVE -> Sfx.receive
                SfxType.TAP -> Sfx.tap
                SfxType.SPARKLE -> Sfx.sparkle
                SfxType.DICE -> Sfx.dice
            }
            PcmPlayer.play(pcm, 0.7f)
        }
    }

    /**
     * Plays a vocal interjection fitting [emotion] and returns its text (to show as a bubble),
     * or null when nothing was played.
     */
    suspend fun voice(c: CharacterEntity, emotion: Emotion, force: Boolean = false): String? {
        if (!settings.voiceEnabled) return null
        val a = c.appearance
        val arch = PersonaPresets.archetype(c.archetypeId)
        val ij = VoiceSynth.choose(emotion, c.genderEnum, catEars = a.ears == 1, sleepy = c.archetypeId == "lazy")
            ?: if (force) VoiceSynth.choose(Emotion.HAPPY, c.genderEnum, a.ears == 1, false) else null
        ij ?: return null
        val voice = VoiceSynth.voiceFor(c.genderEnum, arch.voicePitch, c.voiceSeed)
        val pcm = withContext(Dispatchers.Default) { VoiceSynth.render(ij, voice, Random.nextInt()) }
        PcmPlayer.play(pcm, settings.voiceVolume)
        return ij.text
    }

    /** Preview used by the creator: plays a happy interjection in the given voice. */
    suspend fun previewVoice(c: CharacterEntity): String? = voice(c, Emotion.HAPPY, force = true)
}
