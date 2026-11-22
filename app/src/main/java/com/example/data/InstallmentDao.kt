package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallmentDao {
    @Query("SELECT * FROM installments ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<InstallmentEntity>

    @Query("SELECT COUNT(*) FROM installments")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM installments")
    suspend fun maxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InstallmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<InstallmentEntity>)

    @Update
    suspend fun update(item: InstallmentEntity)

    @Update
    suspend fun updateAll(items: List<InstallmentEntity>)

    @Query("UPDATE installments SET paidQuotas = :paidQuotas, nextPaymentDate = :nextPaymentDate WHERE id = :id")
    suspend fun updatePaymentProgress(id: Int, paidQuotas: Int, nextPaymentDate: String)

    @Query("UPDATE installments SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Int, category: String)

    @Query("SELECT * FROM installments WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): InstallmentEntity?

    @Query("DELETE FROM installments WHERE id = :id")
    suspend fun deleteById(id: Int)
}
