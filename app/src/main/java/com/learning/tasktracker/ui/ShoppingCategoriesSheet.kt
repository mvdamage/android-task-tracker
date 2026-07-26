package com.learning.tasktracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.learning.tasktracker.data.ShoppingCategoryEntity
import com.learning.tasktracker.ui.components.EditorSectionTitle
import com.learning.tasktracker.ui.theme.extendedColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShoppingCategoriesSheet(
    categories: List<ShoppingCategoryEntity>,
    pendingDeleteCategory: ShoppingCategoryEntity?,
    itemsInPendingDeleteCategory: Int,
    onAddCategory: (name: String, colorArgb: Long, iconKey: String) -> Unit,
    onRequestDeleteCategory: (ShoppingCategoryEntity) -> Unit,
    onConfirmDeleteCategory: () -> Unit,
    onDismissDeleteCategory: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableLongStateOf(ShoppingCategoryPresets.defaultColor()) }
    var selectedIconKey by remember { mutableStateOf(ShoppingCategoryPresets.defaultIconKey()) }

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
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Категории покупок",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (categories.isEmpty()) {
                Text(
                    text = "Пока нет категорий. Создайте первую ниже.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((categories.size.coerceAtMost(5) * 56).dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(categories, key = { it.id }) { category ->
                        CategoryListRow(
                            category = category,
                            onDelete = { onRequestDeleteCategory(category) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    color = MaterialTheme.extendedColors.divider,
                    thickness = 0.5.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            EditorSectionTitle(title = "Новая категория")
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = { Text("Название") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            EditorSectionTitle(title = "Цвет")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ShoppingCategoryPresets.colors.forEach { colorArgb ->
                    ColorPickerDot(
                        color = ShoppingCategoryPresets.colorFromArgb(colorArgb),
                        selected = selectedColor == colorArgb,
                        onClick = { selectedColor = colorArgb }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            EditorSectionTitle(title = "Иконка")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShoppingCategoryPresets.icons.forEach { iconOption ->
                    IconPickerChip(
                        iconKey = iconOption.key,
                        imageVector = iconOption.imageVector,
                        color = ShoppingCategoryPresets.colorFromArgb(selectedColor),
                        selected = selectedIconKey == iconOption.key,
                        onClick = { selectedIconKey = iconOption.key }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        onAddCategory(newName, selectedColor, selectedIconKey)
                        newName = ""
                    }
                },
                enabled = newName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Добавить категорию", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    if (pendingDeleteCategory != null) {
        AlertDialog(
            onDismissRequest = onDismissDeleteCategory,
            title = { Text("Удалить категорию?") },
            text = {
                val message = if (itemsInPendingDeleteCategory > 0) {
                    "Категория «${pendingDeleteCategory.name}» будет удалена. " +
                        "$itemsInPendingDeleteCategory " +
                        pluralItems(itemsInPendingDeleteCategory) +
                        " станут без категории."
                } else {
                    "Категория «${pendingDeleteCategory.name}» будет удалена."
                }
                Text(message)
            },
            confirmButton = {
                TextButton(onClick = onConfirmDeleteCategory) {
                    Text("Удалить", color = MaterialTheme.extendedColors.overdue)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteCategory) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun CategoryListRow(
    category: ShoppingCategoryEntity,
    onDelete: () -> Unit
) {
    val color = ShoppingCategoryPresets.colorFromArgb(category.colorArgb)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.18f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = ShoppingCategoryPresets.iconForKey(category.iconKey),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Удалить категорию",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ColorPickerDot(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun IconPickerChip(
    iconKey: String,
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick)
            .then(
                if (selected) Modifier.border(1.5.dp, color, RoundedCornerShape(10.dp))
                else Modifier
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = iconKey,
                tint = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private fun pluralItems(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..14 -> "товаров"
        mod10 == 1 -> "товар"
        mod10 in 2..4 -> "товара"
        else -> "товаров"
    }
}

@Composable
fun ShoppingCategoryBadge(
    category: ShoppingCategoryEntity,
    modifier: Modifier = Modifier
) {
    val color = ShoppingCategoryPresets.colorFromArgb(category.colorArgb)
    Surface(
        modifier = modifier.size(28.dp),
        shape = CircleShape,
        color = color.copy(alpha = 0.18f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = ShoppingCategoryPresets.iconForKey(category.iconKey),
                contentDescription = category.name,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
