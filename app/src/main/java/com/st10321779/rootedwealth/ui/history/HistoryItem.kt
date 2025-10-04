package com.st10321779.rootedwealth.data.model

import com.st10321779.rootedwealth.data.local.entity.ExpenseWithCategory
import com.st10321779.rootedwealth.data.local.entity.Income
import java.util.Date

// This class is perfect for a list that can contain a few specific types.
sealed class HistoryItem(val date: Date) {
    data class ExpenseItem(val expense: ExpenseWithCategory) : HistoryItem(expense.date)
    data class IncomeItem(val income: Income) : HistoryItem(income.date)
}