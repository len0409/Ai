package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TokenDao {
    @Query("SELECT * FROM tokens ORDER BY createdAt DESC")
    fun getAll(): Flow<List<TokenEntity>>

    @Query("SELECT * FROM tokens WHERE platformId = :platformId ORDER BY createdAt DESC")
    fun getByPlatform(platformId: String): Flow<List<TokenEntity>>

    @Query("SELECT * FROM tokens WHERE status = 'active'")
    suspend fun getActiveTokens(): List<TokenEntity>

    @Query("SELECT * FROM tokens WHERE platformId = :platformId AND status = 'active' LIMIT 1")
    suspend fun getActiveTokenForPlatform(platformId: String): TokenEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(token: TokenEntity): Long

    @Query("UPDATE tokens SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE tokens SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: Long, timestamp: Long)

    @Delete
    suspend fun delete(token: TokenEntity)

    @Query("DELETE FROM tokens WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tokens")
    suspend fun clearAll()
}