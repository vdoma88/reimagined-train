package com.animate.companion

import com.animate.companion.data.CharacterEntity
import com.animate.companion.llm.PromptBuilder
import com.animate.companion.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class ExpandedCastTest {
    @Test fun presetsPreserveFaceAndBodyAndUseValidParts() {
        val base = CartoonLook(face = 2, eyes = 1, build = 2, mouth = 1)
        assertEquals(8, CartoonLooks.all.size)
        for (preset in CartoonLooks.all) {
            val look = preset.applyTo(base)
            assertEquals(2, look.face); assertEquals(1, look.eyes); assertEquals(2, look.build); assertEquals(1, look.mouth)
            assertEquals(preset.hair, look.hair); assertEquals(preset.top, look.top)
            assertTrue(look.layers(gender = Gender.FEMALE).containsAll(listOf("hair.${look.hair}", "top.${look.top}")))
            val a = Appearance(illustrationId = CartoonLook.STYLE_ID, cartoonLook = look)
            assertEquals(look, Appearance.fromJson(a.toJson()).resolvedCartoonLook(Gender.FEMALE))
        }
    }

    @Test fun newPersonalitiesAndPerksArePersistedAndReachCharacterPrompt() {
        val ids = listOf("cartographer", "stargazer", "crafty", "storykeeper")
        ids.forEach { assertEquals(it, PersonaPresets.archetype(it).id) }
        assertEquals(PersonaPresets.archetypes.size, PersonaPresets.archetypes.map { it.id }.distinct().size)
        var previous = ""
        val random = Random(107)
        repeat(100) {
            val note = CharacterNoteGenerator.generate(previous, random)
            assertNotEquals(previous, note)
            assertTrue(note.length <= 400)
            assertTrue(note.contains("Особенность:"))
            assertTrue(CharacterNoteGenerator.perks.any { note.contains(it) })
            val c = CharacterEntity(name = "Нора", gender = Gender.FEMALE.name,
                appearanceJson = Appearance().toJson(), archetypeId = ids[it % ids.size],
                professionId = "maid", directionId = "friend", extraNote = note)
            assertTrue(PromptBuilder.systemPrompt(c, "Аня").contains(note))
            previous = note
        }
    }
}
