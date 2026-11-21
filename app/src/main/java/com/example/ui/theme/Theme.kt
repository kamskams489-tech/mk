package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val RasedDarkColorScheme = darkColorScheme(
    primary = RadarCyan,
    onPrimary = OnRadarCyan,
    primaryContainer = RadarCyanContainer,
    onPrimaryContainer = IceWhite,
    secondary = SignalAmber,
    onSecondary = OnSignalAmber,
    secondaryContainer = SignalAmberContainer,
    onSecondaryContainer = SignalAmber,
    tertiary = SafeEmerald,
    onTertiary = ObsidianBase,
    tertiaryContainer = SafeEmeraldContainer,
    onTertiaryContainer = IceWhite,
    background = ObsidianBase,
    onBackground = IceWhite,
    surface = ObsidianSurface,
    onSurface = IceWhite,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    surfaceContainerLowest = ObsidianBase,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurface,
    surfaceContainerHigh = ObsidianSurfaceVariant,
    surfaceContainerHighest = ObsidianSurfaceHigh,
    outline = ObsidianOutline,
    outlineVariant = ObsidianSurfaceHigh,
    error = AlertCrimson,
    onError = OnAlertCrimson,
    errorContainer = AlertCrimsonContainer,
    onErrorContainer = IceWhite
)

private val RasedShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RasedTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RasedDarkColorScheme,
        typography = AppTypography,
        shapes = RasedShapes,
        content = content
    )
}
