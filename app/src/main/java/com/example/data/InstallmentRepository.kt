package com.example.data

import kotlinx.coroutines.flow.Flow

class InstallmentRepository(
    private val dao: InstallmentDao,
    private val recentSearchDao: RecentSearchDao? = null,
    private val priceTargetDao: PriceTargetDao? = null,
    private val scannedPriceDao: ScannedPriceDao? = null,
    private val productNoteDao: ProductNoteDao? = null
) {
    val installments: Flow<List<InstallmentEntity>> = dao.observeAll()
    val recentSearches: Flow<List<RecentSearchEntity>> =
        recentSearchDao?.observeRecent() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    val priceTargets: Flow<List<PriceTargetEntity>> =
        priceTargetDao?.observeAll() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    val scannedPrices: Flow<List<ScannedPriceEntity>> =
        scannedPriceDao?.observeAll() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    val productNotes: Flow<List<ProductNoteEntity>> =
        productNoteDao?.observeAll() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun addProductNote(product: ProductCodeInfo, noteText: String) {
        val noteDao = productNoteDao ?: return
        val normalizedCode = product.code.trim().uppercase()
        val trimmedText = noteText.trim()
        if (normalizedCode.isEmpty() || trimmedText.isEmpty()) return
        noteDao.insert(
            ProductNoteEntity(
                productCode = normalizedCode,
                productName = product.name,
                noteText = trimmedText,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateProductNote(note: ProductNoteEntity, newText: String) {
        val noteDao = productNoteDao ?: return
        val trimmedText = newText.trim()
        if (trimmedText.isEmpty()) return
        noteDao.update(
            note.copy(
                noteText = trimmedText,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteProductNote(noteId: Int) {
        productNoteDao?.deleteById(noteId)
    }

    suspend fun recordScannedPrice(
        product: ProductCodeInfo,
        rawBarcode: String? = null,
        priceOverride: Double? = null
    ) {
        val priceDao = scannedPriceDao ?: return
        val normalizedCode = product.code.trim().uppercase()
        if (normalizedCode.isEmpty()) return
        val priceToStore = (priceOverride ?: product.storePrice).coerceAtLeast(0.01)
        priceDao.insert(
            ScannedPriceEntity(
                productCode = normalizedCode,
                productName = product.name,
                scannedPrice = priceToStore,
                marketAvgPrice = product.averagePrice.coerceAtLeast(0.01),
                rawBarcode = rawBarcode?.trim()?.takeIf { it.isNotEmpty() },
                scannedAt = System.currentTimeMillis()
            )
        )
        priceDao.trimProductToLastFive(normalizedCode)
    }

    suspend fun clearScannedPricesForProduct(productCode: String) {
        val normalizedCode = productCode.trim().uppercase()
        if (normalizedCode.isEmpty()) return
        scannedPriceDao?.deleteAllForProduct(normalizedCode)
    }

    suspend fun savePriceTarget(product: ProductCodeInfo, targetPrice: Double) {
        val targetDao = priceTargetDao ?: return
        val normalizedCode = product.code.trim().uppercase()
        if (normalizedCode.isEmpty()) return
        targetDao.upsert(
            PriceTargetEntity(
                code = normalizedCode,
                productName = product.name,
                targetPrice = targetPrice.coerceAtLeast(1.0),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removePriceTarget(code: String) {
        val normalizedCode = code.trim().uppercase()
        if (normalizedCode.isEmpty()) return
        priceTargetDao?.deleteByCode(normalizedCode)
    }

    suspend fun getAllInstallments(): List<InstallmentEntity> = dao.getAll()

    suspend fun ensureSeeded() {
        if (dao.count() == 0) {
            dao.insertAll(defaultInstallments())
        }
    }

    suspend fun recordRecentSearch(product: ProductCodeInfo) {
        val searchDao = recentSearchDao ?: return
        searchDao.upsert(
            RecentSearchEntity(
                code = product.code,
                productName = product.name,
                searchedAt = System.currentTimeMillis()
            )
        )
        searchDao.trimToLastFive()
    }

    suspend fun clearRecentSearches() {
        recentSearchDao?.clearAll()
    }

    suspend fun restoreDefaultsIfEmpty() {
        if (dao.count() == 0) {
            dao.insertAll(defaultInstallments())
        }
    }

    suspend fun persistOrder(reordered: List<InstallmentEntity>) {
        val updated = reordered.mapIndexed { index, item ->
            item.copy(sortOrder = index)
        }
        dao.updateAll(updated)
    }

    suspend fun registerNextPayment(item: InstallmentEntity) {
        if (item.isCompleted) return
        val nextPaid = (item.paidQuotas + 1).coerceAtMost(item.totalQuotas)
        val nextDate = if (nextPaid >= item.totalQuotas) {
            "Completado"
        } else {
            advanceBiweeklyDate(item.nextPaymentDate)
        }
        dao.updatePaymentProgress(
            id = item.id,
            paidQuotas = nextPaid,
            nextPaymentDate = nextDate
        )
    }

    suspend fun addInstallment(
        title: String,
        code: String,
        category: String,
        initialPaid: Double,
        totalAmount: Double,
        totalQuotas: Int,
        nextPaymentDate: String
    ) {
        val safeQuotas = totalQuotas.coerceAtLeast(1)
        val financed = (totalAmount - initialPaid).coerceAtLeast(0.0)
        val quotaAmount = financed / safeQuotas
        val nextSortOrder = dao.maxSortOrder() + 1
        dao.insert(
            InstallmentEntity(
                title = title.trim(),
                code = code.trim().uppercase().ifEmpty { "PVS-${10 + nextSortOrder}" },
                category = category,
                initialPaid = initialPaid,
                totalAmount = totalAmount,
                biweeklyQuota = quotaAmount,
                totalQuotas = safeQuotas,
                paidQuotas = 0,
                nextPaymentDate = nextPaymentDate.ifBlank { "30 Nov 2026" },
                sortOrder = nextSortOrder
            )
        )
    }

    suspend fun deleteInstallment(id: Int) {
        dao.deleteById(id)
    }

    fun searchProductCode(query: String): ProductCodeInfo? {
        return ProductVerifierCatalog.findByQuery(query)
    }

    private fun advanceBiweeklyDate(current: String): String {
        val days = DueDateUtils.daysUntilDue(current) ?: 0
        return DueDateUtils.formatDateOffsetFromToday(days + 15)
    }

    private fun defaultInstallments(): List<InstallmentEntity> = listOf(
        InstallmentEntity(
            title = "Smart TV 50\" 4K UHD",
            code = "TV-50",
            category = "Tecnología",
            initialPaid = 97.25,
            totalAmount = 389.00,
            biweeklyQuota = 48.63,
            totalQuotas = 6,
            paidQuotas = 4,
            nextPaymentDate = DueDateUtils.formatDateOffsetFromToday(2),
            sortOrder = 0
        ),
        InstallmentEntity(
            title = "Smartphone 128GB 5G",
            code = "CEL-128",
            category = "Telefonía",
            initialPaid = 80.00,
            totalAmount = 320.00,
            biweeklyQuota = 40.00,
            totalQuotas = 6,
            paidQuotas = 2,
            nextPaymentDate = DueDateUtils.formatDateOffsetFromToday(3),
            sortOrder = 1
        ),
        InstallmentEntity(
            title = "Audífonos ANC Studio",
            code = "AUD-05",
            category = "Audio",
            initialPaid = 29.50,
            totalAmount = 118.00,
            biweeklyQuota = 22.13,
            totalQuotas = 4,
            paidQuotas = 3,
            nextPaymentDate = DueDateUtils.formatDateOffsetFromToday(11),
            sortOrder = 2
        ),
        InstallmentEntity(
            title = "Zapatillas Running Pro",
            code = "ZAP-01",
            category = "Calzado",
            initialPaid = 36.25,
            totalAmount = 145.00,
            biweeklyQuota = 27.19,
            totalQuotas = 4,
            paidQuotas = 1,
            nextPaymentDate = DueDateUtils.formatDateOffsetFromToday(16),
            sortOrder = 3
        )
    )
}
