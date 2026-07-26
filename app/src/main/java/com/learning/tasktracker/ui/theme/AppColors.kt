package com.learning.tasktracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.learning.tasktracker.data.Priority

data class ExtendedColors(
    val overdue: Color,
    val overdueContainer: Color,
    val priorityLow: Color,
    val priorityMedium: Color,
    val priorityHigh: Color,
    val shoppingPrimary: Color,
    val shoppingContainer: Color,
    val onShoppingContainer: Color
)

val LightExtendedColors = ExtendedColors(
    overdue = Color(0xFFB71C1C),
    overdueContainer = Color(0xFFFFEBEE),
    priorityLow = Color(0xFF2E7D32),
    priorityMedium = Color(0xFFEF6C00),
    priorityHigh = Color(0xFFC62828),
    shoppingPrimary = Color(0xFF2E7D32),
    shoppingContainer = Color(0xFFE8F5E9),
    onShoppingContainer = Color(0xFF1B5E20)
)

val DarkExtendedColors = ExtendedColors(
    overdue = Color(0xFFFF8A80),
    overdueContainer = Color(0xFF4A1C1C),
    priorityLow = Color(0xFF81C784),
    priorityMedium = Color(0xFFFFB74D),
    priorityHigh = Color(0xFFEF5350),
    shoppingPrimary = Color(0xFF81C784),
    shoppingContainer = Color(0xFF1B3A1F),
    onShoppingContainer = Color(0xFFC8E6C9)
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

val MaterialTheme.extendedColors: ExtendedColors
    @Composable get() = LocalExtendedColors.current

fun Priority.color(extended: ExtendedColors): Color = when (this) {
    Priority.LOW -> extended.priorityLow
    Priority.MEDIUM -> extended.priorityMedium
    Priority.HIGH -> extended.priorityHigh
}
