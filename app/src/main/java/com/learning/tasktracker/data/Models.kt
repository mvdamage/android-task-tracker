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
