package com.learning.tasktracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learning.tasktracker.data.ShoppingItemEntity
import com.learning.tasktracker.ui.components.AnimatedCheckboxScale
import com.learning.tasktracker.ui.components.HorizontalSuggestionPills
import com.learning.tasktracker.ui.components.ShoppingProgressBar
import com.learning.tasktracker.ui.theme.extendedColors

private val quickExamples = listOf("Молоко", "Хлеб", "Яйца", "Сыр", "Овощи")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(viewModel: ShoppingViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val titleHistory by viewModel.titleHistory.collectAsStateWithLifecycle()
    var newItemTitle by remember { mutableStateOf("") }
    val extended = MaterialTheme.extendedColors

    val suggestions = remember(newItemTitle, titleHistory) {
        TaskViewModel.filterTitleSuggestions(titleHistory, newItemTitle)
    }

    fun addCurrentItem() {
        if (newItemTitle.isNotBlank()) {
            viewModel.addItem(newItemTitle)
            newItemTitle = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Список покупок",
                            fontWeight = FontWeight.Bold,
                            color = extended.shoppingPrimary
                        )
                        if (state.items.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ShoppingProgressBar(
                                checked = state.checkedCount,
                                total = state.items.size
                            )
                        } else {
                            Text(
                                "Добавьте товары в плашку ниже",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (state.checkedCount > 0) {
                        IconButton(onClick = viewModel::clearChecked) {
                            Icon(
                                Icons.Outlined.DeleteSweep,
                                contentDescription = "Очистить купленное"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ShoppingInputPanel(
                title = newItemTitle,
                onTitleChange = { newItemTitle = it },
                suggestions = suggestions,
                onAdd = ::addCurrentItem,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            if (state.items.isEmpty()) {
                ShoppingEmptyState(
                    onQuickAdd = { viewModel.addItem(it) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically { it / 4 },
                            exit = fadeOut()
                        ) {
                            ShoppingItemRow(
                                item = item,
                                onToggle = { viewModel.toggleChecked(item) },
                                onDelete = { viewModel.delete(item) }
                            )
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingInputPanel(
    title: String,
    onTitleChange: (String) -> Unit,
    suggestions: List<String>,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = MaterialTheme.extendedColors
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            extended.shoppingContainer,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Text(
                    "Добавить в список",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = extended.onShoppingContainer
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(start = 4.dp, end = 4.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        placeholder = { Text("Товар") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onAdd() })
                    )
                    IconButton(
                        onClick = onAdd,
                        enabled = title.isNotBlank(),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (title.isNotBlank()) extended.shoppingPrimary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Добавить",
                            tint = if (title.isNotBlank()) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (suggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Подсказки",
                        style = MaterialTheme.typography.labelMedium,
                        color = extended.onShoppingContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalSuggestionPills(
                        suggestions = suggestions,
                        onSelect = onTitleChange
                    )
                }
            }
        }
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingItemEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedCheckboxScale(checked = item.isChecked) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = { onToggle() }
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
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Удалить",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShoppingEmptyState(
    onQuickAdd: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = MaterialTheme.extendedColors
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.ShoppingCart,
            contentDescription = null,
            tint = extended.shoppingPrimary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Список покупок пуст",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Добавьте товары через плашку сверху\nили выберите пример:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            quickExamples.take(3).forEach { example ->
                FilterChip(
                    selected = false,
                    onClick = { onQuickAdd(example) },
                    label = { Text(example) }
                )
            }
        }
    }
}
