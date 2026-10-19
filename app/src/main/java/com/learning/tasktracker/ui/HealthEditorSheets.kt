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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.learning.tasktracker.data.DateUtils
import com.learning.tasktracker.data.HealthAnalysisEntity
import com.learning.tasktracker.data.HealthAnalysisStatus
import com.learning.tasktracker.data.HealthPersonEntity
import com.learning.tasktracker.data.HealthPrescriptionEntity
import com.learning.tasktracker.data.HealthPrescriptionType
import com.learning.tasktracker.data.HealthRules
import com.learning.tasktracker.data.RecurrenceType
import com.learning.tasktracker.ui.components.EditorSectionTitle
import java.time.Instant
import java.time.ZoneOffset

sealed class PersonEditorState {
    data object Create : PersonEditorState()
    data class Edit(val person: HealthPersonEntity) : PersonEditorState()
}

sealed class AnalysisEditorState {
    data object Create : AnalysisEditorState()
    data class Edit(val analysis: HealthAnalysisEntity) : AnalysisEditorState()
}

sealed class PrescriptionEditorState {
    data object Create : PrescriptionEditorState()
    data class Edit(val prescription: HealthPrescriptionEntity) : PrescriptionEditorState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PersonEditorSheet(
    state: PersonEditorState,
    onDismiss: () -> Unit,
    onSave: (name: String, note: String) -> Unit,
    onDelete: ((HealthPersonEntity) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val existing = (state as? PersonEditorState.Edit)?.person
    var name by remember(state) { mutableStateOf(existing?.name.orEmpty()) }
    var note by remember(state) { mutableStateOf(existing?.note.orEmpty()) }
    val canSave = name.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (existing == null) "Новый человек" else "Человек",
                    style = MaterialTheme.typography.titleLarge
                )
                Row {
                    if (existing != null && onDelete != null) {
                        IconButton(onClick = { onDelete(existing) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Удалить")
                        }
                    }
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    TextButton(
                        onClick = { onSave(name, note) },
                        enabled = canSave
                    ) { Text("Сохранить") }
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(HealthRules.MAX_NAME_LENGTH) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Имя") },
                singleLine = true,
                colors = healthFieldColors()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Заметка (необязательно)") },
                colors = healthFieldColors()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun AnalysisEditorSheet(
    state: AnalysisEditorState,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        status: HealthAnalysisStatus,
        plannedDateEpochDay: Long?,
        doneDateEpochDay: Long?,
        comment: String
    ) -> Unit,
    onDelete: ((HealthAnalysisEntity) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val existing = (state as? AnalysisEditorState.Edit)?.analysis
    var title by remember(state) { mutableStateOf(existing?.title.orEmpty()) }
    var status by remember(state) {
        mutableStateOf(existing?.status ?: HealthAnalysisStatus.PLANNED)
    }
    var plannedDate by remember(state) { mutableStateOf(existing?.plannedDateEpochDay) }
    var doneDate by remember(state) { mutableStateOf(existing?.doneDateEpochDay) }
    var comment by remember(state) { mutableStateOf(existing?.comment.orEmpty()) }
    var pickingPlanned by remember { mutableStateOf(false) }
    var pickingDone by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (existing == null) "Новый анализ" else "Анализ",
                    style = MaterialTheme.typography.titleLarge
                )
                Row {
                    if (existing != null && onDelete != null) {
                        IconButton(onClick = { onDelete(existing) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Удалить")
                        }
                    }
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    TextButton(
                        onClick = { onSave(title, status, plannedDate, doneDate, comment) },
                        enabled = title.isNotBlank()
                    ) { Text("Сохранить") }
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Название") },
                singleLine = true,
                colors = healthFieldColors()
            )
            Spacer(Modifier.height(12.dp))
            EditorSectionTitle("Статус")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HealthAnalysisStatus.entries.forEach { option ->
                    FilterChip(
                        selected = status == option,
                        onClick = {
                            status = option
                            if (option == HealthAnalysisStatus.DONE && doneDate == null) {
                                doneDate = DateUtils.todayEpochDay()
                            }
                        },
                        label = { Text(option.label) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { pickingPlanned = true }) {
                Text(
                    plannedDate?.let { "Плановая дата: ${DateUtils.formatShort(it)}" }
                        ?: "Плановая дата"
                )
            }
            if (status == HealthAnalysisStatus.DONE) {
                TextButton(onClick = { pickingDone = true }) {
                    Text(
                        doneDate?.let { "Дата сдачи: ${DateUtils.formatShort(it)}" }
                            ?: "Дата сдачи"
                    )
                }
            }
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Комментарий") },
                colors = healthFieldColors()
            )
        }
    }

    if (pickingPlanned) {
        HealthDatePickerDialog(
            initialEpochDay = plannedDate ?: DateUtils.todayEpochDay(),
            allowClear = true,
            onDismiss = { pickingPlanned = false },
            onConfirm = {
                plannedDate = it
                pickingPlanned = false
            }
        )
    }
    if (pickingDone) {
        HealthDatePickerDialog(
            initialEpochDay = doneDate ?: DateUtils.todayEpochDay(),
            allowClear = true,
            onDismiss = { pickingDone = false },
            onConfirm = {
                doneDate = it
                pickingDone = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun PrescriptionEditorSheet(
    state: PrescriptionEditorState,
    hasLinkedTasks: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        type: HealthPrescriptionType,
        title: String,
        dosage: String,
        comment: String,
        startDateEpochDay: Long?,
        endDateEpochDay: Long?,
        visitDateEpochDay: Long?
    ) -> Unit,
    onDelete: ((HealthPrescriptionEntity) -> Unit)? = null,
    onCreateTask: ((HealthPrescriptionEntity) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val existing = (state as? PrescriptionEditorState.Edit)?.prescription
    var type by remember(state) {
        mutableStateOf(existing?.type ?: HealthPrescriptionType.MEDICATION)
    }
    var title by remember(state) { mutableStateOf(existing?.title.orEmpty()) }
    var dosage by remember(state) { mutableStateOf(existing?.dosage.orEmpty()) }
    var comment by remember(state) { mutableStateOf(existing?.comment.orEmpty()) }
    var startDate by remember(state) { mutableStateOf(existing?.startDateEpochDay) }
    var endDate by remember(state) { mutableStateOf(existing?.endDateEpochDay) }
    var visitDate by remember(state) { mutableStateOf(existing?.visitDateEpochDay) }
    var picking by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (existing == null) "Новое назначение" else "Назначение",
                    style = MaterialTheme.typography.titleLarge
                )
                Row {
                    if (existing != null && onDelete != null) {
                        IconButton(onClick = { onDelete(existing) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Удалить")
                        }
                    }
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    TextButton(
                        onClick = {
                            onSave(type, title, dosage, comment, startDate, endDate, visitDate)
                        },
                        enabled = title.isNotBlank()
                    ) { Text("Сохранить") }
                }
            }
            EditorSectionTitle("Тип")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HealthPrescriptionType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option },
                        label = { Text(option.label) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Название") },
                singleLine = true,
                colors = healthFieldColors()
            )
            if (type == HealthPrescriptionType.MEDICATION) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Дозировка") },
                    singleLine = true,
                    colors = healthFieldColors()
                )
                TextButton(onClick = { picking = "start" }) {
                    Text(
                        startDate?.let { "Начало курса: ${DateUtils.formatShort(it)}" }
                            ?: "Начало курса"
                    )
                }
                TextButton(onClick = { picking = "end" }) {
                    Text(
                        endDate?.let { "Конец курса: ${DateUtils.formatShort(it)}" }
                            ?: "Конец курса"
                    )
                }
            } else {
                TextButton(onClick = { picking = "visit" }) {
                    Text(
                        visitDate?.let { "Дата визита: ${DateUtils.formatShort(it)}" }
                            ?: "Дата визита"
                    )
                }
            }
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Комментарий") },
                colors = healthFieldColors()
            )
            if (existing != null && onCreateTask != null) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { onCreateTask(existing) }) {
                    Text(
                        if (hasLinkedTasks) "Создать ещё задачу"
                        else "Создать задачу"
                    )
                }
            }
        }
    }

    when (picking) {
        "start" -> HealthDatePickerDialog(
            initialEpochDay = startDate ?: DateUtils.todayEpochDay(),
            allowClear = true,
            onDismiss = { picking = null },
            onConfirm = {
                startDate = it
                picking = null
            }
        )
        "end" -> HealthDatePickerDialog(
            initialEpochDay = endDate ?: DateUtils.todayEpochDay(),
            allowClear = true,
            onDismiss = { picking = null },
            onConfirm = {
                endDate = it
                picking = null
            }
        )
        "visit" -> HealthDatePickerDialog(
            initialEpochDay = visitDate ?: DateUtils.todayEpochDay(),
            allowClear = true,
            onDismiss = { picking = null },
            onConfirm = {
                visitDate = it
                picking = null
            }
        )
    }
}

