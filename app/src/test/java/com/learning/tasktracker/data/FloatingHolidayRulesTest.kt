package com.learning.tasktracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class FloatingHolidayRulesTest {

    @Test
    fun lastSundayOfNovember_2026() {
        // 29 Nov 2026 is Sunday
        val date = FloatingHolidayRules.resolveInYear(
            year = 2026,
            month = 11,
            weekday = DayOfWeek.SUNDAY,
            occurrence = FloatingOccurrence.LAST
        )
        assertEquals(LocalDate.of(2026, 11, 29), date)
    }

    @Test
    fun firstMondayOfMarch_2026() {
        val date = FloatingHolidayRules.resolveInYear(
            year = 2026,
            month = 3,
            weekday = DayOfWeek.MONDAY,
            occurrence = FloatingOccurrence.FIRST
        )
        assertEquals(LocalDate.of(2026, 3, 2), date)
    }

    @Test
    fun nextOrSame_rollsToNextYearWhenPassed() {
        val today = LocalDate.of(2026, 12, 1).toEpochDay()
        val next = FloatingHolidayRules.nextOrSame(
            today = today,
            month = 11,
            weekday = DayOfWeek.SUNDAY.value,
            occurrence = FloatingOccurrence.LAST
        )
        assertEquals(LocalDate.of(2027, 11, 28).toEpochDay(), next)
    }

    @Test
    fun holidayNormalize_usesFloatingRule() {
        val today = LocalDate.of(2026, 10, 20).toEpochDay()
        val result = TaskKindRules.normalize(
            kind = TaskKind.HOLIDAY,
            priority = Priority.HIGH,
            dueDateEpochDay = null,
            recurrenceType = RecurrenceType.NONE,
            recurrenceWeekdayMask = 0,
            recurrenceEndEpochDay = null,
            dueTimeMinutes = 100,
            dueTimeEndMinutes = null,
            floatingMonth = 11,
            floatingWeekday = DayOfWeek.SUNDAY.value,
            floatingOccurrence = FloatingOccurrence.LAST,
            fallbackDateEpochDay = today
        )
        assertEquals(TaskKind.HOLIDAY, result.kind)
        assertEquals(RecurrenceType.YEARLY, result.recurrenceType)
        assertEquals(LocalDate.of(2026, 11, 29).toEpochDay(), result.dueDateEpochDay)
        assertEquals(11, result.floatingMonth)
        assertNull(result.dueTimeMinutes)
    }
}
