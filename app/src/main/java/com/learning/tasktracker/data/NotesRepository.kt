package com.learning.tasktracker.data

import kotlinx.coroutines.flow.Flow

data class NoteDraftListItem(
    val text: String,
    val isChecked: Boolean = false
)

class NotesRepository(
    private val dao: NoteDao
) {
    fun observeNotes(): Flow<List<NoteEntity>> = dao.observeAll()

    fun observeListItems(): Flow<List<NoteListItemEntity>> = dao.observeAllListItems()

    suspend fun add(
        title: String,
        body: String,
        format: NoteFormat,
        theme: NoteTheme,
        listItems: List<NoteDraftListItem> = emptyList()
    ): Long {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return -1L
        val now = System.currentTimeMillis()
        val noteId = dao.insert(
            NoteEntity(
                title = trimmedTitle,
                body = if (format == NoteFormat.TEXT) body.trim() else "",
                format = format,
                theme = theme,
                createdAt = now,
                updatedAt = now
            )
        )
        if (format == NoteFormat.LIST) {
            val drafts = listItems
                .map { it.copy(text = it.text.trim()) }
                .filter { it.text.isNotEmpty() }
            dao.replaceListItems(
                noteId,
                drafts.mapIndexed { index, draft ->
                    NoteListItemEntity(
                        noteId = noteId,
                        text = draft.text,
                        isChecked = draft.isChecked,
                        sortOrder = index
                    )
                }
            )
        }
        return noteId
    }

    suspend fun update(
        note: NoteEntity,
        title: String,
        body: String,
        format: NoteFormat,
        theme: NoteTheme,
        listItems: List<NoteDraftListItem> = emptyList()
    ) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return
        val now = System.currentTimeMillis()
        dao.update(
            note.copy(
                title = trimmedTitle,
                body = if (format == NoteFormat.TEXT) body.trim() else "",
                format = format,
                theme = theme,
                updatedAt = now
            )
        )
        if (format == NoteFormat.LIST) {
            val drafts = listItems
                .map { it.copy(text = it.text.trim()) }
                .filter { it.text.isNotEmpty() }
            dao.replaceListItems(
                note.id,
                drafts.mapIndexed { index, draft ->
                    NoteListItemEntity(
                        noteId = note.id,
                        text = draft.text,
                        isChecked = draft.isChecked,
                        sortOrder = index,
                        updatedAt = now
                    )
                }
            )
        } else {
            dao.deleteListItemsForNote(note.id)
        }
    }

    suspend fun delete(note: NoteEntity) {
        dao.deleteListItemsForNote(note.id)
        dao.delete(note)
    }

    suspend fun toggleListItem(item: NoteListItemEntity) {
        dao.updateListItem(
            item.copy(
                isChecked = !item.isChecked,
                updatedAt = System.currentTimeMillis()
            )
        )
        dao.getById(item.noteId)?.let { note ->
            dao.update(note.copy(updatedAt = System.currentTimeMillis()))
        }
    }
}
