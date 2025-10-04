package com.st10321779.rootedwealth.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.st10321779.rootedwealth.data.local.AppDatabase
import com.st10321779.rootedwealth.data.model.HistoryItem
import java.util.Calendar
import java.util.Date

enum class FilterPeriod {
    TODAY, WEEK, MONTH, LAST_MONTH
}

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val expenseDao = db.expenseDao()
    private val incomeDao = db.incomeDao()

    private val _dateRange = MutableLiveData<Pair<Date, Date>>()

    // These are now private, intermediate LiveData sources
    private val expensesForPeriod: LiveData<List<HistoryItem.ExpenseItem>> = _dateRange.switchMap { (start, end) ->
        expenseDao.getExpensesWithCategoryInPeriod(start, end)
    }.map { list -> list.map { HistoryItem.ExpenseItem(it) } }

    private val incomeForPeriod: LiveData<List<HistoryItem.IncomeItem>> = _dateRange.switchMap { (start, end) ->
        incomeDao.getIncomeInPeriod(start, end)
    }.map { list -> list.map { HistoryItem.IncomeItem(it) } }

    // This is the public LiveData the UI will observe.
    // It merges the two sources together.
    val combinedHistory = MediatorLiveData<List<HistoryItem>>()

    init {
        // The MediatorLiveData needs to observe its sources.
        combinedHistory.addSource(expensesForPeriod) { mergeAndPost() }
        combinedHistory.addSource(incomeForPeriod) { mergeAndPost() }

        // Set the initial period
        setPeriod(FilterPeriod.MONTH)
    }

    private fun mergeAndPost() {
        val expenses = expensesForPeriod.value ?: emptyList()
        val income = incomeForPeriod.value ?: emptyList()

        val combinedList = (expenses + income).sortedByDescending { it.date }
        combinedHistory.value = combinedList
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

    // Placeholder for Alignment Tracker logic
    fun getAlignmentTrackerData(): LiveData<String> {
        return MutableLiveData("Alignment Tracker: Balanced Saver")
    }
}