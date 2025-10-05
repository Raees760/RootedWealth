package com.st10321779.rootedwealth.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.st10321779.rootedwealth.data.local.entity.Expense
import com.st10321779.rootedwealth.data.local.entity.ExpenseWithCategory
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


    @Query("""
    SELECT 
        e.id, e.amount, e.date, e.notes, e.imageUri, e.isLinked,
        c.name as categoryName, 
        c.icon as categoryIcon, 
        c.color as categoryColor 
    FROM expenses e 
    JOIN categories c ON e.categoryId = c.id 
    WHERE e.date BETWEEN :startDate AND :endDate 
    ORDER BY e.date DESC
""")
    fun getExpensesWithCategoryInPeriod(startDate: Date, endDate: Date): LiveData<List<ExpenseWithCategory>>

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: Long)

    @Update
    suspend fun update(expense: Expense)

    @Query("SELECT * FROM expenses WHERE id = :expenseId LIMIT 1")
    suspend fun getExpenseById(expenseId: Long): Expense?

    // For Analytics
    @Query("SELECT COUNT(DISTINCT STRFTIME('%Y-%m-%d', date / 1000, 'unixepoch')) FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getDistinctLogDaysCount(startDate: Date, endDate: Date): Int

    @Query("SELECT IFNULL(SUM(amount), 0) FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalExpensesInPeriodAsync(startDate: Date, endDate: Date): Double

    //For Gamification
    @Query("SELECT COUNT(id) FROM expenses")
    suspend fun getExpenseCount(): Int

    // Gets total spending for a specific category in a date range.
    // Needed for challenges like "Spend under R500 on Takeout".
    @Query("""
    SELECT SUM(e.amount) 
    FROM expenses e
    JOIN categories c ON e.categoryId = c.id
    WHERE c.name = :categoryName AND e.date BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalSpentForCategory(categoryName: String, startDate: Date, endDate: Date): Double?

    // Gets total spending for a list of categories (e.g., "luxuries").
    @Query("""
    SELECT SUM(e.amount)
    FROM expenses e
    JOIN categories c ON e.categoryId = c.id
    WHERE c.name IN (:categoryNames) AND e.date BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalSpentForCategoryList(categoryNames: List<String>, startDate: Date, endDate: Date): Double?

}
data class CategorySpending(
    val categoryName: String,
    val total: Double
)