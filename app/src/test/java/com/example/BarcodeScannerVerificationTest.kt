package com.example

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.InstallmentRepository
import com.example.data.PriceVerdict
import com.example.data.ProductVerifierCatalog
import com.example.ui.PvsUiState
import com.example.ui.PvsViewModel
import com.example.ui.components.barcodeFormatLabel
import com.example.ui.screens.VerificadorScreen
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.PvsTheme
import com.example.ui.theme.SpanishStrings
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class BarcodeScannerVerificationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `resolveBarcodeValue handles EAN-13, formatted UPC, and QR payload urls`() {
        assertEquals("TV-50", ProductVerifierCatalog.resolveBarcodeValue("7501001005011"))
        assertEquals("TV-50", ProductVerifierCatalog.resolveBarcodeValue("750-1001-005011"))
        assertEquals("TV-50", ProductVerifierCatalog.resolveBarcodeValue("07501001005011"))
        assertEquals("CEL-128", ProductVerifierCatalog.resolveBarcodeValue("https://pvs.app/item/cel-128"))
        assertEquals("7509998887771", ProductVerifierCatalog.resolveBarcodeValue("750-999888-7771"))
    }

    @Test
    fun `barcodeFormatLabel returns readable labels for ML Kit barcode formats`() {
        assertEquals("EAN-13", barcodeFormatLabel(Barcode.FORMAT_EAN_13))
        assertEquals("UPC-A", barcodeFormatLabel(Barcode.FORMAT_UPC_A))
        assertEquals("QR", barcodeFormatLabel(Barcode.FORMAT_QR_CODE))
        assertEquals("Code 128", barcodeFormatLabel(Barcode.FORMAT_CODE_128))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `submitScannedBarcode verifies product price and records Room scan history`() = runTest {
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
                    scannedPriceDao = db.scannedPriceDao()
                )
                val vm = PvsViewModel(repository = repo)

                vm.submitScannedBarcode("7501001005011")

                val verifiedState = vm.uiState.first {
                    it.verifiedProduct?.code == "TV-50" &&
                        it.scannedHistoryFor("TV-50").isNotEmpty()
                }

                val product = verifiedState.verifiedProduct
                assertNotNull(product)
                assertEquals(389.00, product!!.storePrice, 0.01)
                assertEquals(460.00, product.averagePrice, 0.01)
                assertEquals(PriceVerdict.GREEN, product.verdict)
                assertEquals("7501001005011", verifiedState.lastScannedBarcode)
                assertEquals("7501001005011", verifiedState.scannedHistoryFor("TV-50").last().rawBarcode)
            } finally {
                db.close()
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `verificador renders scanner CTAs and verified price pass after barcode scan`() {
        val tv = ProductVerifierCatalog.findByQuery("7501001005011")!!

        composeTestRule.setContent {
            PvsTheme(darkTheme = false) {
                CompositionLocalProvider(LocalAppStrings provides SpanishStrings) {
                    VerificadorScreen(
                        state = PvsUiState(
                            searchQuery = tv.code,
                            verifiedProduct = tv,
                            lastScannedBarcode = "7501001005011"
                        ),
                        onQueryChange = {},
                        onSubmitSearch = {},
                        onSelectExample = {},
                        onClearRecentSearches = {},
                        onUpdateBudget = {},
                        onAdjustBudget = {},
                        onRetryAi = {},
                        onAddToCart = {},
                        onToggleLanguage = {},
                        onOpenNotifications = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("open_barcode_scanner_hero_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open_barcode_scanner_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("scanned_product_showcase_card_TV-50")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("scanned_product_code").assertTextContains("TV-50")
        composeTestRule.onNodeWithTag("scanned_product_price").assertTextContains("$389.00")
        composeTestRule.onNodeWithText("7501001005011", substring = true).assertIsDisplayed()
    }
}
