package com.hydrogen.bot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = HydrogenBlue,
    onPrimary = CanvasWhite,
    primaryContainer = HydrogenPillBg,
    onPrimaryContainer = HydrogenBlue,
    secondary = SlateSecondary,
    onSecondary = CanvasWhite,
    background = CanvasWhite,
    onBackground = SlateDark,
    surface = SurfacePearl,
    onSurface = SlateDark,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = SlateSecondary,
    outline = HairlineBorder
)

@Composable
fun HydrogenBotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
