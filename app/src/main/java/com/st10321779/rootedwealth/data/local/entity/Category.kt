package com.st10321779.rootedwealth.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    var id: String = "", // Changed to String, and a 'var'
    var name: String = "",
    var icon: String = "", // placeholder for icon resource name
    var color: String = "", //hex color string e.g., #FF5733
    var isDefault: Boolean = false,
    var isActive: Boolean = true
)