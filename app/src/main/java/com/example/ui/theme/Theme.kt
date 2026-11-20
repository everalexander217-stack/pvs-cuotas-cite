package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PvsLightColorScheme = lightColorScheme(
    primary = PvsForestGreen,
    onPrimary = Color.White,
    primaryContainer = PvsMintContainer,
    onPrimaryContainer = PvsForestDark,
    secondary = PvsLeafVibrant,
    onSecondary = Color.White,
    secondaryContainer = PvsMintSurface,
    onSecondaryContainer = PvsForestGreen,
    tertiary = PvsForestDark,
    onTertiary = Color.White,
    background = PvsLightBackground,
    onBackground = PvsTextPrimary,
    surface = PvsPureWhite,
    onSurface = PvsTextPrimary,
    surfaceVariant = PvsSoftSurface,
    onSurfaceVariant = PvsTextSecondary,
    outline = PvsSageOutline,
    outlineVariant = PvsMintContainer,
    error = SemaphoreRed,
    onError = Color.White,
    errorContainer = SemaphoreRedBg,
    onErrorContainer = SemaphoreRed
)

private val PvsDarkColorScheme = darkColorScheme(
    primary = PvsLeafVibrant,
    onPrimary = PvsForestDark,
    primaryContainer = PvsForestGreen,
    onPrimaryContainer = PvsMintSurface,
    secondary = PvsLeafBright,
    onSecondary = PvsForestDark,
    secondaryContainer = PvsDarkSurfaceElevated,
    onSecondaryContainer = PvsMintContainer,
    tertiary = PvsMintContainer,
    onTertiary = PvsForestDark,
    background = PvsDarkBackground,
    onBackground = PvsDarkTextPrimary,
    surface = PvsDarkSurface,
    onSurface = PvsDarkTextPrimary,
    surfaceVariant = PvsDarkSurfaceElevated,
    onSurfaceVariant = PvsDarkTextSecondary,
    outline = Color(0xFF2B4D39),
    outlineVariant = Color(0xFF1F3B2B),
    error = SemaphoreRedGlow,
    onError = Color.White,
    errorContainer = Color(0xFF3E1818),
    onErrorContainer = Color(0xFFFCA5A5)
)

val PvsShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun PvsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PvsDarkColorScheme else PvsLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PvsTypography,
        shapes = PvsShapes,
        content = content
    )
}
