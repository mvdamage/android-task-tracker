package com.learning.tasktracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class HealthSnapshot(
    val persons: List<HealthPersonEntity>,
    val analyses: List<HealthAnalysisEntity>,
    val prescriptions: List<HealthPrescriptionEntity>,
    val links: List<HealthPrescriptionTaskLinkEntity>
)

class HealthRepository(
    private val dao: HealthDao,
    private val taskRepository: TaskRepository
) {
    fun observeSnapshot(): Flow<HealthSnapshot> = combine(
        dao.observePersons(),
        dao.observeAnalyses(),
        dao.observeActivePrescriptions(),
        dao.observeLinks()
    ) { persons, analyses, prescriptions, links ->
        HealthSnapshot(persons, analyses, prescriptions, links)
    }

    suspend fun addPerson(name: String, note: String = ""): Long {
        val normalized = HealthRules.normalizePersonName(name) ?: return -1L
        if (dao.personCount() >= HealthRules.MAX_PERSONS) return -1L
        val now = System.currentTimeMillis()
        return dao.insertPerson(
            HealthPersonEntity(
                name = normalized,
                note = note.trim(),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updatePerson(person: HealthPersonEntity, name: String, note: String) {
        val normalized = HealthRules.normalizePersonName(name) ?: return
        dao.updatePerson(
            person.copy(
                name = normalized,
                note = note.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /** Cascades analyses/prescriptions/links via FK; does not delete tasks. */
    suspend fun deletePerson(person: HealthPersonEntity) {
        dao.deletePerson(person)
    }

    suspend fun addAnalysis(
        personId: Long,
        title: String,
        status: HealthAnalysisStatus = HealthAnalysisStatus.PLANNED,
        plannedDateEpochDay: Long? = null,
        doneDateEpochDay: Long? = null,
        comment: String = ""
    ): Long {
        val normalized = HealthRules.normalizeTitle(title) ?: return -1L
        if (dao.getPerson(personId) == null) return -1L
        val resolvedDone = when (status) {
            HealthAnalysisStatus.DONE -> doneDateEpochDay ?: DateUtils.todayEpochDay()
            HealthAnalysisStatus.PLANNED -> null
        }
        val now = System.currentTimeMillis()
        return dao.insertAnalysis(
            HealthAnalysisEntity(
                personId = personId,
                title = normalized,
                status = status,
                plannedDateEpochDay = plannedDateEpochDay,
                doneDateEpochDay = resolvedDone,
                comment = comment.trim(),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateAnalysis(
        analysis: HealthAnalysisEntity,
        title: String,
        status: HealthAnalysisStatus,
        plannedDateEpochDay: Long?,
        doneDateEpochDay: Long?,
        comment: String
    ) {
        val normalized = HealthRules.normalizeTitle(title) ?: return
        val resolvedDone = when (status) {
            HealthAnalysisStatus.DONE -> doneDateEpochDay ?: DateUtils.todayEpochDay()
            HealthAnalysisStatus.PLANNED -> null
        }
        dao.updateAnalysis(
            analysis.copy(
                title = normalized,
                status = status,
                plannedDateEpochDay = plannedDateEpochDay,
                doneDateEpochDay = resolvedDone,
                comment = comment.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteAnalysis(analysis: HealthAnalysisEntity) {
        dao.deleteAnalysis(analysis)
    }

    suspend fun addPrescription(
        personId: Long,
        type: HealthPrescriptionType,
        title: String,
        dosage: String = "",
        comment: String = "",
        startDateEpochDay: Long? = null,
        endDateEpochDay: Long? = null,
        visitDateEpochDay: Long? = null
    ): Long {
        val normalized = HealthRules.normalizeTitle(title) ?: return -1L
        if (dao.getPerson(personId) == null) return -1L
        val now = System.currentTimeMillis()
        return dao.insertPrescription(
            HealthPrescriptionEntity(
                personId = personId,
                type = type,
                title = normalized,
                dosage = if (type == HealthPrescriptionType.MEDICATION) dosage.trim() else "",
                comment = comment.trim(),
                startDateEpochDay = startDateEpochDay,
                endDateEpochDay = endDateEpochDay,
                visitDateEpochDay = if (type == HealthPrescriptionType.VISIT) visitDateEpochDay else null,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updatePrescription(
        prescription: HealthPrescriptionEntity,
        type: HealthPrescriptionType,
        title: String,
        dosage: String,
        comment: String,
        startDateEpochDay: Long?,
        endDateEpochDay: Long?,
        visitDateEpochDay: Long?,
        archived: Boolean = prescription.archived
    ) {
        val normalized = HealthRules.normalizeTitle(title) ?: return
        dao.updatePrescription(
            prescription.copy(
                type = type,
                title = normalized,
                dosage = if (type == HealthPrescriptionType.MEDICATION) dosage.trim() else "",
                comment = comment.trim(),
                startDateEpochDay = startDateEpochDay,
                endDateEpochDay = endDateEpochDay,
                visitDateEpochDay = if (type == HealthPrescriptionType.VISIT) visitDateEpochDay else null,
                archived = archived,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun archivePrescription(prescription: HealthPrescriptionEntity) {
        dao.updatePrescription(
            prescription.copy(archived = true, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun deletePrescription(prescription: HealthPrescriptionEntity) {
        dao.deletePrescription(prescription)
    }

    /**
     * Creates a normal TASK visible in the task list and links it to the prescription.
     * @return task id, or -1 on failure
     */
    suspend fun createTaskFromPrescription(
        prescription: HealthPrescriptionEntity,
        personName: String,
        dueDateEpochDay: Long?,
        dueTimeMinutes: Int? = null,
        recurrenceType: RecurrenceType = RecurrenceType.NONE,
        recurrenceWeekdayMask: Int = 0,
        recurrenceEndEpochDay: Long? = null
    ): Long {
        val current = dao.getPrescription(prescription.id) ?: return -1L
        val title = HealthRules.taskTitleFor(current.type, current.title)
        val notes = HealthRules.taskNotesFor(personName)
        val taskId = taskRepository.add(
            title = title,
            notes = notes,
            priority = Priority.MEDIUM,
            dueDateEpochDay = dueDateEpochDay,
            recurrenceType = recurrenceType,
            recurrenceWeekdayMask = recurrenceWeekdayMask,
            recurrenceEndEpochDay = recurrenceEndEpochDay,
            dueTimeMinutes = dueTimeMinutes,
            dueTimeEndMinutes = null,
            kind = TaskKind.TASK
        )
        if (taskId <= 0L) return -1L
        dao.insertLink(
            HealthPrescriptionTaskLinkEntity(
                prescriptionId = current.id,
                taskId = taskId
            )
        )
        return taskId
    }
}
