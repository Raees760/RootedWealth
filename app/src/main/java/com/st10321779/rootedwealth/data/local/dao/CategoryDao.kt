package com.st10321779.rootedwealth.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.st10321779.rootedwealth.data.local.entity.Category

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: Category)

    @Update
    suspend fun update(category: Category)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteById(categoryId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<Category>)

    // Get ALL categories for the management screen
    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    fun getAllCategories(): LiveData<List<Category>>

    // Get only active categories for the "Add Expense" spinner
    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveCategories(): LiveData<List<Category>>

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?

    // New function to check if a category has any expenses
    @Query("SELECT COUNT(id) FROM expenses WHERE categoryId = :categoryId")
    suspend fun getExpenseCountForCategory(categoryId: Long): Int
}