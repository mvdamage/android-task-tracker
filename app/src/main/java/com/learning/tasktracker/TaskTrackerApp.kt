package com.learning.tasktracker

import android.app.Application
import com.learning.tasktracker.data.AppDatabase
import com.learning.tasktracker.data.DayRolloverStore
import com.learning.tasktracker.data.TaskRepository

class TaskTrackerApp : Application() {
    lateinit var repository: TaskRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = TaskRepository(
            dao = AppDatabase.get(this).taskDao(),
            dayRolloverStore = DayRolloverStore(this)
        )
    }
}
