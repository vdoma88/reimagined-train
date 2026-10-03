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

/**
 * Shared mystery-forest backdrop. Kept under the old function name to avoid screen churn.
 * Uses lantern dust, stars and pine silhouettes instead of sakura petals.
 */
@Composable
fun SakuraBackground(
    modifier: Modifier = Modifier,
    petals: Int = 14,
    content: @Composable BoxScope.() -> Unit,
) {
    val particles = remember {
        List(petals.coerceAtLeast(10)) { Random(it * 43 + 11) }
            .map { r -> FloatArray(5) { r.nextFloat() } }
    }
    val t = rememberInfiniteTransition(label = "mysteryDust")
    val time by t.animateFloat(
        0f,
        1f,
        infiniteRepeatable(tween(28000, easing = LinearEasing)),
        label = "mysteryDustTime",
    )

    Box(modifier.fillMaxSize().background(Palette.background)) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Palette.Teal.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.14f, size.height * 0.08f),
                    radius = size.width * 0.72f,
                ),
                radius = size.width * 0.72f,
                center = Offset(size.width * 0.14f, size.height * 0.08f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Palette.Amber.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.86f, size.height * 0.72f),
                    radius = size.width * 0.78f,
                ),
                radius = size.width * 0.78f,
                center = Offset(size.width * 0.86f, size.height * 0.72f),
            )

            // Quiet notebook-star field.
            val grid = 42.dp.toPx()
            var y = grid * 0.65f
            var row = 0
            while (y < size.height * 0.72f) {
                var x = if (row % 2 == 0) grid * 0.55f else grid
                while (x < size.width) {
                    drawCircle(
                        color = Palette.Paper.copy(alpha = 0.045f),
                        radius = 1.15.dp.toPx(),
                        center = Offset(x, y),
                    )
                    x += grid
                }
                y += grid
                row++
            }

            // Slow lantern dust / fireflies.
            particles.forEach { s ->
                val speed = 0.4f + s[0] * 0.7f
                val progress = (time * speed + s[1]) % 1f
                val x = size.width * s[2] +
                    sin((progress * 6f + s[3] * 7f).toDouble()).toFloat() * 22.dp.toPx()
                val yPos = size.height - progress * (size.height + 40.dp.toPx())
                val r = (1.8f + s[4] * 2.6f) * density
                drawCircle(
                    color = if (s[4] > 0.45f) Palette.Amber.copy(alpha = 0.24f + s[4] * 0.28f)
                    else Palette.Teal.copy(alpha = 0.18f + s[4] * 0.22f),
                    radius = r,
                    center = Offset(x, yPos),
                )
            }

            // Distant pine silhouettes.
            fun pine(cx: Float, baseY: Float, h: Float, color: Color) {
                val trunk = Path().apply {
                    moveTo(cx - h * 0.025f, baseY)
                    lineTo(cx + h * 0.025f, baseY)
                    lineTo(cx + h * 0.018f, baseY - h * 0.28f)
                    lineTo(cx - h * 0.018f, baseY - h * 0.28f)
                    close()
                }
                drawPath(trunk, color)
                listOf(0.22f, 0.40f, 0.58f, 0.76f).forEach { level ->
                    val top = baseY - h * level
                    val half = h * (0.11f + level * 0.08f)
                    val tri = Path().apply {
                        moveTo(cx, top - h * 0.22f)
                        lineTo(cx - half, top + h * 0.08f)
                        lineTo(cx + half, top + h * 0.08f)
                        close()
                    }
                    drawPath(tri, color)
                }
            }

            pine(size.width * 0.08f, size.height, size.height * 0.26f, Color(0xFF0B1714))
            pine(size.width * 0.24f, size.height, size.height * 0.20f, Color(0xFF10201B))
            pine(size.width * 0.78f, size.height, size.height * 0.22f, Color(0xFF10201B))
            pine(size.width * 0.93f, size.height, size.height * 0.29f, Color(0xFF0B1714))
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val border = if (selected) {
        BorderStroke(2.dp, Palette.accent)
    } else {
        BorderStroke(1.dp, Palette.GlassBorder)
    }

    Box(
        modifier
            .clip(shape)
            .background(
                if (selected) Brush.linearGradient(listOf(Palette.Amber.copy(alpha = 0.16f), Palette.Teal.copy(alpha = 0.08f)))
                else Brush.verticalGradient(
                    listOf(
                        Palette.NightHigh.copy(alpha = 0.92f),
                        Color(0xFF10211C).copy(alpha = 0.90f),
                    )
                )
            )
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
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (enabled) Palette.accent
                else Brush.linearGradient(listOf(Color(0xFF53615C), Color(0xFF35413D)))
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (enabled) Palette.Paper.copy(alpha = 0.26f) else Color.Transparent,
                ),
                RoundedCornerShape(18.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) Icon(icon, null, tint = Palette.Ink)
            Text(
                text,
                color = Palette.Ink,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Palette.Teal,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.34f)),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Palette.Text,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Badge-like frame: amber rim, deep forest core. */
@Composable
fun AvatarFrame(
    modifier: Modifier = Modifier,
    ring: Dp = 3.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Palette.accentWide)
            .padding(ring)
            .clip(RoundedCornerShape(19.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF315A48),
                        Color(0xFF1A332A),
                        Color(0xFF0D1715),
                    )
                )
            ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
