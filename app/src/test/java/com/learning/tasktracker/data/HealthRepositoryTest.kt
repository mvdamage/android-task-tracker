package com.learning.tasktracker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HealthRepositoryTest {

    private lateinit var healthRepo: HealthRepository
    private lateinit var taskRepo: TaskRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("day_rollover", Context.MODE_PRIVATE).edit().clear().commit()
        val db = AppDatabase.createInMemory(context)
        taskRepo = TaskRepository(db.taskDao(), db.subtaskDao(), DayRolloverStore(context))
        healthRepo = HealthRepository(db.healthDao(), taskRepo)
    }

    @Test
    fun personLimit_rejectsFourth() = runTest {
        assertTrue(healthRepo.addPerson("A") > 0)
        assertTrue(healthRepo.addPerson("B") > 0)
        assertTrue(healthRepo.addPerson("C") > 0)
        assertEquals(-1L, healthRepo.addPerson("D"))
        assertEquals(3, healthRepo.observeSnapshot().first().persons.size)
    }

    @Test
    fun createTaskFromPrescription_appearsInTaskList() = runTest {
        val personId = healthRepo.addPerson("Марина")
        val prescriptionId = healthRepo.addPrescription(
            personId = personId,
            type = HealthPrescriptionType.MEDICATION,
            title = "Амоксициллин",
            dosage = "500 мг"
        )
        val prescription = healthRepo.observeSnapshot().first()
            .prescriptions.first { it.id == prescriptionId }
        val due = HealthRules.dueDateFromDaysAhead(3)!!
        val taskId = healthRepo.createTaskFromPrescription(
            prescription = prescription,
            personName = "Марина",
            dueDateEpochDay = due,
            dueTimeMinutes = 9 * 60
        )
        assertTrue(taskId > 0)
        val tasks = taskRepo.observeTasks().first()
        val task = tasks.first { it.id == taskId }
        assertEquals("Приём: Амоксициллин", task.title)
        assertEquals(due, task.dueDateEpochDay)
        assertEquals(TaskKind.TASK, task.kind)
        assertEquals("Здоровье: Марина", task.notes)
    }

    @Test
    fun plannedDoctorVisit_createsVisibleTaskWithTime() = runTest {
        val personId = healthRepo.addPerson("Марина")
        val due = DateUtils.todayEpochDay() + 2
        val time = 14 * 60 + 30
        val analysisId = healthRepo.addAnalysis(
            personId = personId,
            kind = HealthAnalysisKind.DOCTOR,
            title = "Терапевт",
            status = HealthAnalysisStatus.PLANNED,
            plannedDateEpochDay = due,
            plannedTimeMinutes = time
        )
        assertTrue(analysisId > 0)
        val analysis = healthRepo.observeSnapshot().first()
            .analyses.first { it.id == analysisId }
        assertTrue(analysis.linkedTaskId != null && analysis.linkedTaskId!! > 0)
        val task = taskRepo.getById(analysis.linkedTaskId!!)!!
        assertEquals("Приём: Терапевт", task.title)
        assertEquals(due, task.dueDateEpochDay)
        assertEquals(time, task.dueTimeMinutes)
        assertEquals(TaskKind.TASK, task.kind)
        assertEquals(false, task.isDone)
    }

    @Test
    fun deletePerson_keepsCreatedTasks() = runTest {
        val personId = healthRepo.addPerson("Иван")
        val person = healthRepo.observeSnapshot().first().persons.first { it.id == personId }
        val prescriptionId = healthRepo.addPrescription(
            personId = personId,
            type = HealthPrescriptionType.VISIT,
            title = "Терапевт"
        )
        val prescription = healthRepo.observeSnapshot().first()
            .prescriptions.first { it.id == prescriptionId }
        val taskId = healthRepo.createTaskFromPrescription(
            prescription = prescription,
            personName = "Иван",
            dueDateEpochDay = DateUtils.todayEpochDay() + 1
        )
        healthRepo.deletePerson(person)
        assertTrue(healthRepo.observeSnapshot().first().persons.isEmpty())
        assertTrue(taskRepo.observeTasks().first().any { it.id == taskId })
    }
}
