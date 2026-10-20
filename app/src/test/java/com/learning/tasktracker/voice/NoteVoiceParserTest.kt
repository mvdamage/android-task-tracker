package com.learning.tasktracker.voice

import com.learning.tasktracker.data.NoteFormat
import com.learning.tasktracker.data.NoteTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteVoiceParserTest {

    @Test
    fun plainText_becomesTitleOnly() {
        val draft = NoteVoiceParser.parseNoteVoice("Купить билеты")!!
        assertEquals("Купить билеты", draft.title)
        assertEquals("", draft.body)
        assertEquals(NoteFormat.TEXT, draft.format)
        assertEquals(NoteTheme.GENERAL, draft.theme)
    }

    @Test
    fun listPrefix_splitsItems() {
        val draft = NoteVoiceParser.parseNoteVoice("список: молоко, хлеб и яйца")!!
        assertEquals(NoteFormat.LIST, draft.format)
        assertEquals(listOf("молоко", "хлеб", "яйца"), draft.listItems)
    }

    @Test
    fun themePrefix_setsWork() {
        val draft = NoteVoiceParser.parseNoteVoice("работа: подготовить отчёт к пятнице")!!
        assertEquals(NoteTheme.WORK, draft.theme)
        assertEquals("подготовить отчёт к пятнице", draft.title)
        assertEquals(NoteFormat.TEXT, draft.format)
    }

    @Test
    fun emptyRaw_returnsNull() {
        assertNull(NoteVoiceParser.parseNoteVoice("   "))
    }

    @Test
    fun longText_splitsTitleAndBody() {
        val long = "Первое предложение заметки. " + "дальше текст ".repeat(20)
        val draft = NoteVoiceParser.parseNoteVoice(long)!!
        assertEquals("Первое предложение заметки", draft.title)
        assertTrue(draft.body.isNotBlank())
        assertEquals(NoteFormat.TEXT, draft.format)
    }
}
