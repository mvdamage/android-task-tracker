package com.learning.tasktracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val AnyDoBlue = Color(0xFF0095FF)
private val AnyDoBlueDark = Color(0xFF0077CC)
private val AnyDoBlueLight = Color(0xFFE8F4FF)
private val Ink = Color(0xFF1A1A1A)
private val InkMuted = Color(0xFF8E8E93)
private val Divider = Color(0xFFE5E5EA)

private val LightColors = lightColorScheme(
    primary = AnyDoBlue,
    onPrimary = Color.White,
    primaryContainer = AnyDoBlueLight,
    onPrimaryContainer = AnyDoBlueDark,
    secondary = AnyDoBlueDark,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = InkMuted,
    outline = Color(0xFFC7C7CC),
    outlineVariant = Divider,
    error = Color(0xFFFF3B30)
)

private val DarkColors = darkColorScheme(
    primary = AnyDoBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF003A66),
    onPrimaryContainer = Color(0xFFB3DAFF),
    secondary = AnyDoBlue,
    onSecondary = Color.White,
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F7),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF8E8E93),
    outline = Color(0xFF48484A),
    outlineVariant = Color(0xFF38383A),
    error = Color(0xFFFF453A)
)

@Composable
fun TaskTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val extended = if (darkTheme) DarkExtendedColors else LightExtendedColors
    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content
        )
    }
}
