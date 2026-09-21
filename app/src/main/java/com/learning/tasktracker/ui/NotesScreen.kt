package com.learning.tasktracker.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learning.tasktracker.data.NoteEntity
import com.learning.tasktracker.data.NoteFormat
import com.learning.tasktracker.data.NoteListItemEntity
import com.learning.tasktracker.ui.components.AnyDoDivider
import com.learning.tasktracker.ui.components.CircularTaskCheckbox
import com.learning.tasktracker.ui.theme.extendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(viewModel: NotesViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<NoteEditorState?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        "Заметки",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editor = NoteEditorState.Create },
                modifier = Modifier.testTag(TestTags.ADD_NOTE_FAB),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить заметку")
            }
        }
    ) { padding ->
        if (state.notes.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Notes,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = "Пока нет заметок",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Добавьте текстовую заметку или список",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 88.dp
                )
            ) {
                state.groups.forEach { group ->
                    item(key = "theme_${group.theme.name}") {
                        Text(
                            text = group.theme.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp)
                        )
                    }
                    items(group.notes, key = { it.id }) { note ->
                        val listItems = state.listItemsByNoteId[note.id].orEmpty()
                        NoteRow(
                            note = note,
                            listItems = listItems,
                            onClick = {
                                editor = NoteEditorState.Edit(note, listItems)
                            },
                            onToggleListItem = viewModel::toggleListItem
                        )
                        AnyDoDivider()
                    }
                }
            }
        }
    }

    editor?.let { current ->
        NoteEditorSheet(
            state = current,
            onDismiss = { editor = null },
            onSave = { result ->
                when (current) {
                    is NoteEditorState.Create -> viewModel.addNote(
                        title = result.title,
                        body = result.body,
                        format = result.format,
                        theme = result.theme,
                        listItems = result.listItems
                    )
                    is NoteEditorState.Edit -> viewModel.updateNote(
                        note = current.note,
                        title = result.title,
                        body = result.body,
                        format = result.format,
                        theme = result.theme,
                        listItems = result.listItems
                    )
                }
                editor = null
            },
            onDelete = if (current is NoteEditorState.Edit) {
                {
                    viewModel.deleteNote(current.note)
                    editor = null
                }
            } else {
                null
            }
        )
    }
}

@Composable
private fun NoteRow(
    note: NoteEntity,
    listItems: List<NoteListItemEntity>,
    onClick: () -> Unit,
    onToggleListItem: (NoteListItemEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = note.format.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        when (note.format) {
            NoteFormat.TEXT -> {
                if (note.body.isNotBlank()) {
                    Text(
                        text = note.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            NoteFormat.LIST -> {
                if (listItems.isEmpty()) {
                    Text(
                        text = "Пустой список",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.extendedColors.divider
                    )
                } else {
                    listItems.take(4).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularTaskCheckbox(
                                checked = item.isChecked,
                                onCheckedChange = { onToggleListItem(item) },
                                size = 18.dp
                            )
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (item.isChecked) {
                                    TextDecoration.LineThrough
                                } else {
                                    null
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    if (listItems.size > 4) {
                        Text(
                            text = "ещё ${listItems.size - 4}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, start = 26.dp)
                        )
                    }
                }
            }
        }
    }
}
