package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CfWorkerConfigDao {
    @Query("SELECT * FROM cf_worker_configs ORDER BY addedAt DESC")
    fun getAll(): Flow<List<CfWorkerConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: CfWorkerConfig): Long

    @Query("DELETE FROM cf_worker_configs WHERE url = :url")
    suspend fun deleteWorker(url: String)

    @Query("DELETE FROM cf_worker_configs")
    suspend fun clearAll()
}