package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.PriceTargetEntity
import com.example.data.ProductVerifierCatalog
import com.example.ui.components.PriceTargetTrackerCard
import com.example.ui.components.ScannedProductShowcaseCard
import com.example.ui.theme.EnglishStrings
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.PvsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class PriceTargetTrackerCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `price target tracker shows below target visual indicator and saves presets`() {
        val tv = ProductVerifierCatalog.findByQuery("TV-50")!! // storePrice = 389.0
        var savedPrice: Double? = null

        composeTestRule.setContent {
            PvsTheme {
                CompositionLocalProvider(LocalAppStrings provides EnglishStrings) {
                    var target by remember { mutableStateOf<PriceTargetEntity?>(null) }
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        ScannedProductShowcaseCard(
                            product = tv,
                            scannedBarcode = "7501001005011",
                            targetPrice = target?.targetPrice,
                            onRescanClick = {},
                            onQuickAddClick = {}
                        )
                        PriceTargetTrackerCard(
                            product = tv,
                            activeTarget = target,
                            savedTargets = listOfNotNull(target),
                            onSaveTarget = { newPrice ->
                                savedPrice = newPrice
                                target = PriceTargetEntity(
                                    code = tv.code,
                                    productName = tv.name,
                                    targetPrice = newPrice
                                )
                            },
                            onClearTarget = { target = null },
                            onSelectTrackedCode = {}
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("price_target_tracker_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("target_price_status_badge").performScrollTo().assertTextContains("No target")
        composeTestRule.onNodeWithTag("set_target_price_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("below_target_indicator_banner").assertDoesNotExist()
        composeTestRule.onNodeWithTag("showcase_below_target_badge").assertDoesNotExist()

        // Selecting -10% preset sets target below current storePrice -> above target indicator shown
        composeTestRule.onNodeWithTag("target_preset_10_pct").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(350.10, savedPrice ?: 0.0, 0.05)
        composeTestRule.onNodeWithTag("target_price_value_tile").performScrollTo().assertTextContains("$350.10", substring = true)
        composeTestRule.onNodeWithTag("below_target_indicator_banner").assertDoesNotExist()
        composeTestRule.onNodeWithTag("showcase_below_target_badge").assertDoesNotExist()
    }

    @Test
    fun `below target indicator banner and showcase badge display when scanned price is below target`() {
        val tv = ProductVerifierCatalog.findByQuery("TV-50")!! // storePrice = 389.0
        val highTarget = PriceTargetEntity(
            code = tv.code,
            productName = tv.name,
            targetPrice = 420.00
        )

        composeTestRule.setContent {
            PvsTheme {
                CompositionLocalProvider(LocalAppStrings provides EnglishStrings) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        ScannedProductShowcaseCard(
                            product = tv,
                            scannedBarcode = "7501001005011",
                            targetPrice = highTarget.targetPrice,
                            onRescanClick = {},
                            onQuickAddClick = {}
                        )
                        PriceTargetTrackerCard(
                            product = tv,
                            activeTarget = highTarget,
                            savedTargets = listOf(highTarget),
                            onSaveTarget = {},
                            onClearTarget = {},
                            onSelectTrackedCode = {}
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("showcase_below_target_badge").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("showcase_below_target_badge").assertTextContains("Below target ($420.00)", substring = true)
        composeTestRule.onNodeWithTag("below_target_indicator_banner").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("target_price_status_badge").performScrollTo().assertTextContains("Below target!", substring = true)
    }
}
