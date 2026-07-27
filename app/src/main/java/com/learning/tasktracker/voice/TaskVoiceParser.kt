package com.learning.tasktracker.voice

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

data class TaskVoiceParseResult(
    val title: String,
    val dueDateEpochDay: Long?,
    val dueTimeMinutes: Int?,
    val dueTimeEndMinutes: Int?,
    val truncated: Boolean
)

object TaskVoiceParser {
    private val localeRu = Locale("ru")

    private val monthNames = listOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря"
    )

    private val weekdayPatterns = listOf(
        DayOfWeek.MONDAY to listOf("понедельник", "пн"),
        DayOfWeek.TUESDAY to listOf("вторник", "вт"),
        DayOfWeek.WEDNESDAY to listOf("среду", "среда", "ср"),
        DayOfWeek.THURSDAY to listOf("четверг", "чт"),
        DayOfWeek.FRIDAY to listOf("пятницу", "пятница", "пт"),
        DayOfWeek.SATURDAY to listOf("субботу", "суббота", "сб"),
        DayOfWeek.SUNDAY to listOf("воскресенье", "вс")
    )

    private data class Span(val start: Int, val end: Int)

    private data class DateHit(val span: Span, val epochDay: Long)

    private data class TimeHit(
        val span: Span,
        val startMinutes: Int,
        val endMinutes: Int?
    )

    fun parseTaskVoice(
        raw: String,
        now: LocalDateTime,
        zone: ZoneId = ZoneId.systemDefault()
    ): TaskVoiceParseResult {
        @Suppress("UNUSED_PARAMETER")
        val ignoredZone = zone
        val source = raw.trim()
        if (source.isEmpty()) {
            return TaskVoiceParseResult(
                title = "",
                dueDateEpochDay = null,
                dueTimeMinutes = null,
                dueTimeEndMinutes = null,
                truncated = false
            )
        }

        val lower = source.lowercase(localeRu)
        val timeHit = findTime(lower)
        val dateHit = findDate(lower, now, timeHit)

        val spans = listOfNotNull(dateHit?.span, timeHit?.span)
            .sortedByDescending { it.start }

        var cleaned = source
        for (span in spans) {
            cleaned = cleaned.removeRange(span.start, span.end)
        }
        cleaned = cleaned.replace(Regex("\\s+"), " ").trim()
            .trim(',', '.', ';', ':', '-', '—', '–')
            .replace(Regex("\\s+"), " ")
            .trim()

        val normalizedClean = VoiceTextNormalizer.normalize(cleaned)
        if (normalizedClean.title.isEmpty()) {
            val fallback = VoiceTextNormalizer.normalize(source)
            return TaskVoiceParseResult(
                title = fallback.title,
                dueDateEpochDay = null,
                dueTimeMinutes = null,
                dueTimeEndMinutes = null,
                truncated = fallback.truncated
            )
        }

        var start = timeHit?.startMinutes
        var end = timeHit?.endMinutes
        if (start != null && end != null && end <= start) {
            end = null
        }
        val dueDate = dateHit?.epochDay
        if (dueDate == null) {
            start = null
            end = null
        }

        return TaskVoiceParseResult(
            title = normalizedClean.title,
            dueDateEpochDay = dueDate,
            dueTimeMinutes = start,
            dueTimeEndMinutes = end,
            truncated = normalizedClean.truncated
        )
    }

    private fun findDate(lower: String, now: LocalDateTime, timeHit: TimeHit?): DateHit? {
        val candidates = mutableListOf<DateHit>()

        Regex("""\bпослезавтра\b""").find(lower)?.let {
            candidates += DateHit(Span(it.range.first, it.range.last + 1), now.toLocalDate().plusDays(2).toEpochDay())
        }
        Regex("""\bзавтра\b""").find(lower)?.let {
            candidates += DateHit(Span(it.range.first, it.range.last + 1), now.toLocalDate().plusDays(1).toEpochDay())
        }
        Regex("""\bсегодня\b""").find(lower)?.let {
            candidates += DateHit(Span(it.range.first, it.range.last + 1), now.toLocalDate().toEpochDay())
        }

        Regex("""\bчерез\s+(\d+)\s+(день|дня|дней)\b""").find(lower)?.let { match ->
            val n = match.groupValues[1].toIntOrNull() ?: return@let
            candidates += DateHit(
                Span(match.range.first, match.range.last + 1),
                now.toLocalDate().plusDays(n.toLong()).toEpochDay()
            )
        }

        for ((dayOfWeek, aliases) in weekdayPatterns) {
            val aliasPattern = aliases.joinToString("|") { Regex.escape(it) }
            val regex = Regex("""(?:\bв\s+)?\b($aliasPattern)\b""")
            regex.find(lower)?.let { match ->
                val date = resolveWeekday(
                    target = dayOfWeek,
                    now = now,
                    timeMinutes = timeHit?.startMinutes
                )
                candidates += DateHit(Span(match.range.first, match.range.last + 1), date.toEpochDay())
            }
        }

        Regex("""\b(\d{1,2})\.(\d{1,2})(?:\.(\d{2,4}))?\b""").find(lower)?.let { match ->
            val day = match.groupValues[1].toIntOrNull() ?: return@let
            val month = match.groupValues[2].toIntOrNull() ?: return@let
            val yearRaw = match.groupValues[3]
            val year = when {
                yearRaw.isBlank() -> null
                yearRaw.length == 2 -> 2000 + yearRaw.toInt()
                else -> yearRaw.toIntOrNull()
            }
            resolveAbsoluteDate(day, month, year, now.toLocalDate())?.let { date ->
                candidates += DateHit(Span(match.range.first, match.range.last + 1), date.toEpochDay())
            }
        }

        val monthPattern = monthNames.joinToString("|")
        Regex("""\b(\d{1,2})\s+($monthPattern)(?:\s+(\d{4}))?\b""").find(lower)?.let { match ->
            val day = match.groupValues[1].toIntOrNull() ?: return@let
            val month = monthNames.indexOf(match.groupValues[2]) + 1
            val year = match.groupValues[3].toIntOrNull()
            resolveAbsoluteDate(day, month, year, now.toLocalDate())?.let { date ->
                candidates += DateHit(Span(match.range.first, match.range.last + 1), date.toEpochDay())
            }
        }

        return candidates.minWithOrNull(compareBy<DateHit> { it.span.start }.thenByDescending {
            it.span.end - it.span.start
        })
    }

    private fun findTime(lower: String): TimeHit? {
        val candidates = mutableListOf<TimeHit>()

        Regex("""\bс\s+(\d{1,2})(?::(\d{2}))?\s+до\s+(\d{1,2})(?::(\d{2}))?\b""").find(lower)?.let { match ->
            val startH = match.groupValues[1].toIntOrNull() ?: return@let
            val startM = match.groupValues[2].toIntOrNull() ?: 0
            val endH = match.groupValues[3].toIntOrNull() ?: return@let
            val endM = match.groupValues[4].toIntOrNull() ?: 0
            if (startH !in 0..23 || endH !in 0..23 || startM !in 0..59 || endM !in 0..59) return@let
            candidates += TimeHit(
                Span(match.range.first, match.range.last + 1),
                startH * 60 + startM,
                endH * 60 + endM
            )
        }

        Regex("""\bв\s+(\d{1,2}):(\d{2})\b""").find(lower)?.let { match ->
            val h = match.groupValues[1].toIntOrNull() ?: return@let
            val m = match.groupValues[2].toIntOrNull() ?: return@let
            if (h !in 0..23 || m !in 0..59) return@let
            candidates += TimeHit(Span(match.range.first, match.range.last + 1), h * 60 + m, null)
        }

        Regex("""\bв\s+(\d{1,2})\s+(час|часа|часов)\b""").find(lower)?.let { match ->
            val h = match.groupValues[1].toIntOrNull() ?: return@let
            if (h !in 0..23) return@let
            candidates += TimeHit(Span(match.range.first, match.range.last + 1), h * 60, null)
        }

        Regex("""\bв\s+(\d{1,2})\s+(утра|дня|вечера|ночи)\b""").find(lower)?.let { match ->
            val h = match.groupValues[1].toIntOrNull() ?: return@let
            val part = match.groupValues[2]
            val hour24 = applyDayPart(h, part) ?: return@let
            candidates += TimeHit(Span(match.range.first, match.range.last + 1), hour24 * 60, null)
        }

        Regex("""\bв\s+(\d{1,2})\b(?!\s*(?:января|февраля|марта|апреля|мая|июня|июля|августа|сентября|октября|ноября|декабря|понедельник|вторник|среду|среда|четверг|пятницу|пятница|субботу|суббота|воскресенье|пн|вт|ср|чт|пт|сб|вс)\b)""")
            .find(lower)?.let { match ->
                val h = match.groupValues[1].toIntOrNull() ?: return@let
                if (h !in 0..23) return@let
                candidates += TimeHit(Span(match.range.first, match.range.last + 1), h * 60, null)
            }

        return candidates.minWithOrNull(compareBy<TimeHit> { it.span.start }.thenByDescending {
            it.span.end - it.span.start
        })
    }

    private fun applyDayPart(hour: Int, part: String): Int? {
        if (hour !in 0..12) return null
        return when (part) {
            "утра" -> if (hour == 12) 0 else hour
            "дня" -> if (hour == 12) 12 else hour + 12
            "вечера" -> if (hour == 12) 12 else hour + 12
            "ночи" -> when {
                hour == 12 -> 0
                hour in 1..5 -> hour
                else -> hour + 12
            }
            else -> null
        }
    }

    private fun resolveWeekday(
        target: DayOfWeek,
        now: LocalDateTime,
        timeMinutes: Int?
    ): LocalDate {
        val today = now.toLocalDate()
        var daysToAdd = (target.value - today.dayOfWeek.value + 7) % 7
        if (daysToAdd == 0) {
            if (timeMinutes != null) {
                val nowMinutes = now.toLocalTime().hour * 60 + now.toLocalTime().minute
                if (timeMinutes <= nowMinutes) {
                    daysToAdd = 7
                }
            }
        }
        return today.plusDays(daysToAdd.toLong())
    }

    private fun resolveAbsoluteDate(
        day: Int,
        month: Int,
        year: Int?,
        today: LocalDate
    ): LocalDate? {
        if (month !in 1..12 || day !in 1..31) return null
        return try {
            if (year != null) {
                LocalDate.of(year, month, day)
            } else {
                var candidate = LocalDate.of(today.year, month, day)
                if (candidate.isBefore(today.minusDays(1))) {
                    candidate = candidate.plusYears(1)
                }
                candidate
            }
        } catch (_: Exception) {
            null
        }
    }
}
