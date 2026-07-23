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
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun isOverdue(today: Long = DateUtils.todayEpochDay()): Boolean =
        DateUtils.isOverdue(dueDateEpochDay, isDone, today)
}
