package com.learning.tasktracker.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private enum class AppTab(
    val label: String,
    val outlinedIcon: ImageVector,
    val filledIcon: ImageVector
) {
    TASKS("Задачи", Icons.Outlined.TaskAlt, Icons.Filled.TaskAlt),
    SHOPPING("Покупки", Icons.Outlined.ShoppingCart, Icons.Filled.ShoppingCart)
}

@Composable
fun MainScreen(
    taskViewModel: TaskViewModel,
    shoppingViewModel: ShoppingViewModel
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.TASKS) }
    val shoppingState by shoppingViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar(tonalElevation = 3.dp) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            val icon = if (selectedTab == tab) tab.filledIcon else tab.outlinedIcon
                            if (tab == AppTab.SHOPPING && shoppingState.activeCount > 0) {
                                BadgedBox(
                                    badge = { Badge { Text("${shoppingState.activeCount}") } }
                                ) {
                                    Icon(icon, contentDescription = tab.label)
                                }
                            } else {
                                Icon(icon, contentDescription = tab.label)
                            }
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Crossfade(
                targetState = selectedTab,
                animationSpec = tween(durationMillis = 220),
                label = "tabCrossfade"
            ) { tab ->
                when (tab) {
                    AppTab.TASKS -> TaskTrackerScreen(viewModel = taskViewModel)
                    AppTab.SHOPPING -> ShoppingListScreen(viewModel = shoppingViewModel)
                }
            }
        }
    }
}
