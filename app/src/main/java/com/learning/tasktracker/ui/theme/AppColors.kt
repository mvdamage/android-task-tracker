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
    val onShoppingContainer: Color,
    val checkboxUnchecked: Color,
    val checkboxChecked: Color,
    val sectionHeader: Color,
    val divider: Color
)

val LightExtendedColors = ExtendedColors(
    overdue = Color(0xFFFF3B30),
    overdueContainer = Color(0xFFFFEBEA),
    priorityLow = Color(0xFF34C759),
    priorityMedium = Color(0xFF8E8E93),
    priorityHigh = Color(0xFFFF9500),
    shoppingPrimary = Color(0xFF0095FF),
    shoppingContainer = Color(0xFFE8F4FF),
    onShoppingContainer = Color(0xFF0077CC),
    checkboxUnchecked = Color(0xFFC7C7CC),
    checkboxChecked = Color(0xFF0095FF),
    sectionHeader = Color(0xFF8E8E93),
    divider = Color(0xFFE5E5EA)
)

val DarkExtendedColors = ExtendedColors(
    overdue = Color(0xFFFF453A),
    overdueContainer = Color(0xFF3A1C1C),
    priorityLow = Color(0xFF30D158),
    priorityMedium = Color(0xFF8E8E93),
    priorityHigh = Color(0xFFFF9F0A),
    shoppingPrimary = Color(0xFF0095FF),
    shoppingContainer = Color(0xFF003A66),
    onShoppingContainer = Color(0xFFB3DAFF),
    checkboxUnchecked = Color(0xFF48484A),
    checkboxChecked = Color(0xFF0095FF),
    sectionHeader = Color(0xFF8E8E93),
    divider = Color(0xFF38383A)
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

val MaterialTheme.extendedColors: ExtendedColors
    @Composable get() = LocalExtendedColors.current

fun Priority.color(extended: ExtendedColors): Color = when (this) {
    Priority.LOW -> extended.priorityLow
    Priority.MEDIUM -> extended.priorityMedium
    Priority.HIGH -> extended.priorityHigh
}
