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
import com.st10321779.rootedwealth.gamification.BankLinkSimulator
import com.st10321779.rootedwealth.repository.ExpenseRepository
import com.st10321779.rootedwealth.util.PrefsManager
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val expenseRepository: ExpenseRepository
    private val _currentMonthExpenses = MutableLiveData<List<Expense>>()
    val currentMonthExpenses: LiveData<List<Expense>> = _currentMonthExpenses
    val monthlyBudgetSetting: LiveData<Float> = PrefsManager.getMonthlyBudgetLiveData(application)

    var totalSpentThisMonth: LiveData<Double>
    var totalIncomeThisMonth: LiveData<Double>

    //val monthlyBudget = MutableLiveData<Double>(20000.0)
    // This LiveData will automatically update whenever the budget is changed in Settings.
    val monthlyBudget: LiveData<Float> = PrefsManager.getMonthlyBudgetLiveData(application)
    val spendingByCategory: LiveData<List<CategorySpending>>

    private val _uiState = MutableLiveData<HomeUiState>()
    val uiState: LiveData<HomeUiState> = _uiState

    init {
        val db = AppDatabase.getDatabase(application)
        expenseRepository = ExpenseRepository(db.expenseDao())

        val (start, end) = getCurrentMonthDateRange()
        totalSpentThisMonth = expenseRepository.getTotalSpentInPeriod(start, end)
        totalIncomeThisMonth = db.incomeDao().getTotalIncomeInPeriod(start, end) // Get total income
        spendingByCategory = db.expenseDao().getSpendingByCategory(start, end) // Get category spending
        refreshUiState()
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

    fun onBankLinkStatusChanged(isLinked: Boolean) {
        if (isLinked) {
            viewModelScope.launch {
                // Change the check to use the new DAO function
                val linkedEntryCount = db.incomeDao().getLinkedEntryCount()
                if (linkedEntryCount == 0) {
                    BankLinkSimulator.seedLinkedData(db.categoryDao(), db.expenseDao(), db.incomeDao())
                }
            }
        }
    }
    data class HomeUiState(
        val streakCount: Int = 0,
        val coinBalance: Int = 0,
        val isBankLinked: Boolean = false
    )
    private fun refreshUiState() {
        _uiState.value = HomeUiState(
            streakCount = PrefsManager.getStreakCount(getApplication()),
            coinBalance = PrefsManager.getCoinBalance(getApplication()),
            isBankLinked = PrefsManager.isBankLinked(getApplication())
        )
    }
}
