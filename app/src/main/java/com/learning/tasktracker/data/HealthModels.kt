package com.learning.tasktracker.data

enum class HealthAnalysisStatus(val label: String) {
    PLANNED("Запланирован"),
    DONE("Сдан")
}

enum class HealthPrescriptionType(val label: String) {
    MEDICATION("Лекарство"),
    VISIT("Визит")
}
