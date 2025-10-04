package com.st10321779.rootedwealth.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.st10321779.rootedwealth.data.local.AppDatabase
import com.st10321779.rootedwealth.data.local.entity.Category
import com.st10321779.rootedwealth.data.local.entity.Expense
import com.st10321779.rootedwealth.repository.ExpenseRepository
import kotlinx.coroutines.launch

class AddExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val expenseRepository: ExpenseRepository
    val allCategories: LiveData<List<Category>>

    init {
        val db = AppDatabase.getDatabase(application)
        expenseRepository = ExpenseRepository(db.expenseDao())
        allCategories = db.categoryDao().getActiveCategories()
    }

    fun addExpense(expense: Expense) = viewModelScope.launch {
        expenseRepository.addExpense(expense)
    }
}