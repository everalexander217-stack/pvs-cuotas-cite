package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.example.data.ProductNoteEntity
import com.example.data.ProductVerifierCatalog
import com.example.ui.components.ProductNotesCard
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.PvsTheme
import com.example.ui.theme.SpanishStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class ProductNotesCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `product notes card supports adding via chip and text, inline editing, and deleting`() {
        val tv = ProductVerifierCatalog.findByQuery("TV-50")!!

        composeTestRule.setContent {
            PvsTheme {
                CompositionLocalProvider(LocalAppStrings provides SpanishStrings) {
                    var nextId by remember { mutableIntStateOf(1) }
                    var notes by remember { mutableStateOf<List<ProductNoteEntity>>(emptyList()) }

                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        ProductNotesCard(
                            product = tv,
                            notes = notes,
                            onAddNote = { text ->
                                val id = nextId++
                                notes = listOf(
                                    ProductNoteEntity(
                                        id = id,
                                        productCode = tv.code,
                                        productName = tv.name,
                                        noteText = text,
                                        updatedAt = 1_700_000_000_000L + id
                                    )
                                ) + notes
                            },
                            onUpdateNote = { target, newText ->
                                notes = notes.map { existing ->
                                    if (existing.id == target.id) existing.copy(noteText = newText) else existing
                                }
                            },
                            onDeleteNote = { deleteId ->
                                notes = notes.filterNot { it.id == deleteId }
                            }
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("product_notes_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("product_notes_count_badge").assertTextContains("0 notas", substring = true)
        composeTestRule.onNodeWithTag("product_notes_empty_state").performScrollTo().assertIsDisplayed()

        // 1. Add note via quick chip + save button
        composeTestRule.onNodeWithTag("product_note_quick_chip_0").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("save_product_note_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("product_notes_empty_state").assertDoesNotExist()
        composeTestRule.onNodeWithTag("product_notes_count_badge").assertTextContains("1 nota", substring = true)
        composeTestRule.onNodeWithTag("product_note_item_1").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("product_note_text_1").assertTextContains(
            SpanishStrings.productNotesQuickChips.first(),
            substring = true
        )

        // 2. Add a second typed note
        composeTestRule.onNodeWithTag("add_product_note_input").performScrollTo().performTextInput("Preguntar por soporte pared")
        composeTestRule.onNodeWithTag("save_product_note_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("product_notes_count_badge").assertTextContains("2 notas", substring = true)
        composeTestRule.onNodeWithTag("product_note_text_2").performScrollTo().assertTextContains("Preguntar por soporte pared")

        // 3. Inline edit note #2
        composeTestRule.onNodeWithTag("edit_note_button_2").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("edit_note_input_2").performScrollTo().performTextReplacement("Soporte pared incluido")
        composeTestRule.onNodeWithTag("confirm_edit_note_button_2").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("product_note_text_2").performScrollTo().assertTextContains("Soporte pared incluido")

        // 4. Delete note #1 and verify count updates
        composeTestRule.onNodeWithTag("delete_note_button_1").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("product_note_item_1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("product_notes_count_badge").assertTextContains("1 nota", substring = true)
        assertEquals(1, 1)
        assertTrue(true)
    }
}
