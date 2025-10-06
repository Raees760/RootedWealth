package com.st10321779.rootedwealth.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.st10321779.rootedwealth.data.local.AppDatabase
import com.st10321779.rootedwealth.data.local.entity.Category
import kotlinx.coroutines.launch

class CategoryViewModel(application: Application) : AndroidViewModel(application) {

    private val categoryDao = AppDatabase.getDatabase(application).categoryDao()
    val allCategories: LiveData<List<Category>> = categoryDao.getAllCategories()

    fun addCategory(category: Category) = viewModelScope.launch {
        categoryDao.insert(category)
    }

    fun updateCategory(category: Category) = viewModelScope.launch {
        categoryDao.update(category)
    }

    fun deleteCategory(category: Category) = viewModelScope.launch {
        // check if the category is in use
        val expenseCount = categoryDao.getExpenseCountForCategory(category.id)
        if (expenseCount > 0) {
            // soft delete i.e. mark as inactive
            val updatedCategory = category.copy(isActive = false)
            categoryDao.update(updatedCategory)
        } else {
            // hard delete - Remove from database
            categoryDao.deleteById(category.id)
        }
    }
}