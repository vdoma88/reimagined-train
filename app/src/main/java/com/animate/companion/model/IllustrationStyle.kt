package com.animate.companion.model

import kotlinx.serialization.Serializable

/** Non-destructive presentation controls; source artwork and custom avatar are retained. */
@Serializable
data class IllustrationStyle(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val saturation: Float = 1f,
    val warmth: Float = 0f,
    val mirrored: Boolean = false,
    val motion: Boolean = true,
) {
    fun normalized() = copy(
        zoom = zoom.safe(0.8f, 1.5f, 1f),
        offsetX = offsetX.safe(-0.2f, 0.2f, 0f),
        offsetY = offsetY.safe(-0.2f, 0.2f, 0f),
        saturation = saturation.safe(0f, 1.5f, 1f),
        warmth = warmth.safe(-1f, 1f, 0f),
    )
}

private fun Float.safe(min: Float, max: Float, default: Float) =
    if (isFinite()) coerceIn(min, max) else default
