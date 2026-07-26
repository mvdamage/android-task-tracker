package com.learning.tasktracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {
    @Query(
        """
        SELECT * FROM shopping_items
        ORDER BY isChecked ASC, updatedAt DESC
        """
    )
    fun observeAll(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_categories ORDER BY sortOrder ASC, createdAt ASC")
    fun observeCategories(): Flow<List<ShoppingCategoryEntity>>

    @Query("SELECT COUNT(*) FROM shopping_items WHERE categoryId = :categoryId")
    suspend fun countItemsInCategory(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM shopping_categories")
    suspend fun categoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ShoppingItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: ShoppingCategoryEntity): Long

    @Update
    suspend fun update(item: ShoppingItemEntity)

    @Delete
    suspend fun delete(item: ShoppingItemEntity)

    @Delete
    suspend fun deleteCategory(category: ShoppingCategoryEntity)

    @Query("UPDATE shopping_items SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategoryFromItems(categoryId: Long)

    @Query("DELETE FROM shopping_items WHERE isChecked = 1")
    suspend fun deleteChecked()
}
