package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistrationTaskDao {
    @Query("SELECT * FROM registration_tasks ORDER BY createdAt DESC")
    fun getAll(): Flow<List<RegistrationTask>>

    @Query("SELECT * FROM registration_tasks WHERE status = :status ORDER BY createdAt DESC")
    fun getByStatus(status: String): Flow<List<RegistrationTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: RegistrationTask): Long

    @Query("UPDATE registration_tasks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Delete
    suspend fun deleteTask(task: RegistrationTask)
}