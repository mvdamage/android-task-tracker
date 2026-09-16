package com.learning.tasktracker.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.learning.tasktracker.data.AppDatabase
import com.learning.tasktracker.data.DateUtils
import com.learning.tasktracker.data.DayRolloverStore
import com.learning.tasktracker.data.Priority
import com.learning.tasktracker.data.RecurrenceType
import com.learning.tasktracker.data.SettingsStore
import com.learning.tasktracker.data.TaskRepository
import com.learning.tasktracker.data.TaskViewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskCalendarViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var taskVm: TaskViewModel
    private lateinit var taskRepo: TaskRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("day_rollover", Context.MODE_PRIVATE).edit().clear().commit()
        val db = AppDatabase.createInMemory(context)
        taskRepo = TaskRepository(db.taskDao(), db.subtaskDao(), DayRolloverStore(context))
        taskVm = TaskViewModel(taskRepo, SettingsStore(context))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun calendarMode_showsTasksForSelectedDay() = runTest(dispatcher) {
        val day = LocalDate.of(2026, 7, 15).toEpochDay()
        taskRepo.add(
            title = "Buy milk",
            notes = "",
            priority = Priority.MEDIUM,
            dueDateEpochDay = day,
            recurrenceType = RecurrenceType.NONE,
            recurrenceWeekdayMask = 0,
            recurrenceEndEpochDay = null,
            dueTimeMinutes = null,
            dueTimeEndMinutes = null
        )
        taskRepo.add(
            title = "Other day",
            notes = "",
            priority = Priority.MEDIUM,
            dueDateEpochDay = day + 1,
            recurrenceType = RecurrenceType.NONE,
            recurrenceWeekdayMask = 0,
            recurrenceEndEpochDay = null,
            dueTimeMinutes = null,
            dueTimeEndMinutes = null
        )

        taskVm.setViewMode(TaskViewMode.CALENDAR)
        taskVm.selectCalendarDay(day)

        val state = taskVm.uiState.first { it.viewMode == TaskViewMode.CALENDAR }
        assertEquals(1, state.calendarDayTasks.size)
        assertEquals("Buy milk", state.calendarDayTasks.first().title)
        assertEquals(1, state.taskCountByDay[day])
    }
}
