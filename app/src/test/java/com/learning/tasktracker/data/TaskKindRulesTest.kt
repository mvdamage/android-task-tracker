package com.learning.tasktracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskKindRulesTest {
    @Test
    fun birthdayForcesYearlyAllDayDated() {
        val result = TaskKindRules.normalize(
            kind = TaskKind.BIRTHDAY,
            priority = Priority.HIGH,
            dueDateEpochDay = null,
            recurrenceType = RecurrenceType.NONE,
            recurrenceWeekdayMask = 7,
            recurrenceEndEpochDay = 100,
            dueTimeMinutes = 600,
            dueTimeEndMinutes = 660,
            fallbackDateEpochDay = 10
        )
        assertEquals(TaskKind.BIRTHDAY, result.kind)
        assertEquals(Priority.MEDIUM, result.priority)
        assertEquals(10L, result.dueDateEpochDay)
        assertEquals(RecurrenceType.YEARLY, result.recurrenceType)
        assertEquals(0, result.recurrenceWeekdayMask)
        assertNull(result.recurrenceEndEpochDay)
        assertNull(result.dueTimeMinutes)
        assertNull(result.dueTimeEndMinutes)
    }

    @Test
    fun eventRequiresDateAndKeepsTime() {
        val result = TaskKindRules.normalize(
            kind = TaskKind.EVENT,
            priority = Priority.HIGH,
            dueDateEpochDay = null,
            recurrenceType = RecurrenceType.NONE,
            recurrenceWeekdayMask = 0,
            recurrenceEndEpochDay = null,
            dueTimeMinutes = 600,
            dueTimeEndMinutes = null,
            fallbackDateEpochDay = 42
        )
        assertEquals(TaskKind.EVENT, result.kind)
        assertEquals(42L, result.dueDateEpochDay)
        assertEquals(600, result.dueTimeMinutes)
        assertEquals(Priority.MEDIUM, result.priority)
    }

    @Test
    fun taskKeepsFields() {
        val result = TaskKindRules.normalize(
            kind = TaskKind.TASK,
            priority = Priority.HIGH,
            dueDateEpochDay = 5,
            recurrenceType = RecurrenceType.WEEKLY,
            recurrenceWeekdayMask = 0,
            recurrenceEndEpochDay = 50,
            dueTimeMinutes = 480,
            dueTimeEndMinutes = 510
        )
        assertEquals(TaskKind.TASK, result.kind)
        assertEquals(Priority.HIGH, result.priority)
        assertEquals(5L, result.dueDateEpochDay)
        assertEquals(RecurrenceType.WEEKLY, result.recurrenceType)
        assertEquals(480, result.dueTimeMinutes)
    }
}
