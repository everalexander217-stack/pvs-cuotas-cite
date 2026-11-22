package com.example

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.CategoryCatalog
import com.example.data.CategoryMetricMode
import com.example.data.InstallmentEntity
import com.example.data.InstallmentRepository
import com.example.ui.PvsUiState
import com.example.ui.PvsViewModel
import com.example.ui.components.CategoryPieChartCard
import com.example.ui.screens.MisCuotasScreen
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.PvsTheme
import com.example.ui.theme.SpanishStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class CategoryDistributionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleInstallments(): List<InstallmentEntity> = listOf(
        InstallmentEntity(
            id = 1,
            title = "Smart TV 50\"",
            code = "TV-50",
            category = "Tecnología",
            initialPaid = 100.0,
            totalAmount = 400.0,
            biweeklyQuota = 50.0,
            totalQuotas = 6,
            paidQuotas = 2,
            nextPaymentDate = "25 Nov 2026",
            sortOrder = 0
        ),
        InstallmentEntity(
            id = 2,
            title = "Colchón Ortopédico",
            code = "HOG-01",
            category = "Hogar",
            initialPaid = 50.0,
            totalAmount = 300.0,
            biweeklyQuota = 30.0,
            totalQuotas = 8,
            paidQuotas = 1,
            nextPaymentDate = "28 Nov 2026",
            sortOrder = 1
        ),
        InstallmentEntity(
            id = 3,
            title = "Consulta Dental",
            code = "SAL-02",
            category = "Salud",
            initialPaid = 20.0,
            totalAmount = 120.0,
            biweeklyQuota = 20.0,
            totalQuotas = 5,
            paidQuotas = 0,
            nextPaymentDate = "01 Dic 2026",
            sortOrder = 2
        ),
        InstallmentEntity(
            id = 4,
            title = "Monitor Pagado",
            code = "MON-99",
            category = "Tecnología",
            initialPaid = 50.0,
            totalAmount = 200.0,
            biweeklyQuota = 25.0,
            totalQuotas = 4,
            paidQuotas = 4,
            nextPaymentDate = "Completado",
            sortOrder = 3
        )
    )

    @Test
    fun `computeDistribution aggregates biweekly and total spending by normalized category`() {
        val list = sampleInstallments()

        val biweekly = CategoryCatalog.computeDistribution(list, CategoryMetricMode.BIWEEKLY)
        assertEquals(3, biweekly.size)
        assertEquals("Tecnología", biweekly[0].category)
        assertEquals(50.0, biweekly[0].amount, 0.001)
        assertEquals(0.50f, biweekly[0].percentage, 0.001f)
        assertEquals("Hogar", biweekly[1].category)
        assertEquals(30.0, biweekly[1].amount, 0.001)
        assertEquals("Salud", biweekly[2].category)
        assertEquals(20.0, biweekly[2].amount, 0.001)

        val total = CategoryCatalog.computeDistribution(list, CategoryMetricMode.TOTAL)
        assertEquals("Tecnología", total[0].category)
        assertEquals(600.0, total[0].amount, 0.001)
        assertEquals(2, total[0].planCount)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `viewmodel updateInstallmentCategory updates database and exposes custom category`() = runTest {
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
                    recentSearchDao = db.recentSearchDao()
                )
                val vm = PvsViewModel(repository = repo)

                val initial = vm.uiState.first { it.installments.isNotEmpty() }
                val firstItem = initial.installments.first()

                vm.updateInstallmentCategory(firstItem.id, "  Salud Familiar  ")
                val updated = vm.uiState.first { state ->
                    state.installments.any { it.id == firstItem.id && it.category == "Salud Familiar" }
                }

                assertTrue(updated.availableCategories.contains("Salud Familiar"))
            } finally {
                db.close()
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `dashboard pie chart card displays categories toggles metrics and updates selection`() {
        val items = sampleInstallments()

        composeTestRule.setContent {
            PvsTheme(darkTheme = false) {
                CompositionLocalProvider(LocalAppStrings provides SpanishStrings) {
                    CategoryPieChartCard(installments = items)
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("category_pie_chart_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_pie_canvas_box").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pie_legend_item_Tecnología").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pie_legend_item_Hogar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pie_legend_item_Salud").assertIsDisplayed()

        // Default selection is top slice (Tecnología $50.00)
        composeTestRule.onNodeWithTag("pie_selected_summary")
            .assertTextContains("Tecnología", substring = true)

        // Tap Salud legend row -> updates summary banner
        composeTestRule.onNodeWithTag("pie_legend_item_Salud").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pie_selected_summary")
            .assertTextContains("Salud", substring = true)

        // Toggle Total metric
        composeTestRule.onNodeWithTag("pie_metric_total").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pie_selected_summary")
            .assertTextContains("Total", substring = true)
    }

    @Test
    fun `mis cuotas category badge opens custom category picker and saves new tag`() {
        val item = sampleInstallments().first()
        var capturedId = -1
        var capturedCategory = ""

        composeTestRule.setContent {
            PvsTheme(darkTheme = true) {
                CompositionLocalProvider(LocalAppStrings provides SpanishStrings) {
                    MisCuotasScreen(
                        state = PvsUiState(
                            installments = listOf(item),
                            isLoading = false
                        ),
                        onToggleExpand = {},
                        onMoveInMemory = { _, _ -> },
                        onCommitOrder = {},
                        onMoveStep = { _, _ -> },
                        onRegisterPayment = {},
                        onDeleteInstallment = {},
                        onShowAddDialog = {},
                        onAddCustomInstallment = { _, _, _, _, _, _, _ -> },
                        onRestoreDefaults = {},
                        onToggleLanguage = {},
                        onOpenNotifications = {},
                        onUpdateCategory = { id, cat ->
                            capturedId = id
                            capturedCategory = cat
                        }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("category_badge_TV-50")
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("category_picker_input")
            .performTextReplacement("Mi Etiqueta")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("category_preset_chip_Salud")
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("category_picker_save_button")
            .performClick()

        composeTestRule.waitForIdle()
        assertEquals(item.id, capturedId)
        assertEquals("Salud", capturedCategory)
    }
}
