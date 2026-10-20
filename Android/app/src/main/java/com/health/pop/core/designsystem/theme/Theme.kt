package com.health.pop.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = SteadyBlue,
    onPrimary = SoftSurface,
    primaryContainer = SteadyBlueLight,
    onPrimaryContainer = DeepNavy,
    secondary = PopcornAmber,
    onSecondary = DeepNavy,
    secondaryContainer = PopcornAmberLight,
    onSecondaryContainer = DeepNavy,
    background = CreamBackground,
    onBackground = DeepNavy,
    surface = SoftSurface,
    onSurface = DeepNavy,
    surfaceVariant = SoftCardSurface,
    onSurfaceVariant = SubtitleGray,
    outline = SoftBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9A8FFF),
    onPrimary = Color(0xFF211B54),
    primaryContainer = Color(0xFF39316F),
    onPrimaryContainer = Color(0xFFE9E5FF),
    secondary = PopcornAmber,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurface,
    onSecondaryContainer = DarkNavy,
    background = DarkBackground,
    onBackground = DarkNavy,
    surface = DarkSurface,
    onSurface = DarkNavy,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkSubtitleGray,
    outline = DarkBorder
)

@Composable
fun POPAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
