package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "price_targets")
data class PriceTargetEntity(
    @PrimaryKey val code: String,
    val productName: String,
    val targetPrice: Double,
    val updatedAt: Long = System.currentTimeMillis()
)
