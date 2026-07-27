package com.learning.tasktracker.voice

import com.learning.tasktracker.data.ShoppingCategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TaskVoiceParserTest {
    private val zone = ZoneId.of("Asia/Omsk")
    private val now = LocalDateTime.of(2026, 7, 27, 10, 0) // Monday

    @Test
    fun plainTitle_noDue() {
        val result = TaskVoiceParser.parseTaskVoice("Позвонить маме", now, zone)
        assertEquals("Позвонить маме", result.title)
        assertNull(result.dueDateEpochDay)
        assertNull(result.dueTimeMinutes)
        assertNull(result.dueTimeEndMinutes)
    }

    @Test
    fun tomorrowAt1800() {
        val result = TaskVoiceParser.parseTaskVoice(
            "Позвонить маме завтра в 18:00",
            now,
            zone
        )
        assertEquals("Позвонить маме", result.title)
        assertEquals(now.toLocalDate().plusDays(1).toEpochDay(), result.dueDateEpochDay)
        assertEquals(18 * 60, result.dueTimeMinutes)
        assertNull(result.dueTimeEndMinutes)
    }

    @Test
    fun fridayInterval() {
        // now = Monday 2026-07-27 → next Friday = 2026-07-31
        val result = TaskVoiceParser.parseTaskVoice(
            "Встреча в пятницу с 14 до 15",
            now,
            zone
        )
        assertEquals("Встреча", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 31, 0, 0).toLocalDate().toEpochDay(), result.dueDateEpochDay)
        assertEquals(14 * 60, result.dueTimeMinutes)
        assertEquals(15 * 60, result.dueTimeEndMinutes)
    }

    @Test
    fun todayMorning() {
        val result = TaskVoiceParser.parseTaskVoice(
            "Созвон сегодня в 9 утра",
            now,
            zone
        )
        assertEquals("Созвон", result.title)
        assertEquals(now.toLocalDate().toEpochDay(), result.dueDateEpochDay)
        assertEquals(9 * 60, result.dueTimeMinutes)
    }

    @Test
    fun throughThreeDays() {
        val result = TaskVoiceParser.parseTaskVoice("Отчёт через 3 дня", now, zone)
        assertEquals("Отчёт", result.title)
        assertEquals(now.toLocalDate().plusDays(3).toEpochDay(), result.dueDateEpochDay)
        assertNull(result.dueTimeMinutes)
    }

    @Test
    fun failSoftWhenTitleEmptyAfterCleanup() {
        val result = TaskVoiceParser.parseTaskVoice("завтра в 18:00", now, zone)
        assertEquals("завтра в 18:00", result.title)
        assertNull(result.dueDateEpochDay)
        assertNull(result.dueTimeMinutes)
    }

    @Test
    fun truncateLongTitle() {
        val long = "а".repeat(250)
        val result = TaskVoiceParser.parseTaskVoice(long, now, zone)
        assertEquals(200, result.title.length)
        assertTrue(result.truncated)
    }
}

class ShoppingVoiceParserTest {
    private val products = ShoppingCategoryEntity(id = 1, name = "Продукты", colorArgb = 0, iconKey = "food")
    private val dairy = ShoppingCategoryEntity(id = 2, name = "Молочные", colorArgb = 0, iconKey = "milk")

    @Test
    fun singleItem_noCategory() {
        val lines = ShoppingVoiceParser.parseShoppingVoice(
            "зубная паста",
            categories = emptyList(),
            categoryIdByTitleLower = emptyMap()
        )
        assertEquals(1, lines.size)
        assertEquals("зубная паста", lines[0].title)
        assertNull(lines[0].categoryId)
    }

    @Test
    fun enumerationSplit() {
        val lines = ShoppingVoiceParser.parseShoppingVoice(
            "хлеб, молоко и яйца",
            categories = emptyList(),
            categoryIdByTitleLower = emptyMap()
        )
        assertEquals(listOf("хлеб", "молоко", "яйца"), lines.map { it.title })
    }

    @Test
    fun explicitCategorySuffix() {
        val lines = ShoppingVoiceParser.parseShoppingVoice(
            "молоко в Молочные",
            categories = listOf(dairy),
            categoryIdByTitleLower = emptyMap()
        )
        assertEquals(1, lines.size)
        assertEquals("молоко", lines[0].title)
        assertEquals(2L, lines[0].categoryId)
    }

    @Test
    fun sharedCategoryPrefix() {
        val lines = ShoppingVoiceParser.parseShoppingVoice(
            "в Продукты: хлеб, молоко и яйца",
            categories = listOf(products),
            categoryIdByTitleLower = emptyMap()
        )
        assertEquals(3, lines.size)
        assertTrue(lines.all { it.categoryId == 1L })
        assertEquals(listOf("хлеб", "молоко", "яйца"), lines.map { it.title })
    }

    @Test
    fun emptyFragmentsDropped() {
        val lines = ShoppingVoiceParser.parseShoppingVoice(
            "хлеб,, молоко",
            categories = emptyList(),
            categoryIdByTitleLower = emptyMap()
        )
        assertEquals(listOf("хлеб", "молоко"), lines.map { it.title })
    }

    @Test
    fun historyFallback() {
        val lines = ShoppingVoiceParser.parseShoppingVoice(
            "молоко",
            categories = listOf(dairy),
            categoryIdByTitleLower = mapOf("молоко" to 2L)
        )
        assertEquals(2L, lines.single().categoryId)
    }

    @Test
    fun unknownCategoryIgnored() {
        val lines = ShoppingVoiceParser.parseShoppingVoice(
            "в Несуществующая: хлеб",
            categories = listOf(products),
            categoryIdByTitleLower = emptyMap()
        )
        // shared prefix not matched → whole string treated as one title? 
        // Actually regex matches but category not found → residual stays raw, sharedCategoryId null
        // Then "в Несуществующая: хлеб" is one fragment - local category also won't match
        assertEquals(1, lines.size)
        assertNull(lines[0].categoryId)
    }
}
