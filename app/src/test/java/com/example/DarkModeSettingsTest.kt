package com.example

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.InstallmentRepository
import com.example.notifications.QuotaNotificationHelper
import com.example.ui.PvsViewModel
import com.example.ui.components.PvsNotificationsBottomSheet
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.PvsDarkBackground
import com.example.ui.theme.PvsDarkTextPrimary
import com.example.ui.theme.PvsLightBackground
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
class DarkModeSettingsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `settings sheet renders dark_mode_switch unchecked by default and toggles dark mode`() {
        var latestDarkMode = false

        composeTestRule.setContent {
            var isDarkMode by remember { mutableStateOf(false) }
            PvsTheme(darkTheme = isDarkMode) {
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
                        isDarkMode = isDarkMode,
                        onDarkModeChange = { enabled ->
                            isDarkMode = enabled
                            latestDarkMode = enabled
                        }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dark_mode_switch")
            .performScrollTo()
            .assertIsOff()
            .performClick()

        composeTestRule.waitForIdle()
        assertTrue(latestDarkMode)
        composeTestRule.onNodeWithTag("dark_mode_switch").assertIsOn()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `viewmodel setDarkMode persists preference in SharedPreferences and updates uiState`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val prefs = context.getSharedPreferences("test_dark_mode_prefs", Context.MODE_PRIVATE)
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

                assertFalse(vm.uiState.value.isDarkMode)
                assertFalse(prefs.getBoolean(QuotaNotificationHelper.KEY_DARK_MODE, false))

                vm.setDarkMode(true)
                val darkState = vm.uiState.first { it.isDarkMode }
                assertTrue(darkState.isDarkMode)
                assertTrue(prefs.getBoolean(QuotaNotificationHelper.KEY_DARK_MODE, false))

                vm.setDarkMode(false)
                val lightState = vm.uiState.first { !it.isDarkMode }
                assertFalse(lightState.isDarkMode)
                assertFalse(prefs.getBoolean(QuotaNotificationHelper.KEY_DARK_MODE, true))
            } finally {
                db.close()
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `PvsTheme applies PvsDarkBackground and PvsDarkTextPrimary when darkTheme is true`() {
        var resolvedBg = Color.Unspecified
        var resolvedOnBg = Color.Unspecified
        var isDarkState by mutableStateOf(true)

        composeTestRule.setContent {
            PvsTheme(darkTheme = isDarkState) {
                resolvedBg = MaterialTheme.colorScheme.background
                resolvedOnBg = MaterialTheme.colorScheme.onBackground
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(PvsDarkBackground, resolvedBg)
        assertEquals(PvsDarkTextPrimary, resolvedOnBg)

        isDarkState = false
        composeTestRule.waitForIdle()
        assertEquals(PvsLightBackground, resolvedBg)
    }
}
