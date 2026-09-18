package com.learning.tasktracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.learning.tasktracker.data.DateUtils
import com.learning.tasktracker.data.Priority
import com.learning.tasktracker.data.RecurrenceType
import com.learning.tasktracker.data.TaskEntity
import com.learning.tasktracker.data.TaskFilter
import com.learning.tasktracker.data.TaskViewMode
import com.learning.tasktracker.data.SettingsStore
import com.learning.tasktracker.data.SubtaskEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import com.learning.tasktracker.data.TaskKind
import com.learning.tasktracker.data.TaskRepository
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class TaskGroup(
    val dueDateEpochDay: Long?,
    val title: String,
    val tasks: List<TaskEntity>
) {
    val groupKey: Long get() = DateUtils.toGroupKey(dueDateEpochDay)
}

data class TaskUiState(
    val groups: List<TaskGroup> = emptyList(),
    val filter: TaskFilter = TaskFilter.ALL,
    val viewMode: TaskViewMode = TaskViewMode.LIST,
    val calendarMonthStartEpochDay: Long = DateUtils.firstDayOfMonth(),
    val selectedCalendarDayKey: Long = DateUtils.todayEpochDay(),
    val calendarDayTasks: List<TaskEntity> = emptyList(),
    val calendarSelectedTitle: String = "",
    val taskCountByDay: Map<Long, Int> = emptyMap(),
    val undatedCount: Int = 0,
    val activeCount: Int = 0,
    val doneCount: Int = 0,
    val overdueCount: Int = 0,
    val todayEpochDay: Long = DateUtils.todayEpochDay(),
    val subtasksEnabled: Boolean = false,
    val subtasksByParentId: Map<Long, List<SubtaskEntity>> = emptyMap()
)

