package com.animate.companion.ui.avatar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import com.animate.companion.model.Gender
import com.animate.companion.model.CartoonLook
import com.animate.companion.model.CartoonSpriteSpec
import com.animate.companion.model.IllustrationStyle
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlin.math.min

/** Assets are retained once per process; no Activity or View is retained. */
internal object CartoonArtwork {
    data class Sprite(val spec: CartoonSpriteSpec, val bitmap: Bitmap)
    @Volatile private var cached: Map<String, Sprite>? = null
    fun load(context: Context): Map<String, Sprite> = cached ?: synchronized(this) {
        cached ?: run {
            val specs = context.assets.open("cartoon_layers.json").bufferedReader().use {
                Json.decodeFromString<Map<String, CartoonSpriteSpec>>(it.readText())
            }
            specs.mapValues { (_, spec) ->
                val bitmap = context.assets.open("cartoon/${spec.file}").use {
                    requireNotNull(BitmapFactory.decodeStream(it)) { "Cannot decode ${spec.file}" }
                }
                require(spec.sourceY in 0 until bitmap.height)
                Sprite(spec, bitmap)
            }.also { cached = it }
        }
    }
}

@Composable
internal fun CartoonAvatar(
    look: CartoonLook, style: IllustrationStyle, modifier: Modifier,
    headOnly: Boolean, fullBody: Boolean, blink: Float, breath: Float,
    speaking: Boolean, mouthOpen: Boolean, gender: Gender = Gender.FEMALE,
) {
    val context = LocalContext.current
    val sprites = remember { CartoonArtwork.load(context) }
    val n = look.normalized()
    val s = style.normalized()
    val filter = remember(s) {
        val saturation = ColorMatrix().apply { setSaturation(s.saturation) }
        val c = s.contrast
        val b = (1f - c) * 128f + s.brightness * 255f
        saturation.postConcat(ColorMatrix(floatArrayOf(
            c,0f,0f,0f,b + s.warmth * 18f,
            0f,c,0f,0f,b + s.warmth * 3f,
            0f,0f,c,0f,b - s.warmth * 18f,
            0f,0f,0f,1f,0f,
        )))
        ColorMatrixColorFilter(saturation)
    }
    val imagePaint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    val filterPaint = remember(filter) { Paint().apply { colorFilter = filter } }
    val ink = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(47, 29, 23)
        strokeWidth = 3.5f; strokeCap = Paint.Cap.ROUND; this.style = Paint.Style.STROKE
    } }
    Canvas(modifier.clipToBounds()) {
        val viewHeight = if (headOnly) 320f else if (fullBody) 640f else 440f
        val scale = min(size.width / 400f, size.height / viewHeight) * s.zoom
        drawIntoCanvas { canvas ->
            val c = canvas.nativeCanvas
            val layer = c.saveLayer(RectF(0f, 0f, size.width, size.height), filterPaint)
            c.translate(size.width / 2f + s.offsetX * size.width, size.height / 2f + s.offsetY * size.height)
            c.rotate(s.rotation)
            c.scale(if (s.mirrored) -scale else scale, scale)
            c.translate(-200f, -viewHeight / 2f + breath)
            for (key in n.layers(speaking, mouthOpen, gender)) {
                val sprite = sprites.getValue(key)
                val spec = sprite.spec
                val saved = c.save()
                if (key == "body" || key.startsWith("top.") || key.startsWith("bottom.") ||
                    key == "boots" || key == "sneakers") {
                    c.translate(200f, 267f)
                    c.scale(n.bodyScaleX(gender), n.bodyScaleY(gender))
                    c.translate(-200f, -267f)
                } else {
                    c.translate(200f, 267f)
                    c.scale(n.headScaleX(gender), n.headScaleY(gender))
                    c.translate(-200f, -267f)
                }
                val bitmapClip = c.save()
                // The base kit has a modesty garment. Hide it beneath the selected trousers/skirt.
                if (key == "body") c.clipOutRect(130f, 403f, 279f, 469f)
                if (key.startsWith("eyes.") && blink > 0.5f) {
                    c.clipRect(spec.x, spec.y, spec.x + spec.width, spec.y + 27f)
                }
                c.drawBitmap(sprite.bitmap, Rect(0, spec.sourceY, sprite.bitmap.width, sprite.bitmap.height),
                    RectF(spec.x, spec.y, spec.x + spec.width, spec.y + spec.height), imagePaint)
                c.restoreToCount(bitmapClip)
                if (key.startsWith("eyes.") && blink > 0.5f) {
                    c.drawArc(RectF(126f, 175f, 184f, 205f), 0f, 180f, false, ink)
                    c.drawArc(RectF(215f, 175f, 274f, 205f), 0f, 180f, false, ink)
                }
                c.restoreToCount(saved)
            }
            c.restoreToCount(layer)
        }
    }
}
