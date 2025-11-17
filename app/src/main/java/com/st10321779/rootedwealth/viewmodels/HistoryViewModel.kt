package com.st10321779.rootedwealth.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.st10321779.rootedwealth.data.local.dao.CategorySpending
import com.st10321779.rootedwealth.data.local.entity.Category
import com.st10321779.rootedwealth.data.local.entity.Expense
import com.st10321779.rootedwealth.data.local.entity.ExpenseWithCategory
import com.st10321779.rootedwealth.data.local.entity.Income
import com.st10321779.rootedwealth.data.model.HistoryItem
import com.st10321779.rootedwealth.repository.FirebaseRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

enum class FilterPeriod {
    TODAY, WEEK, MONTH, LAST_MONTH
}

data class AlignmentTrackerInfo(val label: String, val insight: String)


class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseRepository = FirebaseRepository()

    private val _dateRange = MutableLiveData<Pair<Date, Date>>()

    // Raw, unfiltered data streams from Firebase.
    private val allExpenses: LiveData<List<Expense>> = firebaseRepository.getExpensesLiveData()
    private val allIncome: LiveData<List<Income>> = firebaseRepository.getIncomeLiveData()
    private val allCategories: LiveData<List<Category>> = firebaseRepository.getCategoriesLiveData()

    // The final, processed list that the UI will observe.
    val combinedHistory = MediatorLiveData<List<HistoryItem>>()
    val spendingByCategory = MediatorLiveData<List<CategorySpending>>()

    // LiveData specifically for the Line Chart
    val timelineData = MediatorLiveData<List<TimeSeriesDataPoint>>()
    private val _alignmentTrackerData = MutableLiveData<AlignmentTrackerInfo>()
    val alignmentTrackerData: LiveData<AlignmentTrackerInfo> = _alignmentTrackerData

    init {
        // When any source data changes, re-run our processing logic.
        combinedHistory.addSource(_dateRange) { processData() }
        combinedHistory.addSource(allExpenses) { processData() }
        combinedHistory.addSource(allIncome) { processData() }
        combinedHistory.addSource(allCategories) { processData() }
        timelineData.addSource(_dateRange) { processData() }
        timelineData.addSource(allExpenses) { processData() }
        timelineData.addSource(allIncome) { processData() }

        // Set the initial filter period.
        setPeriod(FilterPeriod.MONTH)
    }

    private fun processData() {
        val (start, end) = _dateRange.value ?: return
        val expenses = allExpenses.value ?: emptyList()
        val income = allIncome.value ?: emptyList()
        val categories = allCategories.value ?: emptyList()

        // filter raw data by the selected date range
        val filteredExpenses = expenses.filter { it.date in start..end }
        val filteredIncome = income.filter { it.date in start..end }

        //perform the client-side "Join" to create ExpenseWithCategory
        val categoryMap = categories.associateBy { it.id }
        val expensesWithCategory = filteredExpenses.map { expense ->
            val category = categoryMap[expense.categoryId] ?: Category(name = "Uncategorized", color = "#808080")
            ExpenseWithCategory(
                id = expense.id,
                amount = expense.amount,
                date = expense.date,
                notes = expense.notes,
                imageUri = expense.imageUri,
                isLinked = expense.isLinked,
                categoryName = category.name,
                categoryIcon = category.icon,
                categoryColor = category.color
            )
        }

        //create the final list for the RecyclerView adapter
        val expenseItems = expensesWithCategory.map { HistoryItem.ExpenseItem(it) }
        val incomeItems = filteredIncome.map { HistoryItem.IncomeItem(it) }
        val combinedList = (expenseItems + incomeItems).sortedByDescending { it.date }
        combinedHistory.value = combinedList

        //create the final data for the charts
        val chartData = expensesWithCategory
            .groupBy { it.categoryName }
            .map { (categoryName, items) ->
                CategorySpending(categoryName, items.sumOf { it.amount })
            }
        spendingByCategory.value = chartData

        //process data for the Line Chart
        processTimelineData(filteredExpenses, filteredIncome)

        // calculate the Alignment Tracker
        calculateAlignmentTracker(expenseItems, incomeItems)
    }
    private fun processTimelineData(expenses: List<Expense>, income: List<Income>) {
        // group all transactions by day
        val dailyExpenses = expenses.groupBy { getDayIdentifier(it.date) }
        val dailyIncome = income.groupBy { getDayIdentifier(it.date) }

        // get all unique days from both lists
        val allDays = (dailyExpenses.keys + dailyIncome.keys).distinct().sorted()

        val dataPoints = allDays.map { day ->
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(day) ?: Date()
            val totalIncomeForDay = dailyIncome[day]?.sumOf { it.amount } ?: 0.0
            val totalExpensesForDay = dailyExpenses[day]?.sumOf { it.amount } ?: 0.0

            //calculate the net amount for the day
            val netAmount = totalIncomeForDay - totalExpensesForDay

            TimeSeriesDataPoint(date, netAmount)
        }

        timelineData.value = dataPoints
    }

    // get a consistent string representation of a day (e.g., "2025-10-26")
    private fun getDayIdentifier(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(date)
    }
    fun deleteHistoryItem(item: HistoryItem) = viewModelScope.launch {
        when (item) {
            is HistoryItem.ExpenseItem -> firebaseRepository.deleteExpense(item.expense.id)
            is HistoryItem.IncomeItem -> firebaseRepository.deleteIncome(item.income.id)
        }
    }

    fun setPeriod(period: FilterPeriod) {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time

        when (period) {
            FilterPeriod.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
            }
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
        val startDate = calendar.time
        _dateRange.value = Pair(startDate, endDate)
    }

    fun setCustomPeriod(start: Date, end: Date) {
        _dateRange.value = Pair(start, end)
    }

    private fun calculateAlignmentTracker(expenses: List<HistoryItem.ExpenseItem>, income: List<HistoryItem.IncomeItem>) {
        val totalIncome = income.sumOf { it.income.amount }
        val totalExpenses = expenses.sumOf { it.expense.amount }
        val distinctLogDays = (expenses.map { it.expense.date } + income.map { it.income.date })
            .distinctBy {
                val cal = Calendar.getInstance().apply { time = it }
                cal.get(Calendar.DAY_OF_YEAR)
            }.count()

        val adherenceScore = min(distinctLogDays / 30.0, 1.0)
        val savingsRate = if (totalIncome > 0) (totalIncome - totalExpenses) / totalIncome else 0.0
        val behaviorScore = when {
            savingsRate < 0 -> 0.0
            savingsRate < 0.1 -> 0.3
            savingsRate < 0.25 -> 0.7
            else -> 1.0
        }

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
}