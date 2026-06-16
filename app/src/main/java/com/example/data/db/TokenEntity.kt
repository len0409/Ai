package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tokens")
data class TokenEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val platformId: String,
    val label: String,
    val tokenValue: String,
    val tokenType: String,  // COOKIE, BEARER, API_KEY
    val status: String = "active", // active, expired, invalid
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = 0
)