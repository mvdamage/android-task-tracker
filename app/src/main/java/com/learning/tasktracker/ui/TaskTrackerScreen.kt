package com.learning.tasktracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learning.tasktracker.data.DateUtils
import com.learning.tasktracker.data.TaskEntity
import com.learning.tasktracker.data.TaskFilter
import com.learning.tasktracker.ui.components.AnimatedCheckboxScale
import com.learning.tasktracker.ui.components.PriorityDot
import com.learning.tasktracker.ui.components.StatBadge
import com.learning.tasktracker.ui.theme.extendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTrackerScreen(viewModel: TaskViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val titleHistory by viewModel.titleHistory.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<EditorState?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val extended = MaterialTheme.extendedColors

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            DateUtils.formatTodayHeader(state.todayEpochDay),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatBadge("${state.activeCount} активных")
                            if (state.doneCount > 0) {
                                StatBadge(
                                    text = "${state.doneCount} готово",
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (state.overdueCount > 0) {
                                StatBadge(
                                    text = "${state.overdueCount} просрочено",
                                    containerColor = extended.overdueContainer,
                                    contentColor = extended.overdue
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (state.doneCount > 0) {
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(
                                Icons.Outlined.DeleteSweep,
                                contentDescription = "Очистить выполненные"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editor = EditorState.Create },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Новая задача")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FilterRow(
                selected = state.filter,
                onSelect = viewModel::setFilter,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (state.groups.isEmpty()) {
                EmptyState(
                    filter = state.filter,
                    onAddClick = { editor = EditorState.Create },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 4.dp,
                        bottom = 88.dp
                    )
                ) {
                    state.groups.forEach { group ->
                        val groupOverdue = group.dueDateEpochDay < state.todayEpochDay
                        item(key = "header_${group.dueDateEpochDay}") {
                            Text(
                                text = group.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (groupOverdue) extended.overdue else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(
                                    start = 8.dp,
                                    end = 8.dp,
                                    top = 14.dp,
                                    bottom = 6.dp
                                )
                            )
                        }
                        items(group.tasks, key = { it.id }) { task ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically { it / 3 },
                                exit = fadeOut() + slideOutVertically { -it / 3 }
                            ) {
                                ChecklistItemRow(
                                    task = task,
                                    today = state.todayEpochDay,
                                    onToggle = { viewModel.toggleDone(task) },
                                    onEdit = { editor = EditorState.Edit(task) },
                                    onDelete = { viewModel.delete(task) }
                                )
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }

    editor?.let { current ->
        TaskEditorSheet(
            state = current,
            titleHistory = titleHistory,
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
                        result.dueTimeMinutes
                    )
                    is EditorState.Edit -> viewModel.updateTask(
                        current.task,
                        result.title,
                        result.notes,
                        result.priority,
                        result.dueDateEpochDay,
                        result.recurrenceType,
                        result.recurrenceWeekdayMask,
                        result.dueTimeMinutes
                    )
                }
                editor = null
            }
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Очистить выполненные?") },
            text = {
                Text(
                    "Будут удалены выполненные задачи без повторения. " +
                        "Повторяющиеся задачи при отметке сразу переносятся на следующую дату."
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
private fun FilterRow(
    selected: TaskFilter,
    onSelect: (TaskFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TaskFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(filter.label) }
            )
        }
    }
}

@Composable
private fun EmptyState(
    filter: TaskFilter,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.TaskAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (filter) {
                TaskFilter.ALL -> "Пока нет задач"
                TaskFilter.ACTIVE -> "Нет активных задач"
                TaskFilter.DONE -> "Нет выполненных задач"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Создайте первую задачу — планировать день станет проще",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onAddClick) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Добавить задачу")
        }
    }
}

@Composable
private fun ChecklistItemRow(
    task: TaskEntity,
    today: Long,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val extended = MaterialTheme.extendedColors
    val overdue = task.isOverdue(today)
    val rowBg = when {
        overdue -> extended.overdueContainer
        task.isDone -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        else -> Color.Transparent
    }
    val titleColor = when {
        overdue -> extended.overdue
        task.isDone -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (overdue) {
                    drawRect(
                        color = extended.overdue,
                        size = Size(4.dp.toPx(), size.height)
                    )
                }
            }
            .background(rowBg)
            .clickable(onClick = onEdit)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedCheckboxScale(checked = task.isDone) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = if (overdue) extended.overdue else MaterialTheme.colorScheme.primary,
                    uncheckedColor = if (overdue) extended.overdue else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!task.isDone) {
                    PriorityDot(priority = task.priority)
                }
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (overdue) FontWeight.SemiBold else FontWeight.Normal,
                    color = titleColor,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = DateUtils.formatDueLabel(task.dueDateEpochDay, task.dueTimeMinutes),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (overdue) extended.overdue else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (task.isRecurring) {
                    Text(
                        text = "↻ ${DateUtils.recurrenceLabel(task.recurrenceType, task.recurrenceWeekdayMask)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (overdue) {
                    Text(
                        text = "просрочено",
                        style = MaterialTheme.typography.labelMedium,
                        color = extended.overdue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Удалить",
                tint = if (overdue) extended.overdue else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
