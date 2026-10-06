package com.animate.companion.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

/** Indexes into the preset lists in [AppearancePresets]. Stored as JSON in the database. */
@Serializable
data class Appearance(
    val illustrationId: String? = null,
    val characterLook: CharacterLook? = null,
    val cartoonLook: CartoonLook? = null,
    val useCharacterLook: Boolean = false,
    val illustrationStyle: IllustrationStyle = IllustrationStyle(),
    val illustrationDetails: IllustrationDetails = IllustrationDetails(),
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
    /** Gallery replacement starts with that artwork; toggling inside the editor retains its draft. */
    fun selectIllustration(id: String?): Appearance = if (id == illustrationId) this else copy(
        illustrationId = id, useCharacterLook = false, characterLook = null, cartoonLook = null,
    )

    /** Legacy IDs remain in stored JSON, but all displayed avatars use the new raster kit. */
    fun resolvedCartoonLook(gender: Gender? = null): CartoonLook = (cartoonLook ?: when (illustrationId) {
        "classic" -> CartoonLook(hair = 1, top = 3, bottom = 2)
        "modern" -> CartoonLook(hair = 4, top = 5, shoeStyle = 1)
        "adventure" -> CartoonLook(hair = 1, top = 2, bottom = 1, accessory = 1)
        else -> if (gender == null) CartoonLook() else CartoonLook.random(
            Random(hairStyle + 31 * hairColor + 173 * faceShape + 997 * outfit), gender,
        )
    }).normalized()

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Applies a studio draft to the stored appearance: style *and* detail layers.
         * Returns null when the artwork changed meanwhile, so an old editor never overwrites it.
         */
        fun mergeStudioEdit(current: Appearance, draft: Appearance): Appearance? {
            if (current.illustrationId != draft.illustrationId) return null
            return current.copy(
                characterLook = draft.characterLook?.normalized(),
                cartoonLook = draft.cartoonLook?.normalized(),
                useCharacterLook = draft.useCharacterLook && draft.characterLook != null,
                illustrationStyle = draft.illustrationStyle.normalized(),
                illustrationDetails = draft.illustrationDetails.normalized(),
            )
        }

        fun fromJson(s: String): Appearance =
            runCatching { json.decodeFromString(serializer(), s) }.getOrDefault(Appearance())

        fun random(gender: Gender, random: Random = Random.Default): Appearance {
            val p = AppearancePresets
            val styles = p.hairStyles.indices.filter { gender in p.hairStyles[it].genders }
            return Appearance(
                cartoonLook = CartoonLook.random(random, gender),
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
    MALE("Мужчина", "♂"),
    // Legacy serialized value; never offered when creating a new character.
    NEUTRAL("Девушка", "♀");

    companion object {
        val selectable = listOf(FEMALE, MALE)
    }
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
    LOVE("love", "восхищение", "😍"),
    THINKING("thinking", "задумчивость", "🤔");

    companion object {
        fun fromTag(tag: String?): Emotion =
            entries.firstOrNull { it.tag.equals(tag?.trim(), ignoreCase = true) } ?: NEUTRAL
    }
}

