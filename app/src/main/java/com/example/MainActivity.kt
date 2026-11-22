package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.rounded.AutoAwesome
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.ProductAiRepository
import com.example.ai.PvsAssistantRepository
import com.example.ai.configureAppCheck
import com.example.auth.signOutUser
import com.example.data.AppDatabase
import com.example.data.InstallmentRepository
import com.example.data.cloud.FirestoreSyncRepository
import com.example.notifications.QuotaNotificationHelper
import com.example.notifications.WorkManagerReminderCoordinator
import com.example.ui.PvsTab
import com.example.ui.PvsUiState
import com.example.ui.PvsViewModel
import com.example.ui.components.CoachMarkOverlay
import com.example.ui.components.CoachMarkRegistry
import com.example.ui.components.CoachMarkStepId
import com.example.ui.components.LocalCoachMarkRegistry
import com.example.ui.components.PvsInAppNotificationBanner
import com.example.ui.components.PvsLogoMark
import com.example.ui.components.PvsNotificationsBottomSheet
import com.example.ui.screens.AsistenteScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.InicioScreen
import com.example.ui.screens.MisCuotasScreen
import com.example.ui.screens.PvsIntroSplashScreen
import com.example.ui.screens.PvsTutorialScreen
import com.example.ui.screens.VerificadorScreen
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.EnglishStrings
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.PvsTheme
import com.example.ui.theme.SpanishStrings
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

