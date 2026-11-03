package com.example.localbeat.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

private val CrimsonDark = darkColorScheme(
    primary = BeatColors.Crimson, onPrimary = BeatColors.Ink,
    primaryContainer = BeatColors.Wine, onPrimaryContainer = BeatColors.Text,
    secondary = BeatColors.Highlight, onSecondary = BeatColors.Background,
    secondaryContainer = BeatColors.Wine, onSecondaryContainer = BeatColors.Text,
    tertiary = BeatColors.Highlight,
    background = BeatColors.Background, onBackground = BeatColors.Text,
    surface = BeatColors.Elevated, onSurface = BeatColors.Text,
    surfaceVariant = BeatColors.Surface, onSurfaceVariant = BeatColors.Muted,
    surfaceContainerLowest = BeatColors.Background,
    surfaceContainerLow = BeatColors.Elevated,
    surfaceContainer = BeatColors.Surface,
    surfaceContainerHigh = BeatColors.Card,
    surfaceContainerHighest = BeatColors.Card,
    outline = BeatColors.Border, outlineVariant = BeatColors.Border,
    error = BeatColors.Highlight, onError = BeatColors.Background,
    errorContainer = BeatColors.Wine, onErrorContainer = BeatColors.Text,
)

@Composable
fun LocalBeatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CrimsonDark,
        typography = Typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp),
        ),
        content = { CompositionLocalProvider(LocalContentColor provides BeatColors.Text, content = content) },
    )
}
