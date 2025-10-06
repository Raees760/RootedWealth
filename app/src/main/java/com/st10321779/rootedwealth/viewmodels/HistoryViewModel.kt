package com.st10321779.rootedwealth.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.st10321779.rootedwealth.data.local.AppDatabase
import com.st10321779.rootedwealth.data.local.dao.CategorySpending
import com.st10321779.rootedwealth.data.model.HistoryItem
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import kotlin.math.min

enum class FilterPeriod {
    TODAY, WEEK, MONTH, LAST_MONTH
}

// Data class to hold the result of our tracker calculation
data class AlignmentTrackerInfo(val label: String, val insight: String)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val expenseDao = db.expenseDao()
    private val incomeDao = db.incomeDao()

    private val _dateRange = MutableLiveData<Pair<Date, Date>>()

    val combinedHistory: MediatorLiveData<List<HistoryItem>> = MediatorLiveData()

    val spendingByCategory: LiveData<List<CategorySpending>> = _dateRange.switchMap { (start, end) ->
        expenseDao.getSpendingByCategory(start, end)
    }

    private val _alignmentTrackerData = MutableLiveData<AlignmentTrackerInfo>()
    val alignmentTrackerData: LiveData<AlignmentTrackerInfo> = _alignmentTrackerData

    init {
        val expensesForPeriod = _dateRange.switchMap { (start, end) ->
            expenseDao.getExpensesWithCategoryInPeriod(start, end)
        }.map { list -> list.map { HistoryItem.ExpenseItem(it) } }

        val incomeForPeriod = _dateRange.switchMap { (start, end) ->
            incomeDao.getIncomeInPeriod(start, end)
        }.map { list -> list.map { HistoryItem.IncomeItem(it) } }

        combinedHistory.addSource(expensesForPeriod) { mergeAndPost(it, incomeForPeriod.value) }
        combinedHistory.addSource(incomeForPeriod) { mergeAndPost(expensesForPeriod.value, it) }

        setPeriod(FilterPeriod.MONTH)
        calculateAlignmentTracker()
    }

    private fun mergeAndPost(expenses: List<HistoryItem.ExpenseItem>?, income: List<HistoryItem.IncomeItem>?) {
        val combinedList = (expenses ?: emptyList()) + (income ?: emptyList())
        combinedHistory.value = combinedList.sortedByDescending { it.date }
    }

    fun setPeriod(period: FilterPeriod) {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time

        when (period) {
            FilterPeriod.TODAY -> calendar.set(Calendar.HOUR_OF_DAY, 0)
            FilterPeriod.WEEK -> calendar.add(Calendar.DAY_OF_YEAR, -7)
            FilterPeriod.MONTH -> calendar.set(Calendar.DAY_OF_MONTH, 1)
            FilterPeriod.LAST_MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val lastMonthEndDate = calendar.clone() as Calendar
                lastMonthEndDate.set(Calendar.DAY_OF_MONTH, lastMonthEndDate.getActualMaximum(Calendar.DAY_OF_MONTH))
                _dateRange.value = Pair(calendar.time, lastMonthEndDate.time)
                return
            }
        }
        _dateRange.value = Pair(calendar.time, endDate)
    }

    private fun calculateAlignmentTracker() = viewModelScope.launch {
        val end = Calendar.getInstance().time
        val startCal = Calendar.getInstance()
        startCal.add(Calendar.DAY_OF_MONTH, -30)
        val start = startCal.time

        //get raw data from DB
        val totalIncome = incomeDao.getTotalIncomeInPeriodAsync(start, end)
        val totalExpenses = expenseDao.getTotalExpensesInPeriodAsync(start, end)
        val distinctLogDays = expenseDao.getDistinctLogDaysCount(start, end)

        // calculate Metrics
        // Adherence (0 to 1) (How discplined in recording entries)
        val adherenceScore = min(distinctLogDays / 30.0, 1.0)

        // Spending Behaviour (0 to 1, where 1 is a good saver)
        val savingsRate = if (totalIncome > 0) (totalIncome - totalExpenses) / totalIncome else 0.0
        val behaviorScore = when { // remapping linearly
            savingsRate < 0 -> 0.0 // in debt
            savingsRate < 0.1 -> 0.3 // low savings
            savingsRate < 0.25 -> 0.7 // good savings
            else -> 1.0 // excellent saver
        }

        // Gi ve Labels
        val behaviorLabel = if (behaviorScore > 0.6) "Saver" else "Spender"
        val adherenceLabel = if (adherenceScore > 0.6) "Planner" else "Chaotic"

        val finalLabel = when {
            behaviorScore > 0.6 && adherenceScore > 0.6 -> "Financial Virtuoso"
            behaviorScore > 0.6 -> "Steady Saver"
            adherenceScore > 0.6 -> "Diamond in the Rough"
            else -> "Impulsive Spender"
        }

        val finalInsight = when {

            behaviorScore > 0.6 && adherenceScore > 0.6 -> "You've mastered the art of wealth management. Keep up the amazing work!"
            behaviorScore > 0.6 -> "You're on the right track with saving! Try logging your expenses daily to make your financial picture even clearer."
            adherenceScore > 0.6 -> "You track your money diligently. Now try applying that discipline to your saving habits too!"
            else -> "Your spending patterns show a lack of control. You need to make some drastic lifestyle changes."
        }

        _alignmentTrackerData.postValue(AlignmentTrackerInfo(finalLabel, finalInsight))
    }

    fun deleteHistoryItem(item: HistoryItem) = viewModelScope.launch {
        when (item) {
            is HistoryItem.ExpenseItem -> db.expenseDao().deleteExpenseById(item.expense.id)
            is HistoryItem.IncomeItem -> db.incomeDao().deleteIncomeById(item.income.id)
        }
    }
}