package com.st10321779.rootedwealth.repository

import androidx.lifecycle.LiveData
import com.st10321779.rootedwealth.data.local.dao.ExpenseDao
import com.st10321779.rootedwealth.data.local.entity.Expense
import java.util.Date

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    fun getExpensesInPeriod(startDate: Date, endDate: Date): LiveData<List<Expense>> {
        return expenseDao.getExpensesInPeriod(startDate, endDate)
    }

    fun getTotalSpentInPeriod(startDate: Date, endDate: Date): LiveData<Double> {
        return expenseDao.getTotalSpentInPeriod(startDate, endDate)
    }

    suspend fun addExpense(expense: Expense) {
        expenseDao.insert(expense)
    }
}