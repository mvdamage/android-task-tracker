package com.learning.tasktracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learning.tasktracker.data.DateUtils
import com.learning.tasktracker.data.HealthAnalysisEntity
import com.learning.tasktracker.data.HealthAnalysisKind
import com.learning.tasktracker.data.HealthAnalysisStatus
import com.learning.tasktracker.data.HealthPersonEntity
import com.learning.tasktracker.data.HealthPrescriptionEntity
import com.learning.tasktracker.data.HealthPrescriptionType
import com.learning.tasktracker.data.HealthRules
import com.learning.tasktracker.data.labelFor
import com.learning.tasktracker.ui.components.AnyDoDivider
import kotlinx.coroutines.launch

private sealed class HealthDialog {
    data object AddPerson : HealthDialog()
    data class EditPerson(val person: HealthPersonEntity) : HealthDialog()
    data object AddAnalysis : HealthDialog()
    data class EditAnalysis(val analysis: HealthAnalysisEntity) : HealthDialog()
    data object AddPrescription : HealthDialog()
    data class AddPrescriptionForVisit(val visit: HealthAnalysisEntity) : HealthDialog()
    data class EditPrescription(val prescription: HealthPrescriptionEntity) : HealthDialog()
    data class CreateTask(val prescription: HealthPrescriptionEntity) : HealthDialog()
    data class ConfirmDeletePerson(val person: HealthPersonEntity) : HealthDialog()
    data object DoctorVisitHistory : HealthDialog()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(viewModel: HealthViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<HealthDialog?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        "Здоровье",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (state.canAddPerson) {
                                dialog = HealthDialog.AddPerson
                            } else {
                                scope.launch {
                                    snackbar.showSnackbar(
                                        "Можно добавить не больше ${HealthRules.MAX_PERSONS} человек"
                                    )
                                }
                            }
                        },
                        enabled = state.persons.isNotEmpty(),
                        modifier = Modifier.testTag(TestTags.HEALTH_ADD_PERSON)
                    ) {
                        Icon(
                            Icons.Outlined.PersonAdd,
                            contentDescription = "Добавить человека",
                            tint = if (state.canAddPerson) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                muted
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (state.selectedPerson != null) {
                FloatingActionButton(
                    onClick = { dialog = HealthDialog.AddPrescription },
                    modifier = Modifier.testTag(TestTags.HEALTH_ADD_PRESCRIPTION_FAB),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить назначение")
                }
            }
        }
    ) { padding ->
        when {
            state.persons.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.Medication,
                        contentDescription = null,
                        tint = muted,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "Добавьте человека",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Для него можно вести анализы/врачей и назначения. До ${HealthRules.MAX_PERSONS} человек.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        onClick = { dialog = HealthDialog.AddPerson },
                        modifier = Modifier.testTag(TestTags.HEALTH_EMPTY_ADD_PERSON)
                    ) {
                        Text("Добавить человека")
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Не является медицинской рекомендацией. Данные хранятся только на устройстве.",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 88.dp
                    )
                ) {
                    item {
                        PersonSwitcher(
                            persons = state.persons,
                            selectedId = state.selectedPersonId,
                            onSelect = viewModel::selectPerson,
                            onEditSelected = {
                                state.selectedPerson?.let {
                                    dialog = HealthDialog.EditPerson(it)
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Не является медицинской рекомендацией. Данные только на устройстве.",
                            style = MaterialTheme.typography.bodySmall,
                            color = muted
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    item {
                        SectionHeader(
                            title = "Анализы/врачи",
                            actionLabel = "Добавить",
                            onAction = { dialog = HealthDialog.AddAnalysis },
                            actionTestTag = TestTags.HEALTH_ADD_ANALYSIS
                        )
                        TextButton(
                            onClick = { dialog = HealthDialog.DoctorVisitHistory },
                            modifier = Modifier.testTag(TestTags.HEALTH_DOCTOR_HISTORY)
                        ) {
                            val count = state.doctorVisitHistory.size
                            Text(
                                if (count > 0) "История приёмов ($count)"
                                else "История приёмов"
                            )
                        }
                    }
                    if (state.analyses.isEmpty()) {
                        item {
                            Text(
                                "Пока нет анализов и запланированных приёмов",
                                style = MaterialTheme.typography.bodyMedium,
                                color = muted,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(state.analyses, key = { "a-${it.id}" }) { analysis ->
                            AnalysisRow(
                                analysis = analysis,
                                onClick = { dialog = HealthDialog.EditAnalysis(analysis) },
                                onAddPrescription = if (analysis.kind == HealthAnalysisKind.DOCTOR) {
                                    {
                                        dialog = HealthDialog.AddPrescriptionForVisit(analysis)
                                    }
                                } else {
                                    null
                                }
                            )
                            AnyDoDivider()
                        }
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        SectionHeader(
                            title = "Назначения",
                            actionLabel = null,
                            onAction = null
                        )
                    }
                    if (state.prescriptions.isEmpty()) {
                        item {
                            Text(
                                "Пока нет назначений",
                                style = MaterialTheme.typography.bodyMedium,
                                color = muted,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(state.prescriptions, key = { "p-${it.id}" }) { prescription ->
                            val linked = state.linkedTaskIdsByPrescription[prescription.id]
                                .orEmpty()
                            val visitTitle = prescription.sourceAnalysisId?.let {
                                state.analysisTitleById[it]
                            }
                            PrescriptionRow(
                                prescription = prescription,
                                hasLinkedTasks = linked.isNotEmpty(),
                                sourceVisitTitle = visitTitle,
                                onClick = {
                                    dialog = HealthDialog.EditPrescription(prescription)
                                },
                                onCreateTask = {
                                    dialog = HealthDialog.CreateTask(prescription)
                                }
                            )
                            AnyDoDivider()
                        }
                    }
                }
            }
        }
    }

    when (val d = dialog) {
        HealthDialog.AddPerson -> PersonEditorSheet(
            state = PersonEditorState.Create,
            onDismiss = { dialog = null },
            onSave = { name, note ->
                viewModel.addPerson(name, note) { ok ->
                    dialog = null
                    if (!ok) {
                        scope.launch {
                            snackbar.showSnackbar(
                                "Можно добавить не больше ${HealthRules.MAX_PERSONS} человек"
                            )
                        }
                    }
                }
            }
        )
        is HealthDialog.EditPerson -> PersonEditorSheet(
            state = PersonEditorState.Edit(d.person),
            onDismiss = { dialog = null },
            onSave = { name, note ->
                viewModel.updatePerson(d.person, name, note)
                dialog = null
            },
            onDelete = {
                dialog = HealthDialog.ConfirmDeletePerson(d.person)
            }
        )
        HealthDialog.AddAnalysis -> AnalysisEditorSheet(
            state = AnalysisEditorState.Create,
            onDismiss = { dialog = null },
            onSave = { kind, title, status, planned, done, comment ->
                viewModel.addAnalysis(kind, title, status, planned, done, comment)
                dialog = null
            }
        )
        is HealthDialog.EditAnalysis -> AnalysisEditorSheet(
            state = AnalysisEditorState.Edit(d.analysis),
            onDismiss = { dialog = null },
            onSave = { kind, title, status, planned, done, comment ->
                viewModel.updateAnalysis(d.analysis, kind, title, status, planned, done, comment)
                dialog = null
            },
            onDelete = {
                viewModel.deleteAnalysis(d.analysis)
                dialog = null
            }
        )
        HealthDialog.AddPrescription -> PrescriptionEditorSheet(
            state = PrescriptionEditorState.Create,
            hasLinkedTasks = false,
            doctorVisits = state.doctorVisits,
            onDismiss = { dialog = null },
            onSave = { type, title, dosage, comment, start, end, visit, sourceAnalysisId ->
                viewModel.addPrescription(
                    type, title, dosage, comment, start, end, visit, sourceAnalysisId
                )
                dialog = null
            },
            onSaveMedications = { meds, comment, start, end, sourceAnalysisId ->
                viewModel.addMedications(meds, comment, start, end, sourceAnalysisId)
                dialog = null
            }
        )
        is HealthDialog.AddPrescriptionForVisit -> PrescriptionEditorSheet(
            state = PrescriptionEditorState.Create,
            hasLinkedTasks = false,
            doctorVisits = state.doctorVisits,
            lockedSourceAnalysisId = d.visit.id,
            onDismiss = { dialog = null },
            onSave = { type, title, dosage, comment, start, end, visit, sourceAnalysisId ->
                viewModel.addPrescription(
                    type,
                    title,
                    dosage,
                    comment,
                    start,
                    end,
                    visit,
                    sourceAnalysisId ?: d.visit.id
                )
                dialog = null
            },
            onSaveMedications = { meds, comment, start, end, sourceAnalysisId ->
                viewModel.addMedications(
                    meds,
                    comment,
                    start,
                    end,
                    sourceAnalysisId ?: d.visit.id
                )
                dialog = null
            }
        )
        is HealthDialog.EditPrescription -> PrescriptionEditorSheet(
            state = PrescriptionEditorState.Edit(d.prescription),
            hasLinkedTasks = state.linkedTaskIdsByPrescription[d.prescription.id]
                .orEmpty()
                .isNotEmpty(),
            doctorVisits = state.doctorVisits,
            onDismiss = { dialog = null },
            onSave = { type, title, dosage, comment, start, end, visit, sourceAnalysisId ->
                viewModel.updatePrescription(
                    d.prescription,
                    type,
                    title,
                    dosage,
                    comment,
                    start,
                    end,
                    visit,
                    sourceAnalysisId
                )
                dialog = null
            },
            onDelete = {
                viewModel.deletePrescription(d.prescription)
                dialog = null
            },
            onCreateTask = {
                dialog = HealthDialog.CreateTask(it)
            }
        )
        is HealthDialog.CreateTask -> CreateTaskFromPrescriptionSheet(
            prescription = d.prescription,
            onDismiss = { dialog = null },
            onCreate = { due, time, recurrence ->
                viewModel.createTaskFromPrescription(
                    prescription = d.prescription,
                    dueDateEpochDay = due,
                    dueTimeMinutes = time,
                    recurrenceType = recurrence
                ) { ok ->
                    dialog = null
                    scope.launch {
                        snackbar.showSnackbar(
                            if (ok) "Задача добавлена в список дел"
                            else "Не удалось создать задачу"
                        )
                    }
                }
            }
        )
        HealthDialog.DoctorVisitHistory -> DoctorVisitHistorySheet(
            history = state.doctorVisitHistory,
            onDismiss = { dialog = null },
            onOpenVisit = { visit ->
                dialog = HealthDialog.EditAnalysis(visit)
            }
        )
        is HealthDialog.ConfirmDeletePerson -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Удалить человека?") },
            text = {
                Text(
                    "Удалить «${d.person.name}» и все анализы/врачей и назначения? " +
                        "Задачи в списке дел останутся."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePerson(d.person)
                        dialog = null
                    }
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) { Text("Отмена") }
            }
        )
        null -> Unit
    }
}

@Composable
private fun PersonSwitcher(
    persons: List<HealthPersonEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onEditSelected: () -> Unit
) {
    if (persons.size == 1) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEditSelected),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                persons.first().name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onEditSelected) { Text("Изменить") }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            persons.forEach { person ->
                FilterChip(
                    selected = person.id == selectedId,
                    onClick = { onSelect(person.id) },
                    label = { Text(person.name) }
                )
            }
            TextButton(onClick = onEditSelected) { Text("Изменить") }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    actionTestTag: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                modifier = actionTestTag?.let { Modifier.testTag(it) } ?: Modifier
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun AnalysisRow(
    analysis: HealthAnalysisEntity,
    onClick: () -> Unit,
    onAddPrescription: (() -> Unit)? = null
) {
    val today = DateUtils.todayEpochDay()
    val overdue = analysis.status == HealthAnalysisStatus.PLANNED &&
        analysis.plannedDateEpochDay != null &&
        analysis.plannedDateEpochDay < today
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    analysis.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = buildString {
                    append(analysis.kind.label)
                    append(" · ")
                    append(analysis.status.labelFor(analysis.kind))
                    if (overdue) append(" · просрочен")
                    analysis.plannedDateEpochDay?.let {
                        append(" · ")
                        append(DateUtils.formatShort(it))
                    }
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (onAddPrescription != null) {
                TextButton(onClick = onAddPrescription) { Text("Назначение") }
            }
        }
    }
}

@Composable
private fun PrescriptionRow(
    prescription: HealthPrescriptionEntity,
    hasLinkedTasks: Boolean,
    sourceVisitTitle: String?,
    onClick: () -> Unit,
    onCreateTask: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    prescription.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = buildString {
                    append(prescription.type.label)
                    if (prescription.type == HealthPrescriptionType.MEDICATION &&
                        prescription.dosage.isNotBlank()
                    ) {
                        append(" · ")
                        append(prescription.dosage)
                    }
                    if (prescription.type == HealthPrescriptionType.VISIT) {
                        prescription.visitDateEpochDay?.let {
                            append(" · ")
                            append(DateUtils.formatShort(it))
                        }
                    }
                    if (!sourceVisitTitle.isNullOrBlank()) {
                        append(" · приём: ")
                        append(sourceVisitTitle)
                    }
                    if (hasLinkedTasks) append(" · есть задача")
                }
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onCreateTask) { Text("Задача") }
        }
    }
}
