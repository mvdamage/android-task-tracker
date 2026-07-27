package com.learning.tasktracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.learning.tasktracker.data.ShoppingCategoryEntity
import com.learning.tasktracker.data.ShoppingItemEntity
import com.learning.tasktracker.data.ShoppingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShoppingUiState(
    val items: List<ShoppingItemEntity> = emptyList(),
    val categories: List<ShoppingCategoryEntity> = emptyList(),
    val groups: List<ShoppingCategoryGroup> = emptyList(),
    val activeCount: Int = 0,
    val checkedCount: Int = 0
)

data class ShoppingCategoryGroup(
    val category: ShoppingCategoryEntity?,
    val items: List<ShoppingItemEntity>
)

class ShoppingViewModel(
    private val repository: ShoppingRepository
) : ViewModel() {
    val uiState: StateFlow<ShoppingUiState> = repository.observeShoppingData()
        .map { (items, categories) ->
            ShoppingUiState(
                items = items,
                categories = categories,
                groups = buildCategoryGroups(items, categories),
                activeCount = items.count { !it.isChecked },
                checkedCount = items.count { it.isChecked }
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ShoppingUiState()
        )

    val titleHistory: StateFlow<List<String>> = repository.observeItems()
        .map { items ->
            items.groupBy { it.title.trim() }
                .filterKeys { it.isNotEmpty() }
                .entries
                .sortedByDescending { (_, group) -> group.maxOf { it.updatedAt } }
                .map { it.key }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val categoryIdByTitleLower: StateFlow<Map<String, Long?>> = repository.observeItems()
        .map { items ->
            items.groupBy { it.title.trim().lowercase() }
                .mapValues { (_, group) ->
                    group.maxByOrNull { it.updatedAt }?.categoryId
                }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyMap()
        )

    private val _pendingDeleteCategory = MutableStateFlow<ShoppingCategoryEntity?>(null)
    val pendingDeleteCategory: StateFlow<ShoppingCategoryEntity?> = _pendingDeleteCategory

    fun addItem(title: String, categoryId: Long? = null) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.add(title, categoryId) }
    }

    fun addSuggestedItem(title: String) {
        if (title.isBlank()) return
        val categoryId = categoryIdByTitleLower.value[title.trim().lowercase()]
        addItem(title, categoryId)
    }

    fun toggleChecked(item: ShoppingItemEntity) {
        viewModelScope.launch { repository.toggleChecked(item) }
    }

    fun updateItem(item: ShoppingItemEntity, title: String, categoryId: Long?) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.update(item, title, categoryId) }
    }

    fun delete(item: ShoppingItemEntity) {
        viewModelScope.launch { repository.delete(item) }
    }

    fun clearChecked() {
        viewModelScope.launch { repository.clearChecked() }
    }

    fun addCategory(name: String, colorArgb: Long, iconKey: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.addCategory(name, colorArgb, iconKey) }
    }

    fun requestDeleteCategory(category: ShoppingCategoryEntity) {
        viewModelScope.launch {
            _pendingDeleteCategory.value = category
        }
    }

    fun confirmDeleteCategory() {
        val category = _pendingDeleteCategory.value ?: return
        viewModelScope.launch {
            repository.deleteCategory(category)
            _pendingDeleteCategory.value = null
        }
    }

    fun dismissDeleteCategory() {
        _pendingDeleteCategory.value = null
    }

    suspend fun itemsInCategoryCount(categoryId: Long): Int =
        repository.countItemsInCategory(categoryId)

    companion object {
        val defaultSuggestions = listOf(
            "Молоко", "Хлеб", "Яйца", "Сыр", "Овощи", "Фрукты"
        )

        fun shoppingInputSuggestions(
            history: List<String>,
            query: String,
            defaults: List<String> = defaultSuggestions,
            limit: Int = 6
        ): List<String> {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                val recent = history.take(limit)
                if (recent.size >= limit) return recent
                val seen = recent.map { it.lowercase() }.toMutableSet()
                val extras = defaults.filter { it.lowercase() !in seen }
                return (recent + extras).take(limit)
            }
            return TaskViewModel.filterTitleSuggestions(history, trimmed, limit)
        }
    }

    class Factory(private val repository: ShoppingRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ShoppingViewModel(repository) as T
        }
    }
}

private fun sortShoppingItems(items: List<ShoppingItemEntity>): List<ShoppingItemEntity> =
    items.sortedWith(
        compareBy<ShoppingItemEntity> { it.isChecked }
            .thenByDescending { it.updatedAt }
    )

internal fun buildCategoryGroups(
    items: List<ShoppingItemEntity>,
    categories: List<ShoppingCategoryEntity>
): List<ShoppingCategoryGroup> {
    if (items.isEmpty()) return emptyList()

    val knownCategoryIds = categories.map { it.id }.toSet()
    val byCategoryId = items.groupBy { item ->
        item.categoryId?.takeIf { it in knownCategoryIds }
    }
    val groups = mutableListOf<ShoppingCategoryGroup>()

    categories.forEach { category ->
        byCategoryId[category.id]?.let { categoryItems ->
            groups += ShoppingCategoryGroup(category, sortShoppingItems(categoryItems))
        }
    }
    byCategoryId[null]?.let { uncategorized ->
        groups += ShoppingCategoryGroup(null, sortShoppingItems(uncategorized))
    }
    return groups
}
