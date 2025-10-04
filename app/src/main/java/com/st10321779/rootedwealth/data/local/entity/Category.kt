package com.st10321779.rootedwealth.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String, // Placeholder for icon resource name
    val color: String, // Hex color string e.g., #FF5733
    val isDefault: Boolean,
    var isActive: Boolean = true
)