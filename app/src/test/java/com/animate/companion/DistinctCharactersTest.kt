package com.animate.companion

import com.animate.companion.data.CharacterEntity
import com.animate.companion.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.random.Random

class DistinctCharactersTest {
    @Test fun onlyMaleAndFemaleAreOfferedAndOldNeutralRecordsStillOpen() {
        assertEquals(listOf(Gender.FEMALE, Gender.MALE), Gender.selectable)
        val old = CharacterEntity(name = "Друг", gender = "NEUTRAL", appearanceJson = "{}",
            archetypeId = "genki", professionId = "student", directionId = "friend")
        assertEquals(Gender.FEMALE, old.genderEnum)
    }

    @Test fun genderSelectsDifferentFaceAndEyeArtworkAndBodyProportions() {
        val look = CartoonLook(face = 2, eyes = 1, build = 2)
        assertTrue(look.layers(gender = Gender.MALE).containsAll(listOf("face.male.2", "eyes.male.1")))
        assertTrue(look.layers(gender = Gender.FEMALE).containsAll(listOf("face.female.2", "eyes.female.1")))
        assertTrue(look.bodyScaleX(Gender.MALE) > look.bodyScaleX(Gender.FEMALE))
        assertTrue(look.headScaleX(Gender.MALE) < look.headScaleX(Gender.FEMALE))
        assertEquals(look, Appearance.fromJson(Appearance(cartoonLook = look).toJson()).cartoonLook)
    }

    @Test fun newAppearancesHaveVariedRasterLooksAndKeepChosenGender() {
        Gender.selectable.forEach { gender ->
            val looks = (0..100).map { Appearance.random(gender, Random(it)).resolvedCartoonLook(gender) }
            assertTrue(looks.distinct().size > 90)
            assertEquals(setOf(0, 1, 2), looks.map { it.face }.toSet())
            assertEquals(setOf(0, 1, 2), looks.map { it.build }.toSet())
            if (gender == Gender.MALE) assertTrue(looks.all { it.hair in listOf(0, 2, 4, 7) && it.bottom in listOf(0, 1, 3) })
        }
        val old = Appearance(hairStyle = 3, hairColor = 4)
        assertEquals(old.resolvedCartoonLook(Gender.MALE), old.resolvedCartoonLook(Gender.MALE))
    }

    @Test fun everyGenderAndEveryConstructorItemUsesExistingRegisteredSprites() {
        val root = File("src/main/assets").takeIf { it.exists() } ?: File("app/src/main/assets")
        val specs = Json.decodeFromString<Map<String, CartoonSpriteSpec>>(File(root, "cartoon_layers.json").readText())
        val faces = specs.filterKeys { it.startsWith("face.") }.values
        assertEquals(6, faces.size)
        assertEquals(6, faces.map { it.file }.distinct().size)
        Gender.selectable.forEach { gender ->
            repeat(500) { seed ->
                val look = CartoonLook.random(Random(seed), gender)
                look.layers(gender = gender).forEach { key ->
                    val spec = requireNotNull(specs[key]) { key }
                    assertTrue(File(root, "cartoon/${spec.file}").isFile)
                    assertTrue(spec.width > 0 && spec.height > 0)
                    val body = key == "body" || key.startsWith("top.") || key.startsWith("bottom.") || key == "boots" || key == "sneakers"
                    val sx = if (body) look.bodyScaleX(gender) else look.headScaleX(gender)
                    val sy = if (body) look.bodyScaleY(gender) else look.headScaleY(gender)
                    val left = 200 + (spec.x - 200) * sx
                    val top = 267 + (spec.y - 267) * sy
                    assertTrue("$key left", left >= 0)
                    assertTrue("$key right", left + spec.width * sx <= 400)
                    assertTrue("$key top", top >= 0)
                    assertTrue("$key bottom", top + spec.height * sy <= 640)
                }
            }
        }
        assertTrue(specs.getValue("beanie").y + specs.getValue("beanie").height < specs.getValue("glasses").y)
        assertEquals(150f, specs.getValue("top.2").height)
    }
}
