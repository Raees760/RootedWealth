package com.st10321779.rootedwealth.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.st10321779.rootedwealth.data.local.AppDatabase
import com.st10321779.rootedwealth.data.local.entity.Category
import com.st10321779.rootedwealth.data.local.entity.Expense
import com.st10321779.rootedwealth.repository.FirebaseRepository
import kotlinx.coroutines.launch

class CategoryViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseRepository = FirebaseRepository()
    private val categoryDao = AppDatabase.getDatabase(application).categoryDao()
    val allCategories: LiveData<List<Category>> = firebaseRepository.getCategoriesLiveData()
    private val allExpenses: LiveData<List<Expense>> = firebaseRepository.getExpensesLiveData()


    fun addCategory(category: Category) = viewModelScope.launch {
        firebaseRepository.addCategory(category)
    }
    fun updateCategory(category: Category) = viewModelScope.launch {
        firebaseRepository.updateCategory(category)
    }
    fun deleteCategory(category: Category) = viewModelScope.launch {
        // check if the category is in
        val isCategoryInUse = allExpenses.value?.any { it.categoryId == category.id } ?: false

        if (isCategoryInUse) {
            // soft delete i.e. mark as inactive and update in Firebase
            val updatedCategory = category.copy(isActive = false)
            firebaseRepository.updateCategory(updatedCategory)
        } else {
            // Hard delete: Remove from Firebase
            firebaseRepository.deleteCategory(category.id)
        }
    }
}