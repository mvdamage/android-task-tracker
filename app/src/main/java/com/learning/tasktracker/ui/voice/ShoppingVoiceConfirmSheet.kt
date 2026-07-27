package com.learning.tasktracker.ui.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.learning.tasktracker.R
import com.learning.tasktracker.data.ShoppingCategoryEntity
import com.learning.tasktracker.ui.ShoppingCategoryPickerRow
import com.learning.tasktracker.voice.ShoppingVoiceLine

data class ShoppingVoiceConfirmLine(
    val title: String,
    val categoryId: Long?,
    val truncated: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingVoiceConfirmSheet(
    lines: List<ShoppingVoiceLine>,
    categories: List<ShoppingCategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (List<ShoppingVoiceConfirmLine>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editable = remember(lines) {
        mutableStateListOf(
            *lines.map {
                ShoppingVoiceConfirmLine(
                    title = it.title,
                    categoryId = it.categoryId,
                    truncated = it.truncated
                )
            }.toTypedArray()
        )
    }
    val validCount = editable.count { it.title.isNotBlank() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = stringResource(R.string.voice_confirm_shopping_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            editable.forEachIndexed { index, line ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = line.title,
                        onValueChange = { newTitle ->
                            editable[index] = editable[index].copy(title = newTitle)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        supportingText = if (line.truncated) {
                            { Text(stringResource(R.string.voice_title_truncated)) }
                        } else {
                            null
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    IconButton(
                        onClick = { editable.removeAt(index) },
                        enabled = editable.size > 1 || line.title.isNotBlank()
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.voice_remove_line)
                        )
                    }
                }
                if (categories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ShoppingCategoryPickerRow(
                        categories = categories,
                        selectedCategoryId = line.categoryId ?: 0L,
                        onSelect = { selected ->
                            editable[index] = editable[index].copy(
                                categoryId = selected.takeIf { it > 0L }
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    onConfirm(
                        editable
                            .map { it.copy(title = it.title.trim()) }
                            .filter { it.title.isNotBlank() }
                    )
                },
                enabled = validCount > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.voice_add_count, validCount))
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(R.string.voice_disclaimer_cancel))
            }
        }
    }
}
