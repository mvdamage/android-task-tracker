package com.learning.tasktracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerLayoutType
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
import androidx.compose.ui.graphics.graphicsLayer
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
    val dueTimeMinutes: Int?,
    val dueTimeEndMinutes: Int?
)

private enum class TimePickerTarget { Start, End }

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
    var dueTimeEndMinutes by remember(state) {
        mutableStateOf(existing?.dueTimeEndMinutes)
    }
    var periodMode by remember(state) {
        mutableStateOf(existing?.dueTimeEndMinutes != null)
    }
    var recurrenceEndEpochDay by remember(state) {
        mutableStateOf(existing?.recurrenceEndEpochDay)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var timePickerTarget by remember { mutableStateOf(TimePickerTarget.Start) }
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
    val timeLabel = when {
        dueDateEpochDay == null -> "Нужна дата"
        dueTimeMinutes == null -> "Без времени"
        periodMode && dueTimeEndMinutes != null ->
            DateUtils.formatTaskTime(dueTimeMinutes, dueTimeEndMinutes).orEmpty()
        dueTimeMinutes != null -> DateUtils.formatTime(dueTimeMinutes!!)
        else -> "Без времени"
    }
    val startTimeLabel = dueTimeMinutes?.let { DateUtils.formatTime(it) } ?: "Не задано"
    val endTimeLabel = dueTimeEndMinutes?.let { DateUtils.formatTime(it) } ?: "Не задано"
    val timeEnabled = dueDateEpochDay != null
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
                            val (startTime, endTime) = DateUtils.normalizedTimePeriod(
                                if (dueDateEpochDay == null) null else dueTimeMinutes,
                                if (dueDateEpochDay == null || !periodMode) null else dueTimeEndMinutes
                            )
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
                                    dueTimeMinutes = startTime,
                                    dueTimeEndMinutes = endTime
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

            if (timeEnabled) {
                FlowRow(
                    modifier = Modifier.padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = periodMode,
                        onClick = {
                            periodMode = !periodMode
                            if (periodMode) {
                                if (dueTimeMinutes == null) {
                                    dueTimeMinutes = DateUtils.hourMinuteToMinutes(9, 0)
                                }
                                val start = dueTimeMinutes!!
                                val end = dueTimeEndMinutes
                                if (end == null || end <= start) {
                                    dueTimeEndMinutes = (start + 30).coerceAtMost(1439)
                                }
                            } else {
                                dueTimeEndMinutes = null
                            }
                        },
                        label = { Text("Период") }
                    )
                }
            }

            if (periodMode && timeEnabled) {
                EditorOptionRow(
                    label = "С",
                    value = startTimeLabel,
                    onClick = {
                        timePickerTarget = TimePickerTarget.Start
                        showTimePicker = true
                    }
                )
                HorizontalDivider(color = MaterialTheme.extendedColors.divider, thickness = 0.5.dp)
                EditorOptionRow(
                    label = "До",
                    value = endTimeLabel,
                    onClick = {
                        timePickerTarget = TimePickerTarget.End
                        showTimePicker = true
                    }
                )
            } else {
                EditorOptionRow(
                    label = "Время",
                    value = timeLabel,
                    enabled = timeEnabled,
                    onClick = {
                        timePickerTarget = TimePickerTarget.Start
                        showTimePicker = true
                    }
                )
            }
            if (timeEnabled) {
                FlowRow(
                    modifier = Modifier.padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(9 to 0, 12 to 0, 15 to 0, 18 to 0).forEach { (hour, minute) ->
                        val minutes = DateUtils.hourMinuteToMinutes(hour, minute)
                        FilterChip(
                            selected = dueTimeMinutes == minutes,
                            onClick = {
                                dueTimeMinutes = minutes
                                if (periodMode) {
                                    dueTimeEndMinutes?.let { end ->
                                        if (end <= minutes) {
                                            dueTimeEndMinutes = (minutes + 30).coerceAtMost(1439)
                                        }
                                    } ?: run {
                                        dueTimeEndMinutes = (minutes + 30).coerceAtMost(1439)
                                    }
                                }
                            },
                            label = { Text(DateUtils.formatTime(minutes)) }
                        )
                    }
                    FilterChip(
                        selected = false,
                        onClick = {
                            val minutes = DateUtils.nowMinutesOfDay()
                            dueTimeMinutes = minutes
                            if (periodMode) {
                                dueTimeEndMinutes = (minutes + 30).coerceAtMost(1439)
                            }
                        },
                        label = { Text("Сейчас") }
                    )
                    if (periodMode && dueTimeMinutes != null) {
                        listOf(30 to "+30м", 60 to "+1ч", 120 to "+2ч").forEach { (delta, label) ->
                            FilterChip(
                                selected = dueTimeEndMinutes == (dueTimeMinutes!! + delta).coerceAtMost(1439),
                                onClick = {
                                    dueTimeEndMinutes = (dueTimeMinutes!! + delta).coerceAtMost(1439)
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    if (dueTimeMinutes != null) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                dueTimeMinutes = null
                                dueTimeEndMinutes = null
                                periodMode = false
                            },
                            label = { Text("Без времени") }
                        )
                    }
                }
            }
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
                            dueTimeEndMinutes = null
                            periodMode = false
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

    if (showTimePicker && timeEnabled) {
        val timePickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val timePickerInitial = when (timePickerTarget) {
            TimePickerTarget.Start -> dueTimeMinutes ?: DateUtils.hourMinuteToMinutes(9, 0)
            TimePickerTarget.End -> dueTimeEndMinutes
                ?: ((dueTimeMinutes ?: DateUtils.hourMinuteToMinutes(9, 0)) + 30).coerceAtMost(1439)
        }
        val (initialHour, initialMinute) = DateUtils.minutesToHourMinute(timePickerInitial)
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )
        val pickerTitle = when (timePickerTarget) {
            TimePickerTarget.Start -> "Начало"
            TimePickerTarget.End -> "Конец"
        }
        ModalBottomSheet(
            onDismissRequest = { showTimePicker = false },
            sheetState = timePickerSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showTimePicker = false }) { Text("Отмена") }
                    Text(
                        text = pickerTitle,
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(
                        onClick = {
                            val picked = DateUtils.hourMinuteToMinutes(
                                timePickerState.hour,
                                timePickerState.minute
                            )
                            when (timePickerTarget) {
                                TimePickerTarget.Start -> {
                                    dueTimeMinutes = picked
                                    if (periodMode) {
                                        dueTimeEndMinutes?.let { end ->
                                            if (end <= picked) {
                                                dueTimeEndMinutes = (picked + 30).coerceAtMost(1439)
                                            }
                                        } ?: run {
                                            dueTimeEndMinutes = (picked + 30).coerceAtMost(1439)
                                        }
                                    }
                                }
                                TimePickerTarget.End -> {
                                    val start = dueTimeMinutes ?: DateUtils.hourMinuteToMinutes(9, 0)
                                    if (dueTimeMinutes == null) {
                                        dueTimeMinutes = start
                                    }
                                    dueTimeEndMinutes = if (picked <= start) {
                                        (start + 30).coerceAtMost(1439)
                                    } else {
                                        picked
                                    }
                                    periodMode = true
                                }
                            }
                            showTimePicker = false
                        }
                    ) {
                        Text(
                            "Готово",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = timePickerState,
                        modifier = Modifier.graphicsLayer {
                            scaleX = 0.88f
                            scaleY = 0.88f
                        },
                        layoutType = TimePickerLayoutType.Vertical,
                        colors = TimePickerDefaults.colors(
                            timeSelectorSelectedContainerColor =
                                MaterialTheme.colorScheme.primaryContainer,
                            timeSelectorSelectedContentColor =
                                MaterialTheme.colorScheme.onPrimaryContainer,
                            timeSelectorUnselectedContainerColor =
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            timeSelectorUnselectedContentColor =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            selectorColor = MaterialTheme.colorScheme.primary,
                            clockDialColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            clockDialSelectedContentColor = MaterialTheme.colorScheme.onSurface,
                            clockDialUnselectedContentColor =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = {
                            dueTimeMinutes = null
                            dueTimeEndMinutes = null
                            periodMode = false
                            showTimePicker = false
                        }
                    ) {
                        Text(
                            "Без времени",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
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
