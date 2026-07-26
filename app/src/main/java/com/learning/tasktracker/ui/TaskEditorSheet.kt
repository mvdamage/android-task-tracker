package com.learning.tasktracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.learning.tasktracker.data.DateUtils
import com.learning.tasktracker.data.Priority
import com.learning.tasktracker.data.RecurrenceType
import com.learning.tasktracker.data.TaskEntity
import com.learning.tasktracker.ui.components.EditorSectionTitle
import com.learning.tasktracker.ui.components.HorizontalSuggestionPills
import com.learning.tasktracker.ui.components.PriorityChip
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
    val dueDateEpochDay: Long,
    val recurrenceType: RecurrenceType,
    val recurrenceWeekdayMask: Int,
    val dueTimeMinutes: Int?
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun TaskEditorSheet(
    state: EditorState,
    titleHistory: List<String>,
    onDismiss: () -> Unit,
    onSave: (TaskEditorResult) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val existing = (state as? EditorState.Edit)?.task
    var title by remember(state) { mutableStateOf(existing?.title.orEmpty()) }
    var notes by remember(state) { mutableStateOf(existing?.notes.orEmpty()) }
    var priority by remember(state) { mutableStateOf(existing?.priority ?: Priority.MEDIUM) }
    var dueDateEpochDay by remember(state) {
        mutableLongStateOf(existing?.dueDateEpochDay ?: DateUtils.todayEpochDay())
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
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val titleSuggestions = remember(title, titleHistory) {
        TaskViewModel.filterTitleSuggestions(titleHistory, title)
    }
    val customDaysValid = recurrenceType != RecurrenceType.CUSTOM_DAYS || recurrenceWeekdayMask != 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (existing == null) "Новая задача" else "Редактировать",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))

            EditorSectionTitle("Название")
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (titleSuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                EditorSectionTitle("Подсказки")
                HorizontalSuggestionPills(
                    suggestions = titleSuggestions,
                    onSelect = { title = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            EditorSectionTitle("Заметки")
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Заметки") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            EditorSectionTitle("Когда")
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
            Spacer(modifier = Modifier.height(8.dp))
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
                        Text(dueTimeMinutes?.let { DateUtils.formatTime(it) } ?: "Указать время")
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            EditorSectionTitle("Повторение")
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

            Spacer(modifier = Modifier.height(16.dp))
            EditorSectionTitle("Приоритет")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { value ->
                    PriorityChip(
                        priority = value,
                        selected = priority == value,
                        onClick = { priority = value }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
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
                enabled = title.isNotBlank() && customDaysValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }
        }
    }

    if (showDatePicker) {
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
}
