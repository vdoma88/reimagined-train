package com.animate.companion

import com.animate.companion.model.Appearance
import com.animate.companion.model.AppearancePresets
import com.animate.companion.model.Gender
import com.animate.companion.model.StudioLooks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioLooksTest {
    @Test fun looksPreserveIdentityAndStayCompatibleWithEveryGender() {
        val current = Appearance(skinTone = 5, faceShape = 2)
        for (gender in Gender.entries) for (look in StudioLooks.all) {
            val a = look.applyTo(current, gender)
            assertEquals(current.skinTone, a.skinTone)
            assertEquals(current.faceShape, a.faceShape)
            assertTrue(gender in AppearancePresets.hairStyles[a.hairStyle].genders)
            assertTrue(a.bangs in AppearancePresets.bangs.indices)
            assertTrue(a.hairColor in AppearancePresets.hairColors.indices)
            assertTrue(a.eyeStyle in AppearancePresets.eyeStyles.indices)
            assertTrue(a.eyeColor in AppearancePresets.eyeColors.indices)
            assertTrue(a.mouth in AppearancePresets.mouths.indices)
            assertTrue(a.ears in AppearancePresets.ears.indices)
            assertTrue(a.accessory in AppearancePresets.accessories.indices)
            assertTrue(a.outfit in AppearancePresets.outfits.indices)
            assertTrue(a.outfitColor in AppearancePresets.outfitColors.indices)
            assertEquals(a, Appearance.fromJson(a.toJson()))
        }
    }
}
