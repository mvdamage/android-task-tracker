package com.learning.tasktracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learning.tasktracker.data.ShoppingCategoryEntity
import com.learning.tasktracker.data.ShoppingItemEntity
import com.learning.tasktracker.ui.components.AnyDoDivider
import com.learning.tasktracker.ui.components.CircularTaskCheckbox
import com.learning.tasktracker.ui.components.HorizontalSuggestionPills
import com.learning.tasktracker.ui.components.QuickAddBar
import com.learning.tasktracker.ui.components.ShoppingProgressBar

private val quickExamples = listOf("Молоко", "Хлеб", "Яйца", "Сыр", "Овощи")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(viewModel: ShoppingViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val titleHistory by viewModel.titleHistory.collectAsStateWithLifecycle()
    val pendingDeleteCategory by viewModel.pendingDeleteCategory.collectAsStateWithLifecycle()
    var newItemTitle by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableLongStateOf(0L) }
    var showInput by remember { mutableStateOf(false) }
    var showCategoriesSheet by remember { mutableStateOf(false) }
    var confirmClearChecked by remember { mutableStateOf(false) }
    var deleteCategoryItemCount by remember { mutableIntStateOf(0) }
    val inputFocusRequester = remember { FocusRequester() }

    LaunchedEffect(pendingDeleteCategory) {
        deleteCategoryItemCount = pendingDeleteCategory?.let {
            viewModel.itemsInCategoryCount(it.id)
        } ?: 0
    }

    LaunchedEffect(showInput) {
        if (showInput) {
            inputFocusRequester.requestFocus()
        }
    }

    val suggestions = remember(newItemTitle, titleHistory) {
        TaskViewModel.filterTitleSuggestions(titleHistory, newItemTitle)
    }

    fun addCurrentItem() {
        if (newItemTitle.isNotBlank()) {
            val categoryId = selectedCategoryId.takeIf { it > 0L }
            viewModel.addItem(newItemTitle, categoryId)
            newItemTitle = ""
            selectedCategoryId = 0L
            showInput = false
        }
    }

    val categoryById = remember(state.categories) {
        state.categories.associateBy { it.id }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        "Покупки",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showCategoriesSheet = true }) {
                        Icon(
                            Icons.Outlined.Category,
                            contentDescription = "Категории",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (state.checkedCount > 0) {
                        IconButton(onClick = { confirmClearChecked = true }) {
                            Icon(
                                Icons.Outlined.DeleteSweep,
                                contentDescription = "Очистить купленное",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showInput = true },
                modifier = Modifier.padding(bottom = 8.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить товар")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showInput) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .imePadding()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newItemTitle,
                            onValueChange = { newItemTitle = it },
                            placeholder = { Text("Что купить?") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(inputFocusRequester),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addCurrentItem() }),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        IconButton(
                            onClick = ::addCurrentItem,
                            enabled = newItemTitle.isNotBlank()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Добавить")
                        }
                    }
                    if (suggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalSuggestionPills(
                            suggestions = suggestions,
                            onSelect = { newItemTitle = it }
                        )
                    }
                    if (state.categories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CategoryPickerRow(
                            categories = state.categories,
                            selectedCategoryId = selectedCategoryId,
                            onSelect = { selectedCategoryId = it }
                        )
                    }
                }
            } else {
                QuickAddBar(
                    placeholder = "Добавить в список…",
                    onClick = { showInput = true },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            if (state.items.isEmpty()) {
                ShoppingEmptyState(
                    onQuickAdd = { viewModel.addItem(it) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            } else {
                ShoppingProgressBar(
                    checked = state.checkedCount,
                    total = state.items.size,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                if (state.checkedCount > 0) {
                    TextButton(
                        onClick = { confirmClearChecked = true },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text("Очистить купленное (${state.checkedCount})")
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 8.dp
                    )
                ) {
                    items(state.items, key = { it.id }) { item ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically { it / 4 },
                            exit = fadeOut()
                        ) {
                            ShoppingItemRow(
                                item = item,
                                category = item.categoryId?.let { categoryById[it] },
                                onToggle = { viewModel.toggleChecked(item) }
                            )
                        }
                        AnyDoDivider()
                    }
                }
            }
        }
    }

    if (showCategoriesSheet) {
        ShoppingCategoriesSheet(
            categories = state.categories,
            pendingDeleteCategory = pendingDeleteCategory,
            itemsInPendingDeleteCategory = deleteCategoryItemCount,
            onAddCategory = viewModel::addCategory,
            onRequestDeleteCategory = viewModel::requestDeleteCategory,
            onConfirmDeleteCategory = viewModel::confirmDeleteCategory,
            onDismissDeleteCategory = viewModel::dismissDeleteCategory,
            onDismiss = { showCategoriesSheet = false }
        )
    }

    if (confirmClearChecked) {
        AlertDialog(
            onDismissRequest = { confirmClearChecked = false },
            title = { Text("Очистить купленное?") },
            text = {
                Text(
                    "Будут удалены ${state.checkedCount} " +
                        pluralCheckedItems(state.checkedCount) + " из списка."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearChecked()
                        confirmClearChecked = false
                    }
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearChecked = false }) { Text("Отмена") }
            }
        )
    }
}

private fun pluralCheckedItems(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..14 -> "купленных товаров"
        mod10 == 1 -> "купленный товар"
        mod10 in 2..4 -> "купленных товара"
        else -> "купленных товаров"
    }
}

@Composable
private fun CategoryPickerRow(
    categories: List<ShoppingCategoryEntity>,
    selectedCategoryId: Long,
    onSelect: (Long) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategoryId == 0L,
                onClick = { onSelect(0L) },
                label = { Text("Без категории") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
        items(categories, key = { it.id }) { category ->
            val color = ShoppingCategoryPresets.colorFromArgb(category.colorArgb)
            FilterChip(
                selected = selectedCategoryId == category.id,
                onClick = { onSelect(category.id) },
                label = { Text(category.name) },
                leadingIcon = {
                    Icon(
                        imageVector = ShoppingCategoryPresets.iconForKey(category.iconKey),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.18f),
                    selectedLabelColor = color
                )
            )
        }
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingItemEntity,
    category: ShoppingCategoryEntity?,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularTaskCheckbox(
            checked = item.isChecked,
            onCheckedChange = onToggle
        )
        if (category != null) {
            ShoppingCategoryBadge(
                category = category,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (item.isChecked) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (category != null) 10.dp else 14.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ShoppingEmptyState(
    onQuickAdd: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.ShoppingCart,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Список пуст",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Нажмите + или строку выше",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            quickExamples.take(3).forEach { example ->
                Text(
                    text = example,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onQuickAdd(example) }
                )
            }
        }
    }
}
