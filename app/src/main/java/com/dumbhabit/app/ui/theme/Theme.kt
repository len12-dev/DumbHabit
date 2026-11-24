package com.dumbhabit.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class ColorMode { LIGHT, DARK, FOLLOW_SYSTEM }

/** Palette « écran LCD » tirée des captures : fond sauge, encre vert très foncé. */
data class PixelPalette(val background: Color, val ink: Color, val muted: Color)

val LightPalette = PixelPalette(
    background = Color(0xFFA8BA93),
    ink = Color(0xFF1F2A1D),
    muted = Color(0xFF4F5F49),
)

val DarkPalette = PixelPalette(
    background = Color(0xFF1C2119),
    ink = Color(0xFFA8BA93),
    muted = Color(0xFF7E8F71),
)

private val LocalPalette = staticCompositionLocalOf { LightPalette }

@Composable
fun pixel(): PixelPalette = LocalPalette.current

@Composable
fun DumbHabitTheme(mode: ColorMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ColorMode.LIGHT -> false
        ColorMode.DARK -> true
        ColorMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    }
    val p = if (dark) DarkPalette else LightPalette
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = p.ink,
        onPrimary = p.background,
        background = p.background,
        onBackground = p.ink,
        surface = p.background,
        onSurface = p.ink,
        outline = p.ink,
    )

    CompositionLocalProvider(
        LocalPalette provides p,
        LocalContentColor provides p.ink,
    ) {
        MaterialTheme(colorScheme = scheme, typography = monoTypography()) {
            // Évite que le contenu passe sous la barre de statut et la barre de navigation (mode bord à bord).
            Box(
                Modifier
                    .fillMaxSize()
                    .background(p.background)
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                content()
            }
        }
    }
}

private fun mono(size: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = size.sp,
    lineHeight = (size + 6).sp,
    fontWeight = weight,
)

private fun monoTypography() = Typography(
    displayLarge = mono(44),
    displayMedium = mono(36),
    displaySmall = mono(30),
    headlineLarge = mono(28, FontWeight.Bold),
    headlineMedium = mono(24, FontWeight.Bold),
    headlineSmall = mono(20, FontWeight.Bold),
    titleLarge = mono(20, FontWeight.Bold),
    titleMedium = mono(16, FontWeight.Bold),
    titleSmall = mono(14, FontWeight.Bold),
    bodyLarge = mono(16),
    bodyMedium = mono(14),
    bodySmall = mono(12),
    labelLarge = mono(14, FontWeight.Bold),
    labelMedium = mono(12),
    labelSmall = mono(11),
)
