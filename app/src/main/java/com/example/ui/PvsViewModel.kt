package com.example.ui

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.AiAnalysisUiState
import com.example.ai.AiProductAnalysis
import com.example.ai.ProductAiRepository
import com.example.data.DueDateUtils
import com.example.data.InstallmentEntity
import com.example.data.InstallmentRepository
import com.example.data.PaymentAlert
import com.example.data.PriceTargetEntity
import com.example.data.ProductCodeInfo
import com.example.data.ProductVerifierCatalog
import com.example.data.RecentSearchEntity
import com.example.data.ScannedPriceEntity
import com.example.notifications.QuotaNotificationHelper
import com.example.notifications.QuotaReminderCoordinator
import com.example.ui.theme.AppLanguage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PvsTab {
    INICIO,
    VERIFICADOR,
    MIS_CUOTAS
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
    val notificationsEnabled: Boolean = true,
    val alertThresholdDays: Int = 3,
    val upcomingAlerts: List<PaymentAlert> = emptyList(),
    val isBannerDismissed: Boolean = false,
    val showNotificationsSheet: Boolean = false,
    val lastScannedBarcode: String? = null,
    val priceTargets: Map<String, PriceTargetEntity> = emptyMap(),
    val scannedPriceHistory: Map<String, List<ScannedPriceEntity>> = emptyMap()
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
}

private data class PreferenceUiSlice(
    val language: AppLanguage = AppLanguage.ES,
    val notificationsEnabled: Boolean = true,
    val alertThresholdDays: Int = 3,
    val isBannerDismissed: Boolean = false,
    val showNotificationsSheet: Boolean = false,
    val availableMoney: Double = 250.0
)

class PvsViewModel(
    private val repository: InstallmentRepository,
    private val aiRepository: ProductAiRepository = ProductAiRepository(),
    private val sharedPrefs: SharedPreferences? = null,
    private val reminderCoordinator: QuotaReminderCoordinator? = null
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

    private val aiCache = mutableMapOf<String, AiProductAnalysis>()
    private var aiJob: Job? = null

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
        repository.scannedPrices
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
            notificationsEnabled = prefs.notificationsEnabled,
            alertThresholdDays = prefs.alertThresholdDays,
            upcomingAlerts = alerts,
            isBannerDismissed = prefs.isBannerDismissed,
            showNotificationsSheet = prefs.showNotificationsSheet,
            lastScannedBarcode = scannedBarcode,
            priceTargets = targetList.associateBy { it.code.uppercase() },
            scannedPriceHistory = scannedPricesList
                .groupBy { it.productCode.uppercase() }
                .mapValues { (_, items) -> items.takeLast(5) }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PvsUiState()
    )

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
            repository.savePriceTarget(product, targetPrice)
        }
    }

    fun clearPriceTarget(code: String) {
        viewModelScope.launch {
            repository.removePriceTarget(code)
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
        }
    }

    fun registerPayment(item: InstallmentEntity) {
        viewModelScope.launch {
            repository.registerNextPayment(item)
            if (_prefs.value.notificationsEnabled) {
                reminderCoordinator?.checkNow(force = false)
            }
        }
    }

    fun deleteInstallment(id: Int) {
        viewModelScope.launch {
            repository.deleteInstallment(id)
            _expandedIds.update { it - id }
            if (_prefs.value.notificationsEnabled) {
                reminderCoordinator?.checkNow(force = false)
            }
        }
    }

    fun restoreDefaults() {
        viewModelScope.launch {
            repository.restoreDefaultsIfEmpty()
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
            repository.addInstallment(
                title = title,
                code = code,
                category = category,
                initialPaid = initialPaid,
                totalAmount = totalAmount,
                totalQuotas = totalQuotas,
                nextPaymentDate = nextPaymentDate
            )
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
            repository.addInstallment(
                title = product.name,
                code = product.code,
                category = product.category,
                initialPaid = product.suggestedInitial,
                totalAmount = product.storePrice,
                totalQuotas = product.quotasCount,
                nextPaymentDate = DueDateUtils.formatDateOffsetFromToday(3)
            )
            if (_prefs.value.notificationsEnabled) {
                reminderCoordinator?.checkNow(force = false)
            }
        }
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
        val notificationsEnabled = prefs.getBoolean(QuotaNotificationHelper.KEY_NOTIFICATIONS_ENABLED, true)
        val thresholdDays = prefs.getInt(QuotaNotificationHelper.KEY_ALERT_THRESHOLD_DAYS, 3).coerceIn(1, 14)
        val availableMoney = prefs.getFloat("available_money", 250f).toDouble()
        return PreferenceUiSlice(
            language = language,
            notificationsEnabled = notificationsEnabled,
            alertThresholdDays = thresholdDays,
            availableMoney = availableMoney
        )
    }

    class Factory(
        private val repository: InstallmentRepository,
        private val aiRepository: ProductAiRepository = ProductAiRepository(),
        private val sharedPrefs: SharedPreferences? = null,
        private val reminderCoordinator: QuotaReminderCoordinator? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PvsViewModel(repository, aiRepository, sharedPrefs, reminderCoordinator) as T
        }
    }
}
