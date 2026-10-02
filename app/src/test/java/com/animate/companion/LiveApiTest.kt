package com.animate.companion

import com.animate.companion.data.CharacterEntity
import com.animate.companion.data.MessageEntity
import com.animate.companion.llm.LlmClient
import com.animate.companion.llm.PromptBuilder
import com.animate.companion.llm.Provider
import com.animate.companion.llm.ProviderConfig
import com.animate.companion.model.Appearance
import com.animate.companion.model.Gender
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Real network call against the keyless provider. Run with LIVE_API=1. */
class LiveApiTest {
    @Test
    fun keylessProviderAnswersInCharacter() = runBlocking {
        assumeTrue(System.getenv("LIVE_API") == "1")
        val c = CharacterEntity(
            id = 1, name = "Рин Курогане", gender = Gender.FEMALE.name, appearanceJson = Appearance(ears = 1).toJson(),
            archetypeId = "tsundere", professionId = "maid", directionId = "comedy",
        )
        val history = listOf(MessageEntity(1, 1, "user", "Привет! Сделаешь мне чай?"))
        val raw = LlmClient().complete(
            ProviderConfig(Provider.POLLINATIONS, "", Provider.POLLINATIONS.defaultModel),
            PromptBuilder.buildContext(c, "Аня", history),
        )
        println("RAW: $raw")
        println("PARSED: ${PromptBuilder.parseReply(raw)}")
    }
}
