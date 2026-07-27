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
        dueDateEpochDay: Long? = null,
        recurrenceType: RecurrenceType = RecurrenceType.NONE,
        recurrenceWeekdayMask: Int = 0,
        recurrenceEndEpochDay: Long? = null,
        dueTimeMinutes: Int? = null,
        dueTimeEndMinutes: Int? = null
    ) {
        val alignedDue = DateUtils.alignDueDate(
            dueDateEpochDay,
            recurrenceType,
            recurrenceWeekdayMask
        )
        val (startTime, endTime) = DateUtils.normalizedTimePeriod(dueTimeMinutes, dueTimeEndMinutes)
        dao.insert(
            TaskEntity(
                title = title.trim(),
                notes = notes.trim(),
                priority = priority,
                dueDateEpochDay = alignedDue,
                recurrenceType = recurrenceType,
                recurrenceWeekdayMask = recurrenceWeekdayMask,
                recurrenceEndEpochDay = recurrenceEndEpochDay,
                dueTimeMinutes = if (alignedDue == null) null else startTime,
                dueTimeEndMinutes = if (alignedDue == null) null else endTime
            )
        )
    }

    suspend fun update(task: TaskEntity) {
        val alignedDue = DateUtils.alignDueDate(
            task.dueDateEpochDay,
            task.recurrenceType,
            task.recurrenceWeekdayMask
        )
        val (startTime, endTime) = DateUtils.normalizedTimePeriod(
            task.dueTimeMinutes,
            task.dueTimeEndMinutes
        )
        dao.update(
            task.copy(
                dueDateEpochDay = alignedDue,
                dueTimeMinutes = if (alignedDue == null) null else startTime,
                dueTimeEndMinutes = if (alignedDue == null) null else endTime,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun moveToDay(task: TaskEntity, targetEpochDay: Long?) {
        if (task.dueDateEpochDay == targetEpochDay) return
        if (targetEpochDay == null) {
            dao.update(
                task.copy(
                    dueDateEpochDay = null,
                    dueTimeMinutes = null,
                    dueTimeEndMinutes = null,
                    recurrenceType = RecurrenceType.NONE,
                    recurrenceWeekdayMask = 0,
                    recurrenceEndEpochDay = null,
                    updatedAt = System.currentTimeMillis()
                )
            )
            return
        }
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
        val due = task.dueDateEpochDay
        if (!task.isDone && task.isRecurring && due != null) {
            val next = DateUtils.nextDueDate(
                due,
                task.recurrenceType,
                task.recurrenceInterval,
                task.recurrenceWeekdayMask,
                task.recurrenceEndEpochDay
            )
            if (next != null) {
                val completed = task.copy(isDone = true, updatedAt = now)
                val spawned = task.copy(
                    id = 0,
                    isDone = false,
                    dueDateEpochDay = next,
                    createdAt = now,
                    updatedAt = now
                )
                val newTaskId = dao.completeRecurringOccurrence(completed, spawned)
                subtaskDao.listForParent(task.id).forEach { subtask ->
                    subtaskDao.insert(
                        subtask.copy(
                            id = 0,
                            parentTaskId = newTaskId,
                            isDone = false,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }
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
        dao.deleteCompleted()
    }

    suspend fun processDayRolloverIfNeeded(today: Long = DateUtils.todayEpochDay()) {
        val last = dayRolloverStore.lastCleanupEpochDay()
        if (last < 0) {
            dayRolloverStore.setLastCleanupEpochDay(today)
            return
        }
        if (last < today) {
            dao.deleteCompleted()
            dao.getOverdueRecurring(today).forEach { task ->
                val end = task.recurrenceEndEpochDay
                if (end != null && today > end) {
                    dao.update(
                        task.copy(isDone = true, updatedAt = System.currentTimeMillis())
                    )
                    return@forEach
                }
                val due = task.dueDateEpochDay ?: return@forEach
                val caughtUp = DateUtils.catchUpDueDate(
                    dueDate = due,
                    today = today,
                    type = task.recurrenceType,
                    interval = task.recurrenceInterval,
                    weekdayMask = task.recurrenceWeekdayMask,
                    endEpochDay = end
                )
                if (caughtUp != due) {
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
