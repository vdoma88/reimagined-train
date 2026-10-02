package com.animate.companion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Palette {
    val Night = Color(0xFF171225)
    val NightHigh = Color(0xFF241A36)
    val Glass = Color(0x24FFF9FD)
    val GlassBorder = Color(0x47FFD9EA)
    val Sakura = Color(0xFFFF9FC9)
    val Lavender = Color(0xFFC2A8FF)
    val Sky = Color(0xFF9EDCFF)
    val Peach = Color(0xFFFFC2A8)
    val Cream = Color(0xFFFFF5F9)
    val Ink = Color(0xFF3A2545)
    val Text = Color(0xFFFFF7FC)
    val TextDim = Color(0xFFD7C8E3)

    val accent = Brush.linearGradient(listOf(Sakura, Color(0xFFFFC2DD), Lavender))
    val accentWide = Brush.linearGradient(listOf(Sakura, Lavender, Sky))
    val background = Brush.verticalGradient(
        listOf(Color(0xFF33204A), Color(0xFF20172F), Night, Color(0xFF100C19)),
    )
}

private val scheme = darkColorScheme(
    primary = Palette.Sakura,
    onPrimary = Palette.Ink,
    secondary = Palette.Lavender,
    onSecondary = Color(0xFF1A0F33),
    tertiary = Palette.Sky,
    background = Palette.Night,
    onBackground = Palette.Text,
    surface = Palette.NightHigh,
    onSurface = Palette.Text,
    surfaceVariant = Color(0xFF382B4C),
    onSurfaceVariant = Palette.TextDim,
    surfaceContainer = Color(0xFF261C36),
    surfaceContainerHigh = Color(0xFF30223F),
    surfaceContainerHighest = Color(0xFF3B294C),
    outline = Palette.GlassBorder,
    error = Color(0xFFFF6B81),
)

private val typography = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    )
}

@Composable
fun AniMateTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
