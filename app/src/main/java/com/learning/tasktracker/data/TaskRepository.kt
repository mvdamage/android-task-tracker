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
        dueTimeEndMinutes: Int? = null,
        kind: TaskKind = TaskKind.TASK,
        location: String = ""
    ) {
        val normalized = TaskKindRules.normalize(
            kind = kind,
            priority = priority,
            dueDateEpochDay = dueDateEpochDay,
            recurrenceType = recurrenceType,
            recurrenceWeekdayMask = recurrenceWeekdayMask,
            recurrenceEndEpochDay = recurrenceEndEpochDay,
            dueTimeMinutes = dueTimeMinutes,
            dueTimeEndMinutes = dueTimeEndMinutes,
            location = location
        )
        val alignedDue = DateUtils.alignDueDate(
            normalized.dueDateEpochDay,
            normalized.recurrenceType,
            normalized.recurrenceWeekdayMask
        )
        val (startTime, endTime) = DateUtils.normalizedTimePeriod(
            normalized.dueTimeMinutes,
            normalized.dueTimeEndMinutes
        )
        dao.insert(
            TaskEntity(
                title = title.trim(),
                notes = notes.trim(),
                location = normalized.location,
                priority = normalized.priority,
                kind = normalized.kind,
                dueDateEpochDay = alignedDue,
                recurrenceType = normalized.recurrenceType,
                recurrenceWeekdayMask = normalized.recurrenceWeekdayMask,
                recurrenceEndEpochDay = normalized.recurrenceEndEpochDay,
                dueTimeMinutes = if (alignedDue == null) null else startTime,
                dueTimeEndMinutes = if (alignedDue == null) null else endTime
            )
        )
    }

    suspend fun update(task: TaskEntity) {
        val normalized = TaskKindRules.normalize(
            kind = task.kind,
            priority = task.priority,
            dueDateEpochDay = task.dueDateEpochDay,
            recurrenceType = task.recurrenceType,
            recurrenceWeekdayMask = task.recurrenceWeekdayMask,
            recurrenceEndEpochDay = task.recurrenceEndEpochDay,
            dueTimeMinutes = task.dueTimeMinutes,
            dueTimeEndMinutes = task.dueTimeEndMinutes,
            location = task.location
        )
        val alignedDue = DateUtils.alignDueDate(
            normalized.dueDateEpochDay,
            normalized.recurrenceType,
            normalized.recurrenceWeekdayMask
        )
        val (startTime, endTime) = DateUtils.normalizedTimePeriod(
            normalized.dueTimeMinutes,
            normalized.dueTimeEndMinutes
        )
        dao.update(
            task.copy(
                priority = normalized.priority,
                kind = normalized.kind,
                location = normalized.location,
                dueDateEpochDay = alignedDue,
                recurrenceType = normalized.recurrenceType,
                recurrenceWeekdayMask = normalized.recurrenceWeekdayMask,
                recurrenceEndEpochDay = normalized.recurrenceEndEpochDay,
                dueTimeMinutes = if (alignedDue == null) null else startTime,
                dueTimeEndMinutes = if (alignedDue == null) null else endTime,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun moveToDay(task: TaskEntity, targetEpochDay: Long?) {
        if (task.dueDateEpochDay == targetEpochDay) return
        if (targetEpochDay == null) {
            // Calendar items must keep a date (and birthdays keep yearly recurrence).
            if (task.kind == TaskKind.EVENT || task.kind == TaskKind.BIRTHDAY) return
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
        val current = dao.getById(task.id) ?: return
        val now = System.currentTimeMillis()
        val due = current.dueDateEpochDay

        // Birthdays: mark done/undone only — never spawn a duplicate next year.
        if (current.kind == TaskKind.BIRTHDAY) {
            dao.update(current.copy(isDone = !current.isDone, updatedAt = now))
            return
        }

        if (!current.isDone && current.isRecurring && due != null) {
            val next = DateUtils.nextDueDate(
                due,
                current.recurrenceType,
                current.recurrenceInterval,
                current.recurrenceWeekdayMask,
                current.recurrenceEndEpochDay
            )
            if (next != null) {
                val completed = current.copy(isDone = true, updatedAt = now)
                val spawned = current.copy(
                    id = 0,
                    isDone = false,
                    dueDateEpochDay = next,
                    createdAt = now,
                    updatedAt = now
                )
                val newTaskId = dao.completeRecurringOccurrence(completed, spawned)
                subtaskDao.listForParent(current.id).forEach { subtask ->
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
            dao.update(current.copy(isDone = true, updatedAt = now))
            return
        }

        // Undo of a completed recurring occurrence: remove the spawned next copy first.
        if (current.isDone && current.isRecurring && due != null) {
            val next = DateUtils.nextDueDate(
                due,
                current.recurrenceType,
                current.recurrenceInterval,
                current.recurrenceWeekdayMask,
                current.recurrenceEndEpochDay
            )
            if (next != null) {
                val spawned = dao.findActiveContinuation(
                    excludeId = current.id,
                    title = current.title,
                    kind = current.kind,
                    recurrenceType = current.recurrenceType,
                    recurrenceInterval = current.recurrenceInterval,
                    recurrenceWeekdayMask = current.recurrenceWeekdayMask,
                    dueDateEpochDay = next
                )
                if (spawned != null) {
                    subtaskDao.deleteForParent(spawned.id)
                    dao.delete(spawned)
                }
            }
            dao.update(current.copy(isDone = false, updatedAt = now))
            return
        }

        dao.update(current.copy(isDone = !current.isDone, updatedAt = now))
    }

    suspend fun updateNotes(taskId: Long, notes: String) {
        val current = dao.getById(taskId) ?: return
        val trimmed = notes.trim()
        if (current.notes == trimmed) return
        dao.update(
            current.copy(
                notes = trimmed,
                updatedAt = System.currentTimeMillis()
            )
        )
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
            // Completed past birthdays → next year (keep the reminder, no catch-up while active).
            dao.getCompletedPastBirthdays(today).forEach { birthday ->
                val due = birthday.dueDateEpochDay ?: return@forEach
                val next = DateUtils.nextDueDate(
                    currentDue = due,
                    type = RecurrenceType.YEARLY,
                    interval = birthday.recurrenceInterval.coerceAtLeast(1),
                    weekdayMask = 0,
                    endEpochDay = null
                ) ?: return@forEach
                dao.update(
                    birthday.copy(
                        dueDateEpochDay = next,
                        isDone = false,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            dao.deleteCompleted()
            // Do not auto-advance overdue recurring tasks: they stay on the missed
            // period until the user checks them off (toggleDone then spawns next).
            dayRolloverStore.setLastCleanupEpochDay(today)
        }
    }
}
