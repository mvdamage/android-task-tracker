package com.learning.tasktracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learning.tasktracker.data.DateUtils
import com.learning.tasktracker.data.Priority
import com.learning.tasktracker.data.RecurrenceType
import com.learning.tasktracker.data.TaskEntity
import com.learning.tasktracker.data.TaskFilter
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset

private val OverdueRed = Color(0xFFB71C1C)
private val OverdueBg = Color(0xFFFFEBEE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTrackerScreen(viewModel: TaskViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<EditorState?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Трекер задач", fontWeight = FontWeight.Bold)
                        val subtitle = buildString {
                            append("${state.activeCount} активных · ${state.doneCount} готово")
                            if (state.overdueCount > 0) {
                                append(" · ${state.overdueCount} просрочено")
                            }
                        }
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.overdueCount > 0) {
                                OverdueRed
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
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
                                color = if (groupOverdue) OverdueRed else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(
                                    start = 8.dp,
                                    end = 8.dp,
                                    top = 14.dp,
                                    bottom = 6.dp
                                )
                            )
                        }
                        items(group.tasks, key = { it.id }) { task ->
                            ChecklistItemRow(
                                task = task,
                                today = state.todayEpochDay,
                                onToggle = { viewModel.toggleDone(task) },
                                onEdit = { editor = EditorState.Edit(task) },
                                onDelete = { viewModel.delete(task) }
                            )
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
        TaskEditorDialog(
            state = current,
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
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when (filter) {
                TaskFilter.ALL -> "Пока нет задач"
                TaskFilter.ACTIVE -> "Нет активных задач"
                TaskFilter.DONE -> "Нет выполненных задач"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Нажмите +, чтобы добавить первую",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
    val overdue = task.isOverdue(today)
    val rowBg = when {
        overdue -> OverdueBg
        task.isDone -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        else -> Color.Transparent
    }
    val titleColor = when {
        overdue -> OverdueRed
        task.isDone -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable(onClick = onEdit)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isDone,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = if (overdue) OverdueRed else MaterialTheme.colorScheme.primary,
                uncheckedColor = if (overdue) OverdueRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (overdue) FontWeight.SemiBold else FontWeight.Normal,
                color = titleColor,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = DateUtils.formatDueLabel(task.dueDateEpochDay, task.dueTimeMinutes),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (overdue) OverdueRed else MaterialTheme.colorScheme.onSurfaceVariant
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
                        color = OverdueRed,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (task.notes.isNotBlank()) {
                    Text(
                        text = task.notes,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Удалить",
                tint = if (overdue) OverdueRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private sealed interface EditorState {
    data object Create : EditorState
    data class Edit(val task: TaskEntity) : EditorState
}

private data class TaskEditorResult(
    val title: String,
    val notes: String,
    val priority: Priority,
    val dueDateEpochDay: Long,
    val recurrenceType: RecurrenceType,
    val recurrenceWeekdayMask: Int,
    val dueTimeMinutes: Int?
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TaskEditorDialog(
    state: EditorState,
    onDismiss: () -> Unit,
    onSave: (TaskEditorResult) -> Unit
) {
    val existing = (state as? EditorState.Edit)?.task
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var priority by remember { mutableStateOf(existing?.priority ?: Priority.MEDIUM) }
    var dueDateEpochDay by remember {
        mutableLongStateOf(existing?.dueDateEpochDay ?: DateUtils.todayEpochDay())
    }
    var recurrenceType by remember {
        mutableStateOf(existing?.recurrenceType ?: RecurrenceType.NONE)
    }
    var recurrenceWeekdayMask by remember {
        mutableIntStateOf(existing?.recurrenceWeekdayMask ?: 0)
    }
    var dueTimeMinutes by remember {
        mutableStateOf(existing?.dueTimeMinutes)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val customDaysValid = recurrenceType != RecurrenceType.CUSTOM_DAYS || recurrenceWeekdayMask != 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) "Новая задача" else "Редактировать")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Заметки") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Дата",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val today = DateUtils.todayEpochDay()
                    FilterChip(
                        selected = dueDateEpochDay == today,
                        onClick = { dueDateEpochDay = today },
                        label = { Text("Сегодня") }
                    )
                    FilterChip(
                        selected = dueDateEpochDay == today + 1,
                        onClick = { dueDateEpochDay = today + 1 },
                        label = { Text("Завтра") }
                    )
                    FilterChip(
                        selected = dueDateEpochDay != today && dueDateEpochDay != today + 1,
                        onClick = { showDatePicker = true },
                        label = { Text(DateUtils.formatShort(dueDateEpochDay)) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Время",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = dueTimeMinutes == null,
                        onClick = { dueTimeMinutes = null },
                        label = { Text("Без времени") }
                    )
                    FilterChip(
                        selected = dueTimeMinutes != null,
                        onClick = {
                            if (dueTimeMinutes == null) {
                                dueTimeMinutes = DateUtils.hourMinuteToMinutes(9, 0)
                            }
                            showTimePicker = true
                        },
                        label = {
                            Text(
                                dueTimeMinutes?.let { DateUtils.formatTime(it) } ?: "Указать время"
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Повторение",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecurrenceType.entries.forEach { value ->
                        FilterChip(
                            selected = recurrenceType == value,
                            onClick = {
                                recurrenceType = value
                                if (value == RecurrenceType.CUSTOM_DAYS && recurrenceWeekdayMask == 0) {
                                    recurrenceWeekdayMask = DateUtils.weekdayBit(dueDateEpochDay)
                                }
                            },
                            label = { Text(value.label) }
                        )
                    }
                }
                if (recurrenceType == RecurrenceType.CUSTOM_DAYS) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Дни недели",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DayOfWeek.entries.forEach { day ->
                            val bit = DateUtils.weekdayBit(day)
                            FilterChip(
                                selected = recurrenceWeekdayMask and bit != 0,
                                onClick = {
                                    recurrenceWeekdayMask =
                                        DateUtils.toggleWeekdayInMask(recurrenceWeekdayMask, day)
                                },
                                label = { Text(DateUtils.weekdayChipLabel(day)) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Приоритет",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.entries.forEach { value ->
                        FilterChip(
                            selected = priority == value,
                            onClick = { priority = value },
                            label = { Text(value.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        TaskEditorResult(
                            title = title,
                            notes = notes,
                            priority = priority,
                            dueDateEpochDay = dueDateEpochDay,
                            recurrenceType = recurrenceType,
                            recurrenceWeekdayMask = if (recurrenceType == RecurrenceType.CUSTOM_DAYS) {
                                recurrenceWeekdayMask
                            } else {
                                0
                            },
                            dueTimeMinutes = dueTimeMinutes
                        )
                    )
                },
                enabled = title.isNotBlank() && customDaysValid
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )

    if (showDatePicker) {
        // DatePicker uses UTC millis; convert via epoch day at UTC noon for stability
        val initialMillis = DateUtils.fromEpochDay(dueDateEpochDay)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            dueDateEpochDay = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .toEpochDay()
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val timePickerInitial = dueTimeMinutes ?: DateUtils.hourMinuteToMinutes(9, 0)
        val (initialHour, initialMinute) = DateUtils.minutesToHourMinute(timePickerInitial)
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Время") },
            confirmButton = {
                TextButton(
                    onClick = {
                        dueTimeMinutes = DateUtils.hourMinuteToMinutes(
                            timePickerState.hour,
                            timePickerState.minute
                        )
                        showTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Отмена") }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}
