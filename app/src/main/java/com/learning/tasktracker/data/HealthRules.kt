package com.learning.tasktracker.data

/**
 * Pure rules for health domain (limits, task titles, due dates).
 */
object HealthRules {
    const val MAX_PERSONS = 3
    const val MAX_NAME_LENGTH = 100

    fun canAddPerson(currentCount: Int): Boolean = currentCount < MAX_PERSONS

    fun normalizePersonName(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        return trimmed.take(MAX_NAME_LENGTH)
    }

    fun normalizeTitle(title: String): String? {
        val trimmed = title.trim()
        return trimmed.ifEmpty { null }
    }

    fun taskTitleFor(type: HealthPrescriptionType, prescriptionTitle: String): String {
        val base = prescriptionTitle.trim().ifEmpty { "без названия" }
        return when (type) {
            HealthPrescriptionType.MEDICATION -> "Приём: $base"
            HealthPrescriptionType.VISIT -> "Визит: $base"
        }
    }

    fun taskNotesFor(personName: String): String {
        val name = personName.trim()
        return if (name.isEmpty()) "" else "Здоровье: $name"
    }

    /**
     * @return due epoch day, or null if [daysAhead] is invalid (< 1).
     */
    fun dueDateFromDaysAhead(daysAhead: Int, today: Long = DateUtils.todayEpochDay()): Long? {
        if (daysAhead < 1) return null
        return today + daysAhead
    }
}
