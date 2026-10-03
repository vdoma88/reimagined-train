package com.animate.companion

import com.animate.companion.audio.SpeechText
import com.animate.companion.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechTest {
    @Test
    fun removesActionsEmojiTagsAndLinks() {
        val spoken = SpeechText.clean("[emo:shy] *краснеет* Б-бака! 😳💕 Смотри https://example.com ня~")
        assertEquals("Бака! Смотри ня!", spoken)
    }

    @Test
    fun keepsPlainText() {
        assertEquals("Привет, как дела?", SpeechText.clean("Привет, как дела?"))
    }

    @Test
    fun onlyActionsMeansSilence() {
        assertTrue(SpeechText.clean("*машет рукой* *улыбается*").isBlank())
    }

    @Test
    fun voicesDifferByGenderAndCharacter() {
        val girl = SpeechText.voiceFor(Gender.FEMALE, "genki", 1.15f, 1, 1f)
        val boy = SpeechText.voiceFor(Gender.MALE, "kuudere", 0.92f, 1, 1f)
        assertTrue(girl.pitch > boy.pitch)
        assertTrue(girl.rate > boy.rate)
        assertNotEquals(SpeechText.voiceFor(Gender.FEMALE, "deredere", 1f, 1, 1f), SpeechText.voiceFor(Gender.FEMALE, "deredere", 1f, 2, 1f))
        val slow = SpeechText.voiceFor(Gender.FEMALE, "deredere", 1f, 1, 0.6f)
        assertFalse(slow.rate > 0.7f)
    }
}
