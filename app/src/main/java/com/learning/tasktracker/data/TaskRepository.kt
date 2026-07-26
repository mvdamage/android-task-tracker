package com.learning.tasktracker.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val dao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val dayRolloverStore: DayRolloverStore
) {
    fun observeTasks(): Flow<List<TaskEntity>> = dao.observeAll()

    fun observeSubtasks(): Flow<List<SubtaskEntity>> = subtaskDao.observeAll()

    suspend fun add(
        title: String,
        notes: String,
        priority: Priority,
        dueDateEpochDay: Long = DateUtils.todayEpochDay(),
        recurrenceType: RecurrenceType = RecurrenceType.NONE,
        recurrenceWeekdayMask: Int = 0,
        recurrenceEndEpochDay: Long? = null,
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
                recurrenceEndEpochDay = recurrenceEndEpochDay,
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

    suspend fun moveToDay(task: TaskEntity, targetEpochDay: Long) {
        if (task.dueDateEpochDay == targetEpochDay) return
        val alignedDue = DateUtils.alignToRecurrence(
            targetEpochDay,
            task.recurrenceType,
            task.recurrenceWeekdayMask
        )
        dao.update(
            task.copy(
                dueDateEpochDay = alignedDue,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleDone(task: TaskEntity) {
        val now = System.currentTimeMillis()
        if (!task.isDone && task.isRecurring) {
            val next = DateUtils.nextDueDate(
                task.dueDateEpochDay,
                task.recurrenceType,
                task.recurrenceInterval,
                task.recurrenceWeekdayMask,
                task.recurrenceEndEpochDay
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
            dao.update(task.copy(isDone = true, updatedAt = now))
            return
        }
        dao.update(task.copy(isDone = !task.isDone, updatedAt = now))
    }

    suspend fun delete(task: TaskEntity) {
        subtaskDao.deleteForParent(task.id)
        dao.delete(task)
    }

    suspend fun addSubtask(parentTaskId: Long, title: String) {
        if (title.isBlank()) return
        val sortOrder = subtaskDao.countForParent(parentTaskId)
        subtaskDao.insert(
            SubtaskEntity(
                parentTaskId = parentTaskId,
                title = title.trim(),
                sortOrder = sortOrder
            )
        )
    }

    suspend fun toggleSubtask(subtask: SubtaskEntity) {
        subtaskDao.update(
            subtask.copy(isDone = !subtask.isDone, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun deleteSubtask(subtask: SubtaskEntity) {
        subtaskDao.delete(subtask)
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
                val end = task.recurrenceEndEpochDay
                if (end != null && today > end) {
                    dao.update(
                        task.copy(isDone = true, updatedAt = System.currentTimeMillis())
                    )
                    return@forEach
                }
                val caughtUp = DateUtils.catchUpDueDate(
                    dueDate = task.dueDateEpochDay,
                    today = today,
                    type = task.recurrenceType,
                    interval = task.recurrenceInterval,
                    weekdayMask = task.recurrenceWeekdayMask,
                    endEpochDay = end
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
