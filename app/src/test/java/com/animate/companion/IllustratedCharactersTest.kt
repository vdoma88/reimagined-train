package com.animate.companion

import com.animate.companion.model.Appearance
import com.animate.companion.model.AppearancePresets
import com.animate.companion.model.Gender
import com.animate.companion.model.IllustratedCharacters
import org.junit.Assert.*
import org.junit.Test

class IllustratedCharactersTest {
    @Test fun legacyAppearanceKeepsItsCustomizableIdentity() {
        val a = Appearance.fromJson("""{"hairStyle":5,"hairColor":8,"skinTone":4}""")
        assertNull(a.illustrationId)
        assertEquals(5, a.hairStyle)
        assertEquals(8, a.hairColor)
        assertEquals(4, a.skinTone)
    }

    @Test fun illustrationCanBeSelectedAndRemovedWithoutLosingCustomOptions() {
        val original = Appearance(hairColor = 7, eyeColor = 4, ears = 2, heterochromia = true)
        for (art in IllustratedCharacters.all) {
            val selected = Appearance.fromJson(original.copy(illustrationId = art.id).toJson())
            assertEquals(art.id, selected.illustrationId)
            assertEquals(original, selected.copy(illustrationId = null))
            assertEquals(art.description, AppearancePresets.describe(selected, Gender.NEUTRAL))
        }
        assertNull(IllustratedCharacters.find("unknown-future-art"))
    }
}
