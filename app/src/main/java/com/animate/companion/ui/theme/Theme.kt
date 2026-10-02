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
    val Night = Color(0xFF0E0A1F)
    val NightHigh = Color(0xFF1A1433)
    val Glass = Color(0x1FFFFFFF)
    val GlassBorder = Color(0x33FFFFFF)
    val Sakura = Color(0xFFFF7EB6)
    val Lavender = Color(0xFFA78BFA)
    val Sky = Color(0xFF7DD3FC)
    val Peach = Color(0xFFFFB38A)
    val Text = Color(0xFFF4EEFF)
    val TextDim = Color(0xFFB9AED6)

    val accent = Brush.linearGradient(listOf(Sakura, Lavender))
    val accentWide = Brush.linearGradient(listOf(Sakura, Lavender, Sky))
    val background = Brush.verticalGradient(listOf(Color(0xFF1B1140), Night, Color(0xFF0A0716)))
}

private val scheme = darkColorScheme(
    primary = Palette.Sakura,
    onPrimary = Color(0xFF2A0A1C),
    secondary = Palette.Lavender,
    onSecondary = Color(0xFF1A0F33),
    tertiary = Palette.Sky,
    background = Palette.Night,
    onBackground = Palette.Text,
    surface = Palette.NightHigh,
    onSurface = Palette.Text,
    surfaceVariant = Color(0xFF2A2250),
    onSurfaceVariant = Palette.TextDim,
    surfaceContainer = Color(0xFF1C1638),
    surfaceContainerHigh = Color(0xFF241D45),
    surfaceContainerHighest = Color(0xFF2C2453),
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
