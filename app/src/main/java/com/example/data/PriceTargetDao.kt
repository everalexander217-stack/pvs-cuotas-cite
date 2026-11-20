package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceTargetDao {
    @Query("SELECT * FROM price_targets ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PriceTargetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(target: PriceTargetEntity)

    @Query("DELETE FROM price_targets WHERE code = :code")
    suspend fun deleteByCode(code: String)
}
