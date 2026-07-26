package com.learning.tasktracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_categories")
data class ShoppingCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Long,
    val iconKey: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
