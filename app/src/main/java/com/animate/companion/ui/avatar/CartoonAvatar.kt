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
import com.animate.companion.model.CartoonRig
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
                // Generated source artwork is much larger than any display size; decode it at half
                // resolution while preserving the original transparent PNG in the asset catalog.
                val largeArt = spec.file.startsWith("43_") || spec.file.startsWith("44_") ||
                    spec.file.startsWith("45_") || spec.file.startsWith("46_")
                val options = BitmapFactory.Options().apply { inSampleSize = if (largeArt) 2 else 1 }
                val bitmap = context.assets.open("cartoon/${spec.file}").use {
                    requireNotNull(BitmapFactory.decodeStream(it, null, options)) { "Cannot decode ${spec.file}" }
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
    mouthProgress: Float = if (mouthOpen) 1f else 0f,
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
    val honeyHairBack = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(122, 62, 27)
    } }
    Canvas(modifier.clipToBounds()) {
        val viewHeight = CartoonRig.frameHeight(headOnly, fullBody)
        val scale = min(size.width / CartoonRig.FRAME_WIDTH, size.height / viewHeight) * s.zoom
        drawIntoCanvas { canvas ->
            val c = canvas.nativeCanvas
            val layer = c.saveLayer(RectF(0f, 0f, size.width, size.height), filterPaint)
            c.translate(size.width / 2f + s.offsetX * size.width, size.height / 2f + s.offsetY * size.height)
            c.rotate(s.rotation)
            c.scale(if (s.mirrored) -scale else scale, scale)
            c.translate(-CartoonRig.NECK_X, -viewHeight / 2f - CartoonRig.FRAME_TOP + breath)
            // The long wavy cut leaves space below the tapered jaw. A back-hair layer
            // fills that gap behind the face and body, sharing the head's rig transform.
            if (n.hair == 9) {
                val hairFrame = c.save()
                c.translate(CartoonRig.NECK_X, CartoonRig.NECK_Y)
                c.scale(n.headScaleX(gender), n.headScaleY(gender))
                c.translate(-CartoonRig.NECK_X, -CartoonRig.NECK_Y)
                c.drawOval(RectF(105f, 140f, 295f, 305f), honeyHairBack)
                c.restoreToCount(hairFrame)
            }
            for (key in n.layers(speaking, if (speaking) false else mouthOpen, gender)) {
                val sprite = sprites.getValue(key)
                val spec = sprite.spec
                val saved = c.save()
                // Use the same transformed destination as the geometry regression checks.
                val box = CartoonRig.bounds(key, spec, n, gender)
                val sx = (box.right - box.left) / spec.width
                val sy = (box.bottom - box.top) / spec.height
                c.translate(box.left, box.top)
                c.scale(sx, sy)
                c.translate(-spec.x, -spec.y)
                val bitmapClip = c.save()
                // Mask only the base shorts, not the wrists or the first exposed thigh pixels.
                // The selected waistband overlaps the top hem in this same body coordinate space.
                if (key == "body") c.clipOutRect(136f, 403f, 268f, 462f)
                if (key.startsWith("eyes.") && blink > 0f) {
                    val visibleHeight = 27f + (spec.height - 27f) * (1f - blink.coerceIn(0f, 1f))
                    c.clipRect(spec.x, spec.y, spec.x + spec.width, spec.y + visibleHeight)
                }
                fun drawSprite(part: CartoonArtwork.Sprite, alpha: Int) {
                    val item = part.spec
                    imagePaint.alpha = alpha.coerceIn(0, 255)
                    c.drawBitmap(part.bitmap, Rect(0, item.sourceY, part.bitmap.width, part.bitmap.height),
                        RectF(item.x, item.y, item.x + item.width, item.y + item.height), imagePaint)
                }
                if (key.startsWith("mouth.") && speaking) {
                    val amount = mouthProgress.coerceIn(0f, 1f)
                    if (amount < 0.02f) {
                        drawSprite(sprites.getValue("mouth.0"), 255)
                    } else {
                        // Morph one mouth instead of overlaying two outlines, which ghosts at half-open.
                        val mouthFrame = c.save()
                        c.scale(1f, 0.22f + 0.78f * amount, 200f, 251f)
                        drawSprite(sprites.getValue("mouth.1"), 255)
                        c.restoreToCount(mouthFrame)
                    }
                } else {
                    drawSprite(sprite, 255)
                }
                imagePaint.alpha = 255
                c.restoreToCount(bitmapClip)
                if (key.startsWith("eyes.") && blink > 0f) {
                    ink.alpha = (blink.coerceIn(0f, 1f) * 255).toInt()
                    c.drawArc(RectF(126f, 175f, 184f, 205f), 0f, 180f, false, ink)
                    c.drawArc(RectF(215f, 175f, 274f, 205f), 0f, 180f, false, ink)
                    ink.alpha = 255
                }
                c.restoreToCount(saved)
            }
            c.restoreToCount(layer)
        }
    }
}
