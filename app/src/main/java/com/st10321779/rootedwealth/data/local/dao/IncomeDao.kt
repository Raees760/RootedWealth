package com.st10321779.rootedwealth.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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

}