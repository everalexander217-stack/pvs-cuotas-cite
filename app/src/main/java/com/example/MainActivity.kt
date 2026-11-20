package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.ProductAiRepository
import com.example.ai.configureAppCheck
import com.example.data.AppDatabase
import com.example.data.InstallmentRepository
import com.example.notifications.QuotaNotificationHelper
import com.example.notifications.WorkManagerReminderCoordinator
import com.example.ui.PvsTab
import com.example.ui.PvsUiState
import com.example.ui.PvsViewModel
import com.example.ui.components.PvsInAppNotificationBanner
import com.example.ui.components.PvsLogoMark
import com.example.ui.components.PvsNotificationsBottomSheet
import com.example.ui.screens.InicioScreen
import com.example.ui.screens.MisCuotasScreen
import com.example.ui.screens.PvsIntroSplashScreen
import com.example.ui.screens.PvsTutorialScreen
import com.example.ui.screens.VerificadorScreen
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.EnglishStrings
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.PvsForestDark
import com.example.ui.theme.PvsForestGreen
import com.example.ui.theme.PvsMintContainer
import com.example.ui.theme.PvsTheme
import com.example.ui.theme.SpanishStrings
import kotlinx.coroutines.launch

private enum class PvsAppStage {
    INTRO,
    TUTORIAL,
    MAIN
}

class MainActivity : ComponentActivity() {
    private var activeViewModel: PvsViewModel? = null
    private var openMainImmediately: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // REQUIRED FIRST: Configure Firebase App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        val prefs = getSharedPreferences(QuotaNotificationHelper.PREFS_NAME, Context.MODE_PRIVATE)
        val hasSeenTutorial = prefs.getBoolean("tutorial_completed", false)
        val launchFromNotification = hasNotificationLaunchAction(intent)
        openMainImmediately = launchFromNotification

        QuotaNotificationHelper.ensureChannel(applicationContext)

        val database = AppDatabase.getInstance(applicationContext)
        val repository = InstallmentRepository(
            dao = database.installmentDao(),
            recentSearchDao = database.recentSearchDao(),
            priceTargetDao = database.priceTargetDao(),
            scannedPriceDao = database.scannedPriceDao()
        )
        val aiRepository = ProductAiRepository()
        val reminderCoordinator = WorkManagerReminderCoordinator(applicationContext)

