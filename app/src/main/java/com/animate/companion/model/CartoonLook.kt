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
    val build: Int = 0,
) {
    fun normalized() = copy(hair = hair.coerceIn(0, hairNames.lastIndex), face = face.coerceIn(0, 2),
        eyes = eyes.coerceIn(0, 1), mouth = mouth.coerceIn(0, 1), top = top.coerceIn(0, topNames.lastIndex),
        bottom = bottom.coerceIn(0, bottomNames.lastIndex), accessory = accessory.coerceIn(0, 5), shoeStyle = shoeStyle.coerceIn(0, 1), build = build.coerceIn(0, 2))

    fun layers(speaking: Boolean = false, mouthOpen: Boolean = false, gender: Gender? = null): List<String> {
        val n = normalized()
        return buildList {
            add("body")
            if (n.boots) add(if (n.shoeStyle == 1) "sneakers" else "boots")
            // Trouser cuffs and skirt hems belong in front of the shoe shafts.
            add("bottom.${n.bottom}")
            add("top.${n.top}")
            val family = if (gender == Gender.MALE) "male" else "female"
            add(if (gender == null) "face" else "face.$family.${n.face}")
            add(if (gender == null) "eyes.${n.eyes}" else "eyes.$family.${n.eyes}")
            add("mouth.${if (speaking) if (mouthOpen) 1 else 0 else n.mouth}")
            add("hair.${n.hair}")
            if (n.accessory == 1 || n.accessory == 3 || n.accessory == 5) add("glasses")
            if (n.accessory == 2 || n.accessory == 3) add("cap")
            if (n.accessory == 4 || n.accessory == 5) add("beanie")
        }
    }

    /** All body and wardrobe parts share this transform around the neck anchor. */
    fun bodyScaleX(gender: Gender) = (if (gender == Gender.MALE) 1.12f else 0.96f) *
        (if (normalized().build == 1) 1.06f else 1f)
    fun bodyScaleY(gender: Gender = Gender.FEMALE) =
        (if (gender == Gender.MALE) 1.07f else 1f) *
        (if (normalized().build == 2) if (gender == Gender.MALE) 1.04f else 1.07f else 1f)
    fun headScaleX(gender: Gender) = if (gender == Gender.MALE) 0.90f else 1f
    fun headScaleY(gender: Gender) = if (gender == Gender.MALE) 0.92f else 1f

    fun describe(gender: Gender? = null): String {
        val n = normalized()
        val identity = when (gender) {
            Gender.MALE -> "мужчина; ${maleFaceNames[n.face]}; выраженные брови и широкие плечи"
            Gender.FEMALE -> "девушка; ${femaleFaceNames[n.face]}; выразительные глаза с ресницами"
            else -> "мультяшный герой с большими овальными глазами"
        }
        return "$identity; ${buildNames[n.build]}; ${hairNames[n.hair]}; ${topNames[n.top]}; ${bottomNames[n.bottom]}" +
            (if (!n.boots) "; босиком" else if (n.shoeStyle == 1) "; красные кеды" else "; походные ботинки") +
            when (n.accessory) { 1 -> "; круглые очки"; 2 -> "; кепка"; 3 -> "; круглые очки и кепка"; 4 -> "; красная шапка"; 5 -> "; очки и красная шапка"; else -> "" }
    }

    companion object {
        const val STYLE_ID = "woodland"
        fun random(random: Random = Random.Default, gender: Gender = Gender.FEMALE) = CartoonLook(
            hair = (if (gender == Gender.MALE) listOf(0, 2, 4, 7) else listOf(1, 3, 5, 6)).random(random), face = random.nextInt(3), eyes = random.nextInt(2),
            mouth = random.nextInt(2), top = random.nextInt(topNames.size), bottom = (if (gender == Gender.MALE) listOf(0, 1, 3) else bottomNames.indices.toList()).random(random),
            boots = true, accessory = random.nextInt(6), shoeStyle = random.nextInt(2), build = random.nextInt(3),
        )
        val femaleFaceNames = listOf("Сердечком", "Круглое", "Удлинённое")
        val maleFaceNames = listOf("Квадратная челюсть", "Угловатое", "Широкое")
        val buildNames = listOf("Стройное", "Коренастое", "Высокое")
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
