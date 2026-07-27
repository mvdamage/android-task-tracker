package com.learning.tasktracker.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val dayFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))
    private val shortFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM", Locale("ru"))
    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", Locale("ru"))

    private val weekdayShortLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    /** Drop-target / group key for tasks without a due date. */
    const val UNDATED_GROUP_KEY = Long.MIN_VALUE

    fun groupSortKey(epochDay: Long?): Long = epochDay ?: Long.MAX_VALUE

    fun groupSectionTitle(epochDay: Long?, today: Long = todayEpochDay()): String =
        if (epochDay == null) "Без даты" else sectionTitle(epochDay, today)

    fun toGroupKey(epochDay: Long?): Long = epochDay ?: UNDATED_GROUP_KEY

    fun fromGroupKey(key: Long): Long? = if (key == UNDATED_GROUP_KEY) null else key

    fun todayEpochDay(): Long = LocalDate.now(zone).toEpochDay()

    fun fromEpochDay(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    fun formatFull(epochDay: Long): String = fromEpochDay(epochDay).format(dayFormatter)

    fun formatShort(epochDay: Long): String = fromEpochDay(epochDay).format(shortFormatter)

    fun formatTodayHeader(today: Long = todayEpochDay()): String {
        val date = fromEpochDay(today)
        val dayMonth = date.format(DateTimeFormatter.ofPattern("d MMMM", Locale("ru")))
        return if (today == todayEpochDay()) "Сегодня, $dayMonth" else formatFull(today)
    }

    fun sectionTitle(epochDay: Long, today: Long = todayEpochDay()): String {
        return when (epochDay) {
            today -> "Сегодня"
            today - 1 -> "Вчера"
            today + 1 -> "Завтра"
            else -> formatFull(epochDay)
        }
    }

    fun isOverdue(
        dueDateEpochDay: Long?,
        dueTimeMinutes: Int?,
        dueTimeEndMinutes: Int? = null,
        isDone: Boolean,
        today: Long = todayEpochDay()
    ): Boolean {
        if (isDone || dueDateEpochDay == null) return false
        if (dueDateEpochDay < today) return true
        if (dueDateEpochDay > today) return false
        val start = dueTimeMinutes ?: return false
        val deadline = dueTimeEndMinutes?.takeIf { it > start } ?: start
        return deadline < nowMinutesOfDay()
    }

    fun nowMinutesOfDay(): Int {
        val now = LocalTime.now(zone)
        return now.hour * 60 + now.minute
    }

    fun formatTime(minutes: Int): String =
        LocalTime.of(minutes / 60, minutes % 60).format(timeFormatter)

    fun formatTaskTime(startMinutes: Int?, endMinutes: Int? = null): String? {
        if (startMinutes == null) return null
        val end = endMinutes?.takeIf { it > startMinutes }
        return if (end == null) formatTime(startMinutes) else "${formatTime(startMinutes)}–${formatTime(end)}"
    }

    fun formatDueLabel(epochDay: Long?, timeMinutes: Int?, timeEndMinutes: Int? = null): String {
        if (epochDay == null) return "Без даты"
        val date = formatShort(epochDay)
        val time = formatTaskTime(timeMinutes, timeEndMinutes)
        return if (time != null) "$date, $time" else date
    }

    fun normalizedTimePeriod(startMinutes: Int?, endMinutes: Int?): Pair<Int?, Int?> {
        if (startMinutes == null) return null to null
        val end = endMinutes?.takeIf { it > startMinutes }
        return startMinutes to end
    }

    fun minutesToHourMinute(minutes: Int): Pair<Int, Int> = minutes / 60 to minutes % 60

    fun hourMinuteToMinutes(hour: Int, minute: Int): Int = hour * 60 + minute

    fun millisToEpochDay(millis: Long): Long =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()

    fun weekdayBit(dayOfWeek: DayOfWeek): Int = 1 shl (dayOfWeek.value - 1)

    fun weekdayBit(epochDay: Long): Int = weekdayBit(fromEpochDay(epochDay).dayOfWeek)

    fun toggleWeekdayInMask(mask: Int, dayOfWeek: DayOfWeek): Int {
        val bit = weekdayBit(dayOfWeek)
        return if (mask and bit != 0) mask and bit.inv() else mask or bit
    }

    fun formatWeekdayMask(mask: Int): String {
        if (mask == 0) return ""
        return DayOfWeek.entries
            .filter { weekdayBit(it) and mask != 0 }
            .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale("ru")) }
    }

    fun recurrenceLabel(type: RecurrenceType, weekdayMask: Int): String {
        return when (type) {
            RecurrenceType.NONE -> ""
            RecurrenceType.DAILY -> type.label
            RecurrenceType.WEEKLY -> type.label
            RecurrenceType.MONTHLY -> type.label
            RecurrenceType.YEARLY -> type.label
            RecurrenceType.CUSTOM_DAYS -> formatWeekdayMask(weekdayMask).ifBlank { type.label }
        }
    }

    /** Next occurrence strictly after [currentDue], or null if none within [endEpochDay]. */
    fun nextDueDate(
        currentDue: Long,
        type: RecurrenceType,
        interval: Int = 1,
        weekdayMask: Int = 0,
        endEpochDay: Long? = null
    ): Long? {
        if (type == RecurrenceType.NONE) return null
        val step = interval.coerceAtLeast(1)
        val current = fromEpochDay(currentDue)
        val rawNext = when (type) {
            RecurrenceType.NONE -> null
            RecurrenceType.DAILY -> current.plusDays(step.toLong()).toEpochDay()
            RecurrenceType.WEEKLY -> current.plusWeeks(step.toLong()).toEpochDay()
            RecurrenceType.MONTHLY -> current.plusMonths(step.toLong()).toEpochDay()
            RecurrenceType.YEARLY -> current.plusYears(step.toLong()).toEpochDay()
            RecurrenceType.CUSTOM_DAYS -> {
                if (weekdayMask == 0) null
                else {
                    var date = current.plusDays(1)
                    var found: Long? = null
                    repeat(370) {
                        if (found != null) return@repeat
                        if (weekdayBit(date.dayOfWeek) and weekdayMask != 0) {
                            found = date.toEpochDay()
                        } else {
                            date = date.plusDays(1)
                        }
                    }
                    found
                }
            }
        } ?: return null
        return if (endEpochDay != null && rawNext > endEpochDay) null else rawNext
    }

    fun alignDueDate(
        dueDate: Long?,
        type: RecurrenceType,
        weekdayMask: Int
    ): Long? {
        if (dueDate == null) return null
        return alignToRecurrence(dueDate, type, weekdayMask)
    }

    /** Align due date to the nearest matching weekday on or after [dueDate]. */
    fun alignToRecurrence(
        dueDate: Long,
        type: RecurrenceType,
        weekdayMask: Int
    ): Long {
        if (type != RecurrenceType.CUSTOM_DAYS || weekdayMask == 0) return dueDate
        var date = fromEpochDay(dueDate)
        repeat(7) {
            if (weekdayBit(date.dayOfWeek) and weekdayMask != 0) {
                return date.toEpochDay()
            }
            date = date.plusDays(1)
        }
        return dueDate
    }

    /** Advance overdue recurring tasks to the next occurrence on or after [today]. */
    fun catchUpDueDate(
        dueDate: Long,
        today: Long,
        type: RecurrenceType,
        interval: Int,
        weekdayMask: Int,
        endEpochDay: Long? = null
    ): Long {
        if (type == RecurrenceType.NONE || dueDate >= today) return dueDate
        var due = dueDate
        repeat(400) {
            if (due >= today) return due
            val next = nextDueDate(due, type, interval, weekdayMask, endEpochDay) ?: return due
            due = next
        }
        return due
    }

    fun formatRecurrenceEnd(endEpochDay: Long): String = "до ${formatShort(endEpochDay)}"

    fun weekdayChipLabel(dayOfWeek: DayOfWeek): String = weekdayShortLabels[dayOfWeek.value - 1]
}
