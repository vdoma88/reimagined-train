package com.animate.companion

import com.animate.companion.model.Appearance
import com.animate.companion.model.IllustrationAccessory
import com.animate.companion.model.IllustrationDetails
import com.animate.companion.model.IllustrationExpression
import com.animate.companion.model.IllustrationStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudioSaveTest {
    private val stored = Appearance(illustrationId = "classic", hairColor = 3)

    @Test
    fun savesDetailsAsWellAsStyle() {
        val draft = stored.copy(
            illustrationStyle = IllustrationStyle(zoom = 1.2f, mirrored = true),
            illustrationDetails = IllustrationDetails(expression = IllustrationExpression.BRIGHT, accessory = IllustrationAccessory.STAR_PIN, blush = true),
        )
        val merged = Appearance.mergeStudioEdit(stored, draft)!!
        assertEquals(IllustrationExpression.BRIGHT, merged.illustrationDetails.expression)
        assertEquals(IllustrationAccessory.STAR_PIN, merged.illustrationDetails.accessory)
        assertEquals(true, merged.illustrationDetails.blush)
        assertEquals(1.2f, merged.illustrationStyle.zoom)
        assertEquals(3, merged.hairColor) // untouched fields stay as stored
    }

    @Test
    fun refusesWhenArtworkChangedMeanwhile() {
        assertNull(Appearance.mergeStudioEdit(stored.copy(illustrationId = "modern"), stored))
    }

    @Test
    fun storedCharactersRoundTripThroughJson() {
        val merged = Appearance.mergeStudioEdit(stored, stored.copy(illustrationDetails = IllustrationDetails(blush = true)))!!
        assertEquals(merged, Appearance.fromJson(merged.toJson()))
    }

    @Test
    fun legacyJsonWithoutIllustrationLoads() {
        val legacy = Appearance.fromJson("""{"skinTone":2,"hairStyle":1,"hairColor":4,"ahoge":true}""")
        assertNull(legacy.illustrationId)
        assertEquals(4, legacy.hairColor)
        assertEquals(IllustrationDetails(), legacy.illustrationDetails)
    }
}
