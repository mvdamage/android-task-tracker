package com.learning.tasktracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DateUtilsCalendarTest {

    @Test
    fun calendarMonthGrid_startsOnMonday() {
        // 1 September 2025 is Monday — no leading padding
        val monthStart = LocalDate.of(2025, 9, 1).toEpochDay()
        val grid = DateUtils.calendarMonthGrid(monthStart)
        assertEquals(30, grid.count { it != null })
        assertEquals(monthStart, grid.first())

        // 1 November 2025 is Saturday — five leading cells
        val novemberStart = LocalDate.of(2025, 11, 1).toEpochDay()
        val novemberGrid = DateUtils.calendarMonthGrid(novemberStart)
        assertNull(novemberGrid[0])
        assertEquals(novemberStart, novemberGrid[5])
    }

    @Test
    fun shiftMonth_movesToNextMonth() {
        val september = LocalDate.of(2025, 9, 1).toEpochDay()
        val october = DateUtils.shiftMonth(september, 1)
        assertEquals(LocalDate.of(2025, 10, 1).toEpochDay(), october)
    }
}
