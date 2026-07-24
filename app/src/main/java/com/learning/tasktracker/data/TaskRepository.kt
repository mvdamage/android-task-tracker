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
        dueDateEpochDay: Long = DateUtils.todayEpochDay(),
        recurrenceType: RecurrenceType = RecurrenceType.NONE,
        recurrenceWeekdayMask: Int = 0,
        dueTimeMinutes: Int? = null
    ) {
        val alignedDue = DateUtils.alignToRecurrence(
            dueDateEpochDay,
            recurrenceType,
            recurrenceWeekdayMask
        )
        dao.insert(
            TaskEntity(
                title = title.trim(),
                notes = notes.trim(),
                priority = priority,
                dueDateEpochDay = alignedDue,
                recurrenceType = recurrenceType,
                recurrenceWeekdayMask = recurrenceWeekdayMask,
                dueTimeMinutes = dueTimeMinutes
            )
        )
    }

    suspend fun update(task: TaskEntity) {
        val alignedDue = DateUtils.alignToRecurrence(
            task.dueDateEpochDay,
            task.recurrenceType,
            task.recurrenceWeekdayMask
        )
        dao.update(task.copy(dueDateEpochDay = alignedDue, updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleDone(task: TaskEntity) {
        val now = System.currentTimeMillis()
        if (!task.isDone && task.isRecurring) {
            val next = DateUtils.nextDueDate(
                task.dueDateEpochDay,
                task.recurrenceType,
                task.recurrenceInterval,
                task.recurrenceWeekdayMask
            )
            if (next != null) {
                dao.update(
                    task.copy(
                        isDone = false,
                        dueDateEpochDay = next,
                        updatedAt = now
                    )
                )
                return
            }
        }
        dao.update(task.copy(isDone = !task.isDone, updatedAt = now))
    }

    suspend fun delete(task: TaskEntity) {
        dao.delete(task)
    }

    suspend fun clearCompleted() {
        dao.deleteCompletedNonRecurring()
    }

    suspend fun processDayRolloverIfNeeded(today: Long = DateUtils.todayEpochDay()) {
        val last = dayRolloverStore.lastCleanupEpochDay()
        if (last < 0) {
            dayRolloverStore.setLastCleanupEpochDay(today)
            return
        }
        if (last < today) {
            dao.deleteCompletedNonRecurring()
            dao.getOverdueRecurring(today).forEach { task ->
                val caughtUp = DateUtils.catchUpDueDate(
                    dueDate = task.dueDateEpochDay,
                    today = today,
                    type = task.recurrenceType,
                    interval = task.recurrenceInterval,
                    weekdayMask = task.recurrenceWeekdayMask
                )
                if (caughtUp != task.dueDateEpochDay) {
                    dao.update(
                        task.copy(
                            dueDateEpochDay = caughtUp,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            dayRolloverStore.setLastCleanupEpochDay(today)
        }
    }
}
