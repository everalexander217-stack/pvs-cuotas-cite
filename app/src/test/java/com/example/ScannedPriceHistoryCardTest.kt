package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.ProductVerifierCatalog
import com.example.data.ScannedPriceEntity
import com.example.ui.components.PriceHistoryDashboardCard
import com.example.ui.theme.EnglishStrings
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.PvsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class ScannedPriceHistoryCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `price history dashboard displays last 5 Room scanned prices and updates live`() {
        val tv = ProductVerifierCatalog.findByQuery("TV-50")!!
        val initialHistory = listOf(420.0, 412.0, 405.0, 396.0, 389.0).mapIndexed { idx, price ->
            ScannedPriceEntity(
                id = idx + 1,
                productCode = tv.code,
                productName = tv.name,
                scannedPrice = price,
                marketAvgPrice = tv.averagePrice,
                rawBarcode = "7501001005011",
                scannedAt = 1_700_000_000_000L + idx * 60_000L
            )
        }
        var historyState by mutableStateOf(initialHistory)

        composeTestRule.setContent {
            PvsTheme {
                CompositionLocalProvider(LocalAppStrings provides EnglishStrings) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        PriceHistoryDashboardCard(
                            product = tv,
                            scannedHistory = historyState,
                            onLogScannedPrice = { newPrice ->
                                historyState = (historyState + ScannedPriceEntity(
                                    id = historyState.size + 1,
                                    productCode = tv.code,
                                    productName = tv.name,
                                    scannedPrice = newPrice,
                                    marketAvgPrice = tv.averagePrice,
                                    scannedAt = 1_700_000_600_000L
                                )).takeLast(5)
                            },
                            onClearScannedHistory = { historyState = emptyList() }
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("price_history_dashboard_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("vico_room_price_line_chart").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("price_history_subtitle")
            .assertTextContains("Last 5/5 scanned prices (Local Room DB)", substring = true)
        composeTestRule.onNodeWithTag("history_mode_scans").assertIsDisplayed()
        composeTestRule.onNodeWithTag("log_scanned_price_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("scanned_history_pill_0").performScrollTo().assertTextContains("$420.00", substring = true)
        composeTestRule.onNodeWithTag("scanned_history_pill_4").performScrollTo().assertTextContains("$389.00", substring = true)

        // Switch to 6M benchmark view and back to 5 Scans (Vico Line Chart)
        composeTestRule.onNodeWithTag("history_range_6").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("price_history_canvas").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("price_history_subtitle")
            .assertTextContains("Store price vs. market benchmark", substring = true)

        composeTestRule.onNodeWithTag("history_mode_scans").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("vico_room_price_line_chart").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("price_history_subtitle")
            .assertTextContains("Last 5/5 scanned prices", substring = true)

        // Tapping a scan pill selects that historical point cleanly
        composeTestRule.onNodeWithTag("scanned_history_pill_1").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Rolling 6th scan shifts out oldest ($420.00) and displays newest ($375.00)
        composeTestRule.runOnUiThread {
            historyState = (historyState + ScannedPriceEntity(
                id = 6,
                productCode = tv.code,
                productName = tv.name,
                scannedPrice = 375.0,
                marketAvgPrice = tv.averagePrice,
                scannedAt = 1_700_000_600_000L
            )).takeLast(5)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("scanned_history_pill_0").performScrollTo().assertTextContains("$412.00", substring = true)
        composeTestRule.onNodeWithTag("scanned_history_pill_4").performScrollTo().assertTextContains("$375.00", substring = true)

        // Clearing Room scan history shows empty hint
        composeTestRule.onNodeWithTag("clear_scanned_history_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("scanned_history_empty_hint").performScrollTo().assertIsDisplayed()
    }
}
