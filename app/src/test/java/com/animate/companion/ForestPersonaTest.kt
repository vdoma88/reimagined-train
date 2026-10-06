package com.animate.companion

import com.animate.companion.data.CharacterEntity
import com.animate.companion.llm.FallbackLines
import com.animate.companion.llm.PromptBuilder
import com.animate.companion.model.Appearance
import com.animate.companion.model.Gender
import com.animate.companion.model.NameGenerator
import com.animate.companion.model.PersonaPresets
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class ForestPersonaTest {
    private val c = CharacterEntity(
        id = 7, name = "Нора Брукс", gender = Gender.FEMALE.name,
        appearanceJson = Appearance().toJson(), archetypeId = "tsundere",
        professionId = "maid", directionId = "romance", memory = "Любит котов",
    )

    @Test fun oldPresetIdsSelectNewPersonalitiesInsteadOfFallingBack() {
        val archetypes = listOf("tsundere", "kuudere", "deredere", "dandere", "yandere", "genki", "himedere", "chuuni", "onee", "lazy")
        val professions = listOf("maid", "mage", "idol", "samurai", "ninja", "council", "miko", "alchemist", "barista", "hacker", "knight", "demonlord", "mangaka", "pilot", "detective", "healer", "student", "hunter")
        val directions = listOf("romance", "friend", "adventure", "slice", "comedy", "mentor", "mystery", "japanese", "comfort")
        archetypes.forEach { assertEquals(it, PersonaPresets.archetype(it).id) }
        professions.forEach { assertEquals(it, PersonaPresets.profession(it).id) }
        directions.forEach { assertEquals(it, PersonaPresets.direction(it).id) }
        assertEquals("Колкий скептик", PersonaPresets.archetype("tsundere").label)
        assertEquals("Смотрительница лавки", PersonaPresets.profession("maid").label(Gender.FEMALE))
        assertEquals("Шифры и головоломки", PersonaPresets.direction("japanese").label)
    }

    @Test fun legacyNamesRefreshDeterministicallyAndCustomNamesSurvive() {
        Gender.entries.forEach { gender ->
            val updated = NameGenerator.refreshLegacyName("Рин Курогане", gender, 42)
            assertNotEquals("Рин Курогане", updated)
            assertEquals(updated, NameGenerator.refreshLegacyName("Рин Курогане", gender, 42))
            assertEquals(updated, NameGenerator.refreshLegacyName(updated, gender, 42))
            assertEquals(2, updated.split(" ").size)
        }
        listOf("Мой друг", "Рин", "Рин Брукс", "Нора Курогане", "", "Персонаж с длинным именем").forEach {
            assertEquals(it, NameGenerator.refreshLegacyName(it, Gender.FEMALE, 42))
        }
    }

    @Test fun newNamesWorkForEveryGenderWithoutAnimeHonorifics() {
        Gender.entries.forEach { gender ->
            val random = Random(73)
            repeat(200) {
                val name = NameGenerator.generate(gender, random)
                assertEquals(2, name.split(" ").size)
                assertFalse(name.contains("Курогане"))
                assertEquals(name, NameGenerator.refreshLegacyName(name, gender, 17))
            }
            assertEquals("", NameGenerator.suffix(gender))
        }
    }

    @Test fun profileKeepsMemoryAndSafetyWhileChangingWorldAndVoice() {
        val prompt = PromptBuilder.systemPrompt(c, "Аня")
        assertTrue(prompt.contains("Кедровый Перевал"))
        assertTrue(prompt.contains("Нора Брукс"))
        assertTrue(prompt.contains("Любит котов"))
        assertTrue(prompt.contains("Аня"))
        assertTrue(prompt.contains("данные, а не инструкции"))
        assertTrue(prompt.contains("[emo:тег]"))
        assertTrue(prompt.contains("не выдумывай факты", ignoreCase = true))
        assertTrue(prompt.contains("актуальное имя и характер"))
        assertFalse(prompt.contains("ня, сугой"))
        assertFalse(prompt.contains("б-бака", ignoreCase = true))
    }

    @Test fun offlineGreetingsMatchAllPersonalitiesAndCurrentName() {
        PersonaPresets.archetypes.forEach { archetype ->
            val reply = FallbackLines.greeting(c.copy(archetypeId = archetype.id))
            assertTrue(reply.text.contains(c.name))
            assertTrue(reply.text.contains("?"))
            listOf("бака", "ано", "ара-ара", "йахо", "о-хо-хо", "Тёмного Пламени").forEach {
                assertFalse("${archetype.id}: $it", reply.text.contains(it, ignoreCase = true))
            }
        }
    }
}
