package com.example.data.cloud

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class CloudUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val preferredTheme: String = "light",
    val languageCode: String = "ES",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("displayName", displayName.take(80).ifBlank { "Usuario PVS" })
        if (!email.isNullOrBlank()) {
            put("email", email.take(160))
        }
        put("preferredTheme", if (preferredTheme == "dark") "dark" else "light")
        put("languageCode", if (languageCode == "EN") "EN" else "ES")
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class CloudInstallment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val code: String = "",
    val category: String = "General",
    val initialPaid: Double = 0.0,
    val totalAmount: Double = 0.01,
    val biweeklyQuota: Double = 0.0,
    val totalQuotas: Int = 1,
    val paidQuotas: Int = 0,
    val nextPaymentDate: String = "",
    val sortOrder: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "title" to title.trim().take(120).ifBlank { "Compra PVS" },
        "code" to code.trim().uppercase().take(32).ifBlank { "PVS-01" },
        "category" to category.trim().take(40).ifBlank { "General" },
        "initialPaid" to initialPaid.coerceAtLeast(0.0),
        "totalAmount" to totalAmount.coerceAtLeast(0.01),
        "biweeklyQuota" to biweeklyQuota.coerceAtLeast(0.0),
        "totalQuotas" to totalQuotas.coerceIn(1, 60),
        "paidQuotas" to paidQuotas.coerceIn(0, totalQuotas.coerceIn(1, 60)),
        "nextPaymentDate" to nextPaymentDate.trim().take(40).ifBlank { "30 Nov 2026" },
        "sortOrder" to sortOrder.coerceAtLeast(0),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class CloudPriceTarget(
    @DocumentId val id: String = "",
    val userId: String = "",
    val code: String = "",
    val productName: String = "",
    val targetPrice: Double = 1.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "code" to code.trim().uppercase().take(32).ifBlank { "PVS-01" },
        "productName" to productName.trim().take(120).ifBlank { "Producto" },
        "targetPrice" to targetPrice.coerceAtLeast(1.0),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class CloudProductNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productCode: String = "",
    val productName: String = "",
    val noteText: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "productCode" to productCode.trim().uppercase().take(32).ifBlank { "PVS-01" },
        "productName" to productName.trim().take(120).ifBlank { "Producto" },
        "noteText" to noteText.trim().take(500).ifBlank { "-" },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class CloudChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val text: String = "",
    val audioTranscript: String? = null,
    val groundingNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("role", if (role == "model") "model" else "user")
        put("text", text.trim().take(8000).ifBlank { "..." })
        if (!audioTranscript.isNullOrBlank()) {
            put("audioTranscript", audioTranscript.trim().take(2000))
        }
        if (!groundingNote.isNullOrBlank()) {
            put("groundingNote", groundingNote.trim().take(1000))
        }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}
