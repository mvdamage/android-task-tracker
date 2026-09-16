package com.learning.tasktracker.data

enum class Priority(val label: String) {
    LOW("Низкий"),
    MEDIUM("Средний"),
    HIGH("Высокий")
}

enum class TaskFilter(val label: String) {
    ALL("Все"),
    ACTIVE("Активные"),
    DONE("Готово")
}

enum class TaskViewMode(val label: String) {
    LIST("Список"),
    CALENDAR("Календарь")
}

enum class RecurrenceType(val label: String) {
    NONE("Не повторять"),
    DAILY("Ежедневно"),
    WEEKLY("Раз в неделю"),
    MONTHLY("Раз в месяц"),
    YEARLY("Раз в год"),
    CUSTOM_DAYS("Свои дни")
}
