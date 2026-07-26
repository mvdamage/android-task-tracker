package com.learning.tasktracker.data

import kotlinx.coroutines.flow.Flow

class ShoppingRepository(private val dao: ShoppingDao) {
    fun observeItems(): Flow<List<ShoppingItemEntity>> = dao.observeAll()

    suspend fun add(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        dao.insert(ShoppingItemEntity(title = trimmed))
    }

    suspend fun toggleChecked(item: ShoppingItemEntity) {
        dao.update(
            item.copy(
                isChecked = !item.isChecked,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun delete(item: ShoppingItemEntity) {
        dao.delete(item)
    }

    suspend fun clearChecked() {
        dao.deleteChecked()
    }
}
