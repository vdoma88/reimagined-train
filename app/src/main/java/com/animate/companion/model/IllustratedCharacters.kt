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
        IllustratedCharacter(CartoonLook.STYLE_ID, "Лесные приключения", "42 детали · мужские и женские образы", Gender.FEMALE, CartoonLook().describe()),
    )

    fun find(id: String?): IllustratedCharacter? = all.firstOrNull { it.id == id }
}
