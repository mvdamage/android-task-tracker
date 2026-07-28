package com.learning.tasktracker.usability

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.learning.tasktracker.MainActivity
import com.learning.tasktracker.R
import com.learning.tasktracker.TaskTrackerApp
import com.learning.tasktracker.data.AppDatabase
import com.learning.tasktracker.ui.TestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI journeys for usability cases that need the real screen graph.
 * Run: `./gradlew :app:connectedDebugAndroidTest`
 *
 * Manual-only: UTC-01 think-aloud, UTC-06 gesture discoverability,
 * UTC-19 free explore, UTC-20 subjective scores.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class UsabilityComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearData() {
        val app = ApplicationProvider.getApplicationContext<TaskTrackerApp>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AppDatabase.get(app).clearAllTables()
            app.settingsStore.setSubtasksEnabled(false)
            app.settingsStore.setVoiceDisclaimerAccepted(false)
        }
    }

    /** UTC-01 (partial): tabs and Russian labels are discoverable. */
    @Test
    fun utc01_orientation_tabsVisible() {
        composeRule.onNodeWithTag(TestTags.TAB_TASKS).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.TAB_SHOPPING).assertIsDisplayed()
        composeRule.onNodeWithText("Задачи").assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.TAB_SHOPPING).performClick()
        composeRule.onNodeWithText("Покупки").assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.QUICK_ADD_SHOPPING).assertIsDisplayed()
    }

    /** UTC-02: open editor via quick add and save task under 20s. */
    @Test
    fun utc02_quickAddTask_viaUi() {
        val started = System.nanoTime()
        composeRule.onNodeWithTag(TestTags.QUICK_ADD_TASK).performClick()
        composeRule.onNodeWithTag(TestTags.TASK_EDITOR_TITLE).performTextInput("Оплатить интернет")
        composeRule.onNodeWithTag(TestTags.TASK_EDITOR_DONE).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(TestTags.TASK_EDITOR_TITLE).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText("Оплатить интернет").assertIsDisplayed()
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        assert(elapsedMs < 20_000) { "UI capture took ${elapsedMs}ms (>20s usability budget)" }
    }

    /** UTC-05: filter chips reachable. */
    @Test
    fun utc05_filtersVisibleAndClickable() {
        composeRule.onNodeWithTag(TestTags.FILTER_ALL).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.FILTER_ACTIVE).performClick()
        composeRule.onNodeWithTag(TestTags.FILTER_DONE).performClick()
        composeRule.onNodeWithTag(TestTags.FILTER_ALL).performClick()
    }

    /** UTC-09: shopping quick add path. */
    @Test
    fun utc09_addShoppingItem_viaUi() {
        composeRule.onNodeWithTag(TestTags.TAB_SHOPPING).performClick()
        composeRule.onNodeWithTag(TestTags.QUICK_ADD_SHOPPING).performClick()
        composeRule.onNodeWithText("Что купить?").performTextInput("Молоко")
        composeRule.onNode(hasContentDescription("Добавить"), useUnmergedTree = true).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Молоко")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Молоко").assertIsDisplayed()
    }

    /** UTC-11: first mic tap shows voice disclaimer. */
    @Test
    fun utc11_voiceDisclaimerOnFirstMic() {
        val title = composeRule.activity.getString(R.string.voice_disclaimer_title)
        composeRule.onNodeWithTag(TestTags.VOICE_MIC).performClick()
        composeRule.onNodeWithText(title).assertIsDisplayed()
    }

    /** UTC-14 (partial): after cancelling voice, manual add still works. */
    @Test
    fun utc14_manualAddAfterVoiceCancel() {
        val cancel = composeRule.activity.getString(R.string.voice_disclaimer_cancel)
        composeRule.onNodeWithTag(TestTags.VOICE_MIC).performClick()
        composeRule.onNodeWithText(cancel).performClick()
        composeRule.onNodeWithTag(TestTags.QUICK_ADD_TASK).performClick()
        composeRule.onNodeWithTag(TestTags.TASK_EDITOR_TITLE).performTextInput("чай")
        composeRule.onNodeWithTag(TestTags.TASK_EDITOR_DONE).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("чай")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("чай").assertIsDisplayed()
    }

    /** UTC-17: settings + version. */
    @Test
    fun utc17_settingsAndVersion() {
        composeRule.onNodeWithTag(TestTags.SETTINGS_BUTTON).performClick()
        composeRule.onNodeWithTag(TestTags.SETTINGS_SHEET).assertIsDisplayed()
        composeRule.onNodeWithText("Подзадачи").assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.APP_VERSION).assertIsDisplayed()
    }

    /** UTC-15: switch between tasks and shopping. */
    @Test
    fun utc15_switchTabs_homeOrganizer() {
        composeRule.onNodeWithTag(TestTags.QUICK_ADD_TASK).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.TAB_SHOPPING).performClick()
        composeRule.onNodeWithTag(TestTags.QUICK_ADD_SHOPPING).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.TAB_TASKS).performClick()
        composeRule.onNodeWithTag(TestTags.QUICK_ADD_TASK).assertIsDisplayed()
    }
}
