package com.learning.tasktracker.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val dao: TaskDao,
    private val dayRolloverStore: DayRolloverStore
) {
    fun observeTasks(): Flow<List<TaskEntity>> = dao.observeAll()

    suspend fun add(
        title: String,
        notes: String,
        priority: Priority,
        dueDateEpochDay: Long = DateUtils.todayEpochDay()
    ) {
        dao.insert(
            TaskEntity(
                title = title.trim(),
                notes = notes.trim(),
                priority = priority,
                dueDateEpochDay = dueDateEpochDay
            )
        )
    }

    suspend fun update(task: TaskEntity) {
        dao.update(task.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleDone(task: TaskEntity) {
        dao.update(
            task.copy(
                isDone = !task.isDone,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun delete(task: TaskEntity) {
        dao.delete(task)
    }

    suspend fun clearCompleted() {
        dao.deleteCompleted()
    }

    /**
     * When the calendar day advances, wipe yesterday's completed tasks.
     * Incomplete tasks remain and become overdue.
     */
    suspend fun processDayRolloverIfNeeded(today: Long = DateUtils.todayEpochDay()) {
        val last = dayRolloverStore.lastCleanupEpochDay()
        if (last < 0) {
            dayRolloverStore.setLastCleanupEpochDay(today)
            return
        }
        if (last < today) {
            dao.deleteCompleted()
            dayRolloverStore.setLastCleanupEpochDay(today)
        }
    }
}
