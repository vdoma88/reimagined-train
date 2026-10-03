package com.animate.companion.ui.avatar

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.animate.companion.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class CharacterPath(
    val path: String, val fill: String = "none", val stroke: String = "ink",
    val width: Float = 1.5f, val tx: Float = 0f, val ty: Float = 0f,
    val sx: Float = 1f,
)

internal val hairPalette = listOf(0xFF654337,0xFF242733,0xFFAF5C32,0xFFC8A76D,0xFFE0DCE5,0xFFB45C83,0xFF756198,0xFF3C8589)
internal val skinPalette = listOf(0xFFF3D0B9,0xFFEBC3A5,0xFFD7A17F,0xFFB77C5B,0xFF8C5944,0xFF5C3B32)
internal val irisPalette = listOf(0xFF916739,0xFF478CA9,0xFF66894C,0xFF8D659F,0xFF88929F,0xFFB35F7D)
internal val clothPalette = listOf(0xFF344663,0xFF303543,0xFF647957,0xFFA86681,0xFF83719E,0xFFE1CDA8,0xFF538B91,0xFF964C4F)

private data class PaintedPath(val path: Path, val data: CharacterPath)

@Composable
internal fun LayeredCharacter(
    look: CharacterLook, style: IllustrationStyle, modifier: Modifier,
    headOnly: Boolean, fullBody: Boolean, emotion: Emotion,
    blink: Float, breath: Float, talking: Boolean,
) {
    val context = LocalContext.current
    val layers = remember(context) {
        val data = context.assets.open("character_layers.json").bufferedReader().use { it.readText() }
        Json.decodeFromString<Map<String, List<CharacterPath>>>(data).mapValues { (_, pieces) ->
            pieces.map { PaintedPath(PathParser().parsePathString(it.path).toPath(), it) }
        }
    }
    val n = look.normalized()
    val s = style.normalized()
    val palette = remember(n) {
        val skin = Color(skinPalette[n.skin]); val hair = Color(hairPalette[n.hairColor]); val cloth = Color(clothPalette[n.outfitColor])
        mapOf("skin" to skin, "skinShadow" to lerp(skin, Color(0xFF8E4845), .23f),
            "skinLight" to lerp(skin, Color.White, .3f), "hair" to hair,
            "hairShadow" to lerp(hair, Color(0xFF181729), .4f), "hairLight" to lerp(hair, Color(0xFFFFE1C0), .25f),
            "cloth" to cloth, "clothShadow" to lerp(cloth, Color(0xFF151627), .3f),
            "clothLight" to lerp(cloth, Color.White, .2f), "iris" to Color(irisPalette[n.eyeColor]),
            "ink" to Color(0xFF292431), "paper" to Color(0xFFFFF8EC), "white" to Color.White,
            "gold" to Color(0xFFD8B976), "leather" to Color(0xFF755346),
            "accent" to Color(0xFFC7758D), "lip" to Color(0xFFB97677), "blush" to Color(0xFFE4A199))
    }
    val matrix = remember(s) { ColorMatrix().apply {
        setToSaturation(s.saturation)
        for (row in 0..2) {
            for (column in 0..3) this[row,column] = this[row,column] * s.contrast
            this[row,4] = (1f - s.contrast) * 128f + s.brightness * 255f
        }
        this[0,4] += s.warmth * 18f; this[2,4] -= s.warmth * 18f
    } }
    val paint = remember(matrix) { Paint().apply { colorFilter = ColorFilter.colorMatrix(matrix) } }
    Canvas(modifier.clipToBounds().semantics { contentDescription = "Редактируемый персонаж" }) {
        val viewWidth = if (headOnly) 160f else 400f
        val viewHeight = if (headOnly) 170f else if (fullBody) 940f else 410f
        val k = minOf(size.width / viewWidth, size.height / viewHeight)
        drawContext.canvas.saveLayer(androidx.compose.ui.geometry.Rect(Offset.Zero, size), paint)
        try {
            withTransform({
                translate(size.width * s.offsetX, size.height * s.offsetY)
                rotate(s.rotation, center)
                scale(if (s.mirrored) -s.zoom else s.zoom, s.zoom, center)
                translate((size.width-viewWidth*k)/2f, (size.height-viewHeight*k)/2f)
                scale(k,k,Offset.Zero)
                translate(if (headOnly) -120f else 0f, (if (headOnly) -45f else 0f) + breath)
            }) {
                n.layers(emotion, talking).forEach { key ->
                    if (blink > .5f && key.startsWith("eyes.")) {
                        drawLine(palette.getValue("ink"), Offset(158f,138f), Offset(189f,138f), 2f, StrokeCap.Round)
                        drawLine(palette.getValue("ink"), Offset(211f,138f), Offset(242f,138f), 2f, StrokeCap.Round)
                    } else layers.getValue(key).forEach { piece ->
                        val d = piece.data
                        withTransform({ translate(d.tx,d.ty); scale(d.sx,1f,Offset.Zero) }) {
                            if (d.fill != "none") {
                                val c = palette.getValue(d.fill)
                                val shadow = when (d.fill) { "hair" -> palette.getValue("hairShadow"); "cloth" -> palette.getValue("clothShadow"); else -> c }
                                drawPath(piece.path, Brush.linearGradient(listOf(c,shadow), piece.path.getBounds().topLeft, piece.path.getBounds().bottomRight))
                            }
                            if (d.stroke != "none") drawPath(piece.path, palette.getValue(d.stroke), style = Stroke(d.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                    }
                }
            }
        } finally { drawContext.canvas.restore() }
    }
}
