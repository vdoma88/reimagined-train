package com.animate.companion.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.animate.companion.ui.theme.Palette
import kotlin.math.sin
import kotlin.random.Random

/** Night gradient with slowly falling sakura petals. */
@Composable
fun SakuraBackground(modifier: Modifier = Modifier, petals: Int = 14, content: @Composable BoxScope.() -> Unit) {
    val seeds = remember { List(petals) { Random(it * 31 + 7) }.map { r -> FloatArray(5) { r.nextFloat() } } }
    val t = rememberInfiniteTransition(label = "petals")
    val time by t.animateFloat(0f, 1f, infiniteRepeatable(tween(26000, easing = LinearEasing)), label = "petalTime")
    Box(modifier.fillMaxSize().background(Palette.background)) {
        Canvas(Modifier.fillMaxSize()) {
            // Soft shoujo glows + subtle manga halftone dots.
            drawCircle(Brush.radialGradient(listOf(Palette.Lavender.copy(alpha = 0.20f), Color.Transparent), Offset(size.width * 0.15f, size.height * 0.1f), size.width * 0.7f), size.width * 0.7f, Offset(size.width * 0.15f, size.height * 0.1f))
            drawCircle(Brush.radialGradient(listOf(Palette.Sakura.copy(alpha = 0.16f), Color.Transparent), Offset(size.width * 0.9f, size.height * 0.75f), size.width * 0.8f), size.width * 0.8f, Offset(size.width * 0.9f, size.height * 0.75f))
            val dot = 34.dp.toPx()
            var yy = dot * 0.6f
            var row = 0
            while (yy < size.height) {
                var xx = if (row % 2 == 0) dot * 0.5f else dot
                while (xx < size.width) {
                    drawCircle(Palette.Cream.copy(alpha = 0.035f), 1.2.dp.toPx(), Offset(xx, yy))
                    xx += dot
                }
                yy += dot
                row++
            }
            seeds.forEach { s ->
                val speed = 0.6f + s[0] * 0.8f
                val prog = (time * speed + s[1]) % 1f
                val x = size.width * s[2] + sin((prog * 6f + s[3] * 6f).toDouble()).toFloat() * 30.dp.toPx()
                val y = -20f + prog * (size.height + 40f)
                val r = (4f + s[4] * 5f) * density
                rotate(prog * 720f * (if (s[3] > 0.5f) 1 else -1), Offset(x, y)) {
                    val petal = Path().apply {
                        moveTo(x, y - r)
                        quadraticBezierTo(x + r, y, x, y + r)
                        quadraticBezierTo(x - r, y, x, y - r)
                        close()
                    }
                    drawPath(petal, Palette.Sakura.copy(alpha = 0.25f + s[4] * 0.25f))
                }
            }
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val border = if (selected) BorderStroke(2.dp, Palette.accent) else BorderStroke(1.dp, Palette.GlassBorder)
    Box(
        modifier
            .clip(shape)
            .background(if (selected) Palette.Sakura.copy(alpha = 0.18f) else Palette.Glass)
            .border(border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content,
    )
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 56.dp,
) {
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(if (enabled) Palette.accent else Brush.linearGradient(listOf(Color.Gray, Color.DarkGray)))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, null, tint = Color(0xFF2A0A1C))
            Text(text, color = Color(0xFF2A0A1C), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, color: Color = Palette.Lavender) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Palette.Text,
        )
    }
}

/** Circular frame with a gradient ring, used behind avatars. */
@Composable
fun AvatarFrame(modifier: Modifier = Modifier, ring: Dp = 2.dp, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Palette.accentWide)
            .padding(ring)
            .clip(RoundedCornerShape(50))
            .background(Brush.verticalGradient(listOf(Color(0xFF5A3F75), Color(0xFF2A1D3C), Color(0xFF171225)))),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
