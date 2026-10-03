package com.animate.companion

import com.animate.companion.model.IllustrationStyle
import com.animate.companion.model.IllustrationDetails
import com.animate.companion.model.IllustrationExpression
import com.animate.companion.model.IllustrationAccent
import com.animate.companion.model.IllustrationAccessory
import com.animate.companion.model.IllustrationLayerCategory
import com.animate.companion.model.IllustrationLayerRegistry
import com.animate.companion.model.Appearance
import com.animate.companion.model.AppearancePresets
import com.animate.companion.model.Gender
import com.animate.companion.model.IllustratedCharacters
import org.junit.Assert.*
import org.junit.Test

class IllustratedCharactersTest {
    @Test fun layerRegistryUsesStableCompatibleIdsAndRejectsWrongCategories() {
        IllustrationLayerCategory.entries.forEach { category ->
            listOf("classic", "modern", "adventure").forEach { characterId ->
                assertTrue(IllustrationLayerRegistry.forCharacter(characterId, category).isNotEmpty())
            }
        }
        assertTrue(IllustrationLayerRegistry.assets.map { it.id }.distinct().size == IllustrationLayerRegistry.assets.size)

        val details = IllustrationDetails(
            expressionLayerId = "expression.bright",
            hairLayerId = "hair.cool",
            outfitLayerId = "outfit.violet",
            accessoryLayerId = "accessory.star_pin"
        ).normalized()
        assertEquals(IllustrationExpression.BRIGHT, details.resolvedExpression())
        assertEquals(IllustrationAccent.COOL, details.resolvedHairAccent())
        assertEquals(IllustrationAccent.VIOLET, details.resolvedOutfitAccent())
        assertEquals(IllustrationAccessory.STAR_PIN, details.resolvedAccessory())

        val invalid = IllustrationDetails(hairLayerId = "accessory.star_pin").normalized()
        assertNull(invalid.hairLayerId)
    }

    @Test fun detailLayersPersistAndLegacyJsonGetsNeutralDefaults() {
        val details = IllustrationDetails(
            expression = IllustrationExpression.BRIGHT,
            hairAccent = IllustrationAccent.COOL,
            outfitAccent = IllustrationAccent.VIOLET,
            accessory = IllustrationAccessory.STAR_PIN,
            blush = true
        )
        val original = Appearance(illustrationId = "classic", illustrationDetails = details)
        val restored = Appearance.fromJson(original.toJson())
        assertEquals(details, restored.illustrationDetails)

        val legacy = Appearance.fromJson("""{"illustrationId":"classic"}""")
        assertEquals(IllustrationDetails(), legacy.illustrationDetails)
    }

    @Test fun editorSettingsPersistWithoutChangingCustomAvatar() {
        val original = Appearance(hairColor = 7, eyeColor = 4)
        val style = IllustrationStyle(
            zoom = 1.2f,
            offsetX = -0.1f,
            rotation = 3f,
            saturation = 0.5f,
            warmth = 0.3f,
            brightness = 0.08f,
            contrast = 1.12f,
            mirrored = true,
            motion = false
        )
        val edited = Appearance.fromJson(original.copy(illustrationId = "modern", illustrationStyle = style).toJson())
        assertEquals(style, edited.illustrationStyle)
        assertEquals(original, edited.copy(illustrationId = null, illustrationStyle = IllustrationStyle()))
        assertEquals(IllustrationStyle(), Appearance.fromJson("""{"illustrationId":"classic"}""").illustrationStyle)
    }

    @Test fun malformedControlsCannotMakeArtworkUnrenderable() {
        val style = IllustrationStyle(
            zoom = Float.NaN,
            offsetX = Float.POSITIVE_INFINITY,
            offsetY = -99f,
            rotation = 99f,
            saturation = 99f,
            warmth = -99f,
            brightness = Float.NEGATIVE_INFINITY,
            contrast = 9f
        ).normalized()
        assertEquals(1f, style.zoom, 0f)
        assertEquals(0f, style.offsetX, 0f)
        assertEquals(-0.25f, style.offsetY, 0f)
        assertEquals(8f, style.rotation, 0f)
        assertEquals(1.6f, style.saturation, 0f)
        assertEquals(-1f, style.warmth, 0f)
        assertEquals(0f, style.brightness, 0f)
        assertEquals(1.35f, style.contrast, 0f)
    }

    @Test fun stylePresetsStayInsideSafeRenderingRanges() {
        val presets = listOf(
            IllustrationStyle.NATURAL,
            IllustrationStyle.SOFT,
            IllustrationStyle.WARM,
            IllustrationStyle.MANGA,
            IllustrationStyle.CINEMATIC
        )
        presets.forEach { assertEquals(it, it.normalized()) }
        assertNotEquals(IllustrationStyle.NATURAL, IllustrationStyle.MANGA)
        assertNotEquals(IllustrationStyle.NATURAL, IllustrationStyle.CINEMATIC)
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
