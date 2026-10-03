package com.animate.companion.model

import kotlinx.serialization.Serializable

@Serializable
data class IllustrationDetails(
    val expression: IllustrationExpression = IllustrationExpression.NEUTRAL,
    val hairAccent: IllustrationAccent = IllustrationAccent.NONE,
    val outfitAccent: IllustrationAccent = IllustrationAccent.NONE,
    val accessory: IllustrationAccessory = IllustrationAccessory.NONE,
    val blush: Boolean = false,
) {
    fun normalized() = this
}

@Serializable
enum class IllustrationExpression(val label: String) {
    NEUTRAL("Спокойно"),
    SOFT("Мягко"),
    BRIGHT("Радостно"),
    COOL("Уверенно"),
}

@Serializable
enum class IllustrationAccent(val label: String) {
    NONE("Без акцента"),
    WARM("Тёплый"),
    COOL("Холодный"),
    ROSE("Розовый"),
    VIOLET("Фиолетовый"),
}

@Serializable
enum class IllustrationAccessory(val label: String) {
    NONE("Нет"),
    SPARKLE("Искра"),
    HAIR_PIN("Заколка"),
    STAR_PIN("Звезда"),
}
