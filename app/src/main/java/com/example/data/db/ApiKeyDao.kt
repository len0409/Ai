package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiKeyDao {
    @Query("SELECT * FROM api_keys ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ApiKeyItem>>

    @Query("SELECT * FROM api_keys WHERE provider = :provider ORDER BY timestamp DESC")
    fun getByProvider(provider: String): Flow<List<ApiKeyItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ApiKeyItem): Long

    @Query("UPDATE api_keys SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Delete
    suspend fun delete(item: ApiKeyItem)

    @Query("DELETE FROM api_keys WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM api_keys")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM api_keys WHERE status = 'Active'")
    suspend fun countActive(): Int
}