package com.learning.tasktracker.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val dayFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))
    private val shortFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM", Locale("ru"))

    fun todayEpochDay(): Long = LocalDate.now(zone).toEpochDay()

    fun fromEpochDay(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    fun formatFull(epochDay: Long): String = fromEpochDay(epochDay).format(dayFormatter)

    fun formatShort(epochDay: Long): String = fromEpochDay(epochDay).format(shortFormatter)

    fun sectionTitle(epochDay: Long, today: Long = todayEpochDay()): String {
        return when (epochDay) {
            today -> "Сегодня"
            today - 1 -> "Вчера"
            today + 1 -> "Завтра"
            else -> formatFull(epochDay)
        }
    }

    fun isOverdue(dueDateEpochDay: Long, isDone: Boolean, today: Long = todayEpochDay()): Boolean {
        return !isDone && dueDateEpochDay < today
    }

    fun millisToEpochDay(millis: Long): Long =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()
}
