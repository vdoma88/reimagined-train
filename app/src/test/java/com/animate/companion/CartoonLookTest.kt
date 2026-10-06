package com.animate.companion

import com.animate.companion.model.*
import org.junit.Assert.*
import org.junit.Test

class CartoonLookTest {
    @Test fun savedWardrobeSurvivesStudioMergeAndJson() {
        val original = Appearance(illustrationId = CartoonLook.STYLE_ID, hairColor = 7)
        val look = CartoonLook(hair = 3, face = 2, eyes = 1, mouth = 1, top = 2,
            bottom = 2, boots = false, accessory = 3)
        val merged = requireNotNull(Appearance.mergeStudioEdit(original, original.copy(cartoonLook = look)))
        val restored = Appearance.fromJson(merged.toJson())
        assertEquals(look, restored.cartoonLook)
        assertEquals(7, restored.hairColor)
        assertEquals(look.describe(), AppearancePresets.describe(restored, Gender.NEUTRAL))
        assertNull(Appearance.mergeStudioEdit(original.selectIllustration("classic"), merged))
        assertNull(restored.selectIllustration("modern").cartoonLook)
        assertEquals(restored, restored.selectIllustration(CartoonLook.STYLE_ID))
    }

    @Test fun legacyCharactersKeepTheirArtworkAndInvalidIndexesAreClamped() {
        val legacy = Appearance.fromJson("""{"illustrationId":"classic","hairColor":7}""")
        assertNull(legacy.cartoonLook)
        assertEquals("classic", legacy.illustrationId)
        assertEquals(7, legacy.hairColor)
        val look = CartoonLook(hair = 99, eyes = -9, top = -1, bottom = 999, accessory = 99).normalized()
        assertEquals(7, look.hair)
        assertEquals(0, look.eyes)
        assertEquals(0, look.top)
        assertEquals(4, look.bottom)
        assertEquals(5, look.accessory)
        assertTrue(look.layers().indexOf("hair.7") > look.layers().indexOf("eyes.0"))
        assertTrue(look.layers().indexOf("beanie") > look.layers().indexOf("hair.7"))
        assertTrue(look.layers(speaking = true, mouthOpen = true).contains("mouth.1"))
        assertTrue(look.copy(mouth = 1).layers(speaking = true, mouthOpen = false).contains("mouth.0"))
    }
}
