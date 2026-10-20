package com.learning.tasktracker.voice

import com.learning.tasktracker.data.NoteFormat
import com.learning.tasktracker.data.NoteTheme
import java.util.Locale

data class NoteVoiceDraft(
    val title: String,
    val body: String,
    val format: NoteFormat,
    val theme: NoteTheme,
    val listItems: List<String>,
    val truncated: Boolean
)

object NoteVoiceParser {
    private val localeRu = Locale("ru")

    private val listPrefixRegex = Regex(
        """^(?:список|пункты|чеклист)\s*[:\-]?\s*(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val themePrefixRegex = Regex(
        """^(?:тема\s+)?(работа|личное|идеи|дом|другое|общее)\s*[:\-]\s*(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val splitListRegex = Regex("""[,;]|\s+и\s+|\r?\n""", RegexOption.IGNORE_CASE)

    /**
     * Parses spoken text into list item titles (for appending to an existing note).
     * Supports comma/`и`/newline separators and an optional «список:» prefix.
     */
    fun parseListItems(raw: String): List<String> {
        val source = raw.trim()
        if (source.isEmpty()) return emptyList()
        val withoutTheme = themePrefixRegex.find(source)?.groupValues?.get(2)?.trim() ?: source
        val payload = listPrefixRegex.find(withoutTheme)?.groupValues?.get(1)?.trim()
            ?: withoutTheme
        return splitListRegex.split(payload)
            .map { VoiceTextNormalizer.normalize(it).title }
            .filter { it.isNotEmpty() }
    }

    fun parseNoteVoice(raw: String): NoteVoiceDraft? {
        val source = raw.trim()
        if (source.isEmpty()) return null

        var residual = source
        var theme = NoteTheme.GENERAL
        val themeMatch = themePrefixRegex.find(source)
        if (themeMatch != null) {
            theme = themeFromLabel(themeMatch.groupValues[1]) ?: NoteTheme.GENERAL
            residual = themeMatch.groupValues[2].trim()
        }
        if (residual.isEmpty()) return null

        val listMatch = listPrefixRegex.find(residual)
        if (listMatch != null) {
            val payload = listMatch.groupValues[1].trim()
            val items = splitListRegex.split(payload)
                .map { VoiceTextNormalizer.normalize(it).title }
                .filter { it.isNotEmpty() }
            if (items.isEmpty()) return null
            val titleNorm = VoiceTextNormalizer.normalize(
                if (items.size == 1) items.first() else "Список"
            )
            return NoteVoiceDraft(
                title = titleNorm.title,
                body = "",
                format = NoteFormat.LIST,
                theme = theme,
                listItems = items,
                truncated = titleNorm.truncated || items.any {
                    it.length >= VoiceTextNormalizer.MAX_TITLE_LENGTH
                }
            )
        }

        // Multi-line without "список" prefix but looks like a checklist (≥2 short lines)
        val lines = residual.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size >= 2 && lines.all { it.length <= 60 } && !residual.contains('.')) {
            val items = lines.map { VoiceTextNormalizer.normalize(it).title }.filter { it.isNotEmpty() }
            if (items.size >= 2) {
                val titleNorm = VoiceTextNormalizer.normalize("Список")
                return NoteVoiceDraft(
                    title = titleNorm.title,
                    body = "",
                    format = NoteFormat.LIST,
                    theme = theme,
                    listItems = items,
                    truncated = false
                )
            }
        }

        val sentenceSplit = residual.split(Regex("""(?<=[.!?])\s+"""), limit = 2)
        val titleRaw: String
        val bodyRaw: String
        if (sentenceSplit.size == 2 && sentenceSplit[0].length in 1..80) {
            titleRaw = sentenceSplit[0].trim().trimEnd('.', '!', '?')
            bodyRaw = sentenceSplit[1].trim()
        } else if (residual.length > 80) {
            val cut = residual.take(80)
            val space = cut.lastIndexOf(' ')
            val end = if (space >= 40) space else 80
            titleRaw = residual.take(end).trim()
            bodyRaw = residual.drop(end).trim()
        } else {
            titleRaw = residual
            bodyRaw = ""
        }

        val titleNorm = VoiceTextNormalizer.normalize(titleRaw)
        if (titleNorm.title.isEmpty()) return null
        return NoteVoiceDraft(
            title = titleNorm.title,
            body = bodyRaw,
            format = NoteFormat.TEXT,
            theme = theme,
            listItems = emptyList(),
            truncated = titleNorm.truncated
        )
    }

    private fun themeFromLabel(raw: String): NoteTheme? {
        return when (raw.trim().lowercase(localeRu)) {
            "работа" -> NoteTheme.WORK
            "личное" -> NoteTheme.PERSONAL
            "идеи" -> NoteTheme.IDEAS
            "дом" -> NoteTheme.HOME
            "другое" -> NoteTheme.OTHER
            "общее" -> NoteTheme.GENERAL
            else -> null
        }
    }
}
