package com.learning.tasktracker.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.learning.tasktracker.data.AppDatabase
import com.learning.tasktracker.data.DayRolloverStore
import com.learning.tasktracker.data.HealthAnalysisKind
import com.learning.tasktracker.data.HealthAnalysisStatus
import com.learning.tasktracker.data.HealthRepository
import com.learning.tasktracker.data.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HealthSelectedPersonWriteTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var db: AppDatabase
    private lateinit var healthRepo: HealthRepository
    private lateinit var vm: HealthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("day_rollover", Context.MODE_PRIVATE).edit().clear().commit()
        db = AppDatabase.createInMemory(context)
        val taskRepo = TaskRepository(db.taskDao(), db.subtaskDao(), DayRolloverStore(context))
        healthRepo = HealthRepository(db.healthDao(), taskRepo)
        vm = HealthViewModel(healthRepo)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun singlePerson_addAnalysis_withoutExplicitSelect_persists() = runTest(dispatcher) {
        val personId = healthRepo.addPerson("Марина")
        assertTrue(personId > 0)
        // Collect so init sync + stateIn run; do not call selectPerson.
        vm.uiState.first { it.selectedPersonId == personId && it.persons.size == 1 }

        vm.addAnalysis(
            kind = HealthAnalysisKind.DOCTOR,
            title = "Терапевт",
            status = HealthAnalysisStatus.PLANNED,
            plannedDateEpochDay = null,
            plannedTimeMinutes = null,
            doneDateEpochDay = null,
            comment = ""
        )

        val analyses = healthRepo.observeSnapshot().first { it.analyses.isNotEmpty() }.analyses
        assertEquals(1, analyses.size)
        assertEquals("Терапевт", analyses.first().title)
        assertEquals(personId, analyses.first().personId)
    }
}
