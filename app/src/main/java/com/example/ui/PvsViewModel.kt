package com.example.ui

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.AiAnalysisUiState
import com.example.ai.AiProductAnalysis
import com.example.ai.ChatStreamEvent
import com.example.ai.ChatTurn
import com.example.ai.ProductAiRepository
import com.example.ai.PvsAssistantRepository
import com.example.data.CategoryCatalog
import com.example.data.DueDateUtils
import com.example.data.InstallmentEntity
import com.example.data.InstallmentRepository
import com.example.data.PaymentAlert
import com.example.data.PriceTargetEntity
import com.example.data.ProductCodeInfo
import com.example.data.ProductNoteEntity
import com.example.data.ProductVerifierCatalog
import com.example.data.RecentSearchEntity
import com.example.data.ScannedPriceEntity
import com.example.data.cloud.FirestoreSyncRepository
import com.example.notifications.QuotaNotificationHelper
import com.example.notifications.QuotaReminderCoordinator
import com.example.ui.components.CoachMarkStepId
import com.example.ui.theme.AppLanguage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PvsTab {
    INICIO,
    VERIFICADOR,
    MIS_CUOTAS,
    ASISTENTE
}

data class PvsUiState(
    val selectedTab: PvsTab = PvsTab.INICIO,
    val installments: List<InstallmentEntity> = emptyList(),
    val expandedIds: Set<Int> = emptySet(),
    val isLoading: Boolean = true,
    val searchQuery: String = "TV-50",
    val verifiedProduct: ProductCodeInfo? = ProductVerifierCatalog.findByQuery("TV-50"),
    val recentSearches: List<RecentSearchEntity> = emptyList(),
    val availableMoney: Double = 250.0,
    val aiAnalysisState: AiAnalysisUiState = AiAnalysisUiState.Idle,
    val showAddDialog: Boolean = false,
    val language: AppLanguage = AppLanguage.ES,
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val alertThresholdDays: Int = 3,
    val upcomingAlerts: List<PaymentAlert> = emptyList(),
    val isBannerDismissed: Boolean = false,
    val showNotificationsSheet: Boolean = false,
    val lastScannedBarcode: String? = null,
    val priceTargets: Map<String, PriceTargetEntity> = emptyMap(),
    val scannedPriceHistory: Map<String, List<ScannedPriceEntity>> = emptyMap(),
    val productNotes: Map<String, List<ProductNoteEntity>> = emptyMap(),
    val chatMessages: List<ChatTurn> = emptyList(),
    val streamingChatText: String = "",
    val isChatBusy: Boolean = false,
    val chatError: String? = null,
    val chatSearchGroundingEnabled: Boolean = true,
    val chatVoiceReadout: Boolean = false,
    val signedInUserName: String? = null,
    val signedInUserEmail: String? = null,
    val cloudSyncError: String? = null,
    val coachMarkActive: Boolean = false,
    val coachMarkStepIndex: Int = 0,
    val availableCategories: List<String> = CategoryCatalog.defaultPresets
) {
    val activeInstallments: List<InstallmentEntity>
        get() = installments.filter { !it.isCompleted }

    val totalQuincena: Double
        get() = activeInstallments.sumOf { it.biweeklyQuota }

    val freeMoney: Double
        get() = availableMoney - totalQuincena

    val nextDueDateSummary: String
        get() = activeInstallments.firstOrNull()?.nextPaymentDate ?: ""

    fun targetForProduct(code: String?): PriceTargetEntity? =
        code?.trim()?.uppercase()?.let { priceTargets[it] }

    fun scannedHistoryFor(code: String?): List<ScannedPriceEntity> =
        code?.trim()?.uppercase()?.let { scannedPriceHistory[it] }.orEmpty()

    fun notesForProduct(code: String?): List<ProductNoteEntity> =
        code?.trim()?.uppercase()?.let { productNotes[it] }.orEmpty()
}

private data class PreferenceUiSlice(
    val language: AppLanguage = AppLanguage.ES,
    val darkModeEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val alertThresholdDays: Int = 3,
    val isBannerDismissed: Boolean = false,
    val showNotificationsSheet: Boolean = false,
    val availableMoney: Double = 250.0,
    val coachMarkActive: Boolean = false,
    val coachMarkStepIndex: Int = 0
)

private data class ChatUiSlice(
    val messages: List<ChatTurn> = emptyList(),
    val streamingText: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val searchGroundingEnabled: Boolean = true,
    val voiceReadoutEnabled: Boolean = false
)

