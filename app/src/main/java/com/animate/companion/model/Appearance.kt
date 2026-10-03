package com.animate.companion.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

/** Indexes into the preset lists in [AppearancePresets]. Stored as JSON in the database. */
@Serializable
data class Appearance(
    val illustrationId: String? = null,
    val skinTone: Int = 0,
    val faceShape: Int = 0,
    val hairStyle: Int = 0,
    val bangs: Int = 1,
    val hairColor: Int = 0,
    val eyeStyle: Int = 0,
    val eyeColor: Int = 0,
    val mouth: Int = 0,
    val ears: Int = 0,
    val accessory: Int = 0,
    val outfit: Int = 0,
    val outfitColor: Int = 0,
    val ahoge: Boolean = false,
    val blush: Boolean = true,
    val fang: Boolean = false,
    val beautyMark: Boolean = false,
    val heterochromia: Boolean = false,
    val bandaid: Boolean = false,
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(s: String): Appearance =
            runCatching { json.decodeFromString(serializer(), s) }.getOrDefault(Appearance())

        fun random(gender: Gender, random: Random = Random.Default): Appearance {
            val p = AppearancePresets
            val styles = p.hairStyles.indices.filter { gender in p.hairStyles[it].genders }
            return Appearance(
                skinTone = random.nextInt(p.skinTones.size),
                faceShape = random.nextInt(p.faceShapes.size),
                hairStyle = styles.random(random),
                bangs = random.nextInt(p.bangs.size),
                hairColor = random.nextInt(p.hairColors.size),
                eyeStyle = random.nextInt(p.eyeStyles.size),
                eyeColor = random.nextInt(p.eyeColors.size),
                mouth = random.nextInt(p.mouths.size),
                ears = if (random.nextFloat() < 0.4f) random.nextInt(p.ears.size) else 0,
                accessory = if (random.nextFloat() < 0.6f) random.nextInt(p.accessories.size) else 0,
                outfit = random.nextInt(p.outfits.size),
                outfitColor = random.nextInt(p.outfitColors.size),
                ahoge = random.nextFloat() < 0.35f,
                blush = random.nextFloat() < 0.7f,
                fang = random.nextFloat() < 0.25f,
                beautyMark = random.nextFloat() < 0.15f,
                heterochromia = random.nextFloat() < 0.1f,
                bandaid = random.nextFloat() < 0.1f,
            )
        }
    }
}

enum class Gender(val label: String, val emoji: String) {
    FEMALE("Девушка", "♀"),
    MALE("Парень", "♂"),
    NEUTRAL("Андрогин", "✦"),
}

/** Facial expression driven by the `[emo:...]` tag in replies. */
enum class Emotion(val tag: String, val label: String, val emoji: String) {
    NEUTRAL("neutral", "спокойствие", "🙂"),
    HAPPY("happy", "радость", "😊"),
    LAUGH("laugh", "смех", "😆"),
    SHY("shy", "смущение", "😳"),
    ANGRY("angry", "злость", "😤"),
    SAD("sad", "грусть", "😢"),
    SURPRISED("surprised", "удивление", "😲"),
    SMUG("smug", "ехидство", "😏"),
    LOVE("love", "влюблённость", "😍"),
    THINKING("thinking", "задумчивость", "🤔");

    companion object {
        fun fromTag(tag: String?): Emotion =
            entries.firstOrNull { it.tag.equals(tag?.trim(), ignoreCase = true) } ?: NEUTRAL
    }
}
