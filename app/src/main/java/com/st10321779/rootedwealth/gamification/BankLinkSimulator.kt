package com.st10321779.rootedwealth.gamification

import com.st10321779.rootedwealth.data.local.dao.CategoryDao
import com.st10321779.rootedwealth.data.local.dao.ExpenseDao
import com.st10321779.rootedwealth.data.local.dao.IncomeDao
import com.st10321779.rootedwealth.data.local.entity.Expense
import com.st10321779.rootedwealth.data.local.entity.Income
import java.util.Calendar
import java.util.Date
import kotlin.random.Random

object BankLinkSimulator {

    // this is the main function to call to seed the data
    suspend fun seedLinkedData(
        categoryDao: CategoryDao,
        expenseDao: ExpenseDao,
        incomeDao: IncomeDao
    ) {
        // MONTHLY INCOME
        incomeDao.insert(Income(
            amount = 25000.0,
            date = getDate(dayOfMonth = 25),
            source = "Salary (Simulated)",
            notes = "Monthly pay",
            isLinked = true
        ))

        // RECURRING EXPENSES (BILLS)
        val rentCat = categoryDao.getCategoryByName("Rent")
        rentCat?.let {
            expenseDao.insert(Expense(
                amount = 7500.0, date = getDate(1), categoryId = it.id,
                notes = "Monthly Rent (Simulated)", imageUri = null, isLinked = true
            ))
        }

        val utilCat = categoryDao.getCategoryByName("Utilities")
        utilCat?.let {
            expenseDao.insert(Expense(
                amount = 850.0, date = getDate(3), categoryId = it.id,
                notes = "Electricity & Water (Simulated)", imageUri = null, isLinked = true
            ))
        }

        // VARIABLE EXPENSES
        val groceriesCat = categoryDao.getCategoryByName("Groceries")
        val transportCat = categoryDao.getCategoryByName("Transport")
        val takeoutCat = categoryDao.getCategoryByName("Takeout")

        // make some random groceries and transport expenses throughout the month
        for (i in 1..10) {
            groceriesCat?.let {
                expenseDao.insert(Expense(
                    amount = Random.nextDouble(150.0, 600.0),
                    date = getDate(Random.nextInt(2, 28)),
                    categoryId = it.id, notes = "Grocery Store (Simulated)", imageUri = null, isLinked = true
                ))
            }
            transportCat?.let {
                expenseDao.insert(Expense(
                    amount = Random.nextDouble(80.0, 250.0),
                    date = getDate(Random.nextInt(2, 28)),
                    categoryId = it.id, notes = "Fuel/Transport (Simulated)", imageUri = null, isLinked = true
                ))
            }
            takeoutCat?.let {
                if(i < 4) { // only a few takeouts
                    expenseDao.insert(Expense(
                        amount = Random.nextDouble(90.0, 280.0),
                        date = getDate(Random.nextInt(2, 28)),
                        categoryId = it.id, notes = "Takeout (Simulated)", imageUri = null, isLinked = true
                    ))
                }
            }
        }
    }

    private fun getDate(dayOfMonth: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        return calendar.time
    }
}