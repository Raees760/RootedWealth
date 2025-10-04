package com.st10321779.rootedwealth.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.st10321779.rootedwealth.data.local.AppDatabase
import com.st10321779.rootedwealth.data.local.dao.CategorySpending
import com.st10321779.rootedwealth.data.local.entity.Expense
import com.st10321779.rootedwealth.data.local.entity.Income
import com.st10321779.rootedwealth.repository.ExpenseRepository
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val expenseRepository: ExpenseRepository
    private val _currentMonthExpenses = MutableLiveData<List<Expense>>()
    val currentMonthExpenses: LiveData<List<Expense>> = _currentMonthExpenses

    var totalSpentThisMonth: LiveData<Double>
    var totalIncomeThisMonth: LiveData<Double>

    // Mock budget for now
    val monthlyBudget = MutableLiveData<Double>(20000.0)
    val spendingByCategory: LiveData<List<CategorySpending>>

    init {
        val db = AppDatabase.getDatabase(application)
        expenseRepository = ExpenseRepository(db.expenseDao())

        val (start, end) = getCurrentMonthDateRange()
        totalSpentThisMonth = expenseRepository.getTotalSpentInPeriod(start, end)
        totalIncomeThisMonth = db.incomeDao().getTotalIncomeInPeriod(start, end) // Get total income
        spendingByCategory = db.expenseDao().getSpendingByCategory(start, end) // Get category spending
    }
    fun addIncome(income: Income) = viewModelScope.launch {
        // You'll need an IncomeRepository, but for now we can access the DAO directly
        AppDatabase.getDatabase(getApplication()).incomeDao().insert(income)
    }

    private fun getCurrentMonthDateRange(): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startDate = calendar.time

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val endDate = calendar.time

        return Pair(startDate, endDate)
    }
}