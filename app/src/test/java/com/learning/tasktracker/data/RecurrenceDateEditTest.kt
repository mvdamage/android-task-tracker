package com.learning.tasktracker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecurrenceDateEditTest {

    private lateinit var taskRepo: TaskRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("day_rollover", Context.MODE_PRIVATE).edit().clear().commit()
        val db = AppDatabase.createInMemory(context)
        taskRepo = TaskRepository(db.taskDao(), db.subtaskDao(), DayRolloverStore(context))
    }

    @Test
    fun seriesScope_shiftsAnchorKeepsRecurrence() = runTest {
        val today = DateUtils.todayEpochDay()
        val id = taskRepo.add(
            title = "Серия",
            notes = "",
            priority = Priority.MEDIUM,
            dueDateEpochDay = today,
            dueTimeMinutes = 9 * 60,
            recurrenceType = RecurrenceType.DAILY
        )
        val original = taskRepo.getById(id)!!
        taskRepo.updateRecurringDate(
            original = original,
            updated = original.copy(
                dueDateEpochDay = today + 3,
                dueTimeMinutes = 11 * 60
            ),
            scope = RecurrenceDateEditScope.SERIES
        )
        val tasks = taskRepo.observeTasks().first().filter { it.title == "Серия" }
        assertEquals(1, tasks.size)
        val updated = tasks.first()
        assertEquals(today + 3, updated.dueDateEpochDay)
        assertEquals(11 * 60, updated.dueTimeMinutes)
        assertEquals(RecurrenceType.DAILY, updated.recurrenceType)
        assertFalse(updated.isDone)
    }

    @Test
    fun thisOccurrence_detachesAndSpawnsContinuation() = runTest {
        val today = DateUtils.todayEpochDay()
        val id = taskRepo.add(
            title = "Серия",
            notes = "",
            priority = Priority.MEDIUM,
            dueDateEpochDay = today,
            dueTimeMinutes = 9 * 60,
            recurrenceType = RecurrenceType.DAILY
        )
        val original = taskRepo.getById(id)!!
        taskRepo.updateRecurringDate(
            original = original,
            updated = original.copy(
                dueDateEpochDay = today + 5,
                dueTimeMinutes = 18 * 60
            ),
            scope = RecurrenceDateEditScope.THIS_OCCURRENCE
        )
        val tasks = taskRepo.observeTasks().first().filter { it.title == "Серия" }
        assertEquals(2, tasks.size)
        val oneOff = tasks.first { it.id == id }
        assertEquals(today + 5, oneOff.dueDateEpochDay)
        assertEquals(18 * 60, oneOff.dueTimeMinutes)
        assertEquals(RecurrenceType.NONE, oneOff.recurrenceType)
        val continuation = tasks.first { it.id != id }
        assertEquals(today + 1, continuation.dueDateEpochDay)
        assertEquals(9 * 60, continuation.dueTimeMinutes)
        assertEquals(RecurrenceType.DAILY, continuation.recurrenceType)
        assertTrue(!continuation.isDone)
    }

    @Test
    fun thisOccurrence_onCompleted_doesNotDuplicateSeries() = runTest {
        val today = DateUtils.todayEpochDay()
        val id = taskRepo.add(
            title = "Серия",
            notes = "",
            priority = Priority.MEDIUM,
            dueDateEpochDay = today,
            recurrenceType = RecurrenceType.DAILY
        )
        val original = taskRepo.getById(id)!!
        taskRepo.toggleDone(original)
        val afterComplete = taskRepo.observeTasks().first().filter { it.title == "Серия" }
        assertEquals(2, afterComplete.size) // done + next
        val done = afterComplete.first { it.id == id }
        assertTrue(done.isDone)
        assertFalse(TaskRepository.supportsOccurrenceSplit(done))

        taskRepo.updateRecurringDate(
            original = done,
            updated = done.copy(dueDateEpochDay = today + 7),
            scope = RecurrenceDateEditScope.THIS_OCCURRENCE
        )
        val afterDetach = taskRepo.observeTasks().first().filter { it.title == "Серия" }
        assertEquals(2, afterDetach.size) // no third row
        val detached = afterDetach.first { it.id == id }
        assertEquals(today + 7, detached.dueDateEpochDay)
        assertEquals(RecurrenceType.NONE, detached.recurrenceType)
        assertTrue(afterDetach.any { it.id != id && it.recurrenceType == RecurrenceType.DAILY })
    }

    @Test
    fun moveToDay_thisOccurrence_detaches() = runTest {
        val today = DateUtils.todayEpochDay()
        val id = taskRepo.add(
            title = "DnD",
            notes = "",
            priority = Priority.MEDIUM,
            dueDateEpochDay = today,
            recurrenceType = RecurrenceType.WEEKLY
        )
        val original = taskRepo.getById(id)!!
        taskRepo.moveToDay(original, today + 2, RecurrenceDateEditScope.THIS_OCCURRENCE)
        val tasks = taskRepo.observeTasks().first().filter { it.title == "DnD" }
        assertEquals(2, tasks.size)
        assertEquals(RecurrenceType.NONE, tasks.first { it.id == id }.recurrenceType)
        assertEquals(today + 2, tasks.first { it.id == id }.dueDateEpochDay)
        assertTrue(tasks.any { it.id != id && it.recurrenceType == RecurrenceType.WEEKLY })
    }
}