class TaskViewModel(
    private val repository: TaskRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {
    private val filter = MutableStateFlow(TaskFilter.ALL)
    private val viewMode = MutableStateFlow(TaskViewMode.LIST)
    private val calendarMonthStart = MutableStateFlow(DateUtils.firstDayOfMonth())
    private val selectedCalendarDayKey = MutableStateFlow(DateUtils.todayEpochDay())
    private val todayTick = MutableStateFlow(DateUtils.todayEpochDay())
    private val toggleDoneMutex = Mutex()

    val titleHistory: StateFlow<List<String>> = repository.observeTasks()
        .map { tasks ->
            tasks.groupBy { it.title.trim() }
                .filterKeys { it.isNotEmpty() }
                .entries
                .sortedByDescending { (_, group) -> group.maxOf { it.updatedAt } }
                .map { it.key }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val uiState: StateFlow<TaskUiState> = combine(
        combine(
            repository.observeTasks(),
            repository.observeSubtasks(),
            filter,
            viewMode,
            calendarMonthStart
        ) { tasks, subtasks, selectedFilter, mode, monthStart ->
            listOf(tasks, subtasks, selectedFilter, mode, monthStart)
        },
        combine(
            selectedCalendarDayKey,
            todayTick,
            settingsStore.subtasksEnabled
        ) { selectedDayKey, today, subtasksEnabled ->
            Triple(selectedDayKey, today, subtasksEnabled)
        }
    ) { primary, calendarContext ->
        @Suppress("UNCHECKED_CAST")
        val tasks = primary[0] as List<TaskEntity>
        @Suppress("UNCHECKED_CAST")
        val subtasks = primary[1] as List<SubtaskEntity>
        val selectedFilter = primary[2] as TaskFilter
        val mode = primary[3] as TaskViewMode
        val monthStart = primary[4] as Long
        val (selectedDayKey, today, subtasksEnabled) = calendarContext
        val subtasksByParentId = subtasks.groupBy { it.parentTaskId }
        val filtered = when (selectedFilter) {
            TaskFilter.ALL -> tasks
            TaskFilter.ACTIVE -> tasks.filter { !it.isDone }
            TaskFilter.DONE -> tasks.filter { it.isDone }
        }
        val groups = filtered
            .groupBy { it.dueDateEpochDay }
            .entries
            .sortedBy { (day, _) -> DateUtils.groupSortKey(day) }
            .map { (day, dayTasks) ->
                TaskGroup(
                    dueDateEpochDay = day,
                    title = DateUtils.groupSectionTitle(day, today),
                    tasks = dayTasks
                )
            }
        val taskCountByDay = filtered
            .mapNotNull { task -> task.dueDateEpochDay?.let { it to task } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, dayTasks) -> dayTasks.size }
        val undatedCount = filtered.count { it.dueDateEpochDay == null }
        val calendarDayTasks = if (selectedDayKey == DateUtils.UNDATED_GROUP_KEY) {
            filtered.filter { it.dueDateEpochDay == null }
        } else {
            filtered.filter { it.dueDateEpochDay == selectedDayKey }
        }
        val calendarSelectedTitle = if (selectedDayKey == DateUtils.UNDATED_GROUP_KEY) {
            "Без даты"
        } else {
            DateUtils.groupSectionTitle(selectedDayKey, today)
        }
        TaskUiState(
            groups = groups,
            filter = selectedFilter,
            viewMode = mode,
            calendarMonthStartEpochDay = monthStart,
            selectedCalendarDayKey = selectedDayKey,
            calendarDayTasks = calendarDayTasks,
            calendarSelectedTitle = calendarSelectedTitle,
            taskCountByDay = taskCountByDay,
            undatedCount = undatedCount,
            activeCount = tasks.count { !it.isDone },
            doneCount = tasks.count { it.isDone },
            overdueCount = tasks.count { it.isOverdue(today) },
            todayEpochDay = today,
            subtasksEnabled = subtasksEnabled,
            subtasksByParentId = subtasksByParentId
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TaskUiState()
    )

    init {
        onAppVisible()
    }

    fun onAppVisible() {
        viewModelScope.launch {
            repository.processDayRolloverIfNeeded()
            todayTick.value = DateUtils.todayEpochDay()
        }
    }

    fun setFilter(value: TaskFilter) {
        filter.value = value
    }

    fun setViewMode(value: TaskViewMode) {
        viewMode.value = value
        if (value == TaskViewMode.CALENDAR) {
            val today = DateUtils.todayEpochDay()
            calendarMonthStart.value = DateUtils.firstDayOfMonth(today)
            selectedCalendarDayKey.value = today
        }
    }

    fun shiftCalendarMonth(deltaMonths: Int) {
        calendarMonthStart.value = DateUtils.shiftMonth(calendarMonthStart.value, deltaMonths)
        val selected = selectedCalendarDayKey.value
        if (selected != DateUtils.UNDATED_GROUP_KEY &&
            !DateUtils.isSameMonth(selected, calendarMonthStart.value)
        ) {
            selectedCalendarDayKey.value = calendarMonthStart.value
        }
    }

    fun selectCalendarDay(epochDay: Long) {
        selectedCalendarDayKey.value = epochDay
        calendarMonthStart.value = DateUtils.firstDayOfMonth(epochDay)
    }

    fun selectUndatedCalendarBucket() {
        selectedCalendarDayKey.value = DateUtils.UNDATED_GROUP_KEY
    }

    fun addTask(
        title: String,
        notes: String,
        priority: Priority,
        dueDateEpochDay: Long?,
        recurrenceType: RecurrenceType,
        recurrenceWeekdayMask: Int,
        recurrenceEndEpochDay: Long?,
        dueTimeMinutes: Int?,
        dueTimeEndMinutes: Int?,
        kind: TaskKind = TaskKind.TASK,
        location: String = ""
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.add(
                title = title,
                notes = notes,
                location = location,
                priority = priority,
                kind = kind,
                dueDateEpochDay = dueDateEpochDay,
                recurrenceType = recurrenceType,
                recurrenceWeekdayMask = recurrenceWeekdayMask,
                recurrenceEndEpochDay = recurrenceEndEpochDay,
                dueTimeMinutes = dueTimeMinutes,
                dueTimeEndMinutes = dueTimeEndMinutes
            )
        }
    }

    fun updateTask(
        task: TaskEntity,
        title: String,
        notes: String,
        priority: Priority,
        dueDateEpochDay: Long?,
        recurrenceType: RecurrenceType,
        recurrenceWeekdayMask: Int,
        recurrenceEndEpochDay: Long?,
        dueTimeMinutes: Int?,
        dueTimeEndMinutes: Int?,
        kind: TaskKind = task.kind,
        location: String = ""
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.update(
                task.copy(
                    title = title.trim(),
                    notes = notes.trim(),
                    location = location,
                    priority = priority,
                    kind = kind,
                    dueDateEpochDay = dueDateEpochDay,
                    recurrenceType = recurrenceType,
                    recurrenceWeekdayMask = recurrenceWeekdayMask,
                    recurrenceEndEpochDay = recurrenceEndEpochDay,
                    dueTimeMinutes = dueTimeMinutes,
                    dueTimeEndMinutes = dueTimeEndMinutes
                )
            )
        }
    }

    fun toggleDone(task: TaskEntity) {
        viewModelScope.launch {
            toggleDoneMutex.withLock {
                repository.toggleDone(task)
            }
        }
    }

    fun delete(task: TaskEntity) {
        viewModelScope.launch { repository.delete(task) }
    }

    fun clearCompleted() {
        viewModelScope.launch { repository.clearCompleted() }
    }

    fun setSubtasksEnabled(enabled: Boolean) {
        settingsStore.setSubtasksEnabled(enabled)
    }

    fun addSubtask(parentTaskId: Long, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.addSubtask(parentTaskId, title) }
    }

    fun toggleSubtask(subtask: SubtaskEntity) {
        viewModelScope.launch { repository.toggleSubtask(subtask) }
    }

    fun deleteSubtask(subtask: SubtaskEntity) {
        viewModelScope.launch { repository.deleteSubtask(subtask) }
    }

    fun moveTaskToDay(task: TaskEntity, targetGroupKey: Long) {
        viewModelScope.launch {
            repository.moveToDay(task, DateUtils.fromGroupKey(targetGroupKey))
        }
    }

    companion object {
        fun filterTitleSuggestions(history: List<String>, query: String, limit: Int = 5): List<String> {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return emptyList()
            return history
                .asSequence()
                .filter { it.contains(trimmed, ignoreCase = true) }
                .filter { !it.equals(trimmed, ignoreCase = true) }
                .sortedWith(
                    compareBy(
                        { !it.startsWith(trimmed, ignoreCase = true) },
                        { it.lowercase() }
                    )
                )
                .take(limit)
                .toList()
        }
    }

    class Factory(
        private val repository: TaskRepository,
        private val settingsStore: SettingsStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TaskViewModel(repository, settingsStore) as T
        }
    }
}
