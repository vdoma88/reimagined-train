package com.animate.companion.model

enum class IllustrationLayerCategory {
    EXPRESSION,
    HAIR,
    OUTFIT,
    ACCESSORY,
}

data class IllustrationLayerAsset(
    val id: String,
    val title: String,
    val category: IllustrationLayerCategory,
    val compatibleCharacters: Set<String>,
    val resourceName: String? = null,
    val fallback: IllustrationLayerFallback,
)

sealed interface IllustrationLayerFallback {
    data class Expression(val value: IllustrationExpression) : IllustrationLayerFallback
    data class Accent(val value: IllustrationAccent) : IllustrationLayerFallback
    data class Accessory(val value: IllustrationAccessory) : IllustrationLayerFallback
}

/**
 * Stable layer IDs used by the editor and persistence layer.
 *
 * resourceName is deliberately nullable: bundled transparent PNG/WebP assets can be added
 * gradually. Until then the renderer uses [fallback] and the saved ID remains unchanged.
 */
object IllustrationLayerRegistry {
    val assets = listOf(
        IllustrationLayerAsset(
            id = "expression.neutral",
            title = "Спокойно",
            category = IllustrationLayerCategory.EXPRESSION,
            compatibleCharacters = allCharacters(),
            fallback = IllustrationLayerFallback.Expression(IllustrationExpression.NEUTRAL),
        ),
        IllustrationLayerAsset(
            id = "expression.soft",
            title = "Мягко",
            category = IllustrationLayerCategory.EXPRESSION,
            compatibleCharacters = allCharacters(),
            resourceName = "layer_expression_soft",
            fallback = IllustrationLayerFallback.Expression(IllustrationExpression.SOFT),
        ),
        IllustrationLayerAsset(
            id = "expression.bright",
            title = "Радостно",
            category = IllustrationLayerCategory.EXPRESSION,
            compatibleCharacters = allCharacters(),
            resourceName = "layer_expression_bright",
            fallback = IllustrationLayerFallback.Expression(IllustrationExpression.BRIGHT),
        ),
        IllustrationLayerAsset(
            id = "expression.cool",
            title = "Уверенно",
            category = IllustrationLayerCategory.EXPRESSION,
            compatibleCharacters = allCharacters(),
            resourceName = "layer_expression_cool",
            fallback = IllustrationLayerFallback.Expression(IllustrationExpression.COOL),
        ),
        IllustrationLayerAsset(
            id = "hair.none",
            title = "Без акцента",
            category = IllustrationLayerCategory.HAIR,
            compatibleCharacters = allCharacters(),
            fallback = IllustrationLayerFallback.Accent(IllustrationAccent.NONE),
        ),
        IllustrationLayerAsset(
            id = "hair.warm",
            title = "Тёплый",
            category = IllustrationLayerCategory.HAIR,
            compatibleCharacters = allCharacters(),
            resourceName = "layer_hair_warm",
            fallback = IllustrationLayerFallback.Accent(IllustrationAccent.WARM),
        ),
        IllustrationLayerAsset(
            id = "hair.cool",
            title = "Холодный",
            category = IllustrationLayerCategory.HAIR,
            compatibleCharacters = allCharacters(),
            resourceName = "layer_hair_cool",
            fallback = IllustrationLayerFallback.Accent(IllustrationAccent.COOL),
        ),
        IllustrationLayerAsset(
            id = "hair.rose",
            title = "Розовый",
            category = IllustrationLayerCategory.HAIR,
            compatibleCharacters = allCharacters(),
            resourceName = "layer_hair_rose",
            fallback = IllustrationLayerFallback.Accent(IllustrationAccent.ROSE),
        ),
        IllustrationLayerAsset(
            id = "hair.violet",
            title = "Фиолетовый",
            category = IllustrationLayerCategory.HAIR,
            compatibleCharacters = allCharacters(),
            resourceName = "layer_hair_violet",
            fallback = IllustrationLayerFallback.Accent(IllustrationAccent.VIOLET),
        ),
        IllustrationLayerAsset(
            id = "outfit.none",
            title = "Без акцента",
            category = IllustrationLayerCategory.OUTFIT,
            compatibleCharacters = allCharacters(),
            fallback = IllustrationLayerFallback.Accent(IllustrationAccent.NONE),
        ),
        IllustrationLayerAsset(
            id = "outfit.warm",
            title = "Тёплый",
            category = IllustrationLayerCategory.OUTFIT,
            compatibleCharacters = allCharacters(),
            resourceName = "layer_outfit_warm",
            fallback = IllustrationLayerFallback.Accent(IllustrationAccent.WARM),
        ),
        IllustrationLayerAsset(
            id = "outfit.cool",
            title = "Холодный",
            category = IllustrationLayerCategory.OUTFIT,
            compatibleCharacters = allCharacters(),
            resourceName = "layer_outfit_cool",
            fallback = IllustrationLayerFallback.Accent(IllustrationAccent.COOL),
        ),
        IllustrationLayerAsset(
            id = "outfit.rose",
            title = "Розовый",
            category = IllustrationLayerCategory.OUTFIT,
            compatibleCharacters = allCharacters(),
            resourceName = "layer_outfit_rose",
            fallback = IllustrationLayerFallback.Accent(IllustrationAccent.ROSE),
        ),
        IllustrationLayerAsset(
            id = "outfit.violet",
            title = "Фиолетовый",
            category = IllustrationLayerCategory.OUTFIT,
            compatibleCharacters = allCharacters(),
            resourceName = "layer_outfit_violet",
            fallback = IllustrationLayerFallback.Accent(IllustrationAccent.VIOLET),
        ),
        IllustrationLayerAsset(
            id = "accessory.none",
            title = "Нет",
            category = IllustrationLayerCategory.ACCESSORY,
            compatibleCharacters = allCharacters(),
            fallback = IllustrationLayerFallback.Accessory(IllustrationAccessory.NONE),
        ),
        IllustrationLayerAsset(
            id = "accessory.sparkle",
            title = "Искра",
            category = IllustrationLayerCategory.ACCESSORY,
            compatibleCharacters = allCharacters(),
            resourceName = "layer_accessory_sparkle",
            fallback = IllustrationLayerFallback.Accessory(IllustrationAccessory.SPARKLE),
        ),
        IllustrationLayerAsset(
            id = "accessory.hair_pin",
            title = "Заколка",
            category = IllustrationLayerCategory.ACCESSORY,
            compatibleCharacters = allCharacters(),
            resourceName = "layer_accessory_hair_pin",
            fallback = IllustrationLayerFallback.Accessory(IllustrationAccessory.HAIR_PIN),
        ),
        IllustrationLayerAsset(
            id = "accessory.star_pin",
            title = "Звезда",
            category = IllustrationLayerCategory.ACCESSORY,
            compatibleCharacters = allCharacters(),
            resourceName = "layer_accessory_star_pin",
            fallback = IllustrationLayerFallback.Accessory(IllustrationAccessory.STAR_PIN),
        ),
    )

    fun forCharacter(characterId: String?, category: IllustrationLayerCategory): List<IllustrationLayerAsset> =
        assets.filter { it.category == category && characterId != null && characterId in it.compatibleCharacters }

    fun find(id: String?): IllustrationLayerAsset? = assets.firstOrNull { it.id == id }

    private fun allCharacters() = setOf("classic", "modern", "adventure")
}
