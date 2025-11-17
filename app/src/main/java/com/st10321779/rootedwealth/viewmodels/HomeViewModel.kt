package com.st10321779.rootedwealth.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.st10321779.rootedwealth.data.local.dao.CategorySpending
import com.st10321779.rootedwealth.data.local.entity.Category
import com.st10321779.rootedwealth.data.local.entity.Expense
import com.st10321779.rootedwealth.data.local.entity.Income
import com.st10321779.rootedwealth.repository.FirebaseRepository
import com.st10321779.rootedwealth.util.PrefsManager
import kotlinx.coroutines.launch
import java.util.*
import kotlin.random.Random

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // SETUP
    // this is the single source of truth now. No more 'db' or 'ExpenseRepository'.
    private val firebaseRepository = FirebaseRepository()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // LIVE DATA SOURCES FROM FIREBASE
    // these update automatically whenever the online database changes
    private val allExpenses: LiveData<List<Expense>> = firebaseRepository.getExpensesLiveData()
    private val allIncome: LiveData<List<Income>> = firebaseRepository.getIncomeLiveData()
    private val allCategories: LiveData<List<Category>> = firebaseRepository.getCategoriesLiveData()

    // this updates automatically when settings change
    val budgetGoals: LiveData<Pair<Float, Float>>  = PrefsManager.getBudgetLiveData(application)

    //UI STATE AND PROCESSED DATA
    // these calculate totals on the fly based on the raw Firebase data
    val totalSpentThisMonth = MediatorLiveData<Double>()
    val totalIncomeThisMonth = MediatorLiveData<Double>()
    val spendingByCategory = MediatorLiveData<List<CategorySpending>>()

    data class HomeUiState(
        val userName: String = "User",
        val streakCount: Int = 0,
        val coinBalance: Int = 0,
        val isBankLinked: Boolean = false
    )
    private val _uiState = MutableLiveData<HomeUiState>()
    val uiState: LiveData<HomeUiState> = _uiState

    init {
        // When raw data changes, trigger recalculations
        totalSpentThisMonth.addSource(allExpenses) { recalculateTotals() }
        totalIncomeThisMonth.addSource(allIncome) { recalculateTotals() }

        // Update the pie chart data when expenses or categories change
        spendingByCategory.addSource(allExpenses) { recalculateCategorySpending() }
        spendingByCategory.addSource(allCategories) { recalculateCategorySpending() }

        refreshUiState()
        seedDefaultCategoriesIfFirstTime()
    }

    private fun recalculateTotals() {
        val expenses = allExpenses.value ?: emptyList()
        val income = allIncome.value ?: emptyList()

        val (start, end) = getCurrentMonthDateRange()

        // Filter by this month and sum
        totalSpentThisMonth.value = expenses
            .filter { it.date in start..end }
            .sumOf { it.amount }

        totalIncomeThisMonth.value = income
            .filter { it.date in start..end }
            .sumOf { it.amount }
    }

    private fun recalculateCategorySpending() {
        val expenses = allExpenses.value ?: emptyList()
        val categories = allCategories.value ?: emptyList()
        val (start, end) = getCurrentMonthDateRange()

        val categoryMap = categories.associateBy { it.id }
        val filteredExpenses = expenses.filter { it.date in start..end }

        // Group expenses by category name and sum them up
        val chartData = filteredExpenses
            .groupBy { expense ->
                categoryMap[expense.categoryId]?.name ?: "Uncategorized"
            }
            .map { (categoryName, items) ->
                CategorySpending(categoryName, items.sumOf { it.amount })
            }

        spendingByCategory.value = chartData
    }

    private fun refreshUiState() {
        val currentUser = auth.currentUser
        val userName = currentUser?.email?.split('@')?.get(0)?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        } ?: "User"

        _uiState.value = HomeUiState(
            userName = userName,
            streakCount = PrefsManager.getStreakCount(getApplication()),
            coinBalance = PrefsManager.getCoinBalance(getApplication()),
            isBankLinked = PrefsManager.isBankLinked(getApplication())
        )
    }

    private fun seedDefaultCategoriesIfFirstTime() = viewModelScope.launch {
        if (!firebaseRepository.hasDefaultCategories()) {
            firebaseRepository.seedDefaultCategories()
            firebaseRepository.seedDefaultTheme()
        }
    }

    // ACTIONS

    fun addIncome(income: Income) = viewModelScope.launch {
        // Directly add to Firebase
        firebaseRepository.addIncome(income)
    }

    private fun getCurrentMonthDateRange(): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startDate = calendar.time

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDate = calendar.time

        return Pair(startDate, endDate)
    }
}