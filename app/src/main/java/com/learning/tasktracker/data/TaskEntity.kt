package com.learning.tasktracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val priority: Priority = Priority.MEDIUM,
    val isDone: Boolean = false,
    /** Calendar day of the task (LocalDate.toEpochDay). */
    val dueDateEpochDay: Long = DateUtils.todayEpochDay(),
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val recurrenceInterval: Int = 1,
    /** Bitmask for CUSTOM_DAYS: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64. */
    val recurrenceWeekdayMask: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun isOverdue(today: Long = DateUtils.todayEpochDay()): Boolean =
        DateUtils.isOverdue(dueDateEpochDay, isDone, today)

    val isRecurring: Boolean get() = recurrenceType != RecurrenceType.NONE
}
