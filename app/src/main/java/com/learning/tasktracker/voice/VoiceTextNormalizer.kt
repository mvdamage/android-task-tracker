package com.learning.tasktracker.voice

object VoiceTextNormalizer {
    const val MAX_TITLE_LENGTH = 200

    data class NormalizedTitle(
        val title: String,
        val truncated: Boolean
    )

    fun normalize(raw: String): NormalizedTitle {
        val collapsed = raw.trim().replace(Regex("\\s+"), " ")
        val truncated = collapsed.length > MAX_TITLE_LENGTH
        return NormalizedTitle(
            title = collapsed.take(MAX_TITLE_LENGTH),
            truncated = truncated
        )
    }
}
