package com.learning.tasktracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.learning.tasktracker.data.NoteDraftListItem
import com.learning.tasktracker.data.NoteEntity
import com.learning.tasktracker.data.NoteFormat
import com.learning.tasktracker.data.NoteListItemEntity
import com.learning.tasktracker.data.NoteTheme
import com.learning.tasktracker.data.NotesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotesUiState(
    val notes: List<NoteEntity> = emptyList(),
    val listItemsByNoteId: Map<Long, List<NoteListItemEntity>> = emptyMap(),
    val groups: List<NoteThemeGroup> = emptyList()
)

data class NoteThemeGroup(
    val theme: NoteTheme,
    val notes: List<NoteEntity>
)

class NotesViewModel(
    private val repository: NotesRepository
) : ViewModel() {
    val uiState: StateFlow<NotesUiState> = combine(
        repository.observeNotes(),
        repository.observeListItems()
    ) { notes, items ->
        val byNoteId = items.groupBy { it.noteId }
        NotesUiState(
            notes = notes,
            listItemsByNoteId = byNoteId,
            groups = NoteTheme.entries.mapNotNull { theme ->
                val themeNotes = notes.filter { it.theme == theme }
                if (themeNotes.isEmpty()) null else NoteThemeGroup(theme, themeNotes)
            }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        NotesUiState()
    )

    fun addNote(
        title: String,
        body: String,
        format: NoteFormat,
        theme: NoteTheme,
        listItems: List<NoteDraftListItem>
    ) {
        viewModelScope.launch {
            repository.add(title, body, format, theme, listItems)
        }
    }

    fun updateNote(
        note: NoteEntity,
        title: String,
        body: String,
        format: NoteFormat,
        theme: NoteTheme,
        listItems: List<NoteDraftListItem>
    ) {
        viewModelScope.launch {
            repository.update(note, title, body, format, theme, listItems)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch { repository.delete(note) }
    }

    fun toggleListItem(item: NoteListItemEntity) {
        viewModelScope.launch { repository.toggleListItem(item) }
    }

    class Factory(
        private val repository: NotesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotesViewModel(repository) as T
        }
    }
}
