package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductNoteDao {
    @Query("SELECT * FROM product_notes ORDER BY updatedAt DESC, id DESC")
    fun observeAll(): Flow<List<ProductNoteEntity>>

    @Query("SELECT * FROM product_notes WHERE productCode = :productCode ORDER BY updatedAt DESC, id DESC")
    fun observeForProduct(productCode: String): Flow<List<ProductNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: ProductNoteEntity): Long

    @Query("SELECT * FROM product_notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): ProductNoteEntity?

    @Update
    suspend fun update(note: ProductNoteEntity)

    @Query("DELETE FROM product_notes WHERE id = :id")
    suspend fun deleteById(id: Int)
}
