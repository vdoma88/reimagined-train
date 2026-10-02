package com.animate.companion.model

/** Curated combinations use existing preset indexes; saved appearance JSON stays compatible. */
data class StudioLook(val label: String, val description: String, private val style: Appearance) {
    fun applyTo(current: Appearance, gender: Gender): Appearance {
        val hair = style.hairStyle.takeIf { gender in AppearancePresets.hairStyles[it].genders }
            ?: if (gender == Gender.MALE) 8 else 14
        return style.copy(skinTone = current.skinTone, faceShape = current.faceShape, hairStyle = hair)
    }
}

object StudioLooks {
    val all = listOf(
        StudioLook("Сакура-айдол", "Клубничный розовый, бантик и звёздный взгляд",
            Appearance(hairStyle = 1, hairColor = 0, eyeStyle = 6, eyeColor = 3, accessory = 1, outfit = 8, outfitColor = 2, ahoge = true)),
        StudioLook("Мятный нэко", "Пушистые ушки, уютное худи и кошачья улыбка",
            Appearance(hairStyle = 14, hairColor = 7, eyeStyle = 4, eyeColor = 5, mouth = 1, ears = 1, accessory = 8, outfit = 2, outfitColor = 6, fang = true)),
        StudioLook("Лунный маг", "Серебро, аметист и маленькие созвездия",
            Appearance(hairStyle = 2, hairColor = 2, eyeStyle = 12, eyeColor = 3, ears = 4, accessory = 12, outfit = 14, outfitColor = 5)),
        StudioLook("Кицунэ", "Медовые пряди, лисьи ушки и праздничное кимоно",
            Appearance(hairStyle = 13, hairColor = 10, eyeStyle = 9, eyeColor = 4, mouth = 4, ears = 2, accessory = 10, outfit = 4, outfitColor = 4)),
        StudioLook("Облачный зайка", "Небесные оттенки, мягкий свитер и цветок",
            Appearance(hairStyle = 14, hairColor = 1, eyeStyle = 2, eyeColor = 5, ears = 3, accessory = 5, outfit = 12, outfitColor = 1)),
        StudioLook("Лавандовый лофай", "Наушники, сонный взгляд и любимое худи",
            Appearance(hairStyle = 2, bangs = 2, hairColor = 6, eyeStyle = 5, eyeColor = 7, accessory = 4, outfit = 2, outfitColor = 5, ahoge = true)),
    )
}
