package com.animate.companion

import com.animate.companion.audio.LofiTracks
import com.animate.companion.model.*
import org.junit.Assert.*
import org.junit.Test

class ForestRefreshTest {
    @Test fun oldCharactersUseRasterLooksWithoutLosingStoredIdentity() {
        assertEquals(listOf(CartoonLook.STYLE_ID), IllustratedCharacters.all.map { it.id })
        listOf(null, "classic", "modern", "adventure").forEach { id ->
            val stored = Appearance(illustrationId = id, hairColor = 7)
            assertEquals(stored, Appearance.fromJson(stored.toJson()))
            assertTrue(stored.resolvedCartoonLook().layers().contains("face"))
            assertEquals(stored.resolvedCartoonLook().describe(), AppearancePresets.describe(stored, Gender.NEUTRAL))
            val edited = stored.copy(cartoonLook = CartoonLook(hair = 7, top = 7, bottom = 4, shoeStyle = 1, accessory = 5))
            val merged = requireNotNull(Appearance.mergeStudioEdit(stored, edited))
            assertEquals(edited.cartoonLook, Appearance.fromJson(merged.toJson()).resolvedCartoonLook())
            assertEquals(7, merged.hairColor)
        }
    }

    @Test fun newWardrobeAndCombinedAccessoriesSurviveSave() {
        for (hair in CartoonLook.hairNames.indices) for (top in CartoonLook.topNames.indices) {
            val look = CartoonLook(hair = hair, top = top, bottom = 4, shoeStyle = 1, accessory = 5)
            val stored = Appearance(illustrationId = CartoonLook.STYLE_ID, cartoonLook = look)
            assertEquals(look, Appearance.fromJson(stored.toJson()).resolvedCartoonLook())
            assertTrue(look.layers().containsAll(listOf("hair.$hair", "top.$top", "bottom.4", "sneakers", "glasses", "beanie")))
            assertFalse(look.layers().contains("boots"))
        }
    }

    @Test fun soundtrackHasThreeDistinctOriginalStructuredMelodies() {
        assertEquals(3, LofiTracks.all.size)
        assertEquals(3, LofiTracks.all.map { it.title }.distinct().size)
        assertEquals(3, LofiTracks.all.map { it.melody.toList() }.distinct().size)
        LofiTracks.all.forEach { track ->
            assertEquals(32, track.melody.size)
            assertTrue(track.melody.any { it == -1 })
            assertTrue(track.melody.filter { it >= 0 }.distinct().size >= 6)
            assertTrue(track.melody.all { it == -1 || it in 60..96 })
        }
    }
}
