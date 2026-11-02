package com.learning.tasktracker.data

enum class HealthAnalysisKind(val label: String) {
    LAB("Анализ"),
    DOCTOR("Врач")
}

enum class HealthAnalysisStatus(val label: String) {
    PLANNED("Запланирован"),
    DONE("Сдан")
}

/** Specialty / category for doctor visits (history is grouped by this). */
enum class DoctorVisitCategory(val label: String) {
    DENTISTRY("Стоматология"),
    OPHTHALMOLOGY("Офтальмология"),
    COSMETOLOGY("Косметология"),
    THERAPY("Терапия"),
    ENT("ЛОР"),
    DERMATOLOGY("Дерматология"),
    GYNECOLOGY("Гинекология"),
    CARDIOLOGY("Кардиология"),
    NEUROLOGY("Неврология"),
    ENDOCRINOLOGY("Эндокринология"),
    ORTHOPEDICS("Ортопедия"),
    SURGERY("Хирургия"),
    UROLOGY("Урология"),
    PEDIATRICS("Педиатрия"),
    PSYCHOLOGY("Психология"),
    OTHER("Другое")
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
