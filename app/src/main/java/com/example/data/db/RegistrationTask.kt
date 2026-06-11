package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registration_tasks")
data class RegistrationTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val provider: String = "",
    val status: String = "pending",
    val accountEmail: String = "",
    val tempMailId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)