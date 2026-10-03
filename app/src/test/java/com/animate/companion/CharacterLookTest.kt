package com.animate.companion

import com.animate.companion.model.*
import org.junit.Assert.*
import org.junit.Test

class CharacterLookTest {
    @Test fun legacyArtworkIsNotAutomaticallyReplaced() {
        val old = Appearance.fromJson("""{"illustrationId":"classic","hairColor":3}""")
        assertFalse(old.useCharacterLook)
        assertNull(old.characterLook)
        assertEquals(3, old.hairColor)
    }
    @Test fun studioSavesRealPartsAndPreservesOriginalCustomization() {
        val original = Appearance(illustrationId = "modern", hairColor = 5)
        val look = CharacterLook(hair = 4, face = 2, skin = 4, eyes = 3, eyeColor = 5, outfit = 3, outfitColor = 7, accessory = 3, freckles = true)
        val saved = Appearance.mergeStudioEdit(original, original.copy(characterLook = look, useCharacterLook = true))!!
        assertEquals(look, Appearance.fromJson(saved.toJson()).characterLook)
        assertTrue(saved.useCharacterLook)
        assertEquals(5, saved.hairColor)
        val restored = Appearance.mergeStudioEdit(saved, saved.copy(useCharacterLook = false))!!
        assertFalse(restored.useCharacterLook)
        assertEquals(look, restored.characterLook)
        assertNull(Appearance.mergeStudioEdit(saved.copy(illustrationId = "classic"), saved))
    }
    @Test fun invalidValuesCannotSelectMissingLayers() {
        val n = CharacterLook(hair = 900, outfit = -1, eyes = 44, skin = -2).normalized()
        assertEquals(5, n.hair); assertEquals(0, n.outfit); assertEquals(3, n.eyes); assertEquals(0, n.skin)
    }
    @Test fun changingPartsReplacesTheirLayersWithoutChangingOtherParts() {
        val a = CharacterLook()
        val b = a.copy(hair = 3, eyes = 2, face = 1, outfit = 2)
        assertTrue(b.layers().containsAll(listOf("back.3", "front.3", "face.1", "eyes.2", "outfit.2")))
        assertFalse(b.layers().contains("outfit.0"))
        assertTrue(b.layers().contains("body"))
        assertTrue(b.layers(talking = true).contains("mouth.2"))
        assertTrue(b.layers(Emotion.HAPPY).contains("mouth.1"))
    }
}
