package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scanned_prices",
    indices = [Index(value = ["productCode", "scannedAt"])]
)
data class ScannedPriceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productCode: String,
    val productName: String,
    val scannedPrice: Double,
    val marketAvgPrice: Double,
    val rawBarcode: String? = null,
    val scannedAt: Long = System.currentTimeMillis()
)
