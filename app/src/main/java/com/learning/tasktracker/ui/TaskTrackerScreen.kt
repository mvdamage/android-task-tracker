package com.learning.tasktracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learning.tasktracker.data.DateUtils
import com.learning.tasktracker.data.Priority
import com.learning.tasktracker.data.SubtaskEntity
import com.learning.tasktracker.data.TaskEntity
import com.learning.tasktracker.data.TaskFilter
import com.learning.tasktracker.ui.components.AnyDoDivider
import com.learning.tasktracker.ui.components.CircularTaskCheckbox
import com.learning.tasktracker.ui.components.DaySectionHeader
import com.learning.tasktracker.ui.components.FilterSegmentRow
import com.learning.tasktracker.ui.components.PriorityDot
import com.learning.tasktracker.ui.components.QuickAddBar
import com.learning.tasktracker.ui.components.QuickDropDayRow
import com.learning.tasktracker.ui.components.TaskDayDragState
import com.learning.tasktracker.ui.components.TaskDragGhost
import com.learning.tasktracker.ui.components.TaskListTitle
import com.learning.tasktracker.ui.components.draggingRowAlpha
import com.learning.tasktracker.ui.components.rememberTaskDayDragState
import com.learning.tasktracker.ui.components.taskDragSource
import com.learning.tasktracker.ui.theme.extendedColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTrackerScreen(viewModel: TaskViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val titleHistory by viewModel.titleHistory.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<EditorState?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val dragState = rememberTaskDayDragState()
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val extended = MaterialTheme.extendedColors
    var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentRootCoordinates by rememberUpdatedState(rootCoordinates)

    LaunchedEffect(dragState.isDragging) {
        if (!dragState.isDragging) return@LaunchedEffect
        val edgeSizePx = with(density) { 72.dp.toPx() }
        val maxSpeedPx = with(density) { 28.dp.toPx() }
        while (dragState.isDragging) {
            val delta = dragState.scrollDeltaForPosition(
                dragState.dragPosition,
                edgeSizePx,
                maxSpeedPx
            )
            if (delta != 0f) {
                listState.scrollBy(delta)
                dragState.refreshHoveredDay()
            }
            delay(16)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Column {
                        Text(
                            text = DateUtils.formatTodayHeader(state.todayEpochDay),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.overdueCount > 0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${state.overdueCount} просрочено",
                                style = MaterialTheme.typography.labelMedium,
                                color = extended.overdue
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "Настройки",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (state.doneCount > 0) {
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(
                                Icons.Outlined.DeleteSweep,
                                contentDescription = "Очистить выполненные",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editor = EditorState.Create },
                modifier = Modifier.padding(bottom = 8.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Новая задача")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .onGloballyPositioned { coordinates ->
                    rootCoordinates = coordinates
                    val bounds = coordinates.boundsInRoot()
                    dragState.updateOverlayOrigin(Offset(bounds.left, bounds.top))
                    dragState.updateViewport(bounds)
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (!dragState.isDragging) continue
                            val coordinates = currentRootCoordinates ?: continue
                            event.changes.forEach { change ->
                                if (change.pressed) {
                                    dragState.updateDragPosition(
                                        coordinates.localToRoot(change.position)
                                    )
                                }
                            }
                            if (event.changes.all { !it.pressed }) {
                                dragState.finishDrag { task, day ->
                                    viewModel.moveTaskToDay(task, day)
                                }
                            }
                        }
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                QuickAddBar(
                    placeholder = "Добавить задачу…",
                    onClick = { editor = EditorState.Create },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                if (dragState.isDragging) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Перетащите на день",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        QuickDropDayRow(
                            today = state.todayEpochDay,
                            dragState = dragState
                        )
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .onGloballyPositioned { coordinates ->
                            dragState.updateScrollArea(coordinates.boundsInRoot())
                        },
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 8.dp
                    )
                ) {
                    item(key = "filter_row") {
                        FilterSegmentRow(
                            items = TaskFilter.entries.map { filter ->
                                filter.label to { viewModel.setFilter(filter) }
                            },
                            selectedIndex = TaskFilter.entries.indexOf(state.filter),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        )
                    }

                    if (state.groups.isEmpty()) {
                        item(key = "empty_state") {
                            EmptyState(
                                filter = state.filter,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 32.dp)
                            )
                        }
                    } else {
                        state.groups.forEachIndexed { groupIndex, group ->
                            item(key = "header_${group.groupKey}") {
                                DaySectionHeader(
                                    title = group.title,
                                    day = group.groupKey,
                                    dragState = dragState,
                                    topPadding = if (groupIndex == 0) 0.dp else 12.dp
                                )
                            }
                            items(group.tasks, key = { it.id }) { task ->
                                val subtasks = state.subtasksByParentId[task.id].orEmpty()
                                ChecklistItemRow(
                                    task = task,
                                    today = state.todayEpochDay,
                                    subtasksEnabled = state.subtasksEnabled,
                                    subtasks = subtasks,
                                    dragState = dragState,
                                    onToggle = { viewModel.toggleDone(task) },
                                    onToggleSubtask = viewModel::toggleSubtask,
                                    onEdit = { editor = EditorState.Edit(task) }
                                )
                                AnyDoDivider()
                            }
                        }
                    }
                }
            }
            dragState.draggedTask?.let { dragged ->
                TaskDragGhost(task = dragged, dragState = dragState)
            }
        }
    }

    editor?.let { current ->
        val editTaskId = (current as? EditorState.Edit)?.task?.id
        TaskEditorSheet(
            state = current,
            titleHistory = titleHistory,
            subtasksEnabled = state.subtasksEnabled,
            subtasks = editTaskId?.let { state.subtasksByParentId[it] }.orEmpty(),
            onAddSubtask = { title -> editTaskId?.let { viewModel.addSubtask(it, title) } },
            onToggleSubtask = viewModel::toggleSubtask,
            onDeleteSubtask = viewModel::deleteSubtask,
            onDeleteTask = if (current is EditorState.Edit) {
                { viewModel.delete(current.task); editor = null }
            } else {
                null
            },
            onDismiss = { editor = null },
            onSave = { result ->
                when (current) {
                    is EditorState.Create -> viewModel.addTask(
                        result.title,
                        result.notes,
                        result.priority,
                        result.dueDateEpochDay,
                        result.recurrenceType,
                        result.recurrenceWeekdayMask,
                        result.recurrenceEndEpochDay,
                        result.dueTimeMinutes,
                        result.dueTimeEndMinutes
                    )
                    is EditorState.Edit -> viewModel.updateTask(
                        current.task,
                        result.title,
                        result.notes,
                        result.priority,
                        result.dueDateEpochDay,
                        result.recurrenceType,
                        result.recurrenceWeekdayMask,
                        result.recurrenceEndEpochDay,
                        result.dueTimeMinutes,
                        result.dueTimeEndMinutes
                    )
                }
                editor = null
            }
        )
    }

    if (showSettings) {
        SettingsSheet(
            subtasksEnabled = state.subtasksEnabled,
            onSubtasksEnabledChange = viewModel::setSubtasksEnabled,
            onDismiss = { showSettings = false }
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Очистить выполненные?") },
            text = {
                Text(
                    "Будут удалены выполненные задачи без повторения. " +
                        "Повторяющиеся задачи при отметке остаются выполненными на текущий день, " +
                            "а следующее повторение создаётся на новую дату."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCompleted()
                        confirmClear = false
                    }
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun EmptyState(
    filter: TaskFilter,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when (filter) {
                TaskFilter.ALL -> "Нет задач"
                TaskFilter.ACTIVE -> "Всё выполнено!"
                TaskFilter.DONE -> "Нет выполненных"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when (filter) {
                TaskFilter.ALL -> "Нажмите + или строку выше, чтобы добавить"
                TaskFilter.ACTIVE -> "Отличная работа — можно отдохнуть"
                TaskFilter.DONE -> "Выполненные задачи появятся здесь"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChecklistItemRow(
    task: TaskEntity,
    today: Long,
    subtasksEnabled: Boolean,
    subtasks: List<SubtaskEntity>,
    dragState: TaskDayDragState,
    onToggle: () -> Unit,
    onToggleSubtask: (SubtaskEntity) -> Unit,
    onEdit: () -> Unit
) {
    val extended = MaterialTheme.extendedColors
    val overdue = task.isOverdue(today)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .draggingRowAlpha(task, dragState)
            .taskDragSource(task, dragState, onTap = onEdit)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        CircularTaskCheckbox(
            checked = task.isDone,
            onCheckedChange = onToggle,
            modifier = Modifier.padding(top = 2.dp),
            checkedColor = if (overdue && !task.isDone) extended.overdue else extended.checkboxChecked
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PriorityDot(priority = task.priority)
                TaskListTitle(
                    text = task.title,
                    done = task.isDone,
                    overdue = overdue && !task.isDone
                )
            }
            val subtitle = buildTaskSubtitle(task, today, subtasksEnabled, subtasks)
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (overdue && !task.isDone) {
                        extended.overdue
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (subtasksEnabled && subtasks.isNotEmpty()) {
                subtasks.forEach { subtask ->
                    SubtaskRow(
                        subtask = subtask,
                        onToggle = { onToggleSubtask(subtask) }
                    )
                }
            }
        }
    }
}

private fun buildTaskSubtitle(
    task: TaskEntity,
    today: Long,
    subtasksEnabled: Boolean,
    subtasks: List<SubtaskEntity>
): String {
    val parts = mutableListOf<String>()
    if (task.dueTimeMinutes != null) {
        parts += DateUtils.formatTaskTime(task.dueTimeMinutes, task.dueTimeEndMinutes).orEmpty()
    }
    if (task.priority != Priority.MEDIUM) {
        parts += task.priority.label
    }
    if (task.isRecurring) {
        parts += DateUtils.recurrenceLabel(task.recurrenceType, task.recurrenceWeekdayMask)
        task.recurrenceEndEpochDay?.let { parts += DateUtils.formatRecurrenceEnd(it) }
    }
    if (subtasksEnabled && subtasks.isNotEmpty()) {
        val doneCount = subtasks.count { it.isDone }
        parts += "$doneCount/${subtasks.size}"
    }
    if (task.isOverdue(today) && !task.isDone) {
        parts += "просрочено"
    }
    return parts.joinToString(" · ")
}

@Composable
private fun SubtaskRow(
    subtask: SubtaskEntity,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularTaskCheckbox(
            checked = subtask.isDone,
            onCheckedChange = onToggle,
            size = 20.dp
        )
        Text(
            text = subtask.title,
            modifier = Modifier.padding(start = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (subtask.isDone) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (subtask.isDone) TextDecoration.LineThrough else null,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
