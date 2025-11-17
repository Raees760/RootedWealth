package com.st10321779.rootedwealth.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "income")
data class Income(
    @PrimaryKey
    var id: String = "",
    var amount: Double = 0.0,
    var date: Date = Date(),
    var source: String? = null,
    var notes: String? = null,
    var isLinked: Boolean = false
)