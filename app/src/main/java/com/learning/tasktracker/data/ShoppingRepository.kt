package com.learning.tasktracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ShoppingRepository(private val dao: ShoppingDao) {
    fun observeItems(): Flow<List<ShoppingItemEntity>> = dao.observeAll()

    fun observeCategories(): Flow<List<ShoppingCategoryEntity>> = dao.observeCategories()

    fun observeShoppingData(): Flow<Pair<List<ShoppingItemEntity>, List<ShoppingCategoryEntity>>> =
        combine(observeItems(), observeCategories()) { items, categories ->
            items to categories
        }

    suspend fun add(title: String, categoryId: Long? = null) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        dao.insert(ShoppingItemEntity(title = trimmed, categoryId = categoryId))
    }

    suspend fun toggleChecked(item: ShoppingItemEntity) {
        dao.update(
            item.copy(
                isChecked = !item.isChecked,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun update(item: ShoppingItemEntity, title: String, categoryId: Long?) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        dao.update(
            item.copy(
                title = trimmed,
                categoryId = categoryId,
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

    suspend fun addCategory(name: String, colorArgb: Long, iconKey: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        dao.insertCategory(
            ShoppingCategoryEntity(
                name = trimmed,
                colorArgb = colorArgb,
                iconKey = iconKey,
                sortOrder = dao.categoryCount()
            )
        )
    }

    suspend fun deleteCategory(category: ShoppingCategoryEntity) {
        dao.clearCategoryFromItems(category.id)
        dao.deleteCategory(category)
    }

    suspend fun countItemsInCategory(categoryId: Long): Int =
        dao.countItemsInCategory(categoryId)
}
