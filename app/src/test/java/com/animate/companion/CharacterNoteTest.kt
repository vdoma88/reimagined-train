package com.animate.companion

import com.animate.companion.model.CharacterNoteGenerator
import com.animate.companion.ui.create.CreatorViewModel
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class CharacterNoteTest {
    @Test fun descriptionsFitTheCreatorLimitAndDiceAvoidsRepeats() {
        val random = Random(17)
        var previous = ""
        val seen = mutableSetOf<String>()
        repeat(1000) {
            val note = CharacterNoteGenerator.generate(previous, random)
            assertTrue(note.length in 1..400)
            assertNotEquals(previous, note)
            seen += note
            previous = note
        }
        assertTrue(seen.size > 500)
    }

    @Test fun creatorIncludesGeneratedNoteAndKeepsManualEditsAndClearing() {
        val vm = CreatorViewModel()
        assertTrue(vm.note.isNotBlank())
        assertEquals(vm.note, vm.draft().extraNote)
        vm.note = "Мой персонаж любит море"
        assertEquals("Мой персонаж любит море", vm.draft().extraNote)
        vm.note = ""
        assertEquals("", vm.draft().extraNote)
    }

    @Test fun rerollingDescriptionLeavesOtherCharacterChoicesAlone() {
        val vm = CreatorViewModel()
        val before = vm.draft()
        vm.randomizeNote()
        assertNotEquals(before.extraNote, vm.note)
        val after = vm.draft()
        assertEquals(before.copy(extraNote = vm.note), after.copy(
            createdAt = before.createdAt, lastMessageAt = before.lastMessageAt,
        ))
    }
}
