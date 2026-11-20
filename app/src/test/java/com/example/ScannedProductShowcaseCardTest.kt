package com.example

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.content.IntentCompat
import androidx.test.core.app.ApplicationProvider
import com.example.data.ProductVerifierCatalog
import com.example.ui.components.ScannedProductShowcaseCard
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.PvsTheme
import com.example.ui.theme.SpanishStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class ScannedProductShowcaseCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `scanned product showcase card renders details and triggers actions`() {
        val tv = ProductVerifierCatalog.findByQuery("TV-50")!!
        var rescanClicked = false
        var quickAddClicked = false

        composeTestRule.setContent {
            PvsTheme {
                CompositionLocalProvider(LocalAppStrings provides SpanishStrings) {
                    ScannedProductShowcaseCard(
                        product = tv,
                        scannedBarcode = "7501001005011",
                        onRescanClick = { rescanClicked = true },
                        onQuickAddClick = { quickAddClicked = true }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("scanned_product_showcase_card_TV-50").assertIsDisplayed()
        composeTestRule.onNodeWithTag("scanned_product_name").assertTextContains("Smart TV 50\" 4K UHD")
        composeTestRule.onNodeWithTag("scanned_product_code").assertTextContains("TV-50")
        composeTestRule.onNodeWithTag("scanned_product_price").assertTextContains("$389.00")
        composeTestRule.onNodeWithTag("scanned_product_biweekly_quota").assertTextContains("$48.63")
        composeTestRule.onNodeWithTag("scanned_product_status_pill").assertTextContains("Escaneado")
        composeTestRule.onNodeWithTag("share_product_button").assertIsDisplayed()

        composeTestRule.onNodeWithTag("scanned_card_rescan_button").performClick()
        composeTestRule.onNodeWithTag("scanned_card_quick_add_button").performClick()
        composeTestRule.onNodeWithTag("share_product_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(rescanClicked)
        assertTrue(quickAddClicked)

        val app = ApplicationProvider.getApplicationContext<Application>()
        val startedChooser = Shadows.shadowOf(app).nextStartedActivity
        assertNotNull(startedChooser)
        assertEquals(Intent.ACTION_CHOOSER, startedChooser?.action)
        val innerIntent = startedChooser?.let {
            IntentCompat.getParcelableExtra(it, Intent.EXTRA_INTENT, Intent::class.java)
        }
        assertEquals(Intent.ACTION_SEND, innerIntent?.action)
        val sharedText = innerIntent?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        assertTrue(sharedText.contains("Smart TV 50\" 4K UHD"))
        assertTrue(sharedText.contains("$389.00"))
        assertTrue(sharedText.contains("$48.63"))
    }
}
