package com.learning.tasktracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.learning.tasktracker.data.DateUtils
import com.learning.tasktracker.data.Priority
import com.learning.tasktracker.data.RecurrenceType
import com.learning.tasktracker.data.TaskEntity
import com.learning.tasktracker.data.TaskFilter
import com.learning.tasktracker.data.SettingsStore
import com.learning.tasktracker.data.SubtaskEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import com.learning.tasktracker.data.TaskRepository
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskGroup(
    val dueDateEpochDay: Long,
    val title: String,
    val tasks: List<TaskEntity>
)

data class TaskUiState(
    val groups: List<TaskGroup> = emptyList(),
    val filter: TaskFilter = TaskFilter.ALL,
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
    private val todayTick = MutableStateFlow(DateUtils.todayEpochDay())

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
        repository.observeTasks(),
        repository.observeSubtasks(),
        filter,
        todayTick,
        settingsStore.subtasksEnabled
    ) { tasks, subtasks, selectedFilter, today, subtasksEnabled ->
        val subtasksByParentId = subtasks.groupBy { it.parentTaskId }
        val filtered = when (selectedFilter) {
            TaskFilter.ALL -> tasks
            TaskFilter.ACTIVE -> tasks.filter { !it.isDone }
            TaskFilter.DONE -> tasks.filter { it.isDone }
        }
        val groups = filtered
            .groupBy { it.dueDateEpochDay }
            .toSortedMap()
            .map { (day, dayTasks) ->
                TaskGroup(
                    dueDateEpochDay = day,
                    title = DateUtils.sectionTitle(day, today),
                    tasks = dayTasks
                )
            }
        TaskUiState(
            groups = groups,
            filter = selectedFilter,
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

    fun addTask(
        title: String,
        notes: String,
        priority: Priority,
        dueDateEpochDay: Long,
        recurrenceType: RecurrenceType,
        recurrenceWeekdayMask: Int,
        recurrenceEndEpochDay: Long?,
        dueTimeMinutes: Int?
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.add(
                title = title,
                notes = notes,
                priority = priority,
                dueDateEpochDay = dueDateEpochDay,
                recurrenceType = recurrenceType,
                recurrenceWeekdayMask = recurrenceWeekdayMask,
                recurrenceEndEpochDay = recurrenceEndEpochDay,
                dueTimeMinutes = dueTimeMinutes
            )
        }
    }

    fun updateTask(
        task: TaskEntity,
        title: String,
        notes: String,
        priority: Priority,
        dueDateEpochDay: Long,
        recurrenceType: RecurrenceType,
        recurrenceWeekdayMask: Int,
        recurrenceEndEpochDay: Long?,
        dueTimeMinutes: Int?
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.update(
                task.copy(
                    title = title.trim(),
                    notes = notes.trim(),
                    priority = priority,
                    dueDateEpochDay = dueDateEpochDay,
                    recurrenceType = recurrenceType,
                    recurrenceWeekdayMask = recurrenceWeekdayMask,
                    recurrenceEndEpochDay = recurrenceEndEpochDay,
                    dueTimeMinutes = dueTimeMinutes
                )
            )
        }
    }

    fun toggleDone(task: TaskEntity) {
        viewModelScope.launch { repository.toggleDone(task) }
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

    fun moveTaskToDay(task: TaskEntity, targetEpochDay: Long) {
        viewModelScope.launch { repository.moveToDay(task, targetEpochDay) }
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
