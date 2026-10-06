package com.animate.companion.model

import kotlinx.serialization.Serializable
import kotlin.random.Random

/** Independent raster parts. Missing data uses the default kit without migrating old avatars. */
@Serializable
data class CartoonLook(
    val hair: Int = 0,
    val face: Int = 0,
    val eyes: Int = 0,
    val mouth: Int = 0,
    val top: Int = 0,
    val bottom: Int = 0,
    val boots: Boolean = true,
    val accessory: Int = 0,
    val shoeStyle: Int = 0,
) {
    fun normalized() = copy(hair = hair.coerceIn(0, hairNames.lastIndex), face = face.coerceIn(0, 2),
        eyes = eyes.coerceIn(0, 1), mouth = mouth.coerceIn(0, 1), top = top.coerceIn(0, topNames.lastIndex),
        bottom = bottom.coerceIn(0, bottomNames.lastIndex), accessory = accessory.coerceIn(0, 5), shoeStyle = shoeStyle.coerceIn(0, 1))

    fun layers(speaking: Boolean = false, mouthOpen: Boolean = false): List<String> {
        val n = normalized()
        return buildList {
            add("body"); add("bottom.${n.bottom}")
            if (n.boots) add(if (n.shoeStyle == 1) "sneakers" else "boots")
            add("top.${n.top}"); add("face"); add("eyes.${n.eyes}")
            add("mouth.${if (speaking) if (mouthOpen) 1 else 0 else n.mouth}")
            add("hair.${n.hair}")
            if (n.accessory == 1 || n.accessory == 3 || n.accessory == 5) add("glasses")
            if (n.accessory == 2 || n.accessory == 3) add("cap")
            if (n.accessory == 4 || n.accessory == 5) add("beanie")
        }
    }

    fun describe(): String {
        val n = normalized()
        return "мультяшный герой с большими овальными глазами; ${hairNames[n.hair]}; ${topNames[n.top]}; ${bottomNames[n.bottom]}" +
            (if (!n.boots) "; босиком" else if (n.shoeStyle == 1) "; красные кеды" else "; походные ботинки") +
            when (n.accessory) { 1 -> "; круглые очки"; 2 -> "; кепка"; 3 -> "; круглые очки и кепка"; 4 -> "; красная шапка"; 5 -> "; очки и красная шапка"; else -> "" }
    }

    companion object {
        const val STYLE_ID = "woodland"
        fun random(random: Random = Random.Default) = CartoonLook(
            hair = random.nextInt(hairNames.size), face = random.nextInt(3), eyes = random.nextInt(2),
            mouth = random.nextInt(2), top = random.nextInt(topNames.size), bottom = random.nextInt(bottomNames.size),
            boots = random.nextBoolean(), accessory = random.nextInt(6), shoeStyle = random.nextInt(2),
        )
        val hairNames = listOf("Короткие каштановые", "Рыжие волны", "Тёмные кудри", "Светлый хвост", "Чёрные с бирюзовой прядью", "Рыжее каре", "Две косы", "Серебристые лохмы")
        val topNames = listOf("Бирюзовая куртка", "Красный свитер", "Жилет исследователя", "Клетчатая рубашка", "Жёлтое худи", "Джинсовая куртка", "Свитшот с призраком", "Пальто исследователя")
        val bottomNames = listOf("Джинсы", "Шорты карго", "Бордовая юбка", "Походные брюки", "Синяя юбка")
    }
}

/** Pixel positions in a common 400×640 composition, independent of viewport size. */
@Serializable
data class CartoonSpriteSpec(
    val file: String,
    val x: Float, val y: Float,
    val width: Float, val height: Float,
    val sourceY: Int = 0,
)
