package com.learning.tasktracker.data

import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    val subtasksEnabled: Flow<Boolean> = callbackFlow {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_SUBTASKS_ENABLED) {
                trySend(prefs.getBoolean(KEY_SUBTASKS_ENABLED, false))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getBoolean(KEY_SUBTASKS_ENABLED, false))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun isSubtasksEnabled(): Boolean = prefs.getBoolean(KEY_SUBTASKS_ENABLED, false)

    fun setSubtasksEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SUBTASKS_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_SUBTASKS_ENABLED = "subtasks_enabled"
    }
}
