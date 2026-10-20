package com.learning.tasktracker.ui.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.learning.tasktracker.R
import com.learning.tasktracker.data.NoteEntity
import com.learning.tasktracker.data.NoteFormat
import com.learning.tasktracker.data.NoteTheme
import com.learning.tasktracker.ui.components.EditorSectionTitle
import com.learning.tasktracker.voice.NoteVoiceDraft

data class NoteVoiceConfirmResult(
    val title: String,
    val body: String,
    val format: NoteFormat,
    val theme: NoteTheme,
    val listItems: List<String>,
    /** When set, append [listItems] to this existing list note instead of creating a new one. */
    val appendToNoteId: Long? = null
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteVoiceConfirmSheet(
    draft: NoteVoiceDraft,
    listNotes: List<NoteEntity> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (NoteVoiceConfirmResult) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(draft) { mutableStateOf(draft.title) }
    var body by remember(draft) { mutableStateOf(draft.body) }
    var format by remember(draft) { mutableStateOf(draft.format) }
    var theme by remember(draft) { mutableStateOf(draft.theme) }
    var appendToNoteId by remember(draft) { mutableStateOf<Long?>(null) }
    val listItems = remember(draft) {
        mutableStateListOf<String>().also { list ->
            list.addAll(draft.listItems.ifEmpty { listOf("") })
        }
    }
    val appending = appendToNoteId != null
    val canSave = if (appending) {
        listItems.any { it.isNotBlank() }
    } else {
        title.isNotBlank() && (
            format == NoteFormat.TEXT || listItems.any { it.isNotBlank() }
            )
    }

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
                text = if (appending) {
                    stringResource(R.string.voice_confirm_note_append_title)
                } else {
                    stringResource(R.string.voice_confirm_note_title)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (listNotes.isNotEmpty()) {
                EditorSectionTitle("Куда добавить")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = appendToNoteId == null,
                        onClick = {
                            appendToNoteId = null
                            if (format == NoteFormat.TEXT && listItems.none { it.isNotBlank() }) {
                                // keep current
                            }
                        },
                        label = { Text("Новая заметка") }
                    )
                    listNotes.forEach { note ->
                        FilterChip(
                            selected = appendToNoteId == note.id,
                            onClick = {
                                appendToNoteId = note.id
                                format = NoteFormat.LIST
                                if (listItems.none { it.isNotBlank() } && title.isNotBlank()) {
                                    listItems.clear()
                                    listItems.add(title)
                                }
                            },
                            label = {
                                Text(
                                    note.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!appending) {
                EditorSectionTitle("Формат")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NoteFormat.entries.forEach { option ->
                        FilterChip(
                            selected = format == option,
                            onClick = { format = option },
                            label = { Text(option.label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                EditorSectionTitle("Тема")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NoteTheme.entries.forEach { option ->
                        FilterChip(
                            selected = theme == option,
                            onClick = { theme = option },
                            label = { Text(option.label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.voice_confirm_note_title_hint)) },
                    singleLine = true,
                    supportingText = if (draft.truncated) {
                        { Text(stringResource(R.string.voice_title_truncated)) }
                    } else {
                        null
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
            if (!appending && format == NoteFormat.TEXT) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.voice_confirm_note_body_hint)) },
                    minLines = 3,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else if (appending || format == NoteFormat.LIST) {
                Spacer(modifier = Modifier.height(8.dp))
                EditorSectionTitle("Пункты")
                listItems.forEachIndexed { index, item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedTextField(
                            value = item,
                            onValueChange = { listItems[index] = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text("Пункт ${index + 1}") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        if (listItems.size > 1) {
                            IconButton(onClick = { listItems.removeAt(index) }) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = stringResource(R.string.voice_remove_line)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                TextButton(
                    onClick = { listItems.add("") }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Ещё пункт")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.voice_disclaimer_cancel))
                }
                Button(
                    onClick = {
                        onConfirm(
                            NoteVoiceConfirmResult(
                                title = title.trim(),
                                body = body.trim(),
                                format = if (appending) NoteFormat.LIST else format,
                                theme = theme,
                                listItems = listItems.map { it.trim() }.filter { it.isNotEmpty() },
                                appendToNoteId = appendToNoteId
                            )
                        )
                    },
                    enabled = canSave
                ) {
                    Text(stringResource(R.string.voice_add))
                }
            }
        }
    }
}
