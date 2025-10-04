package com.st10321779.rootedwealth.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val date: Date,
    val categoryId: Long,
    val notes: String?,
    val imageUri: String?, // Store URI of the image
    val isLinked: Boolean = false // For simulated bank entries
)
data class ExpenseWithCategory(
    // We don't need @Embedded here since we are selecting columns individually
    val id: Long,
    val amount: Double,
    val date: Date,
    val notes: String?,
    val imageUri: String?,
    val isLinked: Boolean,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String
)