package com.animate.companion.ui.avatar

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.animate.companion.R
import com.animate.companion.model.IllustratedCharacter
import com.animate.companion.model.IllustrationAccent
import com.animate.companion.model.IllustrationAccessory
import com.animate.companion.model.IllustrationDetails
import com.animate.companion.model.IllustrationExpression
import com.animate.companion.model.IllustrationStyle
import kotlin.math.roundToInt

/** High resolution offline artwork plus independent non-destructive detail layers. */
@Composable
internal fun IllustratedAvatar(
    character: IllustratedCharacter,
    modifier: Modifier,
    headOnly: Boolean,
    fullBody: Boolean,
    animated: Boolean,
    style: IllustrationStyle = IllustrationStyle(),
    details: IllustrationDetails = IllustrationDetails(),
) {
    val settings = style.normalized()
    val colors = ColorMatrix().apply {
        setToSaturation(settings.saturation)
        val contrastOffset = (1f - settings.contrast) * 128f
        val brightnessOffset = settings.brightness * 255f
        for (row in 0..2) {
            for (column in 0..3) this[row, column] = this[row, column] * settings.contrast
            this[row, 4] = contrastOffset + brightnessOffset
        }
        this[0, 4] += settings.warmth * 18f
        this[2, 4] -= settings.warmth * 18f
    }
    val resource = when (character.id) {
        "modern" -> R.drawable.character_modern
        "adventure" -> R.drawable.character_adventure
        else -> R.drawable.character_classic
    }
    val bitmap = ImageBitmap.imageResource(resource)
    val drift = if (animated && settings.motion && !headOnly) {
        val transition = rememberInfiniteTransition(label = "illustrationBreathing")
        val value by transition.animateFloat(0f, 0.006f,
            infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "illustrationDrift")
        value
    } else 0f

    Canvas(modifier.clipToBounds().semantics { contentDescription = character.title }) {
        val source = when {
            headOnly -> IntOffset((bitmap.width * 0.28f).roundToInt(), 0) to
                IntSize((bitmap.width * 0.44f).roundToInt(), (bitmap.height * 0.293f).roundToInt())
            fullBody -> IntOffset.Zero to IntSize(bitmap.width, bitmap.height)
            else -> IntOffset((bitmap.width * 0.18f).roundToInt(), 0) to
                IntSize((bitmap.width * 0.64f).roundToInt(), (bitmap.height * 0.46f).roundToInt())
        }
        val scale = minOf(size.width / source.second.width, size.height / source.second.height)
        val width = (source.second.width * scale).roundToInt().coerceAtLeast(1)
        val height = (source.second.height * scale).roundToInt().coerceAtLeast(1)

        withTransform({
            translate(size.width * settings.offsetX, size.height * settings.offsetY)
            rotate(settings.rotation, center)
            scale(if (settings.mirrored) -settings.zoom else settings.zoom, settings.zoom, center)
        }) {
            drawImage(
                bitmap,
                srcOffset = source.first,
                srcSize = source.second,
                dstOffset = IntOffset(((size.width - width) / 2f).roundToInt(),
                    ((size.height - height) / 2f + size.height * drift).roundToInt()),
                dstSize = IntSize(width, height),
                filterQuality = FilterQuality.High,
                colorFilter = ColorFilter.colorMatrix(colors)
            )
            drawDetailLayers(details, headOnly, fullBody)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDetailLayers(
    details: IllustrationDetails,
    headOnly: Boolean,
    fullBody: Boolean,
) {
    val faceY = if (fullBody) size.height * 0.18f else size.height * 0.30f
    val faceX = size.width * 0.5f
    val faceRadius = size.minDimension * if (headOnly) 0.13f else 0.085f

    accentColor(details.resolvedHairAccent())?.let { color ->
        drawCircle(
            color = color.copy(alpha = 0.12f),
            radius = faceRadius * 1.75f,
            center = Offset(faceX, faceY - faceRadius * 0.35f)
        )
    }

    if (!headOnly) {
        accentColor(details.resolvedOutfitAccent())?.let { color ->
            val top = if (fullBody) size.height * 0.36f else size.height * 0.57f
            drawRect(
                color = color.copy(alpha = 0.10f),
                topLeft = Offset(size.width * 0.31f, top),
                size = androidx.compose.ui.geometry.Size(size.width * 0.38f, size.height * 0.24f)
            )
        }
    }

    when (details.resolvedExpression()) {
        IllustrationExpression.NEUTRAL -> Unit
        IllustrationExpression.SOFT -> {
            drawCircle(Color(0xFFFF9EB5).copy(alpha = 0.16f), faceRadius * 0.24f,
                Offset(faceX - faceRadius * 0.62f, faceY + faceRadius * 0.38f))
            drawCircle(Color(0xFFFF9EB5).copy(alpha = 0.16f), faceRadius * 0.24f,
                Offset(faceX + faceRadius * 0.62f, faceY + faceRadius * 0.38f))
        }
        IllustrationExpression.BRIGHT -> {
            drawCircle(Color.White.copy(alpha = 0.55f), faceRadius * 0.08f,
                Offset(faceX - faceRadius * 0.42f, faceY - faceRadius * 0.08f))
            drawCircle(Color.White.copy(alpha = 0.55f), faceRadius * 0.08f,
                Offset(faceX + faceRadius * 0.42f, faceY - faceRadius * 0.08f))
        }
        IllustrationExpression.COOL -> {
            drawLine(
                Color.White.copy(alpha = 0.35f),
                Offset(faceX - faceRadius * 0.55f, faceY - faceRadius * 0.34f),
                Offset(faceX - faceRadius * 0.18f, faceY - faceRadius * 0.38f),
                strokeWidth = faceRadius * 0.055f,
                cap = StrokeCap.Round
            )
        }
    }

    if (details.blush) {
        drawCircle(Color(0xFFFF7898).copy(alpha = 0.18f), faceRadius * 0.30f,
            Offset(faceX - faceRadius * 0.72f, faceY + faceRadius * 0.34f))
        drawCircle(Color(0xFFFF7898).copy(alpha = 0.18f), faceRadius * 0.30f,
            Offset(faceX + faceRadius * 0.72f, faceY + faceRadius * 0.34f))
    }

    val pin = Offset(faceX + faceRadius * 0.95f, faceY - faceRadius * 0.78f)
    when (details.resolvedAccessory()) {
        IllustrationAccessory.NONE -> Unit
        IllustrationAccessory.SPARKLE -> {
            drawLine(Color.White.copy(alpha = 0.85f), pin + Offset(-faceRadius * 0.2f, 0f),
                pin + Offset(faceRadius * 0.2f, 0f), faceRadius * 0.045f, cap = StrokeCap.Round)
            drawLine(Color.White.copy(alpha = 0.85f), pin + Offset(0f, -faceRadius * 0.2f),
                pin + Offset(0f, faceRadius * 0.2f), faceRadius * 0.045f, cap = StrokeCap.Round)
        }
        IllustrationAccessory.HAIR_PIN -> {
            drawLine(Color(0xFFFFD166), pin + Offset(-faceRadius * 0.28f, -faceRadius * 0.08f),
                pin + Offset(faceRadius * 0.22f, faceRadius * 0.12f), faceRadius * 0.09f, cap = StrokeCap.Round)
        }
        IllustrationAccessory.STAR_PIN -> {
            drawCircle(Color(0xFFFFD166), faceRadius * 0.13f, pin)
            drawCircle(Color.White.copy(alpha = 0.75f), faceRadius * 0.045f, pin)
        }
    }
}

private fun accentColor(accent: IllustrationAccent): Color? = when (accent) {
    IllustrationAccent.NONE -> null
    IllustrationAccent.WARM -> Color(0xFFFFA85A)
    IllustrationAccent.COOL -> Color(0xFF53C7FF)
    IllustrationAccent.ROSE -> Color(0xFFFF6F9F)
    IllustrationAccent.VIOLET -> Color(0xFFAA7CFF)
}
