package com.learning.tasktracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.learning.tasktracker.data.NoteDraftListItem
import com.learning.tasktracker.data.NoteEntity
import com.learning.tasktracker.data.NoteFormat
import com.learning.tasktracker.data.NoteListItemEntity
import com.learning.tasktracker.data.NoteTheme
import com.learning.tasktracker.ui.components.CircularTaskCheckbox
import com.learning.tasktracker.ui.components.EditorSectionTitle
import com.learning.tasktracker.ui.theme.extendedColors

sealed class NoteEditorState {
    data object Create : NoteEditorState()
    data class Edit(
        val note: NoteEntity,
        val listItems: List<NoteListItemEntity>
    ) : NoteEditorState()
}

data class NoteEditorResult(
    val title: String,
    val body: String,
    val format: NoteFormat,
    val theme: NoteTheme,
    val listItems: List<NoteDraftListItem>
)

private data class DraftListRow(
    val text: String,
    val isChecked: Boolean
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun NoteEditorSheet(
    state: NoteEditorState,
    onDismiss: () -> Unit,
    onSave: (NoteEditorResult) -> Unit,
    onDelete: (() -> Unit)?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val existing = (state as? NoteEditorState.Edit)?.note
    var title by remember(state) { mutableStateOf(existing?.title.orEmpty()) }
    var body by remember(state) { mutableStateOf(existing?.body.orEmpty()) }
    var format by remember(state) {
        mutableStateOf(existing?.format ?: NoteFormat.TEXT)
    }
    var theme by remember(state) {
        mutableStateOf(existing?.theme ?: NoteTheme.GENERAL)
    }
    val listRows = remember(state) {
        mutableStateListOf<DraftListRow>().also { list ->
            val items = (state as? NoteEditorState.Edit)?.listItems.orEmpty()
            list.addAll(items.map { DraftListRow(it.text, it.isChecked) })
        }
    }
    var newListItem by remember(state) { mutableStateOf("") }
    val canSave = title.isNotBlank() && (
        format == NoteFormat.TEXT ||
            listRows.any { it.text.isNotBlank() } ||
            newListItem.isNotBlank()
        )

    fun buildResult(): NoteEditorResult {
        val drafts = listRows
            .map { NoteDraftListItem(it.text, it.isChecked) }
            .toMutableList()
        if (format == NoteFormat.LIST && newListItem.isNotBlank()) {
            drafts += NoteDraftListItem(newListItem.trim(), false)
        }
        return NoteEditorResult(
            title = title,
            body = body,
            format = format,
            theme = theme,
            listItems = drafts
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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
                        if (canSave) onSave(buildResult())
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
                placeholder = { Text("Заголовок") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.extendedColors.divider
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
            EditorSectionTitle("Формат")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NoteFormat.entries.forEach { value ->
                    FilterChip(
                        selected = format == value,
                        onClick = { format = value },
                        label = { Text(value.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            EditorSectionTitle("Тема")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NoteTheme.entries.forEach { value ->
                    FilterChip(
                        selected = theme == value,
                        onClick = { theme = value },
                        label = { Text(value.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            if (format == NoteFormat.TEXT) {
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = { Text("Текст заметки") },
                    minLines = 5,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.extendedColors.divider
                    )
                )
            } else {
                EditorSectionTitle("Пункты списка")
                listRows.forEachIndexed { index, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularTaskCheckbox(
                            checked = row.isChecked,
                            onCheckedChange = {
                                listRows[index] = row.copy(isChecked = !row.isChecked)
                            },
                            size = 20.dp
                        )
                        OutlinedTextField(
                            value = row.text,
                            onValueChange = { listRows[index] = row.copy(text = it) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.extendedColors.divider
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = if (row.isChecked) {
                                    TextDecoration.LineThrough
                                } else {
                                    TextDecoration.None
                                }
                            )
                        )
                        IconButton(onClick = { listRows.removeAt(index) }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Удалить пункт"
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newListItem,
                        onValueChange = { newListItem = it },
                        placeholder = { Text("Новый пункт") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.extendedColors.divider
                        )
                    )
                    IconButton(
                        onClick = {
                            if (newListItem.isNotBlank()) {
                                listRows.add(DraftListRow(newListItem.trim(), false))
                                newListItem = ""
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Добавить пункт")
                    }
                }
            }

            if (onDelete != null) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDelete) {
                    Text("Удалить заметку", color = MaterialTheme.extendedColors.overdue)
                }
            }
        }
    }
}
