package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.max

@Entity(tableName = "installments")
data class InstallmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val code: String,
    val category: String,
    val initialPaid: Double,
    val totalAmount: Double,
    val biweeklyQuota: Double,
    val totalQuotas: Int,
    val paidQuotas: Int,
    val nextPaymentDate: String,
    val sortOrder: Int
) {
    val remainingQuotas: Int
        get() = max(0, totalQuotas - paidQuotas)

    val progressFraction: Float
        get() = if (totalQuotas <= 0) 1f else (paidQuotas.toFloat() / totalQuotas.toFloat()).coerceIn(0f, 1f)

    val isCompleted: Boolean
        get() = remainingQuotas == 0
}
