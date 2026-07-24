package com.learning.tasktracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.learning.tasktracker.data.DateUtils
import com.learning.tasktracker.data.Priority
import com.learning.tasktracker.data.RecurrenceType
import com.learning.tasktracker.data.TaskEntity
import com.learning.tasktracker.data.TaskFilter
import com.learning.tasktracker.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    val todayEpochDay: Long = DateUtils.todayEpochDay()
)

class TaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {
    private val filter = MutableStateFlow(TaskFilter.ALL)
    private val todayTick = MutableStateFlow(DateUtils.todayEpochDay())

    val uiState: StateFlow<TaskUiState> = combine(
        repository.observeTasks(),
        filter,
        todayTick
    ) { tasks, selectedFilter, today ->
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
            todayEpochDay = today
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

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TaskViewModel(repository) as T
        }
    }
}