        setContent {
            PvsTheme {
                val viewModel: PvsViewModel = viewModel(
                    factory = PvsViewModel.Factory(
                        repository = repository,
                        aiRepository = aiRepository,
                        sharedPrefs = prefs,
                        reminderCoordinator = reminderCoordinator
                    )
                )
                activeViewModel = viewModel

                androidx.compose.runtime.LaunchedEffect(intent) {
                    handleNotificationIntent(intent, viewModel)
                }

                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val strings = if (state.language == AppLanguage.ES) SpanishStrings else EnglishStrings
                var appStage by rememberSaveable {
                    mutableStateOf(if (launchFromNotification) PvsAppStage.MAIN else PvsAppStage.INTRO)
                }

                CompositionLocalProvider(LocalAppStrings provides strings) {
                    AnimatedContent(
                        targetState = appStage,
                        transitionSpec = {
                            (fadeIn(tween(460, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                                scaleIn(
                                    initialScale = 0.96f,
                                    animationSpec = tween(460, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ))
                                .togetherWith(fadeOut(tween(280)))
                        },
                        label = "pvs_app_stage_transition"
                    ) { stage ->
                        when (stage) {
                            PvsAppStage.INTRO -> PvsIntroSplashScreen(
                                onFinished = {
                                    appStage = if (hasSeenTutorial || openMainImmediately) {
                                        PvsAppStage.MAIN
                                    } else {
                                        PvsAppStage.TUTORIAL
                                    }
                                }
                            )

                            PvsAppStage.TUTORIAL -> PvsTutorialScreen(
                                language = state.language,
                                onToggleLanguage = viewModel::toggleLanguage,
                                onFinish = {
                                    prefs.edit().putBoolean("tutorial_completed", true).apply()
                                    appStage = PvsAppStage.MAIN
                                }
                            )

                            PvsAppStage.MAIN -> PvsAppShell(
                                state = state,
                                viewModel = viewModel,
                                onReplayTutorial = { appStage = PvsAppStage.TUTORIAL }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (hasNotificationLaunchAction(intent)) {
            openMainImmediately = true
            activeViewModel?.let { handleNotificationIntent(intent, it) }
        }
    }

    private fun hasNotificationLaunchAction(launchIntent: Intent?): Boolean {
        if (launchIntent == null) return false
        return launchIntent.hasExtra(QuotaNotificationHelper.EXTRA_FOCUS_INSTALLMENT_ID) ||
            launchIntent.getBooleanExtra(QuotaNotificationHelper.EXTRA_OPEN_NOTIFICATIONS_SHEET, false)
    }

    private fun handleNotificationIntent(launchIntent: Intent?, viewModel: PvsViewModel) {
        if (launchIntent == null) return
        val focusId = launchIntent.getIntExtra(QuotaNotificationHelper.EXTRA_FOCUS_INSTALLMENT_ID, -1)
        if (focusId != -1) {
            viewModel.focusInstallmentFromAlert(focusId)
            launchIntent.removeExtra(QuotaNotificationHelper.EXTRA_FOCUS_INSTALLMENT_ID)
        } else if (launchIntent.getBooleanExtra(QuotaNotificationHelper.EXTRA_OPEN_NOTIFICATIONS_SHEET, false)) {
            viewModel.selectTab(PvsTab.MIS_CUOTAS)
            viewModel.setShowNotificationsSheet(true)
            launchIntent.removeExtra(QuotaNotificationHelper.EXTRA_OPEN_NOTIFICATIONS_SHEET)
        }
    }
}

@Composable
fun PvsAppShell(
    state: PvsUiState,
    viewModel: PvsViewModel,
    onReplayTutorial: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 840.dp
        val isMedium = maxWidth >= 600.dp && maxWidth < 840.dp

        if (!isExpanded && state.selectedTab != PvsTab.INICIO) {
            BackHandler {
                viewModel.selectTab(PvsTab.INICIO)
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!isMedium && !isExpanded) {
                    PvsBottomBar(
                        selectedTab = state.selectedTab,
                        onSelectTab = viewModel::selectTab
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                PvsInAppNotificationBanner(
                    alerts = state.upcomingAlerts,
                    visible = !state.isBannerDismissed && state.upcomingAlerts.isNotEmpty(),
                    onReviewClick = { alert ->
                        viewModel.focusInstallmentFromAlert(alert.installment.id)
                    },
                    onDismissClick = viewModel::dismissBanner,
                    modifier = Modifier.fillMaxWidth()
                )

                when {
                    isExpanded -> {
                        Row(modifier = Modifier.fillMaxSize()) {
                            PvsNavigationRail(
                                selectedTab = state.selectedTab,
                                onSelectTab = viewModel::selectTab
                            )
                            if (state.selectedTab == PvsTab.INICIO) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    InicioScreen(
                                        state = state,
                                        onGoToScan = { viewModel.selectTab(PvsTab.VERIFICADOR) },
                                        onGoToQuotas = { viewModel.selectTab(PvsTab.MIS_CUOTAS) },
                                        onQuickSelectCode = viewModel::openVerifierWithCode,
                                        onToggleLanguage = viewModel::toggleLanguage,
                                        onOpenNotifications = { viewModel.setShowNotificationsSheet(true) },
                                        onOpenTutorial = onReplayTutorial,
                                        modifier = Modifier.widthIn(max = 760.dp)
                                    )
                                }
                            } else {
                                MisCuotasScreen(
                                    state = state,
                                    onToggleExpand = viewModel::toggleExpanded,
                                    onMoveInMemory = viewModel::moveItemInMemory,
                                    onCommitOrder = viewModel::commitOrder,
                                    onMoveStep = viewModel::moveItemByStep,
                                    onRegisterPayment = viewModel::registerPayment,
                                    onDeleteInstallment = viewModel::deleteInstallment,
                                    onShowAddDialog = viewModel::setShowAddDialog,
                                    onAddCustomInstallment = viewModel::addCustomInstallment,
                                    onRestoreDefaults = viewModel::restoreDefaults,
                                    onToggleLanguage = viewModel::toggleLanguage,
                                    onOpenNotifications = { viewModel.setShowNotificationsSheet(true) },
                                    onOpenTutorial = onReplayTutorial,
                                    modifier = Modifier
                                        .weight(1.05f)
                                        .fillMaxHeight()
                                )
                                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                VerificadorScreen(
                                    state = state,
                                    onQueryChange = viewModel::updateSearchQuery,
                                    onSubmitSearch = viewModel::submitSearch,
                                    onSelectExample = viewModel::selectExampleCode,
                                    onClearRecentSearches = viewModel::clearRecentSearches,
                                    onUpdateBudget = viewModel::updateAvailableMoney,
                                    onAdjustBudget = viewModel::adjustAvailableMoney,
                                    onRetryAi = viewModel::retryAiAnalysis,
                                    onAddToCart = { product ->
                                        viewModel.addVerifiedProductToCuotas(product)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = strings.addedToCartSnackbar(product.name),
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    },
                                    onToggleLanguage = viewModel::toggleLanguage,
                                    onOpenNotifications = { viewModel.setShowNotificationsSheet(true) },
                                    onOpenTutorial = onReplayTutorial,
                                    onEnsureAiLoaded = viewModel::ensureAiAnalysisForVisibleProduct,
                                    onBarcodeScanned = viewModel::submitScannedBarcode,
                                    onSetPriceTarget = viewModel::setPriceTarget,
                                    onClearPriceTarget = viewModel::clearPriceTarget,
                                    onLogScannedPrice = { product, price ->
                                        viewModel.logObservedScannedPrice(product, price)
                                    },
                                    onClearScannedHistory = viewModel::clearScannedPriceHistory,
                                    modifier = Modifier
                                        .weight(0.95f)
                                        .fillMaxHeight()
                                )
                            }
                        }
                    }

                    isMedium -> {
                        Row(modifier = Modifier.fillMaxSize()) {
                            PvsNavigationRail(
                                selectedTab = state.selectedTab,
                                onSelectTab = viewModel::selectTab
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                PvsTabContent(
                                    state = state,
                                    viewModel = viewModel,
                                    onReplayTutorial = onReplayTutorial,
                                    onVerifiedAdded = { productName ->
                                        scope.launch {
                                            val res = snackbarHostState.showSnackbar(
                                                message = strings.addedToCartSnackbar(productName),
                                                actionLabel = strings.snackbarViewAction,
                                                duration = SnackbarDuration.Short
                                            )
                                            if (res == SnackbarResult.ActionPerformed) {
                                                viewModel.selectTab(PvsTab.MIS_CUOTAS)
                                            }
                                        }
                                    },
                                    modifier = Modifier.widthIn(max = 640.dp)
                                )
                            }
                        }
                    }

                    else -> {
                        PvsTabContent(
                            state = state,
                            viewModel = viewModel,
                            onReplayTutorial = onReplayTutorial,
                            onVerifiedAdded = { productName ->
                                scope.launch {
                                    val res = snackbarHostState.showSnackbar(
                                        message = strings.addedToCartSnackbar(productName),
                                        actionLabel = strings.snackbarViewAction,
                                        duration = SnackbarDuration.Short
                                    )
                                    if (res == SnackbarResult.ActionPerformed) {
                                        viewModel.selectTab(PvsTab.MIS_CUOTAS)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    if (state.showNotificationsSheet) {
        PvsNotificationsBottomSheet(
            alerts = state.upcomingAlerts,
            thresholdDays = state.alertThresholdDays,
            notificationsEnabled = state.notificationsEnabled,
            onNotificationsEnabledChange = viewModel::setNotificationsEnabled,
            onPermissionGranted = viewModel::onNotificationPermissionGranted,
            onSendTestReminder = viewModel::sendTestNotificationReminder,
            onThresholdChange = viewModel::setAlertThresholdDays,
            onReviewInstallment = viewModel::focusInstallmentFromAlert,
            onPayInstallment = viewModel::registerPayment,
            onDismiss = { viewModel.setShowNotificationsSheet(false) }
        )
    }
}

@Composable
private fun PvsTabContent(
    state: PvsUiState,
    viewModel: PvsViewModel,
    onReplayTutorial: () -> Unit,
    onVerifiedAdded: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = state.selectedTab,
        transitionSpec = {
            fadeIn(tween(220)).togetherWith(fadeOut(tween(160)))
        },
        modifier = modifier,
        label = "pvs_tab_transition"
    ) { tab ->
        when (tab) {
            PvsTab.INICIO -> InicioScreen(
                state = state,
                onGoToScan = { viewModel.selectTab(PvsTab.VERIFICADOR) },
                onGoToQuotas = { viewModel.selectTab(PvsTab.MIS_CUOTAS) },
                onQuickSelectCode = viewModel::openVerifierWithCode,
                onToggleLanguage = viewModel::toggleLanguage,
                onOpenNotifications = { viewModel.setShowNotificationsSheet(true) },
                onOpenTutorial = onReplayTutorial
            )

            PvsTab.MIS_CUOTAS -> MisCuotasScreen(
                state = state,
                onToggleExpand = viewModel::toggleExpanded,
                onMoveInMemory = viewModel::moveItemInMemory,
                onCommitOrder = viewModel::commitOrder,
                onMoveStep = viewModel::moveItemByStep,
                onRegisterPayment = viewModel::registerPayment,
                onDeleteInstallment = viewModel::deleteInstallment,
                onShowAddDialog = viewModel::setShowAddDialog,
                onAddCustomInstallment = viewModel::addCustomInstallment,
                onRestoreDefaults = viewModel::restoreDefaults,
                onToggleLanguage = viewModel::toggleLanguage,
                onOpenNotifications = { viewModel.setShowNotificationsSheet(true) },
                onOpenTutorial = onReplayTutorial,
                onGoToVerifier = { viewModel.selectTab(PvsTab.VERIFICADOR) }
            )

            PvsTab.VERIFICADOR -> VerificadorScreen(
                state = state,
                onQueryChange = viewModel::updateSearchQuery,
                onSubmitSearch = viewModel::submitSearch,
                onSelectExample = viewModel::selectExampleCode,
                onClearRecentSearches = viewModel::clearRecentSearches,
                onUpdateBudget = viewModel::updateAvailableMoney,
                onAdjustBudget = viewModel::adjustAvailableMoney,
                onRetryAi = viewModel::retryAiAnalysis,
                onAddToCart = { product ->
                    viewModel.addVerifiedProductToCuotas(product)
                    onVerifiedAdded(product.name)
                },
                onToggleLanguage = viewModel::toggleLanguage,
                onOpenNotifications = { viewModel.setShowNotificationsSheet(true) },
                onOpenTutorial = onReplayTutorial,
                onEnsureAiLoaded = viewModel::ensureAiAnalysisForVisibleProduct,
                onBarcodeScanned = viewModel::submitScannedBarcode,
                onSetPriceTarget = viewModel::setPriceTarget,
                onClearPriceTarget = viewModel::clearPriceTarget,
                onLogScannedPrice = { product, price ->
                    viewModel.logObservedScannedPrice(product, price)
                },
                onClearScannedHistory = viewModel::clearScannedPriceHistory
            )
        }
    }
}

@Composable
private fun PvsBottomBar(
    selectedTab: PvsTab,
    onSelectTab: (PvsTab) -> Unit
) {
    val strings = LocalAppStrings.current
    val haptics = LocalHapticFeedback.current
    val inicioScale by animateFloatAsState(
        targetValue = if (selectedTab == PvsTab.INICIO) 1.14f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "nav_inicio_scale"
    )
    val cuotasScale by animateFloatAsState(
        targetValue = if (selectedTab == PvsTab.MIS_CUOTAS) 1.14f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "nav_cuotas_scale"
    )
    val verifScale by animateFloatAsState(
        targetValue = if (selectedTab == PvsTab.VERIFICADOR) 1.14f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "nav_verif_scale"
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == PvsTab.INICIO,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectTab(PvsTab.INICIO)
            },
            icon = {
                Icon(
                    imageVector = if (selectedTab == PvsTab.INICIO) {
                        Icons.Rounded.Home
                    } else {
                        Icons.Outlined.Home
                    },
                    contentDescription = strings.navInicio,
                    modifier = Modifier.graphicsLayer {
                        scaleX = inicioScale
                        scaleY = inicioScale
                    }
                )
            },
            label = { Text(strings.navInicio) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PvsForestDark,
                selectedTextColor = PvsForestGreen,
                indicatorColor = PvsMintContainer
            ),
            modifier = Modifier.testTag("nav_inicio")
        )

        NavigationBarItem(
            selected = selectedTab == PvsTab.VERIFICADOR,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectTab(PvsTab.VERIFICADOR)
            },
            icon = {
                Icon(
                    imageVector = if (selectedTab == PvsTab.VERIFICADOR) {
                        Icons.Rounded.QrCodeScanner
                    } else {
                        Icons.Outlined.QrCodeScanner
                    },
                    contentDescription = strings.navVerificador,
                    modifier = Modifier.graphicsLayer {
                        scaleX = verifScale
                        scaleY = verifScale
                    }
                )
            },
            label = { Text(strings.navVerificador) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PvsForestDark,
                selectedTextColor = PvsForestGreen,
                indicatorColor = PvsMintContainer
            ),
            modifier = Modifier.testTag("nav_verificador")
        )

        NavigationBarItem(
            selected = selectedTab == PvsTab.MIS_CUOTAS,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectTab(PvsTab.MIS_CUOTAS)
            },
            icon = {
                Icon(
                    imageVector = if (selectedTab == PvsTab.MIS_CUOTAS) {
                        Icons.Rounded.ReceiptLong
                    } else {
                        Icons.Outlined.ReceiptLong
                    },
                    contentDescription = strings.navMisCuotas,
                    modifier = Modifier.graphicsLayer {
                        scaleX = cuotasScale
                        scaleY = cuotasScale
                    }
                )
            },
            label = { Text(strings.navMisCuotas) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PvsForestDark,
                selectedTextColor = PvsForestGreen,
                indicatorColor = PvsMintContainer
            ),
            modifier = Modifier.testTag("nav_mis_cuotas")
        )
    }
}

@Composable
private fun PvsNavigationRail(
    selectedTab: PvsTab,
    onSelectTab: (PvsTab) -> Unit
) {
    val strings = LocalAppStrings.current
    val haptics = LocalHapticFeedback.current
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        header = {
            PvsLogoMark(size = 40.dp, modifier = Modifier.padding(top = 12.dp))
        }
    ) {
        Spacer(modifier = Modifier.width(4.dp))
        NavigationRailItem(
            selected = selectedTab == PvsTab.INICIO,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectTab(PvsTab.INICIO)
            },
            icon = {
                Icon(
                    imageVector = if (selectedTab == PvsTab.INICIO) {
                        Icons.Rounded.Home
                    } else {
                        Icons.Outlined.Home
                    },
                    contentDescription = strings.navInicio
                )
            },
            label = { Text(strings.navInicio) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = PvsForestDark,
                selectedTextColor = PvsForestGreen,
                indicatorColor = PvsMintContainer
            )
        )
        NavigationRailItem(
            selected = selectedTab == PvsTab.VERIFICADOR,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectTab(PvsTab.VERIFICADOR)
            },
            icon = {
                Icon(
                    imageVector = if (selectedTab == PvsTab.VERIFICADOR) {
                        Icons.Rounded.QrCodeScanner
                    } else {
                        Icons.Outlined.QrCodeScanner
                    },
                    contentDescription = strings.navVerificador
                )
            },
            label = { Text(strings.navVerificador) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = PvsForestDark,
                selectedTextColor = PvsForestGreen,
                indicatorColor = PvsMintContainer
            )
        )
        NavigationRailItem(
            selected = selectedTab == PvsTab.MIS_CUOTAS,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectTab(PvsTab.MIS_CUOTAS)
            },
            icon = {
                Icon(
                    imageVector = if (selectedTab == PvsTab.MIS_CUOTAS) {
                        Icons.Rounded.ReceiptLong
                    } else {
                        Icons.Outlined.ReceiptLong
                    },
                    contentDescription = strings.navMisCuotas
                )
            },
            label = { Text(strings.navMisCuotas) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = PvsForestDark,
                selectedTextColor = PvsForestGreen,
                indicatorColor = PvsMintContainer
            )
        )
    }
}
