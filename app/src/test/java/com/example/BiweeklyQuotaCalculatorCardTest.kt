package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.example.data.ProductVerifierCatalog
import com.example.ui.components.BiweeklyQuotaCalculatorCard
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.PvsTheme
import com.example.ui.theme.SpanishStrings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class BiweeklyQuotaCalculatorCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `calculator updates biweekly quota automatically when total price or payment count changes`() {
        val tv = ProductVerifierCatalog.findByQuery("TV-50")!! // storePrice = 389.0, quotasCount = 6

        composeTestRule.setContent {
            PvsTheme {
                CompositionLocalProvider(LocalAppStrings provides SpanishStrings) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        BiweeklyQuotaCalculatorCard(product = tv)
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("biweekly_quota_calculator_card").performScrollTo().assertIsDisplayed()

        // Default: 389.00 / 6 = 64.83
        composeTestRule.onNodeWithTag("quota_calc_result_tile").assertTextContains("$64.83", substring = true)

        // Enter total price 400.00 -> 400 / 6 = 66.67
        composeTestRule.onNodeWithTag("quota_calc_price_input").performScrollTo().performTextReplacement("400")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("quota_calc_result_tile").assertTextContains("$66.67", substring = true)

        // Select 8 biweekly payments -> 400 / 8 = 50.00
        composeTestRule.onNodeWithTag("quota_calc_count_chip_8").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("quota_calc_result_tile").assertTextContains("$50.00", substring = true)
        composeTestRule.onNodeWithTag("quota_calc_result_tile").assertTextContains("8 cuotas quincenales", substring = true)

        // Reset back to store price (389.00) & 6 payments -> 64.83
        composeTestRule.onNodeWithTag("quota_calc_reset_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("quota_calc_result_tile").assertTextContains("$64.83", substring = true)
    }
}
