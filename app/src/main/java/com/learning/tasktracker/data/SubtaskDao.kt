package com.learning.tasktracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks ORDER BY parentTaskId ASC, sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE parentTaskId = :parentTaskId ORDER BY sortOrder ASC, id ASC")
    fun observeForParent(parentTaskId: Long): Flow<List<SubtaskEntity>>

    @Query("SELECT COUNT(*) FROM subtasks WHERE parentTaskId = :parentTaskId")
    suspend fun countForParent(parentTaskId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subtask: SubtaskEntity): Long

    @Update
    suspend fun update(subtask: SubtaskEntity)

    @Delete
    suspend fun delete(subtask: SubtaskEntity)

    @Query("DELETE FROM subtasks WHERE parentTaskId = :parentTaskId")
    suspend fun deleteForParent(parentTaskId: Long)
}
