package com.learning.tasktracker.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Egg
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalGroceryStore
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.Soap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class ShoppingCategoryIconOption(
    val key: String,
    val imageVector: ImageVector,
    val label: String
)

object ShoppingCategoryPresets {
    val colors: List<Long> = listOf(
        0xFF0095FF,
        0xFF34C759,
        0xFFFF9500,
        0xFFFF3B30,
        0xFFAF52DE,
        0xFF5856D6,
        0xFF00C7BE,
        0xFF8E8E93
    )

    val icons: List<ShoppingCategoryIconOption> = listOf(
        ShoppingCategoryIconOption("grocery", Icons.Outlined.LocalGroceryStore, "Продукты"),
        ShoppingCategoryIconOption("basket", Icons.Outlined.ShoppingBasket, "Корзина"),
        ShoppingCategoryIconOption("restaurant", Icons.Outlined.Restaurant, "Еда"),
        ShoppingCategoryIconOption("fastfood", Icons.Outlined.Fastfood, "Фастфуд"),
        ShoppingCategoryIconOption("bakery", Icons.Outlined.BakeryDining, "Выпечка"),
        ShoppingCategoryIconOption("egg", Icons.Outlined.Egg, "Яйца"),
        ShoppingCategoryIconOption("drink", Icons.Outlined.LocalDrink, "Напитки"),
        ShoppingCategoryIconOption("eco", Icons.Outlined.Eco, "Овощи"),
        ShoppingCategoryIconOption("frozen", Icons.Outlined.AcUnit, "Заморозка"),
        ShoppingCategoryIconOption("cleaning", Icons.Outlined.CleaningServices, "Уборка"),
        ShoppingCategoryIconOption("soap", Icons.Outlined.Soap, "Гигиена"),
        ShoppingCategoryIconOption("medication", Icons.Outlined.Medication, "Аптека"),
        ShoppingCategoryIconOption("pets", Icons.Outlined.Pets, "Питомцы")
    )

    fun colorFromArgb(argb: Long): Color = Color(argb.toInt())

    fun iconForKey(key: String): ImageVector =
        icons.firstOrNull { it.key == key }?.imageVector ?: Icons.Outlined.ShoppingBasket

    fun defaultColor(): Long = colors.first()

    fun defaultIconKey(): String = icons.first().key
}
