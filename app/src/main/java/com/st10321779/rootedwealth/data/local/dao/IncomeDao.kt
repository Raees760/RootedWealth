package com.st10321779.rootedwealth.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.st10321779.rootedwealth.data.local.entity.Income
import java.util.Date

@Dao
interface IncomeDao {
    @Insert
    suspend fun insert(income: Income)

    @Query("SELECT * FROM income WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getIncomeInPeriod(startDate: Date, endDate: Date): LiveData<List<Income>>

    @Query("SELECT SUM(amount) FROM income WHERE date BETWEEN :startDate AND :endDate")
    fun getTotalIncomeInPeriod(startDate: Date, endDate: Date): LiveData<Double>

    @Query("DELETE FROM income WHERE id = :incomeId")
    suspend fun deleteIncomeById(incomeId: Long)

    @Update
    suspend fun update(income: Income)

    @Query("SELECT * FROM income WHERE id = :incomeId LIMIT 1")
    suspend fun getIncomeById(incomeId: Long): Income?

    //Analytics
    @Query("SELECT IFNULL(SUM(amount), 0) FROM income WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalIncomeInPeriodAsync(startDate: Date, endDate: Date): Double

    //For Gamification

    // Old, doesn't work
    // @Query("SELECT EXISTS(SELECT 1 FROM income WHERE isLinked = 1 LIMIT 1)")
    // suspend fun hasLinkedEntries(): Boolean

    // New
    @Query("SELECT COUNT(id) FROM income WHERE isLinked = 1")
    suspend fun getLinkedEntryCount(): Int
}