package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SignalUpGreen,
    onPrimary = DarkCanvas,
    primaryContainer = SignalUpBg,
    onPrimaryContainer = SignalUpGreen,
    secondary = AccentCyan,
    onSecondary = DarkCanvas,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentGold,
    onTertiary = DarkCanvas,
    background = DarkCanvas,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = SignalDownRed,
    onError = TextPrimary,
    errorContainer = SignalDownBg,
    onErrorContainer = SignalDownRed
)

@Composable
fun QtexTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
