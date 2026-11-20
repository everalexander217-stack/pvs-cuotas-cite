package com.example

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.content.IntentCompat
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.DueDateUtils
import com.example.data.InstallmentEntity
import com.example.data.InstallmentRepository
import com.example.data.PaymentAlert
import com.example.data.PriceVerdict
import com.example.data.ProductVerifierCatalog
import com.example.notifications.QuotaNotificationHelper
import com.example.notifications.QuotaReminderCoordinator
import com.example.ui.PvsViewModel
import com.example.ui.components.buildInstallmentShareText
import com.example.ui.components.buildProductShareText
import com.example.ui.components.buildShareChooserIntent
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.EnglishStrings
import com.example.ui.theme.SpanishStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PVS Cuotas", appName)
    }

    @Test
    fun `verifier catalog resolves all four preloaded codes`() {
        val tv = ProductVerifierCatalog.findByQuery("TV-50")
        val cel = ProductVerifierCatalog.findByQuery("cel128")
        val zap = ProductVerifierCatalog.findByQuery("ZAP-01")
        val aud = ProductVerifierCatalog.findByQuery("AUD-05")

        assertNotNull(tv)
        assertNotNull(cel)
        assertNotNull(zap)
        assertNotNull(aud)

        assertEquals(PriceVerdict.GREEN, tv?.verdict)
        assertEquals(PriceVerdict.YELLOW, cel?.verdict)
        assertEquals(PriceVerdict.RED, zap?.verdict)
        assertEquals(PriceVerdict.GREEN, aud?.verdict)
    }

    @Test
    fun `upcoming alerts include items due within 3 days`() {
        val dueSoon = InstallmentEntity(
            id = 1,
            title = "Smart TV",
            code = "TV-50",
            category = "Tecnología",
            initialPaid = 90.0,
            totalAmount = 380.0,
            biweeklyQuota = 48.0,
            totalQuotas = 6,
            paidQuotas = 4,
            nextPaymentDate = DueDateUtils.formatDateOffsetFromToday(2),
            sortOrder = 0
        )
        val dueLater = dueSoon.copy(
            id = 2,
            code = "ZAP-01",
            nextPaymentDate = DueDateUtils.formatDateOffsetFromToday(10)
        )

        val alerts = DueDateUtils.computeUpcomingAlerts(listOf(dueSoon, dueLater), thresholdDays = 3)
        assertEquals(1, alerts.size)
        assertEquals("TV-50", alerts.first().installment.code)
        assertEquals(2, alerts.first().daysRemaining)
    }

    @Test
    fun `recent searches keep only latest 5 distinct queried codes`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = InstallmentRepository(db.installmentDao(), db.recentSearchDao())
            val codes = listOf("TV-50", "CEL-128", "ZAP-01", "AUD-05", "LAP-16", "CAF-02")
            codes.forEachIndexed { idx, code ->
                val product = ProductVerifierCatalog.findByQuery(code)!!
                db.recentSearchDao().upsert(
                    com.example.data.RecentSearchEntity(
                        code = product.code,
                        productName = product.name,
                        searchedAt = 1000L + idx
                    )
                )
                db.recentSearchDao().trimToLastFive()
            }
            val recent = repo.recentSearches.first()
            assertEquals(5, recent.size)
            assertEquals(listOf("CAF-02", "LAP-16", "AUD-05", "ZAP-01", "CEL-128"), recent.map { it.code })
        } finally {
            db.close()
        }
    }

    @Test
    fun `budget affordability subtracts active quincena from available money`() {
        val active = InstallmentEntity(
            id = 1,
            title = "Smart TV",
            code = "TV-50",
            category = "Tecnología",
            initialPaid = 90.0,
            totalAmount = 380.0,
            biweeklyQuota = 60.0,
            totalQuotas = 6,
            paidQuotas = 2,
            nextPaymentDate = DueDateUtils.formatDateOffsetFromToday(3),
            sortOrder = 0
        )
        val state = com.example.ui.PvsUiState(
            installments = listOf(active),
            availableMoney = 250.0
        )
        assertEquals(60.0, state.totalQuincena, 0.001)
        assertEquals(190.0, state.freeMoney, 0.001)
    }

    @Test
    fun `room persists installments order payments and deletions locally`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = InstallmentRepository(db.installmentDao(), db.recentSearchDao())
            repo.ensureSeeded()

            val initial = repo.installments.first()
            assertEquals(4, initial.size)

            // Reorder first two and register payment on first
            val reordered = listOf(initial[1], initial[0], initial[2], initial[3])
            repo.persistOrder(reordered)
            repo.registerNextPayment(reordered.first())

            val afterUpdate = repo.installments.first()
            assertEquals(initial[1].code, afterUpdate.first().code)
            assertEquals(initial[1].paidQuotas + 1, afterUpdate.first().paidQuotas)

            // Add custom installment and delete another
            repo.addInstallment(
                title = "Monitor 27\"",
                code = "MON-27",
                category = "Tecnología",
                initialPaid = 50.0,
                totalAmount = 250.0,
                totalQuotas = 4,
                nextPaymentDate = DueDateUtils.formatDateOffsetFromToday(5)
            )
            repo.deleteInstallment(afterUpdate.last().id)

            val finalItems = repo.installments.first()
            assertEquals(4, finalItems.size)
            assertEquals("MON-27", finalItems.last().code)
        } finally {
            db.close()
        }
    }

    @Test
    fun `notification helper posts channel reminder when permission granted and deduplicates same day`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        app.getSharedPreferences(QuotaNotificationHelper.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        val alert = PaymentAlert(
            installment = InstallmentEntity(
                id = 7,
                title = "Smart TV 50\" 4K UHD",
                code = "TV-50",
                category = "Tecnología",
                initialPaid = 97.25,
                totalAmount = 389.0,
                biweeklyQuota = 48.63,
                totalQuotas = 6,
                paidQuotas = 4,
                nextPaymentDate = DueDateUtils.formatDateOffsetFromToday(1),
                sortOrder = 0
            ),
            daysRemaining = 1
        )

        val postedFirst = QuotaNotificationHelper.postUpcomingReminders(
            context = app,
            alerts = listOf(alert),
            language = AppLanguage.ES,
            force = false
        )
        assertTrue(postedFirst)

        val notifManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowManager = Shadows.shadowOf(notifManager)
        assertNotNull(notifManager.getNotificationChannel(QuotaNotificationHelper.CHANNEL_ID))
        assertEquals(1, shadowManager.allNotifications.size)

        // Second non-forced call with identical alert signature same day should be skipped
        val postedDuplicate = QuotaNotificationHelper.postUpcomingReminders(
            context = app,
            alerts = listOf(alert),
            language = AppLanguage.ES,
            force = false
        )
        assertFalse(postedDuplicate)

        // Canceling clears notifications
        QuotaNotificationHelper.cancelAll(app)
        assertEquals(0, shadowManager.allNotifications.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `viewmodel persists notification settings and triggers reminder coordinator`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val prefs = context.getSharedPreferences("test_pvs_prefs", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()

            val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            var syncCalls = 0
            var lastEnabled: Boolean? = null
            var lastThreshold: Int? = null
            var testReminderTriggered = false

            val fakeCoordinator = object : QuotaReminderCoordinator {
                override fun syncSchedule(enabled: Boolean, thresholdDays: Int, triggerImmediateCheck: Boolean) {
                    syncCalls++
                    lastEnabled = enabled
                    lastThreshold = thresholdDays
                }

                override fun checkNow(force: Boolean) {}

                override fun sendTestReminder(
                    installments: List<InstallmentEntity>,
                    thresholdDays: Int,
                    language: AppLanguage
                ): Boolean {
                    testReminderTriggered = true
                    return true
                }
            }

            try {
                val repo = InstallmentRepository(db.installmentDao(), db.recentSearchDao())
                val vm = PvsViewModel(
                    repository = repo,
                    sharedPrefs = prefs,
                    reminderCoordinator = fakeCoordinator
                )

                vm.setAlertThresholdDays(7)
                vm.setNotificationsEnabled(false)
                assertTrue(syncCalls >= 2)
                assertEquals(false, lastEnabled)
                assertEquals(7, lastThreshold)
                assertFalse(prefs.getBoolean(QuotaNotificationHelper.KEY_NOTIFICATIONS_ENABLED, true))
                assertEquals(7, prefs.getInt(QuotaNotificationHelper.KEY_ALERT_THRESHOLD_DAYS, 3))

                assertTrue(vm.sendTestNotificationReminder())
                assertTrue(testReminderTriggered)
            } finally {
                db.close()
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `price history generates accurate 3M 6M 12M series ending at current prices`() {
        val tv = ProductVerifierCatalog.findByQuery("TV-50")!!
        val history3 = ProductVerifierCatalog.priceHistoryFor(tv, months = 3)
        val history6 = ProductVerifierCatalog.priceHistoryFor(tv, months = 6)
        val history12 = ProductVerifierCatalog.priceHistoryFor(tv, months = 12)

        assertEquals(3, history3.size)
        assertEquals(6, history6.size)
        assertEquals(12, history12.size)

        assertEquals("Hoy", history6.last().labelEs)
        assertEquals("Now", history6.last().labelEn)
        assertEquals(tv.storePrice, history6.last().storePrice, 0.01)
        assertEquals(tv.averagePrice, history6.last().marketAvgPrice, 0.01)

        // TV-50 is a green deal, so historical price was higher and trended down to current deal
        assertTrue(history12.first().storePrice > history12.last().storePrice)

        // Custom AI-estimated product also generates valid deterministic 6M history
        val custom = com.example.data.ProductCodeInfo(
            code = "FRY-08",
            name = "Air Fryer Pro",
            category = "Hogar",
            storePrice = 120.0,
            averagePrice = 100.0,
            suggestedInitial = 30.0,
            quotasCount = 4,
            biweeklyImpact = 22.5,
            verdict = PriceVerdict.RED,
            verdictReason = "Precio alto"
        )
        val customHistory = ProductVerifierCatalog.priceHistoryFor(custom, months = 6)
        assertEquals(6, customHistory.size)
        assertEquals(120.0, customHistory.last().storePrice, 0.01)
        assertEquals(100.0, customHistory.last().marketAvgPrice, 0.01)
    }

    @Test
    fun `barcode resolver handles retail EAN QR URLs and PVS codes`() {
        assertEquals("TV-50", ProductVerifierCatalog.resolveBarcodeValue("7501001005011"))
        assertEquals("AUD-05", ProductVerifierCatalog.resolveBarcodeValue("https://pvs.app/item/AUD-05"))
        assertEquals("CEL-128", ProductVerifierCatalog.resolveBarcodeValue("cel128"))
        assertEquals("8412345678905", ProductVerifierCatalog.resolveBarcodeValue(" 8412345678905 "))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `viewmodel submitScannedBarcode resolves known EAN code and updates searchQuery`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            try {
                val repo = InstallmentRepository(db.installmentDao(), db.recentSearchDao())
                val vm = PvsViewModel(repository = repo)

                vm.submitScannedBarcode("7501001000109")
                val state = vm.uiState.first {
                    it.searchQuery == "ZAP-01" && it.recentSearches.any { r -> r.code == "ZAP-01" }
                }
                assertEquals("ZAP-01", state.searchQuery)
                assertEquals("ZAP-01", state.verifiedProduct?.code)
                assertEquals("7501001000109", state.lastScannedBarcode)

                vm.selectExampleCode("TV-50")
                val manualState = vm.uiState.first {
                    it.searchQuery == "TV-50" && it.recentSearches.any { r -> r.code == "TV-50" }
                }
                assertNull(manualState.lastScannedBarcode)
            } finally {
                db.close()
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `share helper generates prefilled product and installment text and chooser intent`() {
        val tv = ProductVerifierCatalog.findByQuery("TV-50")!!

        val esText = buildProductShareText(tv, scannedBarcode = "7501001005011", strings = SpanishStrings)
        assertTrue(esText.contains("Smart TV 50\" 4K UHD"))
        assertTrue(esText.contains("TV-50 (7501001005011)"))
        assertTrue(esText.contains("$389.00"))
        assertTrue(esText.contains("$48.63"))
        assertTrue(esText.contains("6 cuotas"))
        assertTrue(esText.contains("$97.25"))

        val enText = buildProductShareText(tv, scannedBarcode = null, strings = EnglishStrings)
        assertTrue(enText.contains("Smart TV 50\" 4K UHD"))
        assertTrue(enText.contains("$389.00"))
        assertTrue(enText.contains("$48.63/biweekly"))
        assertTrue(enText.contains("6 quotas"))

        val chooser = buildShareChooserIntent(
            subject = SpanishStrings.shareSubjectProduct(tv.name),
            message = esText,
            chooserTitle = SpanishStrings.shareChooserTitle
        )
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        val innerIntent = IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent::class.java)
        assertNotNull(innerIntent)
        assertEquals(Intent.ACTION_SEND, innerIntent?.action)
        assertEquals("text/plain", innerIntent?.type)
        assertEquals(esText, innerIntent?.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals("PVS Cuotas: Smart TV 50\" 4K UHD", innerIntent?.getStringExtra(Intent.EXTRA_SUBJECT))

        val plan = InstallmentEntity(
            id = 1,
            title = "Audífonos ANC Studio",
            code = "AUD-05",
            category = "Audio",
            initialPaid = 29.50,
            totalAmount = 118.00,
            biweeklyQuota = 22.13,
            totalQuotas = 4,
            paidQuotas = 1,
            nextPaymentDate = "22 Nov 2026",
            sortOrder = 0
        )
        val planShare = buildInstallmentShareText(plan, SpanishStrings)
        assertTrue(planShare.contains("Audífonos ANC Studio"))
        assertTrue(planShare.contains("$118.00"))
        assertTrue(planShare.contains("$22.13/qna"))
        assertTrue(planShare.contains("3 de 4"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `viewmodel and room persist setPriceTarget and clearPriceTarget`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            try {
                val repo = InstallmentRepository(
                    dao = db.installmentDao(),
                    recentSearchDao = db.recentSearchDao(),
                    priceTargetDao = db.priceTargetDao()
                )
                val vm = PvsViewModel(repository = repo)
                val tv = ProductVerifierCatalog.findByQuery("TV-50")!!

                vm.setPriceTarget(tv, 420.0)
                val withTarget = vm.uiState.first { it.targetForProduct("TV-50")?.targetPrice == 420.0 }
                assertNotNull(withTarget.targetForProduct("tv-50"))
                assertEquals("Smart TV 50\" 4K UHD", withTarget.targetForProduct("TV-50")?.productName)

                vm.clearPriceTarget("TV-50")
                val cleared = vm.uiState.first { it.targetForProduct("TV-50") == null }
                assertNull(cleared.targetForProduct("TV-50"))
            } finally {
                db.close()
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `room scannedPriceDao stores and trims only last 5 scanned prices per product`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val tv = ProductVerifierCatalog.findByQuery("TV-50")!!
            val aud = ProductVerifierCatalog.findByQuery("AUD-05")!!
            val repo = InstallmentRepository(
                dao = db.installmentDao(),
                recentSearchDao = db.recentSearchDao(),
                priceTargetDao = db.priceTargetDao(),
                scannedPriceDao = db.scannedPriceDao()
            )

            // Insert 7 scanned prices for TV-50 with increasing timestamps
            val tvPrices = listOf(420.0, 415.0, 410.0, 405.0, 399.0, 395.0, 389.0)
            tvPrices.forEachIndexed { idx, price ->
                db.scannedPriceDao().insert(
                    com.example.data.ScannedPriceEntity(
                        productCode = tv.code,
                        productName = tv.name,
                        scannedPrice = price,
                        marketAvgPrice = tv.averagePrice,
                        rawBarcode = "7501001005011",
                        scannedAt = 1000L + idx * 100L
                    )
                )
                db.scannedPriceDao().trimProductToLastFive(tv.code)
            }

            // Insert 2 scanned prices for AUD-05
            repo.recordScannedPrice(aud, rawBarcode = "7501001000505", priceOverride = 129.0)
            repo.recordScannedPrice(aud, rawBarcode = "7501001000505", priceOverride = 118.0)

            val tvHistory = db.scannedPriceDao().observeForProduct("TV-50").first()
            assertEquals(5, tvHistory.size)
            assertEquals(listOf(410.0, 405.0, 399.0, 395.0, 389.0), tvHistory.map { it.scannedPrice })

            val audHistory = db.scannedPriceDao().observeForProduct("AUD-05").first()
            assertEquals(2, audHistory.size)
            assertEquals(listOf(129.0, 118.0), audHistory.map { it.scannedPrice })

            // Clearing TV-50 must leave AUD-05 intact
            repo.clearScannedPricesForProduct("TV-50")
            assertEquals(0, db.scannedPriceDao().observeForProduct("TV-50").first().size)
            assertEquals(2, db.scannedPriceDao().observeForProduct("AUD-05").first().size)
        } finally {
            db.close()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `viewmodel records scanned prices on barcode scan and observed logs keeping last 5`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            try {
                val repo = InstallmentRepository(
                    dao = db.installmentDao(),
                    recentSearchDao = db.recentSearchDao(),
                    priceTargetDao = db.priceTargetDao(),
                    scannedPriceDao = db.scannedPriceDao()
                )
                val vm = PvsViewModel(repository = repo)
                val tv = ProductVerifierCatalog.findByQuery("TV-50")!!

                vm.submitScannedBarcode("7501001005011")
                vm.logObservedScannedPrice(tv, 402.0)
                vm.logObservedScannedPrice(tv, 395.0)
                vm.logObservedScannedPrice(tv, 391.0)
                vm.logObservedScannedPrice(tv, 384.0)
                vm.logObservedScannedPrice(tv, 379.0)

                val state = vm.uiState.first {
                    val hist = it.scannedHistoryFor("TV-50")
                    hist.size == 5 && hist.last().scannedPrice == 379.0
                }
                val history = state.scannedHistoryFor("TV-50")
                assertEquals(5, history.size)
                assertEquals(listOf(402.0, 395.0, 391.0, 384.0, 379.0), history.map { it.scannedPrice })
            } finally {
                db.close()
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `room productNoteDao inserts updates deletes and isolates notes per product`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val tv = ProductVerifierCatalog.findByQuery("TV-50")!!
            val aud = ProductVerifierCatalog.findByQuery("AUD-05")!!
            val repo = InstallmentRepository(
                dao = db.installmentDao(),
                recentSearchDao = db.recentSearchDao(),
                priceTargetDao = db.priceTargetDao(),
                scannedPriceDao = db.scannedPriceDao(),
                productNoteDao = db.productNoteDao()
            )

            repo.addProductNote(tv, "2 años de garantía en tienda")
            repo.addProductNote(aud, "Incluye estuche de carga")

            val tvNotes = db.productNoteDao().observeForProduct("TV-50").first()
            val audNotes = db.productNoteDao().observeForProduct("AUD-05").first()
            assertEquals(1, tvNotes.size)
            assertEquals("2 años de garantía en tienda", tvNotes.first().noteText)
            assertEquals(1, audNotes.size)
            assertEquals("Incluye estuche de carga", audNotes.first().noteText)

            // Update TV-50 note and verify AUD-05 stays untouched
            repo.updateProductNote(tvNotes.first(), "Garantía extendida 3 años + instalación")
            val updatedTvNotes = db.productNoteDao().observeForProduct("TV-50").first()
            assertEquals("Garantía extendida 3 años + instalación", updatedTvNotes.first().noteText)
            assertEquals("Incluye estuche de carga", db.productNoteDao().observeForProduct("AUD-05").first().first().noteText)

            // Delete TV-50 note
            repo.deleteProductNote(updatedTvNotes.first().id)
            assertTrue(db.productNoteDao().observeForProduct("TV-50").first().isEmpty())
            assertEquals(1, db.productNoteDao().observeForProduct("AUD-05").first().size)
        } finally {
            db.close()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `viewmodel exposes productNotes map and performs add update delete via Room`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            try {
                val repo = InstallmentRepository(
                    dao = db.installmentDao(),
                    recentSearchDao = db.recentSearchDao(),
                    priceTargetDao = db.priceTargetDao(),
                    scannedPriceDao = db.scannedPriceDao(),
                    productNoteDao = db.productNoteDao()
                )
                val vm = PvsViewModel(repository = repo)
                val tv = ProductVerifierCatalog.findByQuery("TV-50")!!

                vm.addProductNote(tv, "Consultar envío gratis")
                val afterAdd = vm.uiState.first {
                    it.notesForProduct("TV-50").any { note -> note.noteText == "Consultar envío gratis" }
                }
                val addedNote = afterAdd.notesForProduct("tv-50").first()
                assertEquals("TV-50", addedNote.productCode)

                vm.updateProductNote(addedNote, "Envío gratis confirmado")
                val afterUpdate = vm.uiState.first {
                    it.notesForProduct("TV-50").any { note -> note.noteText == "Envío gratis confirmado" }
                }
                assertEquals(1, afterUpdate.notesForProduct("TV-50").size)

                vm.deleteProductNote(addedNote.id)
                val afterDelete = vm.uiState.first { it.notesForProduct("TV-50").isEmpty() }
                assertTrue(afterDelete.notesForProduct("TV-50").isEmpty())
            } finally {
                db.close()
            }
        } finally {
            Dispatchers.resetMain()
        }
    }
}
