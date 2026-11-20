package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannedPriceDao {
    @Query("SELECT * FROM scanned_prices ORDER BY scannedAt ASC, id ASC")
    fun observeAll(): Flow<List<ScannedPriceEntity>>

    @Query("SELECT * FROM scanned_prices WHERE productCode = :productCode ORDER BY scannedAt ASC, id ASC")
    fun observeForProduct(productCode: String): Flow<List<ScannedPriceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ScannedPriceEntity)

    @Query(
        """
        DELETE FROM scanned_prices
        WHERE productCode = :productCode
        AND id NOT IN (
            SELECT id FROM scanned_prices
            WHERE productCode = :productCode
            ORDER BY scannedAt DESC, id DESC
            LIMIT 5
        )
        """
    )
    suspend fun trimProductToLastFive(productCode: String)

    @Query("DELETE FROM scanned_prices WHERE productCode = :productCode")
    suspend fun deleteAllForProduct(productCode: String)
}
