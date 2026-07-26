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
interface TaskDao {
    @Query(
        """
        SELECT * FROM tasks
        ORDER BY
            CASE WHEN dueDateEpochDay IS NULL THEN 1 ELSE 0 END,
            dueDateEpochDay ASC,
            isDone ASC,
            CASE priority
                WHEN 'HIGH' THEN 0
                WHEN 'MEDIUM' THEN 1
                WHEN 'LOW' THEN 2
                ELSE 3
            END ASC,
            CASE WHEN dueTimeMinutes IS NULL THEN 1 ELSE 0 END ASC,
            dueTimeMinutes ASC,
            updatedAt DESC
        """
    )
    fun observeAll(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Transaction
    suspend fun completeRecurringOccurrence(completed: TaskEntity, next: TaskEntity): Long {
        update(completed)
        return insert(next)
    }

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE isDone = 1")
    suspend fun deleteCompleted()

    @Query(
        """
        SELECT * FROM tasks
        WHERE recurrenceType != 'NONE' AND isDone = 0
            AND dueDateEpochDay IS NOT NULL AND dueDateEpochDay < :today
        """
    )
    suspend fun getOverdueRecurring(today: Long): List<TaskEntity>
}
