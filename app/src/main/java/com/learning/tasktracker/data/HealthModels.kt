package com.learning.tasktracker.data

enum class HealthAnalysisKind(val label: String) {
    LAB("Анализ"),
    DOCTOR("Врач")
}

enum class HealthAnalysisStatus(val label: String) {
    PLANNED("Запланирован"),
    DONE("Сдан")
}

enum class HealthPrescriptionType(val label: String) {
    MEDICATION("Лекарство"),
    VISIT("Визит")
}

fun HealthAnalysisStatus.labelFor(kind: HealthAnalysisKind): String = when (kind) {
    HealthAnalysisKind.LAB -> label
    HealthAnalysisKind.DOCTOR -> when (this) {
        HealthAnalysisStatus.PLANNED -> "Запланирован"
        HealthAnalysisStatus.DONE -> "Состоялся"
    }
}
