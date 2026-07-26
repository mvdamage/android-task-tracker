package com.learning.tasktracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.learning.tasktracker.data.ShoppingItemEntity
import com.learning.tasktracker.data.ShoppingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShoppingUiState(
    val items: List<ShoppingItemEntity> = emptyList(),
    val activeCount: Int = 0,
    val checkedCount: Int = 0
)

class ShoppingViewModel(
    private val repository: ShoppingRepository
) : ViewModel() {
    val uiState: StateFlow<ShoppingUiState> = repository.observeItems()
        .map { items ->
            ShoppingUiState(
                items = items,
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

    fun addItem(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.add(title) }
    }

    fun toggleChecked(item: ShoppingItemEntity) {
        viewModelScope.launch { repository.toggleChecked(item) }
    }

    fun delete(item: ShoppingItemEntity) {
        viewModelScope.launch { repository.delete(item) }
    }

    fun clearChecked() {
        viewModelScope.launch { repository.clearChecked() }
    }

    class Factory(private val repository: ShoppingRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ShoppingViewModel(repository) as T
        }
    }
}
