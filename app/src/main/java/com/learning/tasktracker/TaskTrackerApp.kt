package com.learning.tasktracker

import android.app.Application
import com.learning.tasktracker.data.AppDatabase
import com.learning.tasktracker.data.DayRolloverStore
import com.learning.tasktracker.data.ShoppingRepository
import com.learning.tasktracker.data.TaskRepository

class TaskTrackerApp : Application() {
    lateinit var repository: TaskRepository
        private set

    lateinit var shoppingRepository: ShoppingRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        repository = TaskRepository(
            dao = db.taskDao(),
            dayRolloverStore = DayRolloverStore(this)
        )
        shoppingRepository = ShoppingRepository(dao = db.shoppingDao())
    }
}
