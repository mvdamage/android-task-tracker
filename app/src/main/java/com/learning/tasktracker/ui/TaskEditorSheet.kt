package com.learning.tasktracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.learning.tasktracker.data.DateUtils
import com.learning.tasktracker.data.Priority
import com.learning.tasktracker.data.RecurrenceType
import com.learning.tasktracker.data.SubtaskEntity
import com.learning.tasktracker.data.TaskEntity
import com.learning.tasktracker.ui.components.CircularTaskCheckbox
import com.learning.tasktracker.ui.components.EditorOptionRow
import com.learning.tasktracker.ui.components.EditorSectionTitle
import com.learning.tasktracker.ui.components.HorizontalSuggestionPills
import com.learning.tasktracker.ui.components.PriorityChip
import com.learning.tasktracker.ui.theme.extendedColors
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset

internal sealed interface EditorState {
    data object Create : EditorState
    data class Edit(val task: TaskEntity) : EditorState
}

internal data class TaskEditorResult(
    val title: String,
    val notes: String,
    val priority: Priority,
    val dueDateEpochDay: Long?,
    val recurrenceType: RecurrenceType,
    val recurrenceWeekdayMask: Int,
    val recurrenceEndEpochDay: Long?,
    val dueTimeMinutes: Int?
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun TaskEditorSheet(
    state: EditorState,
    titleHistory: List<String>,
    subtasksEnabled: Boolean,
    subtasks: List<SubtaskEntity>,
    onAddSubtask: (String) -> Unit,
    onToggleSubtask: (SubtaskEntity) -> Unit,
    onDeleteSubtask: (SubtaskEntity) -> Unit,
    onDeleteTask: (() -> Unit)?,
    onDismiss: () -> Unit,
    onSave: (TaskEditorResult) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val existing = (state as? EditorState.Edit)?.task
    var title by remember(state) { mutableStateOf(existing?.title.orEmpty()) }
    var notes by remember(state) { mutableStateOf(existing?.notes.orEmpty()) }
    var priority by remember(state) { mutableStateOf(existing?.priority ?: Priority.MEDIUM) }
    var dueDateEpochDay by remember(state) {
        mutableStateOf(
            when (state) {
                is EditorState.Edit -> state.task.dueDateEpochDay
                EditorState.Create -> DateUtils.todayEpochDay()
            }
        )
    }
    var recurrenceType by remember(state) {
        mutableStateOf(existing?.recurrenceType ?: RecurrenceType.NONE)
    }
    var recurrenceWeekdayMask by remember(state) {
        mutableIntStateOf(existing?.recurrenceWeekdayMask ?: 0)
    }
    var dueTimeMinutes by remember(state) {
        mutableStateOf(existing?.dueTimeMinutes)
    }
    var recurrenceEndEpochDay by remember(state) {
        mutableStateOf(existing?.recurrenceEndEpochDay)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    var newSubtaskTitle by remember(state) { mutableStateOf("") }

    val titleSuggestions = remember(title, titleHistory) {
        TaskViewModel.filterTitleSuggestions(titleHistory, title)
    }
    val customDaysValid = recurrenceType != RecurrenceType.CUSTOM_DAYS || recurrenceWeekdayMask != 0
    val recurrenceValid = recurrenceType == RecurrenceType.NONE || dueDateEpochDay != null
    val endDateValid = dueDateEpochDay == null ||
        recurrenceEndEpochDay == null ||
        recurrenceEndEpochDay!! >= dueDateEpochDay!!

    val dateLabel = dueDateEpochDay?.let { day ->
        val today = DateUtils.todayEpochDay()
        when (day) {
            today -> "Сегодня"
            today + 1 -> "Завтра"
            else -> DateUtils.formatShort(day)
        }
    } ?: "Без даты"
    val timeLabel = dueTimeMinutes?.let { DateUtils.formatTime(it) } ?: "Без времени"
    val recurrenceLabel = if (recurrenceType == RecurrenceType.NONE) {
        "Не повторяется"
    } else {
        DateUtils.recurrenceLabel(recurrenceType, recurrenceWeekdayMask)
    }
    val endDateLabel = recurrenceEndEpochDay?.let { DateUtils.formatShort(it) } ?: "Без ограничения"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("Отмена") }
                TextButton(
                    onClick = {
                        if (title.isNotBlank() && customDaysValid && endDateValid && recurrenceValid) {
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
                                    recurrenceEndEpochDay = if (recurrenceType == RecurrenceType.NONE) {
                                        null
                                    } else {
                                        recurrenceEndEpochDay
                                    },
                                    dueTimeMinutes = if (dueDateEpochDay == null) null else dueTimeMinutes
                                )
                            )
                        }
                    },
                    enabled = title.isNotBlank() && customDaysValid && endDateValid && recurrenceValid
                ) {
                    Text(
                        "Готово",
                        color = if (title.isNotBlank() && customDaysValid && endDateValid && recurrenceValid) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Название задачи") },
                singleLine = false,
                minLines = 1,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.extendedColors.divider
                )
            )
            if (titleSuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalSuggestionPills(
                    suggestions = titleSuggestions,
                    onSelect = { title = it }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.extendedColors.divider, thickness = 0.5.dp)

            EditorOptionRow(
                label = "Дата",
                value = dateLabel,
                onClick = { showDatePicker = true }
            )
            HorizontalDivider(color = MaterialTheme.extendedColors.divider, thickness = 0.5.dp)

            EditorOptionRow(
                label = "Время",
                value = timeLabel,
                onClick = {
                    if (dueDateEpochDay == null) {
                        dueDateEpochDay = DateUtils.todayEpochDay()
                    }
                    showTimePicker = true
                }
            )
            HorizontalDivider(color = MaterialTheme.extendedColors.divider, thickness = 0.5.dp)

            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = "Приоритет",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.entries.forEach { value ->
                        PriorityChip(
                            priority = value,
                            selected = priority == value,
                            onClick = { priority = value }
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.extendedColors.divider, thickness = 0.5.dp)

            EditorOptionRow(
                label = "Повторение",
                value = recurrenceLabel,
                onClick = { showAdvanced = !showAdvanced }
            )

            if (showAdvanced) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecurrenceType.entries.forEach { value ->
                        FilterChip(
                            selected = recurrenceType == value,
                            onClick = {
                                recurrenceType = value
                                if (value == RecurrenceType.NONE) {
                                    recurrenceEndEpochDay = null
                                } else if (dueDateEpochDay == null) {
                                    dueDateEpochDay = DateUtils.todayEpochDay()
                                }
                                if (value == RecurrenceType.CUSTOM_DAYS && recurrenceWeekdayMask == 0) {
                                    recurrenceWeekdayMask = DateUtils.weekdayBit(
                                        dueDateEpochDay ?: DateUtils.todayEpochDay()
                                    )
                                }
                            },
                            label = { Text(value.label) }
                        )
                    }
                }
                if (recurrenceType == RecurrenceType.CUSTOM_DAYS) {
                    Spacer(modifier = Modifier.height(8.dp))
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
            }

            if (recurrenceType != RecurrenceType.NONE) {
                HorizontalDivider(color = MaterialTheme.extendedColors.divider, thickness = 0.5.dp)
                EditorOptionRow(
                    label = "Повторять до",
                    value = endDateLabel,
                    onClick = { showEndDatePicker = true }
                )
                if (!endDateValid) {
                    Text(
                        text = "Дата окончания не может быть раньше даты задачи",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.extendedColors.overdue,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text("Заметки") },
                minLines = 2,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.extendedColors.divider
                )
            )

            if (subtasksEnabled && existing != null) {
                Spacer(modifier = Modifier.height(16.dp))
                EditorSectionTitle("Подзадачи")
                subtasks.forEach { subtask ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularTaskCheckbox(
                            checked = subtask.isDone,
                            onCheckedChange = { onToggleSubtask(subtask) },
                            size = 20.dp
                        )
                        Text(
                            text = subtask.title,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (subtask.isDone) TextDecoration.LineThrough else null
                        )
                        IconButton(onClick = { onDeleteSubtask(subtask) }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Удалить подзадачу",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newSubtaskTitle,
                        onValueChange = { newSubtaskTitle = it },
                        placeholder = { Text("Новая подзадача") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (newSubtaskTitle.isNotBlank()) {
                                onAddSubtask(newSubtaskTitle)
                                newSubtaskTitle = ""
                            }
                        },
                        enabled = newSubtaskTitle.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Добавить подзадачу")
                    }
                }
            }

            if (onDeleteTask != null) {
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(
                    onClick = onDeleteTask,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Удалить задачу",
                        color = MaterialTheme.extendedColors.overdue
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val initialDay = dueDateEpochDay ?: DateUtils.todayEpochDay()
        val initialMillis = DateUtils.fromEpochDay(initialDay)
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
                Row {
                    TextButton(
                        onClick = {
                            dueDateEpochDay = null
                            dueTimeMinutes = null
                            recurrenceType = RecurrenceType.NONE
                            recurrenceWeekdayMask = 0
                            recurrenceEndEpochDay = null
                            showDatePicker = false
                        }
                    ) { Text("Без даты") }
                    TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
                }
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
        DatePickerDialog(
            onDismissRequest = { showTimePicker = false },
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
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    if (showEndDatePicker) {
        val initialEnd = recurrenceEndEpochDay ?: ((dueDateEpochDay ?: DateUtils.todayEpochDay()) + 30)
        val initialMillis = DateUtils.fromEpochDay(initialEnd)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val endPickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        endPickerState.selectedDateMillis?.let { millis ->
                            recurrenceEndEpochDay = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .toEpochDay()
                        }
                        showEndDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        recurrenceEndEpochDay = null
                        showEndDatePicker = false
                    }
                ) { Text("Без ограничения") }
            }
        ) {
            DatePicker(state = endPickerState)
        }
    }
}
