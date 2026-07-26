package com.learning.tasktracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.learning.tasktracker.data.ShoppingCategoryEntity
import com.learning.tasktracker.data.ShoppingItemEntity
import com.learning.tasktracker.ui.components.EditorSectionTitle
import com.learning.tasktracker.ui.components.HorizontalSuggestionPills
import com.learning.tasktracker.ui.theme.extendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShoppingItemEditorSheet(
    item: ShoppingItemEntity,
    categories: List<ShoppingCategoryEntity>,
    titleHistory: List<String>,
    onDismiss: () -> Unit,
    onSave: (title: String, categoryId: Long?) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(item.id) { mutableStateOf(item.title) }
    var selectedCategoryId by remember(item.id) {
        mutableLongStateOf(item.categoryId ?: 0L)
    }
    val suggestions = remember(title, titleHistory) {
        ShoppingViewModel.shoppingInputSuggestions(titleHistory, title)
    }
    val canSave = title.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("Отмена") }
                TextButton(
                    onClick = {
                        if (canSave) {
                            onSave(title.trim(), selectedCategoryId.takeIf { it > 0L })
                        }
                    },
                    enabled = canSave
                ) {
                    Text(
                        "Готово",
                        color = if (canSave) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Название") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.extendedColors.divider
                )
            )

            if (suggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (title.isBlank()) "Подсказки" else "Похожие",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                HorizontalSuggestionPills(
                    suggestions = suggestions,
                    onSelect = { title = it }
                )
            }

            if (categories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                EditorSectionTitle("Категория")
                Spacer(modifier = Modifier.height(8.dp))
                ShoppingCategoryPickerRow(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onSelect = { selectedCategoryId = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            TextButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Удалить",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
internal fun ShoppingCategoryPickerRow(
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
