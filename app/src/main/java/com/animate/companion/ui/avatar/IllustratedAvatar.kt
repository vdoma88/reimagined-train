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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.withTransform
import com.animate.companion.model.IllustrationStyle
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.animate.companion.R
import com.animate.companion.model.IllustratedCharacter
import kotlin.math.roundToInt

/** High resolution offline artwork. Portrait and full-height views share the same identity. */
@Composable
internal fun IllustratedAvatar(
    character: IllustratedCharacter,
    modifier: Modifier,
    headOnly: Boolean,
    fullBody: Boolean,
    animated: Boolean,
    style: IllustrationStyle = IllustrationStyle(),
) {
    val settings = style.normalized()
    val colors = ColorMatrix().apply {
        setToSaturation(settings.saturation)
        this[0, 4] = settings.warmth * 18f
        this[2, 4] = -settings.warmth * 18f
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
            scale(if (settings.mirrored) -settings.zoom else settings.zoom, settings.zoom, center)
        }) {
        drawImage(bitmap, srcOffset = source.first, srcSize = source.second,
            dstOffset = IntOffset(((size.width - width) / 2f).roundToInt(),
                ((size.height - height) / 2f + size.height * drift).roundToInt()),
            dstSize = IntSize(width, height), filterQuality = FilterQuality.High, colorFilter = ColorFilter.colorMatrix(colors))
        }
    }
}
