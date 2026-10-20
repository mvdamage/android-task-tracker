package com.learning.tasktracker.data

/**
 * Normalizes fields by [TaskKind] before persistence.
 * Birthdays/holidays are yearly all-day dated items; events require a date.
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
        val location: String,
        val floatingMonth: Int? = null,
        val floatingWeekday: Int? = null,
        val floatingOccurrence: FloatingOccurrence? = null
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
        floatingMonth: Int? = null,
        floatingWeekday: Int? = null,
        floatingOccurrence: FloatingOccurrence? = null,
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
            TaskKind.HOLIDAY -> {
                val floatingValid = FloatingHolidayRules.isValid(
                    floatingMonth ?: -1,
                    floatingWeekday ?: -1,
                    floatingOccurrence
                )
                val due = if (floatingValid) {
                    FloatingHolidayRules.nextOrSame(
                        today = fallbackDateEpochDay,
                        month = floatingMonth!!,
                        weekday = floatingWeekday!!,
                        occurrence = floatingOccurrence!!
                    )
                } else {
                    dueDateEpochDay ?: fallbackDateEpochDay
                }
                Normalized(
                    kind = kind,
                    priority = Priority.MEDIUM,
                    dueDateEpochDay = due,
                    recurrenceType = RecurrenceType.YEARLY,
                    recurrenceWeekdayMask = 0,
                    recurrenceEndEpochDay = null,
                    dueTimeMinutes = null,
                    dueTimeEndMinutes = null,
                    location = "",
                    floatingMonth = if (floatingValid) floatingMonth else null,
                    floatingWeekday = if (floatingValid) floatingWeekday else null,
                    floatingOccurrence = if (floatingValid) floatingOccurrence else null
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
