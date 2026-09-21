package com.learning.tasktracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM note_list_items ORDER BY noteId ASC, sortOrder ASC, id ASC")
    fun observeAllListItems(): Flow<List<NoteListItemEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): NoteEntity?

    @Query(
        """
        SELECT * FROM note_list_items
        WHERE noteId = :noteId
        ORDER BY sortOrder ASC, id ASC
        """
    )
    suspend fun listItemsForNote(noteId: Long): List<NoteListItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListItem(item: NoteListItemEntity): Long

    @Update
    suspend fun updateListItem(item: NoteListItemEntity)

    @Delete
    suspend fun deleteListItem(item: NoteListItemEntity)

    @Query("DELETE FROM note_list_items WHERE noteId = :noteId")
    suspend fun deleteListItemsForNote(noteId: Long)

    @Transaction
    suspend fun replaceListItems(noteId: Long, items: List<NoteListItemEntity>) {
        deleteListItemsForNote(noteId)
        items.forEachIndexed { index, item ->
            insertListItem(
                item.copy(
                    id = 0,
                    noteId = noteId,
                    sortOrder = index
                )
            )
        }
    }
}
