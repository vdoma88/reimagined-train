package com.animate.companion

import com.animate.companion.audio.selectVoiceId
import com.animate.companion.audio.SpeechVoice
import com.animate.companion.data.AppSettings
import org.junit.Assert.*
import org.junit.Test

class VoicePreferenceTest {
    @Test fun explicitInstalledVoiceOrSafeFallback() {
        val available = listOf("quality-a", "quality-b")
        assertEquals("quality-b", selectVoiceId("quality-b", available))
        assertEquals("quality-a", selectVoiceId("", available))
        assertEquals("quality-a", selectVoiceId("old-engine-voice", available))
        assertNull(selectVoiceId("missing", emptyList()))
    }
    @Test fun existingSettingsAndVoiceDefaultsRemainAutomatic() {
        assertEquals("", AppSettings().speechVoiceId)
        assertEquals("", SpeechVoice(1f, 1f).deviceVoiceId)
    }
}
