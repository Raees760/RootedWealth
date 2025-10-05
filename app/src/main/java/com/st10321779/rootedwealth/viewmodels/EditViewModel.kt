package com.st10321779.rootedwealth.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.st10321779.rootedwealth.data.local.AppDatabase
import com.st10321779.rootedwealth.data.local.entity.Category
import com.st10321779.rootedwealth.data.local.entity.Expense
import com.st10321779.rootedwealth.data.local.entity.Income
import kotlinx.coroutines.launch

class EditViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val expenseDao = db.expenseDao()
    private val incomeDao = db.incomeDao()

    val allCategories: LiveData<List<Category>> = db.categoryDao().getActiveCategories()

    private val _expenseToEdit = MutableLiveData<Expense?>()
    val expenseToEdit: LiveData<Expense?> = _expenseToEdit

    private val _incomeToEdit = MutableLiveData<Income?>()
    val incomeToEdit: LiveData<Income?> = _incomeToEdit

    fun loadItem(id: Long, isExpense: Boolean) = viewModelScope.launch {
        if (isExpense) {
            _incomeToEdit.postValue(null)
            _expenseToEdit.postValue(expenseDao.getExpenseById(id))
        } else {
            _expenseToEdit.postValue(null)
            _incomeToEdit.postValue(incomeDao.getIncomeById(id))
        }
    }

    fun updateExpense(expense: Expense) = viewModelScope.launch {
        expenseDao.update(expense)
    }

    fun updateIncome(income: Income) = viewModelScope.launch {
        incomeDao.update(income)
    }
}