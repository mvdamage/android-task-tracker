package com.learning.tasktracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    /** Venue / place for events; empty for tasks and birthdays. */
    val location: String = "",
    val priority: Priority = Priority.MEDIUM,
    val kind: TaskKind = TaskKind.TASK,
    val isDone: Boolean = false,
    /** Calendar day of the task (LocalDate.toEpochDay); null = no due date. */
    val dueDateEpochDay: Long? = null,
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val recurrenceInterval: Int = 1,
    /** Bitmask for CUSTOM_DAYS: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64. */
    val recurrenceWeekdayMask: Int = 0,
    /** Last calendar day for recurrence; null = no end date. */
    val recurrenceEndEpochDay: Long? = null,
    /** Minutes from midnight (0–1439); null = all-day task. */
    val dueTimeMinutes: Int? = null,
    /** End of time window (exclusive display as range); null = point in time only. */
    val dueTimeEndMinutes: Int? = null,
    /** 1–12 when holiday uses a floating rule; otherwise null. */
    val floatingMonth: Int? = null,
    /** [java.time.DayOfWeek] value 1=Mon…7=Sun for floating holidays. */
    val floatingWeekday: Int? = null,
    val floatingOccurrence: FloatingOccurrence? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun isOverdue(today: Long = DateUtils.todayEpochDay()): Boolean =
        kind == TaskKind.TASK &&
            DateUtils.isOverdue(dueDateEpochDay, dueTimeMinutes, dueTimeEndMinutes, isDone, today)

    val isRecurring: Boolean get() = recurrenceType != RecurrenceType.NONE

    val isCalendarItem: Boolean get() = kind != TaskKind.TASK

    val hasFloatingHolidayRule: Boolean
        get() = kind == TaskKind.HOLIDAY &&
            FloatingHolidayRules.isValid(
                floatingMonth ?: -1,
                floatingWeekday ?: -1,
                floatingOccurrence
            )
}
