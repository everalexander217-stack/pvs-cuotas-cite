package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_notes",
    indices = [Index(value = ["productCode", "updatedAt"])]
)
data class ProductNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productCode: String,
    val productName: String,
    val noteText: String,
    val updatedAt: Long = System.currentTimeMillis()
)
