package com.animate.companion.model

/** Curated combinations only change clothes and hair; face and body choices stay personal. */
data class CartoonStylePreset(val title: String, val hair: Int, val top: Int, val bottom: Int, val accessory: Int, val shoe: Int) {
    fun applyTo(base: CartoonLook) = base.copy(hair = hair, top = top, bottom = bottom,
        accessory = accessory, boots = true, shoeStyle = shoe).normalized()
}

object CartoonLooks {
    val all = listOf(
        CartoonStylePreset("Звёздный следопыт", 8, 8, 3, 0, 1),
        CartoonStylePreset("Лесная мастерская", 9, 9, 0, 1, 0),
        CartoonStylePreset("Тайны архива", 5, 9, 4, 1, 0),
        CartoonStylePreset("Ночной радиоклуб", 8, 5, 0, 0, 1),
        CartoonStylePreset("Дорога к озеру", 3, 4, 1, 2, 1),
        CartoonStylePreset("Городские легенды", 7, 8, 0, 4, 1),
        CartoonStylePreset("Пикник у костра", 9, 3, 2, 0, 0),
        CartoonStylePreset("Лесная экспедиция", 2, 7, 3, 2, 0),
    )
}
