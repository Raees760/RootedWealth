package com.st10321779.rootedwealth.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "income")
data class Income(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val date: Date,
    val source: String?,
    val notes: String?,
    val isLinked: Boolean = false
)