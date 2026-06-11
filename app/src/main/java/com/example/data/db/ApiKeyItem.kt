package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_keys")
data class ApiKeyItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val keyLabel: String = "",
    val keyValue: String = "",
    val provider: String = "",
    val status: String = "Active",
    val timestamp: Long = System.currentTimeMillis(),
    val poolType: String = "active"
)