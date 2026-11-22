package com.example.data.cloud

import android.content.Context
import com.example.R
import com.example.data.InstallmentEntity
import com.example.data.PriceTargetEntity
import com.example.data.ProductNoteEntity
import com.example.ui.theme.AppLanguage
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreSyncRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeDocId(raw: String): String {
        val sanitized = raw.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        return sanitized.take(120).ifBlank { "doc_item" }
    }

    fun observeUserProfile(): Flow<CloudUserProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        CloudUserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeChatMessages(): Flow<List<CloudChatMessage>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/chatMessages"
        emitAll(
            db.collection("users").document(uid).collection("chatMessages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeInstallments(): Flow<List<CloudInstallment>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/installments"
        emitAll(
            db.collection("users").document(uid).collection("installments")
                .orderBy("sortOrder", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudInstallment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        displayName: String,
        email: String?,
        darkMode: Boolean,
        language: AppLanguage
    ): Result<Unit> {
        var docPath = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
            docPath = ref.path
            val snap = ref.get().await()
            if (!snap.exists()) {
                val model = CloudUserProfile(
                    userId = uid,
                    displayName = displayName,
                    email = email,
                    preferredTheme = if (darkMode) "dark" else "light",
                    languageCode = language.shortCode
                )
                ref.set(model.toCreateMap()).await()
            } else {
                val updates = buildMap<String, Any> {
                    put("displayName", displayName.take(80).ifBlank { "Usuario PVS" })
                    if (!email.isNullOrBlank()) {
                        put("email", email.take(160))
                    }
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                ref.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateThemePreference(darkMode: Boolean): Result<Unit> {
        var docPath = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
            docPath = ref.path
            val themeValue = if (darkMode) "dark" else "light"
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "preferredTheme" to themeValue,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val user = auth.currentUser
                ref.set(
                    CloudUserProfile(
                        userId = uid,
                        displayName = user?.displayName ?: "Usuario PVS",
                        email = user?.email,
                        preferredTheme = themeValue,
                        languageCode = "ES"
                    ).toCreateMap()
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateLanguagePreference(language: AppLanguage): Result<Unit> {
        var docPath = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
            docPath = ref.path
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "languageCode" to language.shortCode,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun syncInstallment(entity: InstallmentEntity): Result<Unit> {
        var docPath = "users/unauthenticated/installments"
        return try {
            val uid = requireUserId()
            val docId = sanitizeDocId("inst_${entity.id}_${entity.code}")
            val ref = db.collection("users").document(uid)
                .collection("installments").document(docId)
            docPath = ref.path
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "title" to entity.title.trim().take(120).ifBlank { "Compra PVS" },
                        "code" to entity.code.trim().uppercase().take(32).ifBlank { "PVS-01" },
                        "category" to entity.category.trim().take(40).ifBlank { "General" },
                        "initialPaid" to entity.initialPaid.coerceAtLeast(0.0),
                        "totalAmount" to entity.totalAmount.coerceAtLeast(0.01),
                        "biweeklyQuota" to entity.biweeklyQuota.coerceAtLeast(0.0),
                        "totalQuotas" to entity.totalQuotas.coerceIn(1, 60),
                        "paidQuotas" to entity.paidQuotas.coerceIn(0, entity.totalQuotas.coerceIn(1, 60)),
                        "nextPaymentDate" to entity.nextPaymentDate.trim().take(40).ifBlank { "30 Nov 2026" },
                        "sortOrder" to entity.sortOrder.coerceAtLeast(0),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val model = CloudInstallment(
                    userId = uid,
                    title = entity.title,
                    code = entity.code,
                    category = entity.category,
                    initialPaid = entity.initialPaid,
                    totalAmount = entity.totalAmount,
                    biweeklyQuota = entity.biweeklyQuota,
                    totalQuotas = entity.totalQuotas,
                    paidQuotas = entity.paidQuotas,
                    nextPaymentDate = entity.nextPaymentDate,
                    sortOrder = entity.sortOrder
                )
                ref.set(model.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteInstallment(entity: InstallmentEntity): Result<Unit> {
        var docPath = "users/unauthenticated/installments"
        return try {
            val uid = requireUserId()
            val docId = sanitizeDocId("inst_${entity.id}_${entity.code}")
            val ref = db.collection("users").document(uid)
                .collection("installments").document(docId)
            docPath = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun syncPriceTarget(target: PriceTargetEntity): Result<Unit> {
        var docPath = "users/unauthenticated/priceTargets"
        return try {
            val uid = requireUserId()
            val docId = sanitizeDocId(target.code.uppercase())
            val ref = db.collection("users").document(uid)
                .collection("priceTargets").document(docId)
            docPath = ref.path
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "productName" to target.productName.trim().take(120).ifBlank { "Producto" },
                        "targetPrice" to target.targetPrice.coerceAtLeast(1.0),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val model = CloudPriceTarget(
                    userId = uid,
                    code = target.code,
                    productName = target.productName,
                    targetPrice = target.targetPrice
                )
                ref.set(model.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deletePriceTarget(code: String): Result<Unit> {
        var docPath = "users/unauthenticated/priceTargets"
        return try {
            val uid = requireUserId()
            val docId = sanitizeDocId(code.uppercase())
            val ref = db.collection("users").document(uid)
                .collection("priceTargets").document(docId)
            docPath = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun syncProductNote(note: ProductNoteEntity): Result<Unit> {
        var docPath = "users/unauthenticated/productNotes"
        return try {
            val uid = requireUserId()
            val docId = sanitizeDocId("note_${note.id}_${note.productCode}")
            val ref = db.collection("users").document(uid)
                .collection("productNotes").document(docId)
            docPath = ref.path
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "productName" to note.productName.trim().take(120).ifBlank { "Producto" },
                        "noteText" to note.noteText.trim().take(500).ifBlank { "-" },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val model = CloudProductNote(
                    userId = uid,
                    productCode = note.productCode,
                    productName = note.productName,
                    noteText = note.noteText
                )
                ref.set(model.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProductNote(noteId: Int, productCode: String): Result<Unit> {
        var docPath = "users/unauthenticated/productNotes"
        return try {
            val uid = requireUserId()
            val docId = sanitizeDocId("note_${noteId}_${productCode}")
            val ref = db.collection("users").document(uid)
                .collection("productNotes").document(docId)
            docPath = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addChatMessage(
        role: String,
        text: String,
        audioTranscript: String? = null,
        groundingNote: String? = null
    ): Result<String> {
        var docPath = "users/unauthenticated/chatMessages"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("chatMessages").document()
            docPath = ref.path
            val model = CloudChatMessage(
                userId = uid,
                role = role,
                text = text,
                audioTranscript = audioTranscript,
                groundingNote = groundingNote
            )
            ref.set(model.toCreateMap()).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun clearChatHistory(messageIds: List<String>): Result<Unit> {
        var docPath = "users/unauthenticated/chatMessages"
        return try {
            val uid = requireUserId()
            val collection = db.collection("users").document(uid).collection("chatMessages")
            docPath = collection.path
            messageIds.filter { it.isNotBlank() }.forEach { msgId ->
                collection.document(sanitizeDocId(msgId)).delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
