package com.animate.companion.model

import kotlinx.serialization.Serializable

/** Independent parts of an editable illustration. Null in Appearance preserves original artwork. */
@Serializable
data class CharacterLook(
    val hair: Int = 0, val hairColor: Int = 0, val face: Int = 0,
    val skin: Int = 0, val eyes: Int = 0, val eyeColor: Int = 0,
    val brows: Int = 0, val mouth: Int = 0, val outfit: Int = 0,
    val outfitColor: Int = 0, val accessory: Int = 0,
    val blush: Boolean = true, val freckles: Boolean = false,
) {
    fun normalized() = copy(
        hair = hair.coerceIn(0, CharacterLookCatalog.hairstyles.lastIndex), hairColor = hairColor.coerceIn(0, 7),
        face = face.coerceIn(0, 2), skin = skin.coerceIn(0, 5),
        eyes = eyes.coerceIn(0, 3), eyeColor = eyeColor.coerceIn(0, 5),
        brows = brows.coerceIn(0, 2), mouth = mouth.coerceIn(0, 2),
        outfit = outfit.coerceIn(0, CharacterLookCatalog.outfits.lastIndex), outfitColor = outfitColor.coerceIn(0, 7),
        accessory = accessory.coerceIn(0, 4),
    )

    /** Ordered, replacement layers; never paint over the flattened original image. */
    fun layers(emotion: Emotion = Emotion.NEUTRAL, talking: Boolean = false): List<String> {
        val n = normalized()
        return buildList {
            add("back.${n.hair}"); add("body"); add("outfit.${n.outfit}"); add("shoes")
            add("face.${n.face}"); add("eyes.${n.eyes}")
            add("brows.${if (emotion == Emotion.ANGRY) 1 else n.brows}")
            add("mouth.${if (talking || emotion == Emotion.SURPRISED) 2 else if (emotion in listOf(Emotion.HAPPY, Emotion.LAUGH, Emotion.LOVE)) 1 else n.mouth}")
            if (n.blush || emotion == Emotion.SHY) add("blush")
            if (n.freckles) add("freckles")
            add("front.${n.hair}"); add("accessory.${n.accessory}")
        }
    }

    companion object {
        fun forCharacter(id: String?) = when (id) {
            "modern" -> CharacterLook(hair = 5, hairColor = 1, face = 2, eyes = 1, eyeColor = 4, brows = 1, outfit = 1, outfitColor = 1, blush = false)
            "adventure" -> CharacterLook(hair = 2, hairColor = 2, eyes = 2, eyeColor = 2, outfit = 2, outfitColor = 2, accessory = 1, freckles = true)
            else -> CharacterLook(accessory = 2)
        }
    }
}
