package com.st10321779.rootedwealth.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.st10321779.rootedwealth.data.local.entity.Expense
import java.util.Date

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: Expense)

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesInPeriod(startDate: Date, endDate: Date): LiveData<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    fun getTotalSpentInPeriod(startDate: Date, endDate: Date): LiveData<Double>

    @Query("""
        SELECT c.name as categoryName, SUM(e.amount) as total
        FROM expenses e
        JOIN categories c ON e.categoryId = c.id
        WHERE e.date BETWEEN :startDate AND :endDate
        GROUP BY c.name
    """)
    fun getSpendingByCategory(startDate: Date, endDate: Date): LiveData<List<CategorySpending>>
}
data class CategorySpending(
    val categoryName: String,
    val total: Double
)