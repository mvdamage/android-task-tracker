package com.learning.tasktracker.usability

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.learning.tasktracker.data.AppDatabase
import com.learning.tasktracker.data.DateUtils
import com.learning.tasktracker.data.DayRolloverStore
import com.learning.tasktracker.data.Priority
import com.learning.tasktracker.data.RecurrenceType
import com.learning.tasktracker.data.SettingsStore
import com.learning.tasktracker.data.ShoppingRepository
import com.learning.tasktracker.data.TaskFilter
import com.learning.tasktracker.data.TaskRepository
import com.learning.tasktracker.ui.ShoppingViewModel
import com.learning.tasktracker.ui.TaskViewModel
import com.learning.tasktracker.voice.ShoppingVoiceParser
import com.learning.tasktracker.voice.TaskVoiceParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Automates **success criteria** of usability cases UTC-01..UTC-20 where possible.
 * Manual-only: UTC-01 think-aloud, UTC-06 gesture discoverability, UTC-19 free explore, UTC-20 SUS.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UsabilityFlowsTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var taskRepo: TaskRepository
    private lateinit var shoppingRepo: ShoppingRepository
    private lateinit var settings: SettingsStore
    private lateinit var taskVm: TaskViewModel
    private lateinit var shoppingVm: ShoppingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("day_rollover", Context.MODE_PRIVATE).edit().clear().commit()
        db = AppDatabase.createInMemory(context)
        settings = SettingsStore(context)
        taskRepo = TaskRepository(db.taskDao(), db.subtaskDao(), DayRolloverStore(context))
        shoppingRepo = ShoppingRepository(db.shoppingDao())
        taskVm = TaskViewModel(taskRepo, settings)
        shoppingVm = ShoppingViewModel(shoppingRepo)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    /** UTC-02: named task appears; blank rejected. */
    @Test
    fun utc02_quickTaskCapture() = runTest(dispatcher) {
        val started = System.nanoTime()
        taskRepo.add(
            title = "Оплатить интернет",
            notes = "",
            priority = Priority.MEDIUM,
            dueDateEpochDay = DateUtils.todayEpochDay()
        )
        val state = taskVm.uiState.first { it.activeCount == 1 }
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        assertTrue(state.groups.any { g -> g.tasks.any { it.title == "Оплатить интернет" } })
        assertTrue("Capture path budget ≤20s; took ${elapsedMs}ms", elapsedMs < 20_000)

        taskRepo.add(title = "   ", notes = "", priority = Priority.MEDIUM)
        assertEquals(1, taskVm.uiState.first().activeCount)
    }

    /** UTC-03: due date + time window. */
    @Test
    fun utc03_dueDateAndTimePeriod() = runTest(dispatcher) {
        val tomorrow = DateUtils.todayEpochDay() + 1
        taskRepo.add(
            title = "Интервал",
            notes = "",
            priority = Priority.HIGH,
            dueDateEpochDay = tomorrow,
            dueTimeMinutes = 18 * 60,
            dueTimeEndMinutes = 19 * 60
        )
        val task = taskVm.uiState.first { it.activeCount == 1 }.groups.flatMap { it.tasks }.first()
        assertEquals(tomorrow, task.dueDateEpochDay)
        assertEquals(18 * 60, task.dueTimeMinutes)
        assertEquals(19 * 60, task.dueTimeEndMinutes)
    }

    /** UTC-04: completing daily task spawns next occurrence. */
    @Test
    fun utc04_dailyRecurrence_spawnsNextOnComplete() = runTest(dispatcher) {
        val today = DateUtils.todayEpochDay()
        taskRepo.add(
            title = "Принять таблетки",
            notes = "",
            priority = Priority.MEDIUM,
            dueDateEpochDay = today,
            recurrenceType = RecurrenceType.DAILY
        )
        val original = taskVm.uiState.first { it.activeCount == 1 }
            .groups.flatMap { it.tasks }.first { it.title == "Принять таблетки" && !it.isDone }
        taskRepo.toggleDone(original)
        val state = taskVm.uiState.first {
            it.groups.flatMap { g -> g.tasks }.count { t -> t.title == "Принять таблетки" } >= 2
        }
        val pills = state.groups.flatMap { it.tasks }.filter { it.title == "Принять таблетки" }
        assertTrue(pills.any { it.isDone && it.dueDateEpochDay == today })
        assertTrue(pills.any { !it.isDone && it.dueDateEpochDay == today + 1 })
    }

    @Test
    fun utc04_recurrenceEnd_doesNotSpawn() = runTest(dispatcher) {
        val today = DateUtils.todayEpochDay()
        taskRepo.add(
            title = "Last",
            notes = "",
            priority = Priority.MEDIUM,
            dueDateEpochDay = today,
            recurrenceType = RecurrenceType.DAILY,
            recurrenceEndEpochDay = today
        )
        val original = taskVm.uiState.first { it.activeCount == 1 }.groups.flatMap { it.tasks }.first()
        taskRepo.toggleDone(original)
        val tasks = taskVm.uiState.first { it.doneCount == 1 }
            .groups.flatMap { it.tasks }.filter { it.title == "Last" }
        assertEquals(1, tasks.size)
        assertTrue(tasks.first().isDone)
    }

    /** UTC-05: filters. */
    @Test
    fun utc05_filters() = runTest(dispatcher) {
        taskRepo.add("A", "", Priority.LOW, DateUtils.todayEpochDay())
        taskRepo.add("B", "", Priority.LOW, null)
        val a = taskVm.uiState.first { it.activeCount == 2 }
            .groups.flatMap { it.tasks }.first { it.title == "A" }
        taskRepo.toggleDone(a)
        taskVm.uiState.first { it.doneCount == 1 }

        taskVm.setFilter(TaskFilter.ACTIVE)
        var state = taskVm.uiState.first { it.filter == TaskFilter.ACTIVE }
        assertTrue(state.groups.flatMap { it.tasks }.none { it.isDone })
        assertTrue(state.groups.flatMap { it.tasks }.any { it.title == "B" })

        taskVm.setFilter(TaskFilter.DONE)
        state = taskVm.uiState.first { it.filter == TaskFilter.DONE }
        assertTrue(state.groups.flatMap { it.tasks }.all { it.isDone })

        taskVm.setFilter(TaskFilter.ALL)
        state = taskVm.uiState.first { it.filter == TaskFilter.ALL }
        assertEquals(2, state.groups.flatMap { it.tasks }.size)
    }

    /** UTC-06: move outcome (gesture discoverability stays manual). */
    @Test
    fun utc06_moveTaskToAnotherDay() = runTest(dispatcher) {
        val today = DateUtils.todayEpochDay()
        taskRepo.add("DnD", "", Priority.MEDIUM, today)
        val task = taskVm.uiState.first { it.activeCount == 1 }.groups.flatMap { it.tasks }.first()
        taskRepo.moveToDay(task, today + 2)
        val moved = taskVm.uiState.first {
            it.groups.flatMap { g -> g.tasks }.any { t -> t.title == "DnD" && t.dueDateEpochDay == today + 2 }
        }.groups.flatMap { it.tasks }.first { it.title == "DnD" }
        assertEquals(today + 2, moved.dueDateEpochDay)
    }

    @Test
    fun utc06_moveToNoDate_clearsSchedule() = runTest(dispatcher) {
        val today = DateUtils.todayEpochDay()
        taskRepo.add(
            title = "NoDate",
            notes = "",
            priority = Priority.MEDIUM,
            dueDateEpochDay = today,
            recurrenceType = RecurrenceType.DAILY,
            dueTimeMinutes = 10 * 60
        )
        val task = taskVm.uiState.first { it.activeCount == 1 }.groups.flatMap { it.tasks }.first()
        taskRepo.moveToDay(task, null)
        val moved = taskVm.uiState.first {
            it.groups.flatMap { g -> g.tasks }.any { t -> t.title == "NoDate" && t.dueDateEpochDay == null }
        }.groups.flatMap { it.tasks }.first { it.title == "NoDate" }
        assertNull(moved.dueDateEpochDay)
        assertNull(moved.dueTimeMinutes)
        assertEquals(RecurrenceType.NONE, moved.recurrenceType)
    }

    /** UTC-07: subtasks behind settings. */
    @Test
    fun utc07_subtasksRequireSettingsToggle() = runTest(dispatcher) {
        assertFalse(taskVm.uiState.value.subtasksEnabled)
        taskVm.setSubtasksEnabled(true)
        assertTrue(taskVm.uiState.first { it.subtasksEnabled }.subtasksEnabled)

        taskRepo.add("Собрать документы", "", Priority.MEDIUM, DateUtils.todayEpochDay())
        val parent = taskVm.uiState.first { it.activeCount == 1 }.groups.flatMap { it.tasks }.first()
        taskRepo.addSubtask(parent.id, "Паспорт")
        taskRepo.addSubtask(parent.id, "Справка")
        taskRepo.addSubtask(parent.id, "Фото")
        val subs = taskVm.uiState.first { (it.subtasksByParentId[parent.id]?.size ?: 0) == 3 }
            .subtasksByParentId[parent.id].orEmpty()
        assertEquals(3, subs.size)
    }

    /** UTC-08: clear completed. */
    @Test
    fun utc08_clearCompletedRemovesDoneOnly() = runTest(dispatcher) {
        taskRepo.add("Done1", "", Priority.LOW, DateUtils.todayEpochDay())
        taskRepo.add("Keep", "", Priority.LOW, DateUtils.todayEpochDay())
        val done = taskVm.uiState.first { it.activeCount == 2 }
            .groups.flatMap { it.tasks }.first { it.title == "Done1" }
        taskRepo.toggleDone(done)
        taskVm.uiState.first { it.doneCount == 1 }
        taskRepo.clearCompleted()
        val state = taskVm.uiState.first { it.doneCount == 0 && it.activeCount == 1 }
        assertEquals(listOf("Keep"), state.groups.flatMap { it.tasks }.map { it.title })
    }

    /** UTC-09 / UTC-10: shopping + checkout. */
    @Test
    fun utc09_utc10_shoppingListAndCheckout() = runTest(dispatcher) {
        shoppingRepo.addCategory("Молочка", 0xFF4CAF50, "milk")
        val categoryId = shoppingVm.uiState.first { it.categories.isNotEmpty() }.categories.first().id

        listOf("Молоко", "Хлеб", "Яйца", "Сыр").forEach { title ->
            shoppingRepo.add(title, if (title == "Молоко" || title == "Сыр") categoryId else null)
        }
        var state = shoppingVm.uiState.first { it.activeCount == 4 }
        assertTrue(state.groups.any { it.category?.name == "Молочка" })

        state.items.filter { !it.isChecked }.take(2).forEach { shoppingRepo.toggleChecked(it) }
        state = shoppingVm.uiState.first { it.checkedCount == 2 }
        assertEquals(2, state.activeCount)

        shoppingRepo.clearChecked()
        state = shoppingVm.uiState.first { it.checkedCount == 0 && it.items.size == 2 }
        assertEquals(2, state.activeCount)
    }

    /** UTC-12: voice task parse + tokens stripped from title. */
    @Test
    fun utc12_voiceTaskParse_doctorTomorrow11() {
        val now = LocalDateTime.of(2026, 7, 27, 10, 0)
        val result = TaskVoiceParser.parseTaskVoice(
            "Встреча с врачом завтра в 11",
            now,
            ZoneId.of("Asia/Omsk")
        )
        assertEquals("Встреча с врачом", result.title)
        assertEquals(now.toLocalDate().plusDays(1).toEpochDay(), result.dueDateEpochDay)
        assertEquals(11 * 60, result.dueTimeMinutes)
        assertFalse(result.title.contains("завтра", ignoreCase = true))
    }

    /** UTC-13: multi shopping lines. */
    @Test
    fun utc13_voiceShopping_multiItems() {
        val lines = ShoppingVoiceParser.parseShoppingVoice(
            raw = "молоко, хлеб и яблоки",
            categories = emptyList(),
            categoryIdByTitleLower = emptyMap()
        )
        assertEquals(listOf("молоко", "хлеб", "яблоки"), lines.map { it.title.lowercase() })
        val withoutBread = lines.filterNot { it.title.equals("хлеб", ignoreCase = true) }
        assertEquals(2, withoutBread.size)
    }

    /** UTC-14: empty voice shopping → no lines. */
    @Test
    fun utc14_emptyVoiceShopping_noLines() {
        val lines = ShoppingVoiceParser.parseShoppingVoice(
            raw = "   ",
            categories = emptyList(),
            categoryIdByTitleLower = emptyMap()
        )
        assertTrue(lines.isEmpty())
    }

    /** UTC-15: tasks + shopping together. */
    @Test
    fun utc15_homeOrganizer_tasksAndShoppingTogether() = runTest(dispatcher) {
        taskRepo.add(
            title = "Приготовить ужин",
            notes = "",
            priority = Priority.MEDIUM,
            dueDateEpochDay = DateUtils.todayEpochDay(),
            dueTimeMinutes = 19 * 60
        )
        shoppingRepo.add("фарш")
        shoppingRepo.add("лук")
        assertEquals(1, taskVm.uiState.first { it.activeCount == 1 }.activeCount)
        assertEquals(2, shoppingVm.uiState.first { it.activeCount == 2 }.activeCount)
    }

    /** UTC-16: overdue counter. */
    @Test
    fun utc16_overdueCount() = runTest(dispatcher) {
        val yesterday = DateUtils.todayEpochDay() - 1
        taskRepo.add(
            title = "Просрочка",
            notes = "",
            priority = Priority.HIGH,
            dueDateEpochDay = yesterday,
            dueTimeMinutes = 9 * 60
        )
        assertTrue(taskVm.uiState.first { it.overdueCount >= 1 }.overdueCount >= 1)
    }

    /** UTC-17: settings defaults. */
    @Test
    fun utc17_settingsDefaults() {
        assertFalse(settings.isSubtasksEnabled())
        assertFalse(settings.isVoiceDisclaimerAccepted())
    }

    /** UTC-18: data survives ViewModel recreation (same DB). */
    @Test
    fun utc18_dataSurvivesViewModelRecreation() = runTest(dispatcher) {
        taskRepo.add("Persist", "", Priority.MEDIUM, DateUtils.todayEpochDay())
        shoppingRepo.add("чай")
        taskVm.uiState.first { it.activeCount == 1 }
        shoppingVm.uiState.first { it.activeCount == 1 }

        val taskVm2 = TaskViewModel(taskRepo, settings)
        val shoppingVm2 = ShoppingViewModel(shoppingRepo)
        assertEquals(1, taskVm2.uiState.first { it.activeCount == 1 }.activeCount)
        assertEquals(1, shoppingVm2.uiState.first { it.activeCount == 1 }.activeCount)
    }
}
