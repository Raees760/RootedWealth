package com.st10321779.rootedwealth.viewmodels

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.st10321779.rootedwealth.data.local.dao.CategorySpending
import com.st10321779.rootedwealth.data.local.entity.Category
import com.st10321779.rootedwealth.data.local.entity.Expense
import com.st10321779.rootedwealth.data.local.entity.Income
import com.st10321779.rootedwealth.repository.FirebaseRepository
import com.st10321779.rootedwealth.util.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        totalSpentThisMonth.addSource(allExpenses) {
            recalculateTotals()
            checkWeeklyChallenges() // Trigger the check
        }
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
    private fun checkWeeklyChallenges() {
        // These challenges only apply if the bank account is linked
        if (!PrefsManager.isBankLinked(getApplication())) return

        val expenses = allExpenses.value ?: return
        val categories = allCategories.value ?: return
        if (expenses.isEmpty() || categories.isEmpty()) return

        val end = Calendar.getInstance().time
        val startCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
        val start = startCal.time

        val weeklyExpenses = expenses.filter { it.date in start..end }

        // Challenge: "Home Meals Are Best"
        val takeoutCat = categories.find { it.name == "Takeout" }
        if (takeoutCat != null) {
            val takeawaySpend = weeklyExpenses
                .filter { it.categoryId == takeoutCat.id }
                .sumOf { it.amount }

            val achievementId = "home_meals_challenge"
            // We'll reset this achievement weekly, so we just check if it's already unlocked in this session
            // A more robust system would use PrefsManager to store the "last earned date"
            if (takeawaySpend < 500 && !PrefsManager.hasAchievement(getApplication(), achievementId)) {
                awardAchievement(getApplication(), achievementId, 75, "Challenge Complete: Home Meals Are Best!")
            }
        }

        // Challenge: "The Simple Life"
        val luxuryCategoryNames = listOf("Entertainment", "Takeout")
        val luxuryCategoryIds = categories
            .filter { it.name in luxuryCategoryNames }
            .map { it.id }

        if (luxuryCategoryIds.isNotEmpty()) {
            val luxurySpend = weeklyExpenses
                .filter { it.categoryId in luxuryCategoryIds }
                .sumOf { it.amount }

            val achievementId = "simple_life_challenge"
            if (luxurySpend < 1500 && !PrefsManager.hasAchievement(getApplication(), achievementId)) {
                awardAchievement(getApplication(), achievementId, 75, "Challenge Complete: The Simple Life!")
            }
        }
    }

    // A helper to award achievements from the ViewModel
    private fun awardAchievement(context: Context, id: String, coins: Int, message: String) {
        // Since we are in a ViewModel, we must use viewModelScope to ensure this runs correctly
        viewModelScope.launch {
            PrefsManager.addCoins(context, coins)
            PrefsManager.setAchievementUnlocked(context, id)
            // Show the Toast on the Main thread
            withContext(Dispatchers.Main) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }
}