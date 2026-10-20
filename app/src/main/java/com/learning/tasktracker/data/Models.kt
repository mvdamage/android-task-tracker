package com.learning.tasktracker.data

enum class Priority(val label: String) {
    LOW("Низкий"),
    MEDIUM("Средний"),
    HIGH("Высокий")
}

enum class TaskKind(val label: String) {
    TASK("Задача"),
    EVENT("Событие"),
    HOLIDAY("Праздник"),
    BIRTHDAY("День рождения")
}

/** N-th weekday in a month for floating holidays (e.g. last Sunday of November). */
enum class FloatingOccurrence(val label: String) {
    FIRST("Первое"),
    SECOND("Второе"),
    THIRD("Третье"),
    FOURTH("Четвёртое"),
    LAST("Последнее")
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

enum class NoteFormat(val label: String) {
    TEXT("Текст"),
    LIST("Список")
}

enum class NoteTheme(val label: String) {
    GENERAL("Общее"),
    WORK("Работа"),
    PERSONAL("Личное"),
    IDEAS("Идеи"),
    HOME("Дом"),
    OTHER("Другое")
}
