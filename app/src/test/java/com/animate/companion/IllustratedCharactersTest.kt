package com.animate.companion

import com.animate.companion.model.IllustrationStyle
import com.animate.companion.model.Appearance
import com.animate.companion.model.AppearancePresets
import com.animate.companion.model.Gender
import com.animate.companion.model.IllustratedCharacters
import org.junit.Assert.*
import org.junit.Test

class IllustratedCharactersTest {
    @Test fun editorSettingsPersistWithoutChangingCustomAvatar() {
        val original = Appearance(hairColor = 7, eyeColor = 4)
        val style = IllustrationStyle(zoom = 1.2f, offsetX = -0.1f, saturation = 0.5f,
            warmth = 0.3f, mirrored = true, motion = false)
        val edited = Appearance.fromJson(original.copy(illustrationId = "modern", illustrationStyle = style).toJson())
        assertEquals(style, edited.illustrationStyle)
        assertEquals(original, edited.copy(illustrationId = null, illustrationStyle = IllustrationStyle()))
        assertEquals(IllustrationStyle(), Appearance.fromJson("""{"illustrationId":"classic"}""").illustrationStyle)
    }

    @Test fun malformedControlsCannotMakeArtworkUnrenderable() {
        val style = IllustrationStyle(zoom = Float.NaN, offsetX = Float.POSITIVE_INFINITY,
            offsetY = -99f, saturation = 99f, warmth = -99f).normalized()
        assertEquals(1f, style.zoom, 0f)
        assertEquals(0f, style.offsetX, 0f)
        assertEquals(-0.2f, style.offsetY, 0f)
        assertEquals(1.5f, style.saturation, 0f)
        assertEquals(-1f, style.warmth, 0f)
    }

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
