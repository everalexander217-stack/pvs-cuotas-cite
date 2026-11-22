package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.example.data.ProductVerifierCatalog
import com.example.ui.PvsUiState
import com.example.ui.screens.VerificadorScreen
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
class ManualSearchAutocompleteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `manual search field filters autocomplete suggestions and selects product code`() {
        var selectedCode: String? = null

        composeTestRule.setContent {
            PvsTheme {
                CompositionLocalProvider(LocalAppStrings provides EnglishStrings) {
                    var uiState by remember {
                        mutableStateOf(
                            PvsUiState(
                                searchQuery = "",
                                verifiedProduct = null
                            )
                        )
                    }

                    VerificadorScreen(
                        state = uiState,
                        onQueryChange = { newQuery ->
                            uiState = uiState.copy(searchQuery = newQuery)
                        },
                        onSubmitSearch = {
                            val match = ProductVerifierCatalog.findByQuery(uiState.searchQuery)
                            uiState = uiState.copy(verifiedProduct = match)
                        },
                        onSelectExample = { code ->
                            selectedCode = code
                            val match = ProductVerifierCatalog.findByQuery(code)
                            uiState = uiState.copy(
                                searchQuery = code,
                                verifiedProduct = match
                            )
                        },
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
        composeTestRule.onNodeWithTag("verifier_search_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("verifier_autocomplete_dropdown").assertDoesNotExist()

        // Type partial code "lap" -> autocomplete dropdown opens showing LAP-16
        composeTestRule.onNodeWithTag("verifier_search_input").performTextReplacement("lap")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("verifier_autocomplete_dropdown").assertIsDisplayed()
        composeTestRule.onNodeWithTag("autocomplete_suggestion_LAP-16").assertIsDisplayed()
        composeTestRule.onNodeWithTag("autocomplete_suggestion_LAP-16").assertTextContains("LAP-16", substring = true)

        // Tap autocomplete suggestion -> selects LAP-16 and closes dropdown
        composeTestRule.onNodeWithTag("autocomplete_suggestion_LAP-16").performClick()
        composeTestRule.waitForIdle()
        assertEquals("LAP-16", selectedCode)
        composeTestRule.onNodeWithTag("verifier_autocomplete_dropdown").assertDoesNotExist()

        // Type unknown code -> shows AI custom fallback action in autocomplete
        composeTestRule.onNodeWithTag("verifier_search_input").performTextReplacement("XYZ-999")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("autocomplete_custom_submit").assertIsDisplayed()
        composeTestRule.onNodeWithTag("close_autocomplete_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("verifier_autocomplete_dropdown").assertDoesNotExist()
    }
}
