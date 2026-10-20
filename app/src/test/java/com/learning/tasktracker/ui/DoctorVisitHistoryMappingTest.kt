package com.learning.tasktracker.ui

import com.learning.tasktracker.data.HealthAnalysisEntity
import com.learning.tasktracker.data.HealthAnalysisKind
import com.learning.tasktracker.data.HealthAnalysisStatus
import com.learning.tasktracker.data.HealthPrescriptionEntity
import com.learning.tasktracker.data.HealthPrescriptionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DoctorVisitHistoryMappingTest {

    @Test
    fun completedDoctorVisits_sortedNewestFirst_withLinkedMeds() {
        val visits = listOf(
            HealthAnalysisEntity(
                id = 1,
                personId = 1,
                kind = HealthAnalysisKind.DOCTOR,
                title = "Терапевт",
                status = HealthAnalysisStatus.DONE,
                plannedDateEpochDay = 100,
                doneDateEpochDay = 100
            ),
            HealthAnalysisEntity(
                id = 2,
                personId = 1,
                kind = HealthAnalysisKind.DOCTOR,
                title = "ЛОР",
                status = HealthAnalysisStatus.DONE,
                plannedDateEpochDay = 200,
                doneDateEpochDay = 200
            ),
            HealthAnalysisEntity(
                id = 3,
                personId = 1,
                kind = HealthAnalysisKind.DOCTOR,
                title = "Будущий",
                status = HealthAnalysisStatus.PLANNED,
                plannedDateEpochDay = 300
            ),
            HealthAnalysisEntity(
                id = 4,
                personId = 1,
                kind = HealthAnalysisKind.LAB,
                title = "ОАК",
                status = HealthAnalysisStatus.DONE,
                doneDateEpochDay = 250
            )
        )
        val meds = listOf(
            HealthPrescriptionEntity(
                id = 10,
                personId = 1,
                type = HealthPrescriptionType.MEDICATION,
                title = "Амоксициллин",
                dosage = "500",
                sourceAnalysisId = 2
            )
        )
        val history = visits
            .filter {
                it.kind == HealthAnalysisKind.DOCTOR && it.status == HealthAnalysisStatus.DONE
            }
            .sortedByDescending { it.doneDateEpochDay ?: it.plannedDateEpochDay ?: Long.MIN_VALUE }
            .map { visit ->
                DoctorVisitHistoryItem(
                    visit = visit,
                    prescriptions = meds.filter { it.sourceAnalysisId == visit.id }
                )
            }

        assertEquals(2, history.size)
        assertEquals("ЛОР", history[0].visit.title)
        assertEquals("Терапевт", history[1].visit.title)
        assertEquals(1, history[0].prescriptions.size)
        assertEquals("Амоксициллин", history[0].prescriptions.first().title)
        assertTrue(history[1].prescriptions.isEmpty())
    }
}
