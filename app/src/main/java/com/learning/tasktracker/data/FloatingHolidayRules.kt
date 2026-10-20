package com.learning.tasktracker.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * Resolves floating holiday rules like "last Sunday of November".
 */
object FloatingHolidayRules {
    private val monthLabels = listOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря"
    )

    private val weekdayLabels = mapOf(
        DayOfWeek.MONDAY to "понедельник",
        DayOfWeek.TUESDAY to "вторник",
        DayOfWeek.WEDNESDAY to "среда",
        DayOfWeek.THURSDAY to "четверг",
        DayOfWeek.FRIDAY to "пятница",
        DayOfWeek.SATURDAY to "суббота",
        DayOfWeek.SUNDAY to "воскресенье"
    )

    fun isValid(month: Int, weekday: Int, occurrence: FloatingOccurrence?): Boolean {
        if (occurrence == null) return false
        if (month !in 1..12) return false
        return runCatching { DayOfWeek.of(weekday) }.isSuccess
    }

    fun resolveInYear(
        year: Int,
        month: Int,
        weekday: DayOfWeek,
        occurrence: FloatingOccurrence
    ): LocalDate {
        val ym = YearMonth.of(year, month)
        return when (occurrence) {
            FloatingOccurrence.LAST -> {
                var day = ym.atEndOfMonth()
                while (day.dayOfWeek != weekday) {
                    day = day.minusDays(1)
                }
                day
            }
            FloatingOccurrence.FIRST,
            FloatingOccurrence.SECOND,
            FloatingOccurrence.THIRD,
            FloatingOccurrence.FOURTH -> {
                var day = ym.atDay(1)
                while (day.dayOfWeek != weekday) {
                    day = day.plusDays(1)
                }
                val offset = when (occurrence) {
                    FloatingOccurrence.FIRST -> 0
                    FloatingOccurrence.SECOND -> 1
                    FloatingOccurrence.THIRD -> 2
                    FloatingOccurrence.FOURTH -> 3
                    FloatingOccurrence.LAST -> 0
                }
                val candidate = day.plusWeeks(offset.toLong())
                if (candidate.monthValue == month) candidate else day
            }
        }
    }

    /** Next occurrence on or after [today] (epoch day). */
    fun nextOrSame(
        today: Long,
        month: Int,
        weekday: Int,
        occurrence: FloatingOccurrence
    ): Long {
        val dow = DayOfWeek.of(weekday)
        val todayDate = DateUtils.fromEpochDay(today)
        val thisYear = resolveInYear(todayDate.year, month, dow, occurrence).toEpochDay()
        if (thisYear >= today) return thisYear
        return resolveInYear(todayDate.year + 1, month, dow, occurrence).toEpochDay()
    }

    /** Occurrence in the year after [dueEpochDay]'s calendar year. */
    fun nextYearAfterDue(
        dueEpochDay: Long,
        month: Int,
        weekday: Int,
        occurrence: FloatingOccurrence
    ): Long {
        val year = DateUtils.fromEpochDay(dueEpochDay).year + 1
        return resolveInYear(year, month, DayOfWeek.of(weekday), occurrence).toEpochDay()
    }

    fun formatLabel(month: Int, weekday: Int, occurrence: FloatingOccurrence): String {
        val monthLabel = monthLabels.getOrElse(month - 1) { "месяца" }
        val weekdayLabel = weekdayLabels[DayOfWeek.of(weekday)] ?: "день"
        val occurrenceLabel = occurrence.label.lowercase(Locale("ru"))
        return "$occurrenceLabel $weekdayLabel $monthLabel"
    }
}
