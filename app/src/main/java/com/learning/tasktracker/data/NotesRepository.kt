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

    /** Deletes list notes where every item is checked (and there is at least one item). */
    suspend fun deleteCompletedListNotes() {
        dao.getListNotes().forEach { note ->
            val items = dao.listItemsForNote(note.id)
            if (items.isNotEmpty() && items.all { it.isChecked }) {
                delete(note)
            }
        }
    }

    /** Removes every checked list item across all notes. */
    suspend fun clearCompletedListItems() {
        dao.deleteCheckedListItems()
        val now = System.currentTimeMillis()
        dao.getListNotes().forEach { note ->
            dao.update(note.copy(updatedAt = now))
        }
    }

    suspend fun appendListItems(noteId: Long, texts: List<String>) {
        val note = dao.getById(noteId) ?: return
        if (note.format != NoteFormat.LIST) return
        val trimmed = texts.map { it.trim() }.filter { it.isNotEmpty() }
        if (trimmed.isEmpty()) return
        val existing = dao.listItemsForNote(noteId)
        var order = existing.size
        val now = System.currentTimeMillis()
        trimmed.forEach { text ->
            dao.insertListItem(
                NoteListItemEntity(
                    noteId = noteId,
                    text = text,
                    isChecked = false,
                    sortOrder = order++,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
        dao.update(note.copy(updatedAt = now))
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
