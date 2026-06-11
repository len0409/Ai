package com.example.service

import com.example.data.db.AppDatabase
import kotlinx.coroutines.flow.first

class KeyPoolManager(private val database: AppDatabase) {

    suspend fun getActiveKeyCount(): Int {
        return database.apiKeyDao().countActive()
    }

    suspend fun getActiveKeys(): List<String> {
        val allKeys = database.apiKeyDao().getAll().first()
        return allKeys.filter { it.status == "Active" }.map { it.keyValue }
    }
}