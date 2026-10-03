package com.animate.companion.model

import kotlinx.serialization.Serializable

/** Non-destructive presentation controls; source artwork and custom avatar are retained. */
@Serializable
data class IllustrationStyle(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f,
    val saturation: Float = 1f,
    val warmth: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val mirrored: Boolean = false,
    val motion: Boolean = true,
) {
    fun normalized() = copy(
        zoom = zoom.safe(0.8f, 1.6f, 1f),
        offsetX = offsetX.safe(-0.25f, 0.25f, 0f),
        offsetY = offsetY.safe(-0.25f, 0.25f, 0f),
        rotation = rotation.safe(-8f, 8f, 0f),
        saturation = saturation.safe(0f, 1.6f, 1f),
        warmth = warmth.safe(-1f, 1f, 0f),
        brightness = brightness.safe(-0.35f, 0.35f, 0f),
        contrast = contrast.safe(0.7f, 1.35f, 1f),
    )

    companion object {
        val NATURAL = IllustrationStyle()
        val SOFT = IllustrationStyle(saturation = 0.88f, warmth = 0.14f, brightness = 0.06f, contrast = 0.92f)
        val WARM = IllustrationStyle(saturation = 1.08f, warmth = 0.42f, brightness = 0.04f, contrast = 1.06f)
        val MANGA = IllustrationStyle(saturation = 1.18f, warmth = -0.04f, brightness = 0.05f, contrast = 1.18f)
        val CINEMATIC = IllustrationStyle(saturation = 0.86f, warmth = 0.18f, brightness = -0.03f, contrast = 1.24f)
    }
}

private fun Float.safe(min: Float, max: Float, default: Float) =
    if (isFinite()) coerceIn(min, max) else default
