package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.InstallmentRepository
import com.example.notifications.QuotaNotificationHelper
import com.example.ui.PvsTab
import com.example.ui.PvsViewModel
import com.example.ui.components.CoachMarkOverlay
import com.example.ui.components.CoachMarkRegistry
import com.example.ui.components.CoachMarkStepId
import com.example.ui.components.LocalCoachMarkRegistry
import com.example.ui.components.PvsNotificationsBottomSheet
import com.example.ui.components.coachMarkTarget
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class CoachMarkOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `viewmodel auto-starts coach marks for new user and persists completion across restart`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val prefs = context.getSharedPreferences("test_coach_mark_prefs", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()

            val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            try {
                val repo = InstallmentRepository(
                    dao = db.installmentDao(),
                    recentSearchDao = db.recentSearchDao()
                )
                val vm = PvsViewModel(
                    repository = repo,
                    sharedPrefs = prefs
                )

                val step0 = vm.uiState.first { it.coachMarkActive }
                assertEquals(0, step0.coachMarkStepIndex)
                assertEquals(PvsTab.INICIO, step0.selectedTab)

                vm.nextCoachMarkStep()
                val step1 = vm.uiState.first { it.coachMarkStepIndex == 1 }
                assertEquals(PvsTab.VERIFICADOR, step1.selectedTab)

                vm.nextCoachMarkStep()
                val step2 = vm.uiState.first { it.coachMarkStepIndex == 2 }
                assertEquals(PvsTab.MIS_CUOTAS, step2.selectedTab)

                vm.previousCoachMarkStep()
                val backToStep1 = vm.uiState.first { it.coachMarkStepIndex == 1 }
                assertEquals(PvsTab.VERIFICADOR, backToStep1.selectedTab)

                vm.nextCoachMarkStep()
                vm.nextCoachMarkStep()
                val step3 = vm.uiState.first { it.coachMarkStepIndex == 3 }
                assertEquals(PvsTab.MIS_CUOTAS, step3.selectedTab)

                vm.nextCoachMarkStep()
                val finished = vm.uiState.first { !it.coachMarkActive }
                assertFalse(finished.coachMarkActive)
                assertTrue(prefs.getBoolean(QuotaNotificationHelper.KEY_COACH_MARK_COMPLETED, false))

                // Returning user should not automatically see coach marks again
                val returningVm = PvsViewModel(
                    repository = repo,
                    sharedPrefs = prefs
                )
                assertFalse(returningVm.uiState.value.coachMarkActive)

                // Replaying brings coach marks back at Step 0 on INICIO
                returningVm.startCoachMarks()
                val replayed = returningVm.uiState.first { it.coachMarkActive }
                assertEquals(0, replayed.coachMarkStepIndex)
                assertEquals(PvsTab.INICIO, replayed.selectedTab)
            } finally {
                db.close()
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `coach mark overlay guides through scan and quota steps and supports skip`() {
        var dismissed = false

        composeTestRule.setContent {
            val registry = remember { CoachMarkRegistry() }
            var visible by remember { mutableStateOf(true) }
            var stepIndex by remember { mutableIntStateOf(0) }
            val activeStep = CoachMarkStepId.entries[stepIndex]

            PvsTheme(darkTheme = true) {
                CompositionLocalProvider(
                    LocalAppStrings provides SpanishStrings,
                    LocalCoachMarkRegistry provides registry
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {
                            Button(
                                onClick = {},
                                modifier = Modifier.coachMarkTarget(CoachMarkStepId.SCAN_ENTRY)
                            ) {
                                Text("Scan Entry")
                            }
                            Button(
                                onClick = {},
                                modifier = Modifier.coachMarkTarget(CoachMarkStepId.SCAN_BUTTON)
                            ) {
                                Text("Scan Barcode")
                            }
                            Button(
                                onClick = {},
                                modifier = Modifier.coachMarkTarget(CoachMarkStepId.ADD_QUOTA_BUTTON)
                            ) {
                                Text("Add Quota")
                            }
                            Button(
                                onClick = {},
                                modifier = Modifier.coachMarkTarget(CoachMarkStepId.QUICK_SCAN_TO_QUOTA)
                            ) {
                                Text("Quick Scan")
                            }
                        }

                        CoachMarkOverlay(
                            visible = visible,
                            currentStepIndex = stepIndex,
                            activeStep = activeStep,
                            registry = registry,
                            onNext = {
                                if (stepIndex < CoachMarkStepId.entries.lastIndex) {
                                    stepIndex += 1
                                } else {
                                    visible = false
                                    dismissed = true
                                }
                            },
                            onPrevious = {
                                if (stepIndex > 0) stepIndex -= 1
                            },
                            onDismiss = {
                                visible = false
                                dismissed = true
                            }
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("coach_mark_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithTag("coach_mark_title")
            .assertTextEquals(SpanishStrings.coachStep1Title)

        // Advance to Step 2 (Scan Barcode Button)
        composeTestRule.onNodeWithTag("coach_mark_next_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("coach_mark_title")
            .assertTextEquals(SpanishStrings.coachStep2Title)
        composeTestRule.onNodeWithTag("coach_mark_back_button").assertIsDisplayed()

        // Advance to Step 3 (Add Quota Button)
        composeTestRule.onNodeWithTag("coach_mark_next_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("coach_mark_title")
            .assertTextEquals(SpanishStrings.coachStep3Title)

        // Skip finishes overlay
        composeTestRule.onNodeWithTag("coach_mark_skip_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(dismissed)
    }

    @Test
    fun `settings bottom sheet exposes replay coach mark button`() {
        var replayClicked = false

        composeTestRule.setContent {
            PvsTheme(darkTheme = false) {
                CompositionLocalProvider(LocalAppStrings provides SpanishStrings) {
                    PvsNotificationsBottomSheet(
                        alerts = emptyList(),
                        thresholdDays = 3,
                        notificationsEnabled = true,
                        onNotificationsEnabledChange = {},
                        onPermissionGranted = {},
                        onSendTestReminder = { true },
                        onThresholdChange = {},
                        onReviewInstallment = {},
                        onPayInstallment = {},
                        onDismiss = {},
                        onStartCoachMark = { replayClicked = true }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("start_coach_mark_button")
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()
        assertTrue(replayClicked)
    }
}
