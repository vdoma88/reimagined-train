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

/**
 * Mystery-adventure palette: dark pine forest, old-paper warmth and electric night accents.
 * Legacy names are intentionally kept so existing screens inherit the redesign without churn.
 */
object Palette {
    val Night = Color(0xFF0E1917)
    val NightHigh = Color(0xFF1A2D27)
    val Glass = Color(0x2639584B)
    val GlassBorder = Color(0x447BAF91)

    // Legacy semantic aliases now mapped to the new system.
    val Sakura = Color(0xFFFFC77B)      // lantern amber
    val Lavender = Color(0xFF9E8CFF)    // strange-night violet
    val Sky = Color(0xFF8FD8B4)         // mystery teal
    val Peach = Color(0xFFF09170)       // campfire clay
    val Cream = Color(0xFFF4E8C8)       // notebook paper
    val Ink = Color(0xFF241B14)         // ink brown
    val Text = Color(0xFFFFF6DD)
    val TextDim = Color(0xFFC8D1C7)

    val Pine = Color(0xFF244A3B)
    val Moss = Color(0xFF4D7A57)
    val Amber = Sakura
    val Teal = Sky
    val Paper = Cream
    val Danger = Color(0xFFE86A5A)

    val accent = Brush.linearGradient(listOf(Color(0xFFFFDC98), Amber, Color(0xFFECA866)))
    val accentWide = Brush.linearGradient(listOf(Amber, Color(0xFFA6D6A7), Teal))
    val background = Brush.verticalGradient(
        listOf(Color(0xFF17352D), Color(0xFF12251F), Night, Color(0xFF09100F)),
    )
}

private val scheme = darkColorScheme(
    primary = Palette.Amber,
    onPrimary = Palette.Ink,
    secondary = Palette.Teal,
    onSecondary = Color(0xFF09231F),
    tertiary = Palette.Lavender,
    background = Palette.Night,
    onBackground = Palette.Text,
    surface = Palette.NightHigh,
    onSurface = Palette.Text,
    surfaceVariant = Color(0xFF243A33),
    onSurfaceVariant = Palette.TextDim,
    surfaceContainer = Color(0xFF172923),
    surfaceContainerHigh = Color(0xFF20352E),
    surfaceContainerHighest = Color(0xFF294238),
    outline = Palette.GlassBorder,
    error = Palette.Danger,
)

private val typography = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.6).sp,
        ),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Bold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
        bodyMedium = bodyMedium.copy(lineHeight = 20.sp),
    )
}

@Composable
fun AniMateTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
