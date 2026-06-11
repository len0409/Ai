package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cf_worker_configs")
data class CfWorkerConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String = "",
    val isActive: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)