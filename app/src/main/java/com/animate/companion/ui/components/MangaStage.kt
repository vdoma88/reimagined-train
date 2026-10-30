package com.animate.companion.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.animate.companion.model.Emotion
import com.animate.companion.ui.theme.Palette

/** Static, bounded decoration behind the avatar: no extra animation loop or touch target. */
@Composable
fun MangaStage(emotion: Emotion, modifier: Modifier = Modifier) {
    val accent = when (emotion) {
        Emotion.LOVE, Emotion.SHY -> Palette.Sakura
        Emotion.SAD, Emotion.THINKING -> Palette.Sky
        Emotion.ANGRY, Emotion.SURPRISED -> Palette.Peach
        else -> Palette.Lavender
    }
    val sceneColor by animateColorAsState(accent, animationSpec = tween(700), label = "sceneMood")
    Canvas(modifier) {
        val unit = size.minDimension / 100f
        if (unit <= 0f) return@Canvas
        val center = Offset(size.width / 2f, size.height * 0.49f)
        drawCircle(Brush.radialGradient(listOf(sceneColor.copy(alpha = 0.20f), Color.Transparent), center, 48f * unit), 48f * unit, center)
        drawCircle(sceneColor.copy(alpha = 0.22f), 37f * unit, center, style = Stroke(0.5f * unit))
        drawArc(sceneColor.copy(alpha = 0.4f), 205f, 112f, false,
            center - Offset(41f * unit, 41f * unit), Size(82f * unit, 82f * unit), style = Stroke(0.8f * unit))
        // Sparse halftone panels frame the silhouette without covering the face.
        for (side in listOf(-1f, 1f)) {
            for (row in 0..5) for (column in 0..2) {
                drawCircle(sceneColor.copy(alpha = 0.14f), 0.45f * unit,
                    center + Offset(side * (34f + column * 4f) * unit, (row * 5f - 10f) * unit))
            }
        }
        val marks = listOf(Triple(-36f, -25f, 2.5f), Triple(35f, -16f, 3.2f), Triple(-39f, 18f, 1.7f), Triple(32f, 28f, 2f))
        marks.forEach { (x, y, radius) ->
            val c = center + Offset(x * unit, y * unit)
            val r = radius * unit
            val star = Path().apply {
                moveTo(c.x, c.y - r); lineTo(c.x + r * 0.3f, c.y - r * 0.3f)
                lineTo(c.x + r, c.y); lineTo(c.x + r * 0.3f, c.y + r * 0.3f)
                lineTo(c.x, c.y + r); lineTo(c.x - r * 0.3f, c.y + r * 0.3f)
                lineTo(c.x - r, c.y); lineTo(c.x - r * 0.3f, c.y - r * 0.3f); close()
            }
            drawPath(star, sceneColor.copy(alpha = 0.58f))
        }
        drawOval(sceneColor.copy(alpha = 0.12f),
            Offset(center.x - 28f * unit, size.height - 7f * unit), Size(56f * unit, 5f * unit))
    }
}