class PvsViewModel(
    private val repository: InstallmentRepository,
    private val aiRepository: ProductAiRepository = ProductAiRepository(),
    private val sharedPrefs: SharedPreferences? = null,
    private val reminderCoordinator: QuotaReminderCoordinator? = null,
    private val firestoreSyncRepository: FirestoreSyncRepository? = null,
    private val assistantRepository: PvsAssistantRepository? = null,
    private val signedInUserName: String? = null,
    private val signedInUserEmail: String? = null
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(PvsTab.INICIO)
    private val _expandedIds = MutableStateFlow<Set<Int>>(emptySet())
    private val _searchQuery = MutableStateFlow("TV-50")
    private val _customVerifiedProduct = MutableStateFlow<ProductCodeInfo?>(null)
    private val _aiAnalysisState = MutableStateFlow<AiAnalysisUiState>(AiAnalysisUiState.Idle)
    private val _showAddDialog = MutableStateFlow(false)
    private val _optimisticOrder = MutableStateFlow<List<InstallmentEntity>?>(null)
    private val _isLoaded = MutableStateFlow(false)
    private val _prefs = MutableStateFlow(loadInitialPreferences(sharedPrefs))
    private val _lastScannedBarcode = MutableStateFlow<String?>(null)
    private val _chatSlice = MutableStateFlow(ChatUiSlice())
    private val _cloudSyncError = MutableStateFlow<String?>(null)

    private val aiCache = mutableMapOf<String, AiProductAnalysis>()
    private var aiJob: Job? = null
    private var chatJob: Job? = null

    init {
        viewModelScope.launch {
            val alreadySeeded = sharedPrefs?.getBoolean("has_seeded_room", false) ?: false
            if (!alreadySeeded) {
                repository.ensureSeeded()
                sharedPrefs?.edit()?.putBoolean("has_seeded_room", true)?.apply()
            }
            _isLoaded.value = true
            val currentPrefs = _prefs.value
            reminderCoordinator?.syncSchedule(
                enabled = currentPrefs.notificationsEnabled,
                thresholdDays = currentPrefs.alertThresholdDays,
                triggerImmediateCheck = currentPrefs.notificationsEnabled
            )

            // Ensure authenticated user profile & initial cloud sync when Firestore is configured
            firestoreSyncRepository?.let { cloud ->
                val result = cloud.ensureUserProfile(
                    displayName = signedInUserName ?: "Usuario PVS",
                    email = signedInUserEmail,
                    darkMode = currentPrefs.darkModeEnabled,
                    language = currentPrefs.language
                )
                result.onFailure { err ->
                    _cloudSyncError.value = err.localizedMessage
                }
                repository.getAllInstallments().forEach { item ->
                    cloud.syncInstallment(item)
                }
            }
        }

        // Observe Cloud Firestore user profile & chat history if signed in
        firestoreSyncRepository?.let { cloud ->
            viewModelScope.launch {
                cloud.observeUserProfile()
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        _cloudSyncError.value = (firestoreError ?: error).message
                    }
                    .collect { profile ->
                        if (profile != null) {
                            val cloudDark = profile.preferredTheme == "dark"
                            if (cloudDark != _prefs.value.darkModeEnabled &&
                                sharedPrefs?.contains(QuotaNotificationHelper.KEY_DARK_MODE) != true
                            ) {
                                sharedPrefs?.edit()?.putBoolean(QuotaNotificationHelper.KEY_DARK_MODE, cloudDark)?.apply()
                                _prefs.update { it.copy(darkModeEnabled = cloudDark) }
                            }
                        }
                    }
            }

            viewModelScope.launch {
                cloud.observeChatMessages()
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        _chatSlice.update { it.copy(error = rootError.message ?: "Error al sincronizar chat") }
                    }
                    .collect { cloudMessages ->
                        val turns = cloudMessages.map { msg ->
                            ChatTurn(
                                id = msg.id.ifBlank { "turn_${msg.hashCode()}" },
                                role = msg.role,
                                text = msg.text,
                                audioTranscript = msg.audioTranscript,
                                groundingNote = msg.groundingNote
                            )
                        }
                        if (turns.isNotEmpty() || _chatSlice.value.messages.isEmpty()) {
                            _chatSlice.update { it.copy(messages = turns) }
                        }
                    }
            }
        }
    }

    val uiState: StateFlow<PvsUiState> = combine(
        _selectedTab,
        repository.installments,
        _expandedIds,
        _searchQuery,
        _showAddDialog,
        _optimisticOrder,
        _isLoaded,
        _prefs,
        repository.recentSearches,
        _customVerifiedProduct,
        _aiAnalysisState,
        _lastScannedBarcode,
        repository.priceTargets,
        repository.scannedPrices,
        repository.productNotes,
        _chatSlice,
        _cloudSyncError
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val tab = values[0] as PvsTab
        @Suppress("UNCHECKED_CAST")
        val dbList = values[1] as List<InstallmentEntity>
        @Suppress("UNCHECKED_CAST")
        val expanded = values[2] as Set<Int>
        val query = values[3] as String
        val showAdd = values[4] as Boolean
        @Suppress("UNCHECKED_CAST")
        val optimistic = values[5] as List<InstallmentEntity>?
        val loaded = values[6] as Boolean
        val prefs = values[7] as PreferenceUiSlice
        @Suppress("UNCHECKED_CAST")
        val recents = values[8] as List<RecentSearchEntity>
        val customProduct = values[9] as ProductCodeInfo?
        val aiState = values[10] as AiAnalysisUiState
        val scannedBarcode = values[11] as String?
        @Suppress("UNCHECKED_CAST")
        val targetList = values[12] as List<PriceTargetEntity>
        @Suppress("UNCHECKED_CAST")
        val scannedPricesList = values[13] as List<ScannedPriceEntity>
        @Suppress("UNCHECKED_CAST")
        val notesList = values[14] as List<ProductNoteEntity>
        val chat = values[15] as ChatUiSlice
        val cloudError = values[16] as String?

        val displayedList = optimistic ?: dbList
        val alerts = DueDateUtils.computeUpcomingAlerts(
            installments = displayedList,
            thresholdDays = prefs.alertThresholdDays
        )
        val catalogMatch = ProductVerifierCatalog.findByQuery(query)
        val activeProduct = catalogMatch ?: customProduct

        PvsUiState(
            selectedTab = tab,
            installments = displayedList,
            expandedIds = expanded,
            isLoading = !loaded && dbList.isEmpty(),
            searchQuery = query,
            verifiedProduct = activeProduct,
            recentSearches = recents,
            availableMoney = prefs.availableMoney,
            aiAnalysisState = aiState,
            showAddDialog = showAdd,
            language = prefs.language,
            isDarkMode = prefs.darkModeEnabled,
            notificationsEnabled = prefs.notificationsEnabled,
            alertThresholdDays = prefs.alertThresholdDays,
            upcomingAlerts = alerts,
            isBannerDismissed = prefs.isBannerDismissed,
            showNotificationsSheet = prefs.showNotificationsSheet,
            lastScannedBarcode = scannedBarcode,
            priceTargets = targetList.associateBy { it.code.uppercase() },
            scannedPriceHistory = scannedPricesList
                .groupBy { it.productCode.uppercase() }
                .mapValues { (_, items) -> items.takeLast(5) },
            productNotes = notesList.groupBy { it.productCode.uppercase() },
            chatMessages = chat.messages,
            streamingChatText = chat.streamingText,
            isChatBusy = chat.busy,
            chatError = chat.error,
            chatSearchGroundingEnabled = chat.searchGroundingEnabled,
            chatVoiceReadout = chat.voiceReadoutEnabled,
            signedInUserName = signedInUserName,
            signedInUserEmail = signedInUserEmail,
            cloudSyncError = cloudError,
            coachMarkActive = prefs.coachMarkActive,
            coachMarkStepIndex = prefs.coachMarkStepIndex,
            availableCategories = CategoryCatalog.collectAvailableCategories(displayedList)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PvsUiState()
    )

    // --- Theme (Light / Night Dark Mode) ---

    fun setDarkMode(enabled: Boolean) {
        sharedPrefs?.edit()
            ?.putBoolean(QuotaNotificationHelper.KEY_DARK_MODE, enabled)
            ?.apply()
        _prefs.update { it.copy(darkModeEnabled = enabled) }
        viewModelScope.launch {
            firestoreSyncRepository?.updateThemePreference(enabled)
        }
    }

    fun toggleDarkMode() {
        setDarkMode(!_prefs.value.darkModeEnabled)
    }

    // --- Interactive Coach Mark Tutorial Layer ---

    fun startCoachMarks() {
        _selectedTab.value = tabForCoachMarkStep(0)
        _prefs.update {
            it.copy(
                showNotificationsSheet = false,
                coachMarkActive = true,
                coachMarkStepIndex = 0
            )
        }
    }

    fun nextCoachMarkStep() {
        val current = _prefs.value.coachMarkStepIndex
        val lastIndex = CoachMarkStepId.entries.lastIndex
        if (current >= lastIndex) {
            dismissCoachMarks()
        } else {
            val next = current + 1
            _selectedTab.value = tabForCoachMarkStep(next)
            _prefs.update { it.copy(coachMarkActive = true, coachMarkStepIndex = next) }
        }
    }

    fun previousCoachMarkStep() {
        val current = _prefs.value.coachMarkStepIndex
        if (current > 0) {
            val prev = current - 1
            _selectedTab.value = tabForCoachMarkStep(prev)
            _prefs.update { it.copy(coachMarkActive = true, coachMarkStepIndex = prev) }
        }
    }

    fun dismissCoachMarks() {
        sharedPrefs?.edit()
            ?.putBoolean(QuotaNotificationHelper.KEY_COACH_MARK_COMPLETED, true)
            ?.apply()
        _prefs.update { it.copy(coachMarkActive = false, coachMarkStepIndex = 0) }
    }

    private fun tabForCoachMarkStep(index: Int): PvsTab {
        val step = CoachMarkStepId.entries.getOrElse(index) { CoachMarkStepId.SCAN_ENTRY }
        return when (step) {
            CoachMarkStepId.SCAN_ENTRY -> PvsTab.INICIO
            CoachMarkStepId.SCAN_BUTTON -> PvsTab.VERIFICADOR
            CoachMarkStepId.ADD_QUOTA_BUTTON -> PvsTab.MIS_CUOTAS
            CoachMarkStepId.QUICK_SCAN_TO_QUOTA -> PvsTab.MIS_CUOTAS
        }
    }

    // --- Product Notes, Price Targets & Installments (Room + Cloud Sync) ---

    fun addProductNote(product: ProductCodeInfo, noteText: String) {
        viewModelScope.launch {
            val created = repository.addProductNote(product, noteText)
            if (created != null) {
                firestoreSyncRepository?.syncProductNote(created)
            }
        }
    }

    fun updateProductNote(note: ProductNoteEntity, newText: String) {
        viewModelScope.launch {
            val updated = repository.updateProductNote(note, newText)
            if (updated != null) {
                firestoreSyncRepository?.syncProductNote(updated)
            }
        }
    }

    fun deleteProductNote(noteId: Int) {
        viewModelScope.launch {
            val deleted = repository.deleteProductNote(noteId)
            if (deleted != null) {
                firestoreSyncRepository?.deleteProductNote(deleted.id, deleted.productCode)
            }
        }
    }

    fun logObservedScannedPrice(product: ProductCodeInfo, observedPrice: Double, rawBarcode: String? = null) {
        viewModelScope.launch {
            repository.recordScannedPrice(
                product = product,
                rawBarcode = rawBarcode ?: _lastScannedBarcode.value,
                priceOverride = observedPrice
            )
        }
    }

    fun clearScannedPriceHistory(code: String) {
        viewModelScope.launch {
            repository.clearScannedPricesForProduct(code)
        }
    }

    fun setPriceTarget(product: ProductCodeInfo, targetPrice: Double) {
        viewModelScope.launch {
            val saved = repository.savePriceTarget(product, targetPrice)
            if (saved != null) {
                firestoreSyncRepository?.syncPriceTarget(saved)
            }
        }
    }

    fun clearPriceTarget(code: String) {
        viewModelScope.launch {
            repository.removePriceTarget(code)
            firestoreSyncRepository?.deletePriceTarget(code)
        }
    }

    fun selectTab(tab: PvsTab) {
        _selectedTab.value = tab
    }

    fun ensureAiAnalysisForVisibleProduct() {
        if (_aiAnalysisState.value is AiAnalysisUiState.Idle) {
            uiState.value.verifiedProduct?.let { active ->
                if (ProductVerifierCatalog.findByQuery(active.code) != null) {
                    analyzeKnownProduct(active, forceRefresh = false)
                }
            }
        }
    }

    fun toggleLanguage() {
        val next = if (_prefs.value.language == AppLanguage.ES) AppLanguage.EN else AppLanguage.ES
        sharedPrefs?.edit()?.putString(QuotaNotificationHelper.KEY_APP_LANGUAGE, next.shortCode)?.apply()
        _prefs.update { it.copy(language = next) }
        viewModelScope.launch {
            firestoreSyncRepository?.updateLanguagePreference(next)
        }
        val active = uiState.value.verifiedProduct
        val shouldAnalyze = _selectedTab.value == PvsTab.VERIFICADOR ||
            _aiAnalysisState.value !is AiAnalysisUiState.Idle
        if (active != null && shouldAnalyze) {
            if (ProductVerifierCatalog.findByQuery(active.code) != null) {
                analyzeKnownProduct(active, forceRefresh = false)
            } else {
                analyzeCustomQuery(_searchQuery.value)
            }
        }
    }

    fun updateAvailableMoney(amount: Double) {
        val sanitized = amount.coerceIn(0.0, 99999.0)
        sharedPrefs?.edit()?.putFloat("available_money", sanitized.toFloat())?.apply()
        _prefs.update { it.copy(availableMoney = sanitized) }
    }

    fun adjustAvailableMoney(delta: Double) {
        updateAvailableMoney(_prefs.value.availableMoney + delta)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPrefs?.edit()
            ?.putBoolean(QuotaNotificationHelper.KEY_NOTIFICATIONS_ENABLED, enabled)
            ?.apply()
        _prefs.update {
            it.copy(
                notificationsEnabled = enabled,
                isBannerDismissed = if (enabled) false else it.isBannerDismissed
            )
        }
        reminderCoordinator?.syncSchedule(
            enabled = enabled,
            thresholdDays = _prefs.value.alertThresholdDays,
            triggerImmediateCheck = enabled
        )
    }

    fun onNotificationPermissionGranted() {
        if (_prefs.value.notificationsEnabled) {
            reminderCoordinator?.syncSchedule(
                enabled = true,
                thresholdDays = _prefs.value.alertThresholdDays,
                triggerImmediateCheck = true
            )
        }
    }

    fun sendTestNotificationReminder(): Boolean {
        val current = uiState.value
        return reminderCoordinator?.sendTestReminder(
            installments = current.installments,
            thresholdDays = current.alertThresholdDays,
            language = current.language
        ) ?: false
    }

    fun setAlertThresholdDays(days: Int) {
        val coerced = days.coerceIn(1, 14)
        sharedPrefs?.edit()
            ?.putInt(QuotaNotificationHelper.KEY_ALERT_THRESHOLD_DAYS, coerced)
            ?.apply()
        _prefs.update {
            it.copy(
                alertThresholdDays = coerced,
                isBannerDismissed = false
            )
        }
        if (_prefs.value.notificationsEnabled) {
            reminderCoordinator?.syncSchedule(
                enabled = true,
                thresholdDays = coerced,
                triggerImmediateCheck = true
            )
        }
    }

    fun dismissBanner() {
        _prefs.update { it.copy(isBannerDismissed = true) }
    }

    fun setShowNotificationsSheet(show: Boolean) {
        _prefs.update { it.copy(showNotificationsSheet = show) }
    }

    fun focusInstallmentFromAlert(installmentId: Int) {
        _selectedTab.value = PvsTab.MIS_CUOTAS
        _expandedIds.update { it + installmentId }
        _prefs.update { it.copy(showNotificationsSheet = false) }
    }

    fun toggleExpanded(id: Int) {
        _expandedIds.update { current ->
            if (id in current) current - id else current + id
        }
    }

    fun moveItemInMemory(fromIndex: Int, toIndex: Int) {
        val currentList = (_optimisticOrder.value ?: uiState.value.installments).toMutableList()
        if (fromIndex !in currentList.indices || toIndex !in currentList.indices || fromIndex == toIndex) {
            return
        }
        val moved = currentList.removeAt(fromIndex)
        currentList.add(toIndex, moved)
        _optimisticOrder.value = currentList
    }

    fun commitOrder() {
        val current = _optimisticOrder.value ?: return
        viewModelScope.launch {
            repository.persistOrder(current)
            _optimisticOrder.value = null
            current.forEachIndexed { idx, item ->
                firestoreSyncRepository?.syncInstallment(item.copy(sortOrder = idx))
            }
        }
    }

    fun moveItemByStep(id: Int, delta: Int) {
        val list = uiState.value.installments.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index == -1) return
        val target = (index + delta).coerceIn(0, list.lastIndex)
        if (target == index) return
        val item = list.removeAt(index)
        list.add(target, item)
        viewModelScope.launch {
            repository.persistOrder(list)
            list.forEachIndexed { idx, entry ->
                firestoreSyncRepository?.syncInstallment(entry.copy(sortOrder = idx))
            }
        }
    }

    fun registerPayment(item: InstallmentEntity) {
        viewModelScope.launch {
            val updated = repository.registerNextPayment(item)
            if (updated != null) {
                firestoreSyncRepository?.syncInstallment(updated)
            }
            if (_prefs.value.notificationsEnabled) {
                reminderCoordinator?.checkNow(force = false)
            }
        }
    }

    fun updateInstallmentCategory(installmentId: Int, category: String) {
        viewModelScope.launch {
            val updated = repository.updateInstallmentCategory(installmentId, category)
            if (updated != null) {
                firestoreSyncRepository?.syncInstallment(updated)
            }
        }
    }

    fun deleteInstallment(id: Int) {
        viewModelScope.launch {
            val deleted = repository.deleteInstallment(id)
            if (deleted != null) {
                firestoreSyncRepository?.deleteInstallment(deleted)
            }
            _expandedIds.update { it - id }
            if (_prefs.value.notificationsEnabled) {
                reminderCoordinator?.checkNow(force = false)
            }
        }
    }

    fun restoreDefaults() {
        viewModelScope.launch {
            repository.restoreDefaultsIfEmpty()
            repository.getAllInstallments().forEach { item ->
                firestoreSyncRepository?.syncInstallment(item)
            }
            if (_prefs.value.notificationsEnabled) {
                reminderCoordinator?.checkNow(force = false)
            }
        }
    }

    fun setShowAddDialog(show: Boolean) {
        _showAddDialog.value = show
    }

    fun addCustomInstallment(
        title: String,
        code: String,
        category: String,
        initialPaid: Double,
        totalAmount: Double,
        totalQuotas: Int,
        nextPaymentDate: String
    ) {
        viewModelScope.launch {
            val created = repository.addInstallment(
                title = title,
                code = code,
                category = category,
                initialPaid = initialPaid,
                totalAmount = totalAmount,
                totalQuotas = totalQuotas,
                nextPaymentDate = nextPaymentDate
            )
            firestoreSyncRepository?.syncInstallment(created)
            _showAddDialog.value = false
            if (_prefs.value.notificationsEnabled) {
                reminderCoordinator?.checkNow(force = false)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _lastScannedBarcode.value = null
        _searchQuery.value = query
        if (query.isBlank()) {
            _customVerifiedProduct.value = null
            _aiAnalysisState.value = AiAnalysisUiState.Idle
            return
        }
        val trimmed = query.trim()
        val match = ProductVerifierCatalog.findByQuery(trimmed)
        if (match != null) {
            _customVerifiedProduct.value = null
            val compactInput = trimmed.uppercase().replace("-", "").replace(" ", "")
            val compactCode = match.code.uppercase().replace("-", "")
            if (compactInput == compactCode) {
                viewModelScope.launch {
                    repository.recordRecentSearch(match)
                }
                analyzeKnownProduct(match)
            }
        }
    }

    fun submitSearch() {
        _lastScannedBarcode.value = null
        executeProductLookup(_searchQuery.value.trim(), isCameraScan = false, rawBarcode = null)
    }

    fun submitScannedBarcode(rawBarcode: String) {
        val trimmedRaw = rawBarcode.trim()
        val resolved = ProductVerifierCatalog.resolveBarcodeValue(trimmedRaw)
        if (resolved.isEmpty()) return
        _lastScannedBarcode.value = trimmedRaw
        _searchQuery.value = resolved
        _customVerifiedProduct.value = null
        executeProductLookup(resolved, isCameraScan = true, rawBarcode = trimmedRaw)
    }

    private fun executeProductLookup(
        raw: String,
        isCameraScan: Boolean = false,
        rawBarcode: String? = null
    ) {
        if (raw.isEmpty()) return
        val match = ProductVerifierCatalog.findByQuery(raw)
        if (match != null) {
            _customVerifiedProduct.value = null
            viewModelScope.launch {
                repository.recordRecentSearch(match)
                if (isCameraScan || uiState.value.scannedHistoryFor(match.code).isEmpty()) {
                    repository.recordScannedPrice(match, rawBarcode = rawBarcode)
                }
            }
            analyzeKnownProduct(match)
        } else {
            analyzeCustomQuery(raw, isCameraScan = isCameraScan, rawBarcode = rawBarcode)
        }
    }

    fun selectExampleCode(code: String) {
        _lastScannedBarcode.value = null
        _searchQuery.value = code
        _customVerifiedProduct.value = null
        val match = ProductVerifierCatalog.findByQuery(code)
        if (match != null) {
            viewModelScope.launch {
                repository.recordRecentSearch(match)
                if (uiState.value.scannedHistoryFor(match.code).isEmpty()) {
                    repository.recordScannedPrice(match)
                }
            }
            analyzeKnownProduct(match)
        } else {
            val recentMatch = uiState.value.recentSearches.firstOrNull {
                it.code.equals(code, ignoreCase = true)
            }
            if (recentMatch != null) {
                _searchQuery.value = recentMatch.productName
                analyzeCustomQuery(recentMatch.productName)
            }
        }
    }

    fun openVerifierWithCode(code: String) {
        selectExampleCode(code)
        _selectedTab.value = PvsTab.VERIFICADOR
    }

    fun retryAiAnalysis() {
        val product = uiState.value.verifiedProduct
        if (product != null && ProductVerifierCatalog.findByQuery(product.code) != null) {
            analyzeKnownProduct(product, forceRefresh = true)
        } else if (_searchQuery.value.isNotBlank()) {
            analyzeCustomQuery(_searchQuery.value)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            repository.clearRecentSearches()
        }
    }

    fun addVerifiedProductToCuotas(product: ProductCodeInfo) {
        viewModelScope.launch {
            val created = repository.addInstallment(
                title = product.name,
                code = product.code,
                category = product.category,
                initialPaid = product.suggestedInitial,
                totalAmount = product.storePrice,
                totalQuotas = product.quotasCount,
                nextPaymentDate = DueDateUtils.formatDateOffsetFromToday(3)
            )
            firestoreSyncRepository?.syncInstallment(created)
            if (_prefs.value.notificationsEnabled) {
                reminderCoordinator?.checkNow(force = false)
            }
        }
    }

    // --- Gemini Multi-Turn Chatbot + Audio Transcription + Search Grounding ---

    fun setChatSearchGrounding(enabled: Boolean) {
        _chatSlice.update { it.copy(searchGroundingEnabled = enabled) }
    }

    fun setChatVoiceReadout(enabled: Boolean) {
        _chatSlice.update { it.copy(voiceReadoutEnabled = enabled) }
    }

    fun dismissChatError() {
        _chatSlice.update { it.copy(error = null) }
    }

    fun clearChatHistory() {
        val ids = _chatSlice.value.messages.map { it.id }
        chatJob?.cancel()
        _chatSlice.update {
            it.copy(
                messages = emptyList(),
                streamingText = "",
                busy = false,
                error = null
            )
        }
        viewModelScope.launch {
            firestoreSyncRepository?.clearChatHistory(ids)
        }
    }

    fun sendChatMessage(message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return
        val assistant = assistantRepository ?: PvsAssistantRepository()
        val lang = _prefs.value.language
        val snapshotHistory = _chatSlice.value.messages
        val userTurn = ChatTurn(
            id = "local_u_${System.currentTimeMillis()}",
            role = "user",
            text = trimmed
        )

        _chatSlice.update {
            it.copy(
                messages = it.messages + userTurn,
                streamingText = "",
                busy = true,
                error = null
            )
        }

        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            firestoreSyncRepository?.addChatMessage(role = "user", text = trimmed)
            try {
                assistant.sendChatMessageStream(
                    history = snapshotHistory,
                    userMessage = trimmed,
                    useSearchGrounding = _chatSlice.value.searchGroundingEnabled,
                    contextSummary = buildFinancialContextSummary(),
                    language = lang
                ).collect { event ->
                    when (event) {
                        is ChatStreamEvent.Chunk -> {
                            _chatSlice.update { it.copy(streamingText = event.accumulatedText) }
                        }
                        is ChatStreamEvent.Completed -> {
                            val modelTurn = ChatTurn(
                                id = "local_m_${System.currentTimeMillis()}",
                                role = "model",
                                text = event.finalText,
                                groundingNote = event.groundingNote
                            )
                            _chatSlice.update { slice ->
                                slice.copy(
                                    messages = slice.messages + modelTurn,
                                    streamingText = "",
                                    busy = false
                                )
                            }
                            firestoreSyncRepository?.addChatMessage(
                                role = "model",
                                text = event.finalText,
                                groundingNote = event.groundingNote
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _chatSlice.update {
                    it.copy(
                        streamingText = "",
                        busy = false,
                        error = e.localizedMessage ?: "Error en el asistente."
                    )
                }
            }
        }
    }

    fun sendVoiceAudio(audioBytes: ByteArray, transcribeOnly: Boolean) {
        val assistant = assistantRepository ?: PvsAssistantRepository()
        val lang = _prefs.value.language

        _chatSlice.update {
            it.copy(busy = true, error = null, streamingText = "")
        }

        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            val result = assistant.transcribeAndProcessAudio(
                audioBytes = audioBytes,
                mimeType = "audio/mp4",
                transcribeOnly = transcribeOnly,
                contextSummary = buildFinancialContextSummary(),
                language = lang
            )
            result.fold(
                onSuccess = { outcome ->
                    val userTurn = ChatTurn(
                        id = "local_vu_${System.currentTimeMillis()}",
                        role = "user",
                        text = outcome.transcript,
                        audioTranscript = outcome.transcript
                    )
                    val modelTurn = ChatTurn(
                        id = "local_vm_${System.currentTimeMillis()}",
                        role = "model",
                        text = outcome.assistantReply
                    )
                    _chatSlice.update { slice ->
                        slice.copy(
                            messages = slice.messages + userTurn + modelTurn,
                            busy = false
                        )
                    }
                    firestoreSyncRepository?.addChatMessage(
                        role = "user",
                        text = outcome.transcript,
                        audioTranscript = outcome.transcript
                    )
                    firestoreSyncRepository?.addChatMessage(
                        role = "model",
                        text = outcome.assistantReply
                    )
                },
                onFailure = { error ->
                    _chatSlice.update {
                        it.copy(
                            busy = false,
                            error = error.message ?: "No se pudo procesar el audio."
                        )
                    }
                }
            )
        }
    }

    private fun buildFinancialContextSummary(): String {
        val snapshot = uiState.value
        val activeCount = snapshot.activeInstallments.size
        val activeList = snapshot.activeInstallments.take(4).joinToString(", ") {
            "${it.code}: ${it.title} (\$${String.format(java.util.Locale.US, "%.2f", it.biweeklyQuota)}/qna)"
        }
        val checkedProduct = snapshot.verifiedProduct?.let {
            "Producto en visor: ${it.code} (${it.name}, tienda \$${it.storePrice}, promedio \$${it.averagePrice})"
        } ?: "Ningún producto en visor"
        return "Disponible quincenal: \$${snapshot.availableMoney}, Comprometido: \$${snapshot.totalQuincena}, " +
            "Libre: \$${snapshot.freeMoney}, Planes activos ($activeCount): [$activeList]. $checkedProduct."
    }

    private fun analyzeKnownProduct(
        product: ProductCodeInfo,
        forceRefresh: Boolean = false
    ) {
        val lang = _prefs.value.language
        val cacheKey = "${product.code}_${lang.shortCode}"
        val cached = aiCache[cacheKey]
        if (!forceRefresh && cached != null) {
            _aiAnalysisState.value = AiAnalysisUiState.Success(cached)
            return
        }

        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _aiAnalysisState.value = AiAnalysisUiState.Loading
            val result = aiRepository.analyzeKnownProduct(product, lang)
            result.fold(
                onSuccess = { analysis ->
                    aiCache[cacheKey] = analysis
                    _aiAnalysisState.value = AiAnalysisUiState.Success(analysis)
                },
                onFailure = { error ->
                    _aiAnalysisState.value = AiAnalysisUiState.Error(
                        error.message ?: "No se pudo completar el análisis con IA."
                    )
                }
            )
        }
    }

    private fun analyzeCustomQuery(
        query: String,
        isCameraScan: Boolean = false,
        rawBarcode: String? = null
    ) {
        val lang = _prefs.value.language
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _aiAnalysisState.value = AiAnalysisUiState.Loading
            val result = aiRepository.analyzeCustomProduct(query, lang)
            result.fold(
                onSuccess = { analysis ->
                    val estimated = analysis.estimatedProduct
                    if (estimated != null) {
                        _customVerifiedProduct.value = estimated
                        repository.recordRecentSearch(estimated)
                        if (isCameraScan || uiState.value.scannedHistoryFor(estimated.code).isEmpty()) {
                            repository.recordScannedPrice(estimated, rawBarcode = rawBarcode)
                        }
                    }
                    _aiAnalysisState.value = AiAnalysisUiState.Success(analysis)
                },
                onFailure = { error ->
                    _aiAnalysisState.value = AiAnalysisUiState.Error(
                        error.message ?: "No se pudo consultar el producto con IA."
                    )
                }
            )
        }
    }

    private fun loadInitialPreferences(prefs: SharedPreferences?): PreferenceUiSlice {
        if (prefs == null) return PreferenceUiSlice()
        val langCode = prefs.getString(QuotaNotificationHelper.KEY_APP_LANGUAGE, AppLanguage.ES.shortCode)
        val language = if (langCode == AppLanguage.EN.shortCode) AppLanguage.EN else AppLanguage.ES
        val darkMode = prefs.getBoolean(QuotaNotificationHelper.KEY_DARK_MODE, false)
        val notificationsEnabled = prefs.getBoolean(QuotaNotificationHelper.KEY_NOTIFICATIONS_ENABLED, true)
        val thresholdDays = prefs.getInt(QuotaNotificationHelper.KEY_ALERT_THRESHOLD_DAYS, 3).coerceIn(1, 14)
        val availableMoney = prefs.getFloat("available_money", 250f).toDouble()
        val coachMarkCompleted = prefs.getBoolean(QuotaNotificationHelper.KEY_COACH_MARK_COMPLETED, false)
        return PreferenceUiSlice(
            language = language,
            darkModeEnabled = darkMode,
            notificationsEnabled = notificationsEnabled,
            alertThresholdDays = thresholdDays,
            availableMoney = availableMoney,
            coachMarkActive = !coachMarkCompleted,
            coachMarkStepIndex = 0
        )
    }

    class Factory(
        private val repository: InstallmentRepository,
        private val aiRepository: ProductAiRepository = ProductAiRepository(),
        private val sharedPrefs: SharedPreferences? = null,
        private val reminderCoordinator: QuotaReminderCoordinator? = null,
        private val firestoreSyncRepository: FirestoreSyncRepository? = null,
        private val assistantRepository: PvsAssistantRepository? = null,
        private val signedInUserName: String? = null,
        private val signedInUserEmail: String? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PvsViewModel(
                repository = repository,
                aiRepository = aiRepository,
                sharedPrefs = sharedPrefs,
                reminderCoordinator = reminderCoordinator,
                firestoreSyncRepository = firestoreSyncRepository,
                assistantRepository = assistantRepository,
                signedInUserName = signedInUserName,
                signedInUserEmail = signedInUserEmail
            ) as T
        }
    }
}
