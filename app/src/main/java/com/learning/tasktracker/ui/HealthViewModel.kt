package com.learning.tasktracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.learning.tasktracker.data.HealthAnalysisEntity
import com.learning.tasktracker.data.HealthAnalysisKind
import com.learning.tasktracker.data.HealthAnalysisStatus
import com.learning.tasktracker.data.HealthPersonEntity
import com.learning.tasktracker.data.HealthPrescriptionEntity
import com.learning.tasktracker.data.HealthPrescriptionType
import com.learning.tasktracker.data.HealthRepository
import com.learning.tasktracker.data.HealthRules
import com.learning.tasktracker.data.RecurrenceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HealthUiState(
    val persons: List<HealthPersonEntity> = emptyList(),
    val selectedPersonId: Long? = null,
    val selectedPerson: HealthPersonEntity? = null,
    val analyses: List<HealthAnalysisEntity> = emptyList(),
    val doctorVisits: List<HealthAnalysisEntity> = emptyList(),
    val prescriptions: List<HealthPrescriptionEntity> = emptyList(),
    val analysisTitleById: Map<Long, String> = emptyMap(),
    val linkedTaskIdsByPrescription: Map<Long, List<Long>> = emptyMap(),
    val canAddPerson: Boolean = true
)

class HealthViewModel(
    private val repository: HealthRepository
) : ViewModel() {
    private val selectedPersonId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<HealthUiState> = combine(
        repository.observeSnapshot(),
        selectedPersonId
    ) { snapshot, selectedId ->
        val persons = snapshot.persons
        val resolvedId = when {
            persons.isEmpty() -> null
            selectedId != null && persons.any { it.id == selectedId } -> selectedId
            else -> persons.first().id
        }
        val personAnalyses = snapshot.analyses
            .filter { it.personId == resolvedId }
            .sortedWith(
                compareBy<HealthAnalysisEntity> { it.kind != HealthAnalysisKind.DOCTOR }
                    .thenBy { it.status != HealthAnalysisStatus.PLANNED }
                    .thenBy { it.plannedDateEpochDay ?: Long.MAX_VALUE }
                    .thenByDescending { it.createdAt }
            )
        val personPrescriptions = snapshot.prescriptions.filter { it.personId == resolvedId }
        HealthUiState(
            persons = persons,
            selectedPersonId = resolvedId,
            selectedPerson = persons.firstOrNull { it.id == resolvedId },
            analyses = personAnalyses,
            doctorVisits = personAnalyses.filter { it.kind == HealthAnalysisKind.DOCTOR },
            prescriptions = personPrescriptions,
            analysisTitleById = personAnalyses.associate { it.id to it.title },
            linkedTaskIdsByPrescription = snapshot.links.groupBy({ it.prescriptionId }, { it.taskId }),
            canAddPerson = HealthRules.canAddPerson(persons.size)
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HealthUiState()
    )

    fun selectPerson(personId: Long) {
        selectedPersonId.value = personId
    }

    fun addPerson(name: String, note: String = "", onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.addPerson(name, note)
            if (id > 0L) selectedPersonId.value = id
            onResult(id > 0L)
        }
    }

    fun updatePerson(person: HealthPersonEntity, name: String, note: String) {
        viewModelScope.launch { repository.updatePerson(person, name, note) }
    }

    fun deletePerson(person: HealthPersonEntity) {
        viewModelScope.launch { repository.deletePerson(person) }
    }

    fun addAnalysis(
        kind: HealthAnalysisKind,
        title: String,
        status: HealthAnalysisStatus,
        plannedDateEpochDay: Long?,
        doneDateEpochDay: Long?,
        comment: String
    ) {
        val personId = selectedPersonId.value ?: return
        viewModelScope.launch {
            repository.addAnalysis(
                personId = personId,
                kind = kind,
                title = title,
                status = status,
                plannedDateEpochDay = plannedDateEpochDay,
                doneDateEpochDay = doneDateEpochDay,
                comment = comment
            )
        }
    }

    fun updateAnalysis(
        analysis: HealthAnalysisEntity,
        kind: HealthAnalysisKind,
        title: String,
        status: HealthAnalysisStatus,
        plannedDateEpochDay: Long?,
        doneDateEpochDay: Long?,
        comment: String
    ) {
        viewModelScope.launch {
            repository.updateAnalysis(
                analysis, kind, title, status, plannedDateEpochDay, doneDateEpochDay, comment
            )
        }
    }

    fun deleteAnalysis(analysis: HealthAnalysisEntity) {
        viewModelScope.launch { repository.deleteAnalysis(analysis) }
    }

    fun addPrescription(
        type: HealthPrescriptionType,
        title: String,
        dosage: String,
        comment: String,
        startDateEpochDay: Long?,
        endDateEpochDay: Long?,
        visitDateEpochDay: Long?,
        sourceAnalysisId: Long? = null
    ) {
        val personId = selectedPersonId.value ?: return
        viewModelScope.launch {
            repository.addPrescription(
                personId = personId,
                type = type,
                title = title,
                dosage = dosage,
                comment = comment,
                startDateEpochDay = startDateEpochDay,
                endDateEpochDay = endDateEpochDay,
                visitDateEpochDay = visitDateEpochDay,
                sourceAnalysisId = sourceAnalysisId
            )
        }
    }

    fun addMedications(
        medications: List<MedicationDraft>,
        comment: String,
        startDateEpochDay: Long?,
        endDateEpochDay: Long?,
        sourceAnalysisId: Long? = null
    ) {
        val personId = selectedPersonId.value ?: return
        viewModelScope.launch {
            medications
                .map { it.copy(title = it.title.trim(), dosage = it.dosage.trim()) }
                .filter { it.title.isNotEmpty() }
                .forEach { draft ->
                    repository.addPrescription(
                        personId = personId,
                        type = HealthPrescriptionType.MEDICATION,
                        title = draft.title,
                        dosage = draft.dosage,
                        comment = comment,
                        startDateEpochDay = startDateEpochDay,
                        endDateEpochDay = endDateEpochDay,
                        visitDateEpochDay = null,
                        sourceAnalysisId = sourceAnalysisId
                    )
                }
        }
    }

    fun updatePrescription(
        prescription: HealthPrescriptionEntity,
        type: HealthPrescriptionType,
        title: String,
        dosage: String,
        comment: String,
        startDateEpochDay: Long?,
        endDateEpochDay: Long?,
        visitDateEpochDay: Long?,
        sourceAnalysisId: Long? = prescription.sourceAnalysisId
    ) {
        viewModelScope.launch {
            repository.updatePrescription(
                prescription,
                type,
                title,
                dosage,
                comment,
                startDateEpochDay,
                endDateEpochDay,
                visitDateEpochDay,
                sourceAnalysisId = sourceAnalysisId
            )
        }
    }

    fun archivePrescription(prescription: HealthPrescriptionEntity) {
        viewModelScope.launch { repository.archivePrescription(prescription) }
    }

    fun deletePrescription(prescription: HealthPrescriptionEntity) {
        viewModelScope.launch { repository.deletePrescription(prescription) }
    }

    fun createTaskFromPrescription(
        prescription: HealthPrescriptionEntity,
        dueDateEpochDay: Long?,
        dueTimeMinutes: Int?,
        recurrenceType: RecurrenceType,
        recurrenceWeekdayMask: Int = 0,
        onResult: (Boolean) -> Unit = {}
    ) {
        val personName = uiState.value.selectedPerson?.name.orEmpty()
        viewModelScope.launch {
            val taskId = repository.createTaskFromPrescription(
                prescription = prescription,
                personName = personName,
                dueDateEpochDay = dueDateEpochDay,
                dueTimeMinutes = dueTimeMinutes,
                recurrenceType = recurrenceType,
                recurrenceWeekdayMask = recurrenceWeekdayMask
            )
            onResult(taskId > 0L)
        }
    }

    class Factory(
        private val repository: HealthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HealthViewModel(repository) as T
        }
    }
}