private enum class CreateTaskDueMode { DAYS_AHEAD, DATE }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CreateTaskFromPrescriptionSheet(
    prescription: HealthPrescriptionEntity,
    onDismiss: () -> Unit,
    onCreate: (
        dueDateEpochDay: Long?,
        dueTimeMinutes: Int?,
        recurrenceType: RecurrenceType
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var dueMode by remember {
        mutableStateOf(
            if (prescription.type == HealthPrescriptionType.VISIT &&
                prescription.visitDateEpochDay != null
            ) {
                CreateTaskDueMode.DATE
            } else {
                CreateTaskDueMode.DAYS_AHEAD
            }
        )
    }
    var daysAheadText by remember { mutableStateOf("1") }
    var dueDate by remember {
        mutableStateOf(
            prescription.visitDateEpochDay ?: DateUtils.todayEpochDay()
        )
    }
    var timeEnabled by remember { mutableStateOf(false) }
    var hourText by remember { mutableStateOf("9") }
    var minuteText by remember { mutableStateOf("0") }
    var recurrence by remember { mutableStateOf(RecurrenceType.NONE) }
    var pickingDate by remember { mutableStateOf(false) }
    val previewTitle = HealthRules.taskTitleFor(prescription.type, prescription.title)
    val daysAhead = daysAheadText.toIntOrNull()
    val duePreview = when (dueMode) {
        CreateTaskDueMode.DAYS_AHEAD ->
            daysAhead?.let { HealthRules.dueDateFromDaysAhead(it) }
        CreateTaskDueMode.DATE -> dueDate
    }
    val canCreate = when (dueMode) {
        CreateTaskDueMode.DAYS_AHEAD -> daysAhead != null && daysAhead >= 1
        CreateTaskDueMode.DATE -> true
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Создать задачу", style = MaterialTheme.typography.titleLarge)
                Row {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    TextButton(
                        enabled = canCreate,
                        onClick = {
                            val time = if (timeEnabled) {
                                val h = hourText.toIntOrNull()?.coerceIn(0, 23) ?: 9
                                val m = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: 0
                                h * 60 + m
                            } else {
                                null
                            }
                            onCreate(duePreview, time, recurrence)
                        }
                    ) { Text("Создать") }
                }
            }
            Text(previewTitle, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            EditorSectionTitle("Срок")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = dueMode == CreateTaskDueMode.DAYS_AHEAD,
                    onClick = { dueMode = CreateTaskDueMode.DAYS_AHEAD },
                    label = { Text("Через N дней") }
                )
                FilterChip(
                    selected = dueMode == CreateTaskDueMode.DATE,
                    onClick = { dueMode = CreateTaskDueMode.DATE },
                    label = { Text("На дату") }
                )
            }
            if (dueMode == CreateTaskDueMode.DAYS_AHEAD) {
                OutlinedTextField(
                    value = daysAheadText,
                    onValueChange = { daysAheadText = it.filter { ch -> ch.isDigit() }.take(3) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Через сколько дней") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = healthFieldColors()
                )
            } else {
                TextButton(onClick = { pickingDate = true }) {
                    Text("Дата: ${DateUtils.formatShort(dueDate)}")
                }
            }
            duePreview?.let {
                Text(
                    "Будет: ${DateUtils.formatShort(it)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            FilterChip(
                selected = timeEnabled,
                onClick = { timeEnabled = !timeEnabled },
                label = { Text("Указать время") }
            )
            if (timeEnabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { hourText = it.filter { ch -> ch.isDigit() }.take(2) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Час") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = healthFieldColors()
                    )
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { minuteText = it.filter { ch -> ch.isDigit() }.take(2) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Мин") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = healthFieldColors()
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            EditorSectionTitle("Повтор")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    RecurrenceType.NONE,
                    RecurrenceType.DAILY,
                    RecurrenceType.WEEKLY,
                    RecurrenceType.MONTHLY
                ).forEach { option ->
                    FilterChip(
                        selected = recurrence == option,
                        onClick = { recurrence = option },
                        label = { Text(option.label) }
                    )
                }
            }
        }
    }

    if (pickingDate) {
        HealthDatePickerDialog(
            initialEpochDay = dueDate,
            allowClear = false,
            onDismiss = { pickingDate = false },
            onConfirm = { day ->
                if (day != null) dueDate = day
                pickingDate = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HealthDatePickerDialog(
    initialEpochDay: Long,
    allowClear: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit
) {
    val initialMillis = DateUtils.fromEpochDay(initialEpochDay)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val day = pickerState.selectedDateMillis?.let { millis ->
                        Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                            .toEpochDay()
                    }
                    onConfirm(day)
                }
            ) { Text("OK") }
        },
        dismissButton = {
            Row {
                if (allowClear) {
                    TextButton(onClick = { onConfirm(null) }) { Text("Без даты") }
                }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
private fun healthFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface
)
