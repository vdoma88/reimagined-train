package com.animate.companion.model

import kotlinx.serialization.Serializable

@Serializable
data class IllustrationDetails(
    val expression: IllustrationExpression = IllustrationExpression.NEUTRAL,
    val hairAccent: IllustrationAccent = IllustrationAccent.NONE,
    val outfitAccent: IllustrationAccent = IllustrationAccent.NONE,
    val accessory: IllustrationAccessory = IllustrationAccessory.NONE,
    val blush: Boolean = false,
    val expressionLayerId: String? = null,
    val hairLayerId: String? = null,
    val outfitLayerId: String? = null,
    val accessoryLayerId: String? = null,
) {
    fun normalized() = copy(
        expressionLayerId = validLayer(expressionLayerId, IllustrationLayerCategory.EXPRESSION),
        hairLayerId = validLayer(hairLayerId, IllustrationLayerCategory.HAIR),
        outfitLayerId = validLayer(outfitLayerId, IllustrationLayerCategory.OUTFIT),
        accessoryLayerId = validLayer(accessoryLayerId, IllustrationLayerCategory.ACCESSORY),
    )

    fun resolvedExpression(): IllustrationExpression =
        (IllustrationLayerRegistry.find(expressionLayerId)?.fallback as? IllustrationLayerFallback.Expression)?.value
            ?: expression

    fun resolvedHairAccent(): IllustrationAccent =
        (IllustrationLayerRegistry.find(hairLayerId)?.fallback as? IllustrationLayerFallback.Accent)?.value
            ?: hairAccent

    fun resolvedOutfitAccent(): IllustrationAccent =
        (IllustrationLayerRegistry.find(outfitLayerId)?.fallback as? IllustrationLayerFallback.Accent)?.value
            ?: outfitAccent

    fun resolvedAccessory(): IllustrationAccessory =
        (IllustrationLayerRegistry.find(accessoryLayerId)?.fallback as? IllustrationLayerFallback.Accessory)?.value
            ?: accessory

    private fun validLayer(id: String?, category: IllustrationLayerCategory): String? {
        if (id == null) return null
        val asset = IllustrationLayerRegistry.find(id) ?: return null
        return id.takeIf { asset.category == category }
    }
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
