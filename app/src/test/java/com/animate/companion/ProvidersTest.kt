package com.animate.companion

import com.animate.companion.data.AppSettings
import com.animate.companion.llm.LlmException
import com.animate.companion.llm.Provider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProvidersTest {
    @Test
    fun everyProviderHasAShortCuratedList() {
        Provider.entries.forEach { p ->
            assertTrue(p.name, p.models.isNotEmpty() && p.models.size <= 3)
            assertEquals(p.name, p.models.size, p.models.map { it.id }.toSet().size)
            assertEquals(p.models.first().id, p.defaultModel)
            assertEquals(3, p.keySteps.size)
        }
    }

    @Test
    fun retiredModelsAreGone() {
        val ids = Provider.entries.flatMap { p -> p.models.map { it.id } }
        listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "grok-3-mini").forEach { assertFalse(it, it in ids) }
    }

    @Test
    fun kidsSeeOnlyThreeMainChoices() {
        assertEquals(listOf(Provider.GEMINI, Provider.GROQ, Provider.OPENROUTER), Provider.entries.filterNot { it.advanced })
    }

    @Test
    fun unknownModelIsDetected() {
        assertTrue(LlmException("Groq: HTTP 404 model not found", false, 404).isUnknownModel)
        assertTrue(LlmException("HTTP 400 The model `x` does not exist", false, 400).isUnknownModel)
        assertFalse(LlmException("HTTP 400 bad request", false, 400).isUnknownModel)
        assertFalse(LlmException("HTTP 429", true, 429).isUnknownModel)
    }

    @Test
    fun blankModelMeansDefault() {
        val s = AppSettings(models = mapOf(Provider.GROQ to ""))
        assertEquals("openai/gpt-oss-120b", s.config(Provider.GROQ).model)
    }
}
