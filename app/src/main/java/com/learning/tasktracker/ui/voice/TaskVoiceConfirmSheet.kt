package com.learning.tasktracker.ui.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.learning.tasktracker.R
import com.learning.tasktracker.data.DateUtils
import com.learning.tasktracker.ui.components.EditorOptionRow
import com.learning.tasktracker.ui.theme.extendedColors
import com.learning.tasktracker.voice.TaskVoiceParseResult
import java.time.Instant
import java.time.ZoneOffset

data class TaskVoiceConfirmResult(
    val title: String,
    val dueDateEpochDay: Long?,
    val dueTimeMinutes: Int?,
    val dueTimeEndMinutes: Int?
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskVoiceConfirmSheet(
    parsed: TaskVoiceParseResult,
    onDismiss: () -> Unit,
    onConfirm: (TaskVoiceConfirmResult) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(parsed) { mutableStateOf(parsed.title) }
    var dueDateEpochDay by remember(parsed) { mutableStateOf(parsed.dueDateEpochDay) }
    var dueTimeMinutes by remember(parsed) { mutableStateOf(parsed.dueTimeMinutes) }
    var dueTimeEndMinutes by remember(parsed) { mutableStateOf(parsed.dueTimeEndMinutes) }
    var periodMode by remember(parsed) {
        mutableStateOf(parsed.dueTimeEndMinutes != null)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var timePickerTarget by remember { mutableStateOf(TimeTarget.Start) }

    val today = DateUtils.todayEpochDay()
    val canSave = title.isNotBlank()
    val dateLabel = dueDateEpochDay?.let { DateUtils.formatFull(it) } ?: "Не задана"
    val timeLabel = when {
        dueDateEpochDay == null -> "Нужна дата"
        dueTimeMinutes == null -> "Не задано"
        periodMode && dueTimeEndMinutes != null ->
            DateUtils.formatTaskTime(dueTimeMinutes, dueTimeEndMinutes).orEmpty()
        else -> DateUtils.formatTime(dueTimeMinutes!!)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = stringResource(R.string.voice_confirm_task_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text(stringResource(R.string.voice_confirm_task_name_hint)) },
                singleLine = false,
                minLines = 1,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                supportingText = if (parsed.truncated) {
                    { Text(stringResource(R.string.voice_title_truncated)) }
                } else {
                    null
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = dueDateEpochDay == today,
                    onClick = {
                        dueDateEpochDay = today
                    },
                    label = { Text(stringResource(R.string.voice_chip_today)) }
                )
                FilterChip(
                    selected = dueDateEpochDay == today + 1,
                    onClick = {
                        dueDateEpochDay = today + 1
                    },
                    label = { Text(stringResource(R.string.voice_chip_tomorrow)) }
                )
                FilterChip(
                    selected = false,
                    onClick = {
                        dueDateEpochDay = null
                        dueTimeMinutes = null
                        dueTimeEndMinutes = null
                        periodMode = false
                    },
                    label = { Text(stringResource(R.string.voice_chip_clear_due)) }
                )
            }

            HorizontalDivider(
                color = MaterialTheme.extendedColors.divider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            EditorOptionRow(
                label = stringResource(R.string.voice_date_label),
                value = dateLabel,
                onClick = { showDatePicker = true }
            )
            HorizontalDivider(color = MaterialTheme.extendedColors.divider, thickness = 0.5.dp)

            if (dueDateEpochDay != null) {
                FlowRow(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                if (dueTimeEndMinutes == null || dueTimeEndMinutes!! <= start) {
                                    dueTimeEndMinutes = (start + 30).coerceAtMost(1439)
                                }
                            } else {
                                dueTimeEndMinutes = null
                            }
                        },
                        label = { Text(stringResource(R.string.voice_period_chip)) }
                    )
                }
                if (periodMode) {
                    EditorOptionRow(
                        label = stringResource(R.string.voice_time_from),
                        value = dueTimeMinutes?.let { DateUtils.formatTime(it) } ?: "Не задано",
                        onClick = {
                            timePickerTarget = TimeTarget.Start
                            showTimePicker = true
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.extendedColors.divider, thickness = 0.5.dp)
                    EditorOptionRow(
                        label = stringResource(R.string.voice_time_to),
                        value = dueTimeEndMinutes?.let { DateUtils.formatTime(it) } ?: "Не задано",
                        onClick = {
                            timePickerTarget = TimeTarget.End
                            showTimePicker = true
                        }
                    )
                } else {
                    EditorOptionRow(
                        label = stringResource(R.string.voice_time_label),
                        value = timeLabel,
                        onClick = {
                            timePickerTarget = TimeTarget.Start
                            showTimePicker = true
                        }
                    )
                }
                if (dueTimeMinutes != null) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            dueTimeMinutes = null
                            dueTimeEndMinutes = null
                            periodMode = false
                        },
                        label = { Text(stringResource(R.string.voice_clear_time)) },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val date = dueDateEpochDay
                    onConfirm(
                        TaskVoiceConfirmResult(
                            title = title.trim(),
                            dueDateEpochDay = date,
                            dueTimeMinutes = if (date == null) null else dueTimeMinutes,
                            dueTimeEndMinutes = if (date == null || !periodMode) null else dueTimeEndMinutes
                        )
                    )
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(R.string.voice_add))
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(R.string.voice_disclaimer_cancel))
            }
        }
    }

    if (showDatePicker) {
        val initialDay = dueDateEpochDay ?: today
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
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.voice_disclaimer_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val initial = when (timePickerTarget) {
            TimeTarget.Start -> dueTimeMinutes ?: DateUtils.hourMinuteToMinutes(9, 0)
            TimeTarget.End -> dueTimeEndMinutes
                ?: ((dueTimeMinutes ?: DateUtils.hourMinuteToMinutes(9, 0)) + 30)
        }
        val (hour, minute) = DateUtils.minutesToHourMinute(initial)
        val timeState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertTimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                val picked = DateUtils.hourMinuteToMinutes(timeState.hour, timeState.minute)
                when (timePickerTarget) {
                    TimeTarget.Start -> {
                        dueTimeMinutes = picked
                        if (periodMode) {
                            val end = dueTimeEndMinutes
                            if (end == null || end <= picked) {
                                dueTimeEndMinutes = (picked + 30).coerceAtMost(1439)
                            }
                        }
                    }
                    TimeTarget.End -> {
                        val start = dueTimeMinutes ?: picked
                        dueTimeMinutes = start
                        dueTimeEndMinutes = if (picked <= start) {
                            (start + 30).coerceAtMost(1439)
                        } else {
                            picked
                        }
                    }
                }
                showTimePicker = false
            },
            content = { TimePicker(state = timeState) }
        )
    }
}

private enum class TimeTarget { Start, End }

@Composable
private fun AlertTimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.voice_disclaimer_cancel))
            }
        },
        text = { content() }
    )
}