private enum class PvsAppStage {
    INTRO,
    TUTORIAL,
    AUTH,
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
            scannedPriceDao = database.scannedPriceDao(),
            productNoteDao = database.productNoteDao()
        )
        val aiRepository = ProductAiRepository()
        val assistantRepository = PvsAssistantRepository()
        val firestoreSyncRepository = FirestoreSyncRepository(applicationContext)
        val reminderCoordinator = WorkManagerReminderCoordinator(applicationContext)

        setContent {
            var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener { auth ->
                    currentUser = auth.currentUser
                }
                Firebase.auth.addAuthStateListener(listener)
                onDispose { Firebase.auth.removeAuthStateListener(listener) }
            }

            var rootDarkMode by remember {
                mutableStateOf(prefs.getBoolean(QuotaNotificationHelper.KEY_DARK_MODE, false))
            }
            var rootLanguage by remember {
                mutableStateOf(
                    if (prefs.getString(QuotaNotificationHelper.KEY_APP_LANGUAGE, AppLanguage.ES.shortCode) == AppLanguage.EN.shortCode) {
                        AppLanguage.EN
                    } else {
                        AppLanguage.ES
                    }
                )
            }

            LaunchedEffect(rootDarkMode) {
                enableEdgeToEdge(
                    statusBarStyle = if (rootDarkMode) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                    navigationBarStyle = if (rootDarkMode) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    }
                )
            }

            val rootStrings = if (rootLanguage == AppLanguage.ES) SpanishStrings else EnglishStrings

            PvsTheme(darkTheme = rootDarkMode) {
                CompositionLocalProvider(LocalAppStrings provides rootStrings) {
                    var appStage by rememberSaveable {
                        mutableStateOf(
                            when {
                                launchFromNotification && Firebase.auth.currentUser != null -> PvsAppStage.MAIN
                                launchFromNotification -> PvsAppStage.AUTH
                                else -> PvsAppStage.INTRO
                            }
                        )
                    }

                    // Keep stage synchronized when user signs out
                    LaunchedEffect(currentUser) {
                        if (currentUser == null && appStage == PvsAppStage.MAIN) {
                            activeViewModel = null
                            appStage = PvsAppStage.AUTH
                        } else if (currentUser != null && appStage == PvsAppStage.AUTH) {
                            appStage = PvsAppStage.MAIN
                        }
                    }

                    AnimatedContent(
                        targetState = appStage,
                        transitionSpec = {
                            (fadeIn(tween(420, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                                scaleIn(
                                    initialScale = 0.96f,
                                    animationSpec = tween(420, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ))
                                .togetherWith(fadeOut(tween(240)))
                        },
                        label = "pvs_app_stage_transition"
                    ) { stage ->
                        when (stage) {
                            PvsAppStage.INTRO -> PvsIntroSplashScreen(
                                onFinished = {
                                    appStage = when {
                                        currentUser == null && (hasSeenTutorial || openMainImmediately) -> PvsAppStage.AUTH
                                        hasSeenTutorial || openMainImmediately -> PvsAppStage.MAIN
                                        else -> PvsAppStage.TUTORIAL
                                    }
                                }
                            )

                            PvsAppStage.TUTORIAL -> PvsTutorialScreen(
                                language = rootLanguage,
                                onToggleLanguage = {
                                    val next = if (rootLanguage == AppLanguage.ES) AppLanguage.EN else AppLanguage.ES
                                    prefs.edit().putString(QuotaNotificationHelper.KEY_APP_LANGUAGE, next.shortCode).apply()
                                    rootLanguage = next
                                },
                                onFinish = {
                                    prefs.edit().putBoolean("tutorial_completed", true).apply()
                                    appStage = if (currentUser != null) PvsAppStage.MAIN else PvsAppStage.AUTH
                                }
                            )

                            PvsAppStage.AUTH -> AuthScreen(
                                language = rootLanguage,
                                isDarkMode = rootDarkMode,
                                onToggleLanguage = {
                                    val next = if (rootLanguage == AppLanguage.ES) AppLanguage.EN else AppLanguage.ES
                                    prefs.edit().putString(QuotaNotificationHelper.KEY_APP_LANGUAGE, next.shortCode).apply()
                                    rootLanguage = next
                                },
                                onToggleDarkMode = {
                                    val next = !rootDarkMode
                                    prefs.edit().putBoolean(QuotaNotificationHelper.KEY_DARK_MODE, next).apply()
                                    rootDarkMode = next
                                },
                                onAuthSuccess = {
                                    currentUser = Firebase.auth.currentUser
                                    if (currentUser != null) {
                                        appStage = PvsAppStage.MAIN
                                    }
                                }
                            )

                            PvsAppStage.MAIN -> {
                                val user = currentUser
                                if (user == null) {
                                    AuthScreen(
                                        language = rootLanguage,
                                        isDarkMode = rootDarkMode,
                                        onToggleLanguage = {
                                            val next = if (rootLanguage == AppLanguage.ES) AppLanguage.EN else AppLanguage.ES
                                            prefs.edit().putString(QuotaNotificationHelper.KEY_APP_LANGUAGE, next.shortCode).apply()
                                            rootLanguage = next
                                        },
                                        onToggleDarkMode = {
                                            val next = !rootDarkMode
                                            prefs.edit().putBoolean(QuotaNotificationHelper.KEY_DARK_MODE, next).apply()
                                            rootDarkMode = next
                                        },
                                        onAuthSuccess = {
                                            currentUser = Firebase.auth.currentUser
                                        }
                                    )
                                } else {
                                    val uid = user.uid
                                    key(uid) {
                                        val viewModel: PvsViewModel = viewModel(
                                            key = uid,
                                            factory = PvsViewModel.Factory(
                                                repository = repository,
                                                aiRepository = aiRepository,
                                                sharedPrefs = prefs,
                                                reminderCoordinator = reminderCoordinator,
                                                firestoreSyncRepository = firestoreSyncRepository,
                                                assistantRepository = assistantRepository,
                                                signedInUserName = user.displayName ?: "Usuario PVS",
                                                signedInUserEmail = user.email
                                            )
                                        )
                                        activeViewModel = viewModel

                                        LaunchedEffect(intent, uid) {
                                            handleNotificationIntent(intent, viewModel)
                                        }

                                        val state by viewModel.uiState.collectAsStateWithLifecycle()

                                        LaunchedEffect(state.isDarkMode, state.language) {
                                            rootDarkMode = state.isDarkMode
                                            rootLanguage = state.language
                                        }

                                        val strings = if (state.language == AppLanguage.ES) SpanishStrings else EnglishStrings
                                        CompositionLocalProvider(LocalAppStrings provides strings) {
                                            PvsAppShell(
                                                state = state,
                                                viewModel = viewModel,
                                                onReplayTutorial = { viewModel.startCoachMarks() },
                                                onSignedOut = {
                                                    activeViewModel = null
                                                    currentUser = null
                                                    appStage = PvsAppStage.AUTH
                                                }
                                            )
                                        }
                                    }
                                }
                            }
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
    onReplayTutorial: () -> Unit = {},
    onSignedOut: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val coachMarkRegistry = remember { CoachMarkRegistry() }

    CompositionLocalProvider(LocalCoachMarkRegistry provides coachMarkRegistry) {
        Box(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isExpanded = maxWidth >= 840.dp
                val isMedium = maxWidth >= 600.dp && maxWidth < 840.dp

                if (!isExpanded && state.selectedTab != PvsTab.INICIO && !state.coachMarkActive) {
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
                            when (state.selectedTab) {
                                PvsTab.INICIO -> {
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
                                }

                                PvsTab.ASISTENTE -> {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        AsistenteScreen(
                                            state = state,
                                            onSendMessage = viewModel::sendChatMessage,
                                            onSendVoiceAudio = viewModel::sendVoiceAudio,
                                            onToggleSearchGrounding = viewModel::setChatSearchGrounding,
                                            onToggleVoiceReadout = viewModel::setChatVoiceReadout,
                                            onClearChat = viewModel::clearChatHistory,
                                            onDismissError = viewModel::dismissChatError,
                                            onToggleLanguage = viewModel::toggleLanguage,
                                            onOpenNotifications = { viewModel.setShowNotificationsSheet(true) },
                                            onOpenTutorial = onReplayTutorial,
                                            modifier = Modifier.widthIn(max = 780.dp)
                                        )
                                    }
                                }

                                else -> {
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
                                        onUpdateCategory = viewModel::updateInstallmentCategory,
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
                                        onAddProductNote = viewModel::addProductNote,
                                        onUpdateProductNote = viewModel::updateProductNote,
                                        onDeleteProductNote = viewModel::deleteProductNote,
                                        modifier = Modifier
                                            .weight(0.95f)
                                            .fillMaxHeight()
                                    )
                                }
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

        val activeCoachStep = CoachMarkStepId.entries.getOrElse(state.coachMarkStepIndex) {
            CoachMarkStepId.SCAN_ENTRY
        }
        CoachMarkOverlay(
            visible = state.coachMarkActive && !state.showNotificationsSheet && !state.showAddDialog,
            currentStepIndex = state.coachMarkStepIndex,
            activeStep = activeCoachStep,
            registry = coachMarkRegistry,
            onNext = viewModel::nextCoachMarkStep,
            onPrevious = viewModel::previousCoachMarkStep,
            onDismiss = viewModel::dismissCoachMarks,
            modifier = Modifier.fillMaxSize()
        )
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
            onDismiss = { viewModel.setShowNotificationsSheet(false) },
            isDarkMode = state.isDarkMode,
            onDarkModeChange = viewModel::setDarkMode,
            signedInUserName = state.signedInUserName,
            signedInUserEmail = state.signedInUserEmail,
            onSignOut = {
                viewModel.setShowNotificationsSheet(false)
                signOutUser(
                    credentialManager = credentialManager,
                    scope = scope,
                    onSignOutComplete = onSignedOut
                )
            },
            onStartCoachMark = viewModel::startCoachMarks
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
                onGoToVerifier = { viewModel.selectTab(PvsTab.VERIFICADOR) },
                onUpdateCategory = viewModel::updateInstallmentCategory
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
                onClearScannedHistory = viewModel::clearScannedPriceHistory,
                onAddProductNote = viewModel::addProductNote,
                onUpdateProductNote = viewModel::updateProductNote,
                onDeleteProductNote = viewModel::deleteProductNote
            )

            PvsTab.ASISTENTE -> AsistenteScreen(
                state = state,
                onSendMessage = viewModel::sendChatMessage,
                onSendVoiceAudio = viewModel::sendVoiceAudio,
                onToggleSearchGrounding = viewModel::setChatSearchGrounding,
                onToggleVoiceReadout = viewModel::setChatVoiceReadout,
                onClearChat = viewModel::clearChatHistory,
                onDismissError = viewModel::dismissChatError,
                onToggleLanguage = viewModel::toggleLanguage,
                onOpenNotifications = { viewModel.setShowNotificationsSheet(true) },
                onOpenTutorial = onReplayTutorial
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
    val asistenteScale by animateFloatAsState(
        targetValue = if (selectedTab == PvsTab.ASISTENTE) 1.14f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "nav_asistente_scale"
    )

    val navColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer
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
            label = { Text(strings.navInicio, maxLines = 1) },
            colors = navColors,
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
            label = { Text(strings.navVerificador, maxLines = 1) },
            colors = navColors,
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
            label = { Text(strings.navMisCuotas, maxLines = 1) },
            colors = navColors,
            modifier = Modifier.testTag("nav_mis_cuotas")
        )

        NavigationBarItem(
            selected = selectedTab == PvsTab.ASISTENTE,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectTab(PvsTab.ASISTENTE)
            },
            icon = {
                Icon(
                    imageVector = if (selectedTab == PvsTab.ASISTENTE) {
                        Icons.Rounded.AutoAwesome
                    } else {
                        Icons.Outlined.AutoAwesome
                    },
                    contentDescription = strings.navAsistente,
                    modifier = Modifier.graphicsLayer {
                        scaleX = asistenteScale
                        scaleY = asistenteScale
                    }
                )
            },
            label = { Text(strings.navAsistente, maxLines = 1) },
            colors = navColors,
            modifier = Modifier.testTag("nav_asistente")
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
    val railColors = NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer
    )

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
                    imageVector = if (selectedTab == PvsTab.INICIO) Icons.Rounded.Home else Icons.Outlined.Home,
                    contentDescription = strings.navInicio
                )
            },
            label = { Text(strings.navInicio) },
            colors = railColors
        )
        NavigationRailItem(
            selected = selectedTab == PvsTab.VERIFICADOR,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectTab(PvsTab.VERIFICADOR)
            },
            icon = {
                Icon(
                    imageVector = if (selectedTab == PvsTab.VERIFICADOR) Icons.Rounded.QrCodeScanner else Icons.Outlined.QrCodeScanner,
                    contentDescription = strings.navVerificador
                )
            },
            label = { Text(strings.navVerificador) },
            colors = railColors
        )
        NavigationRailItem(
            selected = selectedTab == PvsTab.MIS_CUOTAS,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectTab(PvsTab.MIS_CUOTAS)
            },
            icon = {
                Icon(
                    imageVector = if (selectedTab == PvsTab.MIS_CUOTAS) Icons.Rounded.ReceiptLong else Icons.Outlined.ReceiptLong,
                    contentDescription = strings.navMisCuotas
                )
            },
            label = { Text(strings.navMisCuotas) },
            colors = railColors
        )
        NavigationRailItem(
            selected = selectedTab == PvsTab.ASISTENTE,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectTab(PvsTab.ASISTENTE)
            },
            icon = {
                Icon(
                    imageVector = if (selectedTab == PvsTab.ASISTENTE) Icons.Rounded.AutoAwesome else Icons.Outlined.AutoAwesome,
                    contentDescription = strings.navAsistente
                )
            },
            label = { Text(strings.navAsistente) },
            colors = railColors
        )
    }
}
