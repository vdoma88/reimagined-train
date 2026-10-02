package com.animate.companion

import com.animate.companion.data.AppSettings
import com.animate.companion.data.CharacterEntity
import com.animate.companion.data.MessageEntity
import com.animate.companion.llm.LlmClient
import com.animate.companion.llm.PromptBuilder
import com.animate.companion.llm.Provider
import com.animate.companion.model.Appearance
import com.animate.companion.model.Emotion
import com.animate.companion.model.Gender
import com.animate.companion.model.NameGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LogicTest {
    private val c = CharacterEntity(
        id = 7, name = "Рин Курогане", gender = Gender.FEMALE.name, appearanceJson = Appearance().toJson(),
        archetypeId = "tsundere", professionId = "maid", directionId = "romance", memory = "- Любит котов", summarizedUntilId = 2,
    )

    @Test
    fun parsesEmotionTag() {
        val r = PromptBuilder.parseReply("[emo:shy] *краснеет* Б-бака!")
        assertEquals(Emotion.SHY, r.emotion)
        assertEquals("*краснеет* Б-бака!", r.text)
        assertEquals(Emotion.ANGRY, PromptBuilder.parseReply("[angry] Хмф.").emotion)
        assertEquals(Emotion.LOVE, PromptBuilder.parseReply("[ EMOTION: Love ] привет").emotion)
    }

    @Test
    fun stripsThinkingAndStrayTags() {
        val r = PromptBuilder.parseReply("<think>hmm</think>\n[emo:happy] Привет! [emo:laugh] ахаха")
        assertEquals(Emotion.HAPPY, r.emotion)
        assertFalse(r.text.contains("[emo"))
        assertFalse(r.text.contains("think"))
    }

    @Test
    fun guessesEmotionWithoutTag() {
        assertEquals(Emotion.LAUGH, PromptBuilder.parseReply("Ахаха, ну ты даёшь").emotion)
        assertEquals(Emotion.NEUTRAL, PromptBuilder.parseReply("Хорошо.").emotion)
    }

    @Test
    fun contextSkipsSummarizedMessagesAndStartsWithUser() {
        val history = (1L..5L).map { MessageEntity(it, 7, if (it % 2 == 0L) "user" else "assistant", "m$it", "happy") }
        val ctx = PromptBuilder.buildContext(c, "Аня", history)
        assertEquals("system", ctx[0].role)
        assertTrue(ctx[0].content.contains("Любит котов"))
        assertTrue(ctx[0].content.contains("Аня"))
        assertEquals("user", ctx[1].role)
        assertFalse(ctx.any { it.content == "m1" || it.content == "m2" })
        assertTrue(ctx.last().content.startsWith("[emo:happy]"))
    }

    @Test
    fun providerChainFallsBackToKeylessProvider() {
        val noKeys = AppSettings(provider = Provider.GEMINI)
        assertEquals(listOf(Provider.POLLINATIONS), noKeys.providerChain().map { it.provider })
        val withKeys = AppSettings(provider = Provider.GROQ, keys = mapOf(Provider.GEMINI to "g", Provider.GROQ to "q"))
        assertEquals(listOf(Provider.GROQ, Provider.GEMINI, Provider.POLLINATIONS), withKeys.providerChain().map { it.provider })
        val noFallback = withKeys.copy(autoFallback = false)
        assertEquals(listOf(Provider.GROQ), noFallback.providerChain().map { it.provider })
        assertEquals("gemini-flash-latest", noKeys.config(Provider.GEMINI).model)
    }

    @Test
    fun parsesOpenAiResponse() {
        val raw = """{"choices":[{"index":0,"message":{"role":"assistant","content":" [emo:happy] Ня! "}}]}"""
        assertEquals("[emo:happy] Ня!", LlmClient().parseContent(raw))
    }

    @Test
    fun namesAreGenderedAndTwoPart() {
        val r = Random(1)
        repeat(50) {
            Gender.entries.forEach { g -> assertEquals(2, NameGenerator.generate(g, r).split(" ").size) }
        }
    }

    @Test
    fun appearanceRoundTripsAndRespectsGender() {
        val a = Appearance.random(Gender.MALE, Random(3))
        assertEquals(a, Appearance.fromJson(a.toJson()))
        assertEquals(Appearance(), Appearance.fromJson("garbage"))
        repeat(100) {
            val m = Appearance.random(Gender.MALE, Random(it))
            assertTrue(m.hairStyle !in setOf(0, 1, 5, 6, 7, 9, 10, 11))
        }
    }
}
