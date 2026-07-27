package com.learning.tasktracker.voice

import com.learning.tasktracker.data.ShoppingCategoryEntity
import java.util.Locale

data class ShoppingVoiceLine(
    val title: String,
    val categoryId: Long?,
    val truncated: Boolean
)

object ShoppingVoiceParser {
    private val localeRu = Locale("ru")

    private val sharedCategoryRegex =
        Regex(
            """^(?:в\s+категорию|категория|в)\s+(.+?)\s*:\s*(.+)$""",
            RegexOption.IGNORE_CASE
        )

    private val localCategoryRegex =
        Regex(
            """^(.+?)\s+(?:в\s+категорию|в|категория)\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

    private val splitRegex = Regex("""[,;]|\s+и\s+|\r?\n""", RegexOption.IGNORE_CASE)

    fun parseShoppingVoice(
        raw: String,
        categories: List<ShoppingCategoryEntity>,
        categoryIdByTitleLower: Map<String, Long?>
    ): List<ShoppingVoiceLine> {
        val source = raw.trim()
        if (source.isEmpty()) return emptyList()

        val categoriesByName = categories.associateBy { it.name.trim().lowercase(localeRu) }

        var residual = source
        var sharedCategoryId: Long? = null
        val sharedMatch = sharedCategoryRegex.find(source)
        if (sharedMatch != null) {
            val nameKey = sharedMatch.groupValues[1].trim().lowercase(localeRu)
            val matched = categoriesByName[nameKey]
            if (matched != null) {
                sharedCategoryId = matched.id
                residual = sharedMatch.groupValues[2]
            }
        }

        return splitRegex.split(residual)
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { fragment ->
                var titlePart = fragment
                var localCategoryId: Long? = null
                val localMatch = localCategoryRegex.find(fragment)
                if (localMatch != null) {
                    val nameKey = localMatch.groupValues[2].trim().lowercase(localeRu)
                    val matched = categoriesByName[nameKey]
                    if (matched != null) {
                        localCategoryId = matched.id
                        titlePart = localMatch.groupValues[1]
                    }
                }

                val normalized = VoiceTextNormalizer.normalize(titlePart)
                if (normalized.title.isEmpty()) return@mapNotNull null

                val categoryId = localCategoryId
                    ?: sharedCategoryId
                    ?: categoryIdByTitleLower[normalized.title.lowercase(localeRu)]

                ShoppingVoiceLine(
                    title = normalized.title,
                    categoryId = categoryId,
                    truncated = normalized.truncated
                )
            }
            .toList()
    }
}
