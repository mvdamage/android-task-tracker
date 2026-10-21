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
        kind: HealthAnalysisKind = HealthAnalysisKind.LAB,
        title: String,
        status: HealthAnalysisStatus = HealthAnalysisStatus.PLANNED,
        plannedDateEpochDay: Long? = null,
        plannedTimeMinutes: Int? = null,
        doneDateEpochDay: Long? = null,
        comment: String = ""
    ): Long {
        val normalized = HealthRules.normalizeTitle(title) ?: return -1L
        if (dao.getPerson(personId) == null) return -1L
        val resolvedDone = when (status) {
            HealthAnalysisStatus.DONE -> doneDateEpochDay ?: DateUtils.todayEpochDay()
            HealthAnalysisStatus.PLANNED -> null
        }
        val resolvedTime =
            if (kind == HealthAnalysisKind.DOCTOR && plannedDateEpochDay != null) {
                plannedTimeMinutes
            } else {
                null
            }
        val now = System.currentTimeMillis()
        val analysisId = dao.insertAnalysis(
            HealthAnalysisEntity(
                personId = personId,
                kind = kind,
                title = normalized,
                status = status,
                plannedDateEpochDay = plannedDateEpochDay,
                plannedTimeMinutes = resolvedTime,
                doneDateEpochDay = resolvedDone,
                comment = comment.trim(),
                createdAt = now,
                updatedAt = now
            )
        )
        if (analysisId > 0L) {
            val saved = dao.getAnalysis(analysisId) ?: return analysisId
            syncDoctorVisitTask(saved)
        }
        return analysisId
    }

    suspend fun updateAnalysis(
        analysis: HealthAnalysisEntity,
        kind: HealthAnalysisKind,
        title: String,
        status: HealthAnalysisStatus,
        plannedDateEpochDay: Long?,
        plannedTimeMinutes: Int? = analysis.plannedTimeMinutes,
        doneDateEpochDay: Long?,
        comment: String
    ) {
        val normalized = HealthRules.normalizeTitle(title) ?: return
        val resolvedDone = when (status) {
            HealthAnalysisStatus.DONE -> doneDateEpochDay ?: DateUtils.todayEpochDay()
            HealthAnalysisStatus.PLANNED -> null
        }
        val resolvedTime =
            if (kind == HealthAnalysisKind.DOCTOR && plannedDateEpochDay != null) {
                plannedTimeMinutes
            } else {
                null
            }
        val updated = analysis.copy(
            kind = kind,
            title = normalized,
            status = status,
            plannedDateEpochDay = plannedDateEpochDay,
            plannedTimeMinutes = resolvedTime,
            doneDateEpochDay = resolvedDone,
            comment = comment.trim(),
            updatedAt = System.currentTimeMillis()
        )
        dao.updateAnalysis(updated)
        syncDoctorVisitTask(dao.getAnalysis(analysis.id) ?: updated)
    }

    suspend fun deleteAnalysis(analysis: HealthAnalysisEntity) {
        dao.deleteAnalysis(analysis)
    }

    /**
     * Ensures a planned doctor visit has a visible TASK with the same date/time.
     * Marks the linked task done when the visit is completed.
     */
    private suspend fun syncDoctorVisitTask(analysis: HealthAnalysisEntity) {
        if (analysis.kind != HealthAnalysisKind.DOCTOR) return
        val personName = dao.getPerson(analysis.personId)?.name.orEmpty()
        val title = HealthRules.doctorVisitTaskTitle(analysis.title)
        val notes = HealthRules.taskNotesFor(personName)
        val existingTaskId = analysis.linkedTaskId
        val existingTask = existingTaskId?.let { taskRepository.getById(it) }

        when {
            analysis.status == HealthAnalysisStatus.PLANNED &&
                analysis.plannedDateEpochDay != null -> {
                if (existingTask != null) {
                    taskRepository.update(
                        existingTask.copy(
                            title = title,
                            notes = notes,
                            dueDateEpochDay = analysis.plannedDateEpochDay,
                            dueTimeMinutes = analysis.plannedTimeMinutes,
                            dueTimeEndMinutes = null,
                            isDone = false,
                            kind = TaskKind.TASK
                        )
                    )
                } else {
                    val taskId = taskRepository.add(
                        title = title,
                        notes = notes,
                        priority = Priority.MEDIUM,
                        dueDateEpochDay = analysis.plannedDateEpochDay,
                        dueTimeMinutes = analysis.plannedTimeMinutes,
                        dueTimeEndMinutes = null,
                        kind = TaskKind.TASK
                    )
                    if (taskId > 0L) {
                        dao.updateAnalysis(analysis.copy(linkedTaskId = taskId))
                    }
                }
            }
            analysis.status == HealthAnalysisStatus.DONE && existingTask != null -> {
                if (!existingTask.isDone) {
                    taskRepository.update(existingTask.copy(isDone = true))
                }
            }
        }
    }

    suspend fun addPrescription(
        personId: Long,
        type: HealthPrescriptionType,
        title: String,
        dosage: String = "",
        comment: String = "",
        startDateEpochDay: Long? = null,
        endDateEpochDay: Long? = null,
        visitDateEpochDay: Long? = null,
        sourceAnalysisId: Long? = null
    ): Long {
        val normalized = HealthRules.normalizeTitle(title) ?: return -1L
        if (dao.getPerson(personId) == null) return -1L
        val linkedVisitId = resolveDoctorVisitId(personId, sourceAnalysisId)
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
                sourceAnalysisId = linkedVisitId,
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
        sourceAnalysisId: Long? = prescription.sourceAnalysisId,
        archived: Boolean = prescription.archived
    ) {
        val normalized = HealthRules.normalizeTitle(title) ?: return
        val linkedVisitId = resolveDoctorVisitId(prescription.personId, sourceAnalysisId)
        dao.updatePrescription(
            prescription.copy(
                type = type,
                title = normalized,
                dosage = if (type == HealthPrescriptionType.MEDICATION) dosage.trim() else "",
                comment = comment.trim(),
                startDateEpochDay = startDateEpochDay,
                endDateEpochDay = endDateEpochDay,
                visitDateEpochDay = if (type == HealthPrescriptionType.VISIT) visitDateEpochDay else null,
                sourceAnalysisId = linkedVisitId,
                archived = archived,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun resolveDoctorVisitId(personId: Long, sourceAnalysisId: Long?): Long? {
        if (sourceAnalysisId == null) return null
        val analysis = dao.getAnalysis(sourceAnalysisId) ?: return null
        if (analysis.personId != personId) return null
        if (analysis.kind != HealthAnalysisKind.DOCTOR) return null
        return analysis.id
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

    /**
     * Creates (or updates) a TASK for a lab analysis / doctor visit and stores [HealthAnalysisEntity.linkedTaskId].
     */
    suspend fun createTaskFromAnalysis(
        analysis: HealthAnalysisEntity,
        personName: String,
        dueDateEpochDay: Long?,
        dueTimeMinutes: Int? = null,
        recurrenceType: RecurrenceType = RecurrenceType.NONE,
        recurrenceWeekdayMask: Int = 0,
        recurrenceEndEpochDay: Long? = null
    ): Long {
        val current = dao.getAnalysis(analysis.id) ?: return -1L
        val title = HealthRules.analysisTaskTitle(current.kind, current.title)
        val notes = HealthRules.taskNotesFor(personName)
        val existing = current.linkedTaskId?.let { taskRepository.getById(it) }
        if (existing != null) {
            taskRepository.update(
                existing.copy(
                    title = title,
                    notes = notes,
                    dueDateEpochDay = dueDateEpochDay,
                    dueTimeMinutes = dueTimeMinutes,
                    dueTimeEndMinutes = null,
                    recurrenceType = recurrenceType,
                    recurrenceWeekdayMask = recurrenceWeekdayMask,
                    recurrenceEndEpochDay = recurrenceEndEpochDay,
                    isDone = false,
                    kind = TaskKind.TASK
                )
            )
            return existing.id
        }
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
        dao.updateAnalysis(
            current.copy(
                linkedTaskId = taskId,
                plannedDateEpochDay = dueDateEpochDay ?: current.plannedDateEpochDay,
                plannedTimeMinutes = dueTimeMinutes ?: current.plannedTimeMinutes,
                updatedAt = System.currentTimeMillis()
            )
        )
        return taskId
    }
}
