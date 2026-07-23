package com.learning.tasktracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue = Color(0xFF0F3D68)
private val BlueSoft = Color(0xFF1F5F96)
private val Sky = Color(0xFF7EC8FF)
private val Paper = Color(0xFFF3F7FB)
private val Ink = Color(0xFF102033)

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = Sky.copy(alpha = 0.45f),
    onPrimaryContainer = Ink,
    secondary = BlueSoft,
    onSecondary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE2EBF4),
    onSurfaceVariant = Color(0xFF3D5166),
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = Sky,
    onPrimary = Ink,
    primaryContainer = Blue,
    onPrimaryContainer = Sky,
    secondary = BlueSoft,
    onSecondary = Color.White,
    background = Color(0xFF0B1520),
    onBackground = Color(0xFFE7F0F8),
    surface = Color(0xFF122033),
    onSurface = Color(0xFFE7F0F8),
    surfaceVariant = Color(0xFF1C2E44),
    onSurfaceVariant = Color(0xFFB7C7D8),
    error = Color(0xFFFFB4AB)
)

@Composable
fun TaskTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
