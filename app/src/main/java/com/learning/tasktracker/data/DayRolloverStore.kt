package com.learning.tasktracker.data

import android.content.Context

class DayRolloverStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "day_rollover",
        Context.MODE_PRIVATE
    )

    fun lastCleanupEpochDay(): Long =
        prefs.getLong(KEY_LAST_CLEANUP, -1L)

    fun setLastCleanupEpochDay(epochDay: Long) {
        prefs.edit().putLong(KEY_LAST_CLEANUP, epochDay).apply()
    }

    companion object {
        private const val KEY_LAST_CLEANUP = "last_cleanup_epoch_day"
    }
}
