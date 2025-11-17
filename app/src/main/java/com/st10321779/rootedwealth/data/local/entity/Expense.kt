package com.st10321779.rootedwealth.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey
    var id: String = "", // Changed to String, and a 'var'
    var amount: Double = 0.0,
    var date: Date = Date(),
    var categoryId: String = "", // Must also be a String
    var notes: String? = null,
    var imageUri: String? = null, // store URI of the image
    var isDefault: Boolean = false, // Added for default categories
    val isLinked: Boolean = false //for  bank entries
)
data class ExpenseWithCategory(
    // don't need @Embedded here since we are selecting columns individually
    val id: String, // Changed to String
    val amount: Double,
    val date: Date,
    val notes: String?,
    val imageUri: String?,
    val isLinked: Boolean,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String
)