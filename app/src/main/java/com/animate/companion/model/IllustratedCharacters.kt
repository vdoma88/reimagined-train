package com.animate.companion.model

data class IllustratedCharacter(
    val id: String,
    val title: String,
    val subtitle: String,
    val gender: Gender,
    val description: String,
)

/** Stable asset IDs persisted inside Appearance JSON. Null keeps the editable avatar. */
object IllustratedCharacters {
    val all = listOf(
        IllustratedCharacter(CartoonLook.STYLE_ID, "Лесные приключения", "Нарисованные слои · свободная сборка", Gender.NEUTRAL, CartoonLook().describe()),
        IllustratedCharacter("classic", "Классическое аниме", "Тёплая рисовка · мягкая светотень", Gender.FEMALE,
            "каштановые волнистые волосы с тёмно-синим бантом, карие глаза, кремовая блузка, тёмно-синий жакет и длинная плиссированная юбка, коричневые сапоги"),
        IllustratedCharacter("modern", "Современная манга", "Детальные пряди · городской стиль", Gender.MALE,
            "чёрные растрёпанные волосы с бирюзовыми прядями, серые глаза, чёрно-белая куртка с бирюзовыми вставками, наушники, цепочки, брюки карго и кроссовки"),
        IllustratedCharacter("adventure", "Мульт-приключение", "Выразительный силуэт · тёплые цвета", Gender.FEMALE,
            "рыжие волосы, круглые очки, веснушки, зелёная куртка, светлая футболка, шорты поверх тёмных легинсов, коричневая сумка и походные ботинки"),
    )

    fun find(id: String?): IllustratedCharacter? = all.firstOrNull { it.id == id }
}

