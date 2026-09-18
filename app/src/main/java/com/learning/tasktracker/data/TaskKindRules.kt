package com.learning.tasktracker.data

/**
 * Normalizes fields by [TaskKind] before persistence.
 * Birthdays are yearly all-day dated items; events require a date.
 */
object TaskKindRules {
    data class Normalized(
        val kind: TaskKind,
        val priority: Priority,
        val dueDateEpochDay: Long?,
        val recurrenceType: RecurrenceType,
        val recurrenceWeekdayMask: Int,
        val recurrenceEndEpochDay: Long?,
        val dueTimeMinutes: Int?,
        val dueTimeEndMinutes: Int?,
        val location: String
    )

    fun normalize(
        kind: TaskKind,
        priority: Priority,
        dueDateEpochDay: Long?,
        recurrenceType: RecurrenceType,
        recurrenceWeekdayMask: Int,
        recurrenceEndEpochDay: Long?,
        dueTimeMinutes: Int?,
        dueTimeEndMinutes: Int?,
        location: String = "",
        fallbackDateEpochDay: Long = DateUtils.todayEpochDay()
    ): Normalized {
        return when (kind) {
            TaskKind.TASK -> Normalized(
                kind = kind,
                priority = priority,
                dueDateEpochDay = dueDateEpochDay,
                recurrenceType = recurrenceType,
                recurrenceWeekdayMask = if (recurrenceType == RecurrenceType.CUSTOM_DAYS) {
                    recurrenceWeekdayMask
                } else {
                    0
                },
                recurrenceEndEpochDay = if (recurrenceType == RecurrenceType.NONE) {
                    null
                } else {
                    recurrenceEndEpochDay
                },
                dueTimeMinutes = dueTimeMinutes,
                dueTimeEndMinutes = dueTimeEndMinutes,
                location = ""
            )
            TaskKind.EVENT -> {
                val due = dueDateEpochDay ?: fallbackDateEpochDay
                Normalized(
                    kind = kind,
                    priority = Priority.MEDIUM,
                    dueDateEpochDay = due,
                    recurrenceType = recurrenceType,
                    recurrenceWeekdayMask = if (recurrenceType == RecurrenceType.CUSTOM_DAYS) {
                        recurrenceWeekdayMask
                    } else {
                        0
                    },
                    recurrenceEndEpochDay = if (recurrenceType == RecurrenceType.NONE) {
                        null
                    } else {
                        recurrenceEndEpochDay
                    },
                    dueTimeMinutes = dueTimeMinutes,
                    dueTimeEndMinutes = dueTimeEndMinutes,
                    location = location.trim()
                )
            }
            TaskKind.BIRTHDAY -> {
                val due = dueDateEpochDay ?: fallbackDateEpochDay
                Normalized(
                    kind = kind,
                    priority = Priority.MEDIUM,
                    dueDateEpochDay = due,
                    recurrenceType = RecurrenceType.YEARLY,
                    recurrenceWeekdayMask = 0,
                    recurrenceEndEpochDay = null,
                    dueTimeMinutes = null,
                    dueTimeEndMinutes = null,
                    location = ""
                )
            }
        }
    }
}
