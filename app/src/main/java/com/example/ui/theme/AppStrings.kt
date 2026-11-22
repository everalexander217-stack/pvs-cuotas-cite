package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.PriceVerdict

enum class AppLanguage(val shortCode: String, val displayName: String) {
    ES("ES", "Español"),
    EN("EN", "English")
}

interface AppStrings {
    val language: AppLanguage
    val navInicio: String
    val navMisCuotas: String
    val navVerificador: String
    val navAsistente: String
    val brandTagline: String
    val quincenaTotalLabel: String
    val perQuincena: String
    val activePlans: (Int) -> String
    val nextDueSummary: (String) -> String
    val noPendingDue: String
    val yourPlansHeader: String
    val tapOrDragHint: String
    val addQuotaBtn: String
    val paidBadge: String
    val allQuotasCovered: String
    val quotasRemainingShort: (Int, String) -> String
    val quincenaUnit: String
    val fieldInicial: String
    val fieldCuotasRestantes: String
    val quotasOfTotal: (Int, Int) -> String
    val fieldProximaFecha: String
    val fieldProgreso: String
    val quotasProgressCount: (Int, Int, Int) -> String
    val actionPayQuota: String
    val actionCompleted: String
    val actionMoveUp: String
    val actionMoveDown: String
    val actionDelete: String
    val emptyCuotasTitle: String
    val emptyCuotasBody: String
    val loadExamples: String
    val dialogAddTitle: String
    val dialogFieldName: String
    val dialogFieldCode: String
    val dialogFieldTotal: String
    val dialogFieldInitial: String
    val dialogFieldQuotas: String
    val dialogFieldDate: String
    val actionSave: String
    val actionCancel: String
    val verifierTitle: String
    val verifierSubtitle: String
    val verifierSearchHint: String
    val verifierExamplesLabel: String
    val verifierRecentSearchesLabel: String
    val verifierClearRecent: String
    val verifierStorePrice: String
    val verifierAvgPrice: String
    val verifierMarketRef: String
    val verifierSavingsPct: (Int) -> String
    val verifierMarkupPct: (Int) -> String
    val verifierQuincenaImpact: String
    val verifierPlanSummary: (Int, String) -> String
    val verifierTotalProjected: (String) -> String
    val verifierAddToCuotas: String
    val verifierNotFoundTitle: String
    val verifierSearchPromptTitle: String
    val verifierNotFoundBody: String
    val addedToCuotasSnackbar: (String) -> String
    val snackbarViewAction: String
    val notifUpcomingTitle: String
    val notifMoreCount: (Int) -> String
    val notifDueDetail: (String, String, String) -> String
    val notifCenterTitle: String
    val notifCenterSubtitle: String
    val notifThresholdLabel: String
    val notifDaysChip: (Int) -> String
    val notifEmptyTitle: String
    val notifEmptyBody: (Int) -> String
    val notifActionReview: String
    val notifActionPay: String
    val notifDismissCd: String
    val urgencyLabel: (Int) -> String
    val verdictHeadline: (PriceVerdict) -> String
    val verdictReason: (String, String) -> String
    val replayTutorialCd: String
    val tutorialSkip: String
    val tutorialNext: String
    val tutorialStart: String
    val tutorialStep1Title: String
    val tutorialStep1Body: String
    val tutorialStep2Title: String
    val tutorialStep2Body: String
    val tutorialStep3Title: String
    val tutorialStep3Body: String
    val quickBuyPromptCta: String
    val verifierHeroTitle: String
    val verifierHeroSubtitle: String
    val verifierSearchAction: String
    val budgetTitle: String
    val budgetTotalLabel: String
    val budgetCommittedLabel: String
    val budgetFreeLabel: String
    val budgetFitsGreat: (String) -> String
    val budgetTightWarning: (String) -> String
    val budgetEditHint: String
    val aiSectionTitle: String
    val aiLoadingText: String
    val aiPowerLabel: String
    val aiDurabilityLabel: String
    val aiSummaryLabel: String
    val aiRetryBtn: String
    val addToCartCta: String
    val addedToCartSnackbar: (String) -> String
    val homeWelcomeTitle: String
    val homeWelcomeSubtitle: String
    val homeMainOptionsLabel: String
    val homeScanCardTitle: String
    val homeScanCardDesc: String
    val homeScanCardBadge: String
    val homeQuotasCardTitle: String
    val homeQuotasCardDesc: String
    val homeQuotasCardBadge: (Int) -> String
    val homeQuickCodesTitle: String
    val homeSummaryQuincena: String
    val homeSummaryAvailable: String
    val notifChannelName: String
    val notifChannelDesc: String
    val notifSystemMultiTitle: (Int) -> String
    val notifSystemToggleTitle: String
    val notifSystemToggleSubtitle: String
    val notifPermissionNeededTitle: String
    val notifPermissionNeededBody: String
    val notifPermissionGrantBtn: String
    val notifPermissionSettingsBtn: String
    val notifTestReminderBtn: String
    val notifTestSentFeedback: String
    val notifTestFailedFeedback: String
    val historyTitle: String
    val historySubtitle: String
    val historyMinLabel: String
    val historyMaxLabel: String
    val historyTrendLabel: String
    val historyLegendStore: String
    val historyLegendMarket: String
    val historyTouchHint: String
    val historyDeltaVsMarket: (String) -> String
    val scannerOpenBtn: String
    val scannerTitle: String
    val scannerSubtitle: String
    val scannerFrameHint: String
    val scannerPermissionTitle: String
    val scannerPermissionBody: String
    val scannerPermissionGrantBtn: String
    val scannerPermissionSettingsBtn: String
    val scannerTorchCd: String
    val scannerSwitchCameraCd: String
    val scannerCloseCd: String
    val scannerCameraError: String
    val scannedCardPassBadge: String
    val scannedCardLiveScan: String
    val scannedCardVerified: String
    val scannedCardCodeLabel: String
    val scannedCardPriceLabel: String
    val scannedCardBiweeklyQuotaLabel: String
    val scannedCardPlanDetail: (Int, String) -> String
    val scannedCardRescanBtn: String
    val scannedCardQuickAddBtn: String
    val shareProductBtn: String
    val shareInstallmentBtn: String
    val shareChooserTitle: String
    val shareSubjectProduct: (String) -> String
    val shareCardHeader: String
    val shareLineCode: (String) -> String
    val shareLinePrice: (String, String, String) -> String
    val shareLineVerdict: (String) -> String
    val shareLineInstallments: (String, String) -> String
    val shareInstallmentHeader: String
    val shareInstallmentTotalLine: (String, String) -> String
    val shareInstallmentScheduleLine: (String, String, String) -> String
    val targetPriceTitle: String
    val targetPriceSubtitle: String
    val targetPriceNoTargetBadge: String
    val targetPriceBelowBadge: (String) -> String
    val targetPriceAboveBadge: (String) -> String
    val targetPriceAtBadge: String
    val targetPriceCurrentLabel: String
    val targetPriceYourGoalLabel: String
    val targetPriceSetBtn: String
    val targetPriceEditBtn: String
    val targetPriceClearBtn: String
    val targetPriceDialogTitle: String
    val targetPriceDialogField: String
    val targetPriceBelowBanner: (String, String, String) -> String
    val targetPriceAboveHint: (String, String) -> String
    val targetPriceSliderLabel: String
    val targetPriceWatchlistLabel: String
    val targetPriceShowcaseBelowChip: (String) -> String
    val historyModeScansChip: String
    val historyScannedSubtitle: (Int) -> String
    val historyScannedLogBtn: String
    val historyScannedEmptyHint: String
    val historyScannedDialogTitle: String
    val historyScannedDialogField: String
    val historyScannedPointLabel: (Int) -> String
    val productNotesTitle: String
    val productNotesSubtitle: String
    val productNotesCountBadge: (Int) -> String
    val productNotesInputHint: String
    val productNotesSaveBtn: String
    val productNotesUpdateBtn: String
    val productNotesEditCd: String
    val productNotesDeleteCd: String
    val productNotesEmptyTitle: String
    val productNotesEmptyBody: String
    val productNotesQuickChips: List<String>
    val verifierManualLabel: String
    val verifierManualSubtitle: String
    val verifierAutocompleteHeader: (Int) -> String
    val verifierAutocompleteCustomAction: (String) -> String
    val verifierClearSearchCd: String
    val quotaCalcTitle: String
    val quotaCalcSubtitle: String
    val quotaCalcTotalPriceLabel: String
    val quotaCalcPaymentsLabel: String
    val quotaCalcPaymentChip: (Int) -> String
    val quotaCalcResultLabel: String
    val quotaCalcSummaryLine: (Int, String) -> String
    val quotaCalcResetPrice: String
    val themeSectionLabel: String
    val themeToggleTitle: String
    val themeToggleSubtitle: String
    val themeDarkBadge: String
    val themeLightBadge: String
    val settingsTitle: String
    val settingsSubtitle: String
    val settingsOpenCd: String
    val accountSectionTitle: String
    val accountSignedInAs: (String) -> String
    val accountSyncActiveBadge: String
    val accountSignOutBtn: String
    val authHeroSubtitle: String
    val authCardTitle: String
    val authCardBody: String
    val authBenefitSync: String
    val authBenefitPrivate: String
    val authSignInButton: String
    val authSigningInBtn: String
    val authFooterNote: String
    val assistantTitle: String
    val assistantSubtitle: String
    val assistantClearChatCd: String
    val assistantGroundingChip: String
    val assistantVoiceTtsChip: String
    val assistantTranscribeOnlyChip: String
    val assistantThinkingLabel: String
    val assistantInputHint: String
    val assistantMicCd: String
    val assistantSendCd: String
    val assistantStopSendCd: String
    val assistantRecordingVoiceLabel: String
    val assistantRecordingTranscribeLabel: String
    val assistantAudioTranscriptBadge: String
    val assistantSpeakMessageCd: String
    val assistantEmptyTitle: String
    val assistantEmptyBody: String
    val assistantStarterPrompts: List<String>
    val assistantMicPermissionDenied: String
    val assistantMicStartError: String
    val assistantMicEmptyError: String
    val coachMarkStepCounter: (Int, Int) -> String
    val coachMarkSkip: String
    val coachMarkBack: String
    val coachMarkNext: String
    val coachMarkFinish: String
    val coachMarkReplayBtn: String
    val coachStep1Title: String
    val coachStep1Body: String
    val coachStep2Title: String
    val coachStep2Body: String
    val coachStep3Title: String
    val coachStep3Body: String
    val coachStep4Title: String
    val coachStep4Body: String
    val dialogFieldCategory: String
    val dialogCategoryHint: String
    val dialogCategoryPresetsLabel: String
    val categoryPickerTitle: String
    val categoryPickerSubtitle: (String) -> String
    val categoryEditAction: String
    val dashboardPieTitle: String
    val dashboardPieSubtitle: (Int) -> String
    val dashboardPieMetricBiweekly: String
    val dashboardPieMetricTotal: String
    val dashboardPieEmptyTitle: String
    val dashboardPieEmptyBody: String
    val dashboardPieManageAction: String
    val dashboardPiePlanCount: (Int) -> String
    val dashboardPieSelectedLabel: String
}

object SpanishStrings : AppStrings {
    override val language = AppLanguage.ES
    override val navInicio = "Inicio"
    override val navMisCuotas = "Mis Cuotas"
    override val navVerificador = "Escanear"
    override val navAsistente = "Asistente"
    override val brandTagline = "Control financiero inteligente"
    override val quincenaTotalLabel = "Total de esta quincena"
    override val perQuincena = "/ quincena"
    override val activePlans: (Int) -> String = { count -> "$count planes activos" }
    override val nextDueSummary: (String) -> String = { date -> "Próx. cobro: $date" }
    override val noPendingDue = "Sin cobros pendientes"
    override val yourPlansHeader = "Tus planes de pago"
    override val tapOrDragHint = "Toca o arrastra ≡"
    override val addQuotaBtn = "Nueva cuota"
    override val paidBadge = "Pagado"
    override val allQuotasCovered = "Todas las cuotas cubiertas"
    override val quotasRemainingShort: (Int, String) -> String = { rem, date -> "$rem cuotas restantes · $date" }
    override val quincenaUnit = "quincena"
    override val fieldInicial = "Inicial"
    override val fieldCuotasRestantes = "Cuotas restantes"
    override val quotasOfTotal: (Int, Int) -> String = { rem, total -> "$rem de $total" }
    override val fieldProximaFecha = "Próxima fecha"
    override val fieldProgreso = "Progreso de pago"
    override val quotasProgressCount: (Int, Int, Int) -> String = { paid, total, pct -> "$paid/$total cuotas ($pct%)" }
    override val actionPayQuota = "Registrar cuota"
    override val actionCompleted = "¡Completado!"
    override val actionMoveUp = "Subir"
    override val actionMoveDown = "Bajar"
    override val actionDelete = "Eliminar"
    override val emptyCuotasTitle = "Aún no tienes cuotas activas"
    override val emptyCuotasBody = "Agrega una compra nueva o restaura los ejemplos precargados para gestionar tu quincena."
    override val loadExamples = "Cargar ejemplos"
    override val dialogAddTitle = "Agregar nueva cuota"
    override val dialogFieldName = "Producto o descripción"
    override val dialogFieldCode = "Código (opcional)"
    override val dialogFieldTotal = "Precio total ($)"
    override val dialogFieldInitial = "Inicial pagada ($)"
    override val dialogFieldQuotas = "Cuotas"
    override val dialogFieldDate = "Próxima fecha (ej. 22 Nov 2026)"
    override val actionSave = "Guardar cuota"
    override val actionCancel = "Cancelar"
    override val verifierTitle = "Verificador por Código"
    override val verifierSubtitle = "Compara precio real antes de comprar"
    override val verifierSearchHint = "Ingresa código (ej. TV-50, CEL-128)"
    override val verifierExamplesLabel = "Ejemplos precargados"
    override val verifierRecentSearchesLabel = "Búsquedas recientes"
    override val verifierClearRecent = "Limpiar"
    override val verifierStorePrice = "Precio de tienda"
    override val verifierAvgPrice = "Precio promedio"
    override val verifierMarketRef = "Referencia mercado"
    override val verifierSavingsPct: (Int) -> String = { pct -> "$pct% ahorro" }
    override val verifierMarkupPct: (Int) -> String = { pct -> "+$pct% vs prom." }
    override val verifierQuincenaImpact = "Impacto en la quincena"
    override val verifierPlanSummary: (Int, String) -> String = { q, init -> "Plan: $q cuotas · Inicial $init" }
    override val verifierTotalProjected: (String) -> String = { total -> "Total: $total" }
    override val verifierAddToCuotas = "Agregar a Mis Cuotas"
    override val verifierNotFoundTitle = "Código no encontrado"
    override val verifierSearchPromptTitle = "Busca un código de producto"
    override val verifierNotFoundBody = "Prueba con alguno de los códigos precargados como TV-50, CEL-128, ZAP-01 o AUD-05."
    override val addedToCuotasSnackbar: (String) -> String = { name -> "$name agregado a Mis Cuotas" }
    override val snackbarViewAction = "Ver"
    override val notifUpcomingTitle = "Pago próximo a vencer"
    override val notifMoreCount: (Int) -> String = { extra -> "+$extra más" }
    override val notifDueDetail: (String, String, String) -> String = { amount, urgency, date -> "$amount · $urgency ($date)" }
    override val notifCenterTitle = "Recordatorios PVS"
    override val notifCenterSubtitle = "Alertas suaves antes de la fecha límite"
    override val notifThresholdLabel = "Avisar con anticipación"
    override val notifDaysChip: (Int) -> String = { d -> if (d == 1) "1 día" else "$d días" }
    override val notifEmptyTitle = "Todo en calma"
    override val notifEmptyBody: (Int) -> String = { d -> "No tienes cuotas por vencer dentro de los próximos $d días." }
    override val notifActionReview = "Revisar"
    override val notifActionPay = "Pagar cuota"
    override val notifDismissCd = "Ocultar aviso"
    override val urgencyLabel: (Int) -> String = { days ->
        when {
            days < 0 -> "Vencida hace ${-days} d"
            days == 0 -> "Vence hoy"
            days == 1 -> "Vence mañana"
            else -> "Vence en $days días"
        }
    }
    override val verdictHeadline: (PriceVerdict) -> String = { verdict ->
        when (verdict) {
            PriceVerdict.GREEN -> "¡Vale la pena!"
            PriceVerdict.YELLOW -> "Piénsalo dos veces"
            PriceVerdict.RED -> "Está caro, busca otra opción"
        }
    }
    override val verdictReason: (String, String) -> String = { _, spanishReason -> spanishReason }
    override val replayTutorialCd = "Ver tutorial"
    override val tutorialSkip = "Saltar"
    override val tutorialNext = "Siguiente"
    override val tutorialStart = "¡Empezar ahora!"
    override val tutorialStep1Title = "Organiza tus Cuotas"
    override val tutorialStep1Body = "Mira el total de tu quincena arriba. Toca cualquier tarjeta para ver su inicial, cuotas restantes y barra animada, o mantén presionado ≡ para reordenarlas."
    override val tutorialStep2Title = "Verifica por Código"
    override val tutorialStep2Body = "Busca códigos como TV-50, CEL-128, ZAP-01 o AUD-05. El semáforo PVS te dirá al instante si vale la pena o está muy caro, guardando tus últimas 5 búsquedas."
    override val tutorialStep3Title = "Alertas Suaves PVS"
    override val tutorialStep3Body = "Recibe recordatorios discretos dentro de la app 3 días antes de cada vencimiento y cambia entre Español o Inglés cuando quieras."
    override val quickBuyPromptCta = "¿Qué quieres comprar hoy? Escanea o pon el código"
    override val verifierHeroTitle = "¿Qué quieres comprar hoy?"
    override val verifierHeroSubtitle = "Escanea, busca o escribe el código para saber al instante si vale la pena."
    override val verifierSearchAction = "Analizar"
    override val budgetTitle = "¿Cuánto dinero tienes?"
    override val budgetTotalLabel = "Tu dinero disponible"
    override val budgetCommittedLabel = "Ya en cuotas"
    override val budgetFreeLabel = "Libre para gastar"
    override val budgetFitsGreat: (String) -> String = { left -> "¡Sí te alcanza! Te quedan $left libres en tu quincena." }
    override val budgetTightWarning: (String) -> String = { over -> "Ajustado: te faltan $over libres para cubrir esta cuota." }
    override val budgetEditHint = "Toca el monto para editar"
    override val aiSectionTitle = "Ficha inteligente (IA)"
    override val aiLoadingText = "Analizando potencia, duración y resumen con IA..."
    override val aiPowerLabel = "Potencia y especificaciones"
    override val aiDurabilityLabel = "Cuánto dura (estimación)"
    override val aiSummaryLabel = "Resumen del producto"
    override val aiRetryBtn = "Reintentar análisis"
    override val addToCartCta = "Añadir al Carrito"
    override val addedToCartSnackbar: (String) -> String = { name -> "$name añadido al Carrito" }
    override val homeWelcomeTitle = "Panel principal PVS"
    override val homeWelcomeSubtitle = "¿Qué te gustaría hacer hoy?"
    override val homeMainOptionsLabel = "Opciones principales"
    override val homeScanCardTitle = "Escanear o Verificar"
    override val homeScanCardDesc = "Ingresa un código o producto, mira el semáforo PVS, calcula tu presupuesto y revisa el análisis IA."
    override val homeScanCardBadge = "Semáforo + IA"
    override val homeQuotasCardTitle = "Gestión de Cuotas"
    override val homeQuotasCardDesc = "Organiza tus planes quincenales, reordena tarjetas, registra pagos y revisa vencimientos."
    override val homeQuotasCardBadge: (Int) -> String = { count -> "$count activos" }
    override val homeQuickCodesTitle = "Acceso rápido por código"
    override val homeSummaryQuincena = "Quincena actual"
    override val homeSummaryAvailable = "Dinero disponible"
    override val notifChannelName = "Recordatorios de cuotas PVS"
    override val notifChannelDesc = "Avisos automáticos cuando una fecha de pago de cuota está por vencer"
    override val notifSystemMultiTitle: (Int) -> String = { count -> "$count cuotas próximas a vencer" }
    override val notifSystemToggleTitle = "Notificaciones en el teléfono"
    override val notifSystemToggleSubtitle = "Aviso automático aunque cierres la app"
    override val notifPermissionNeededTitle = "Permiso de notificaciones requerido"
    override val notifPermissionNeededBody = "Activa el permiso para recibir recordatorios en tu barra de estado antes de cada vencimiento."
    override val notifPermissionGrantBtn = "Permitir notificaciones"
    override val notifPermissionSettingsBtn = "Abrir ajustes"
    override val notifTestReminderBtn = "Probar notificación"
    override val notifTestSentFeedback = "¡Notificación de prueba enviada a tu teléfono!"
    override val notifTestFailedFeedback = "Activa primero el permiso de notificaciones en tu teléfono."
    override val historyTitle = "Historial de precios"
    override val historySubtitle = "Tendencia tienda vs. promedio de mercado"
    override val historyMinLabel = "Mínimo"
    override val historyMaxLabel = "Máximo"
    override val historyTrendLabel = "Tendencia"
    override val historyLegendStore = "Precio tienda"
    override val historyLegendMarket = "Promedio mercado"
    override val historyTouchHint = "Toca o desliza sobre la gráfica para ver cada mes"
    override val historyDeltaVsMarket: (String) -> String = { diff -> "$diff vs. prom." }
    override val scannerOpenBtn = "Escanear código"
    override val scannerTitle = "Escáner de códigos PVS"
    override val scannerSubtitle = "Apunta al código de barras o QR del producto"
    override val scannerFrameHint = "Centra el código de barras dentro del marco verde"
    override val scannerPermissionTitle = "Permiso de cámara necesario"
    override val scannerPermissionBody = "Autoriza la cámara para escanear códigos de barras (EAN, UPC, QR) directo en la tienda."
    override val scannerPermissionGrantBtn = "Activar cámara"
    override val scannerPermissionSettingsBtn = "Abrir ajustes"
    override val scannerTorchCd = "Encender o apagar linterna"
    override val scannerSwitchCameraCd = "Cambiar cámara"
    override val scannerCloseCd = "Cerrar escáner"
    override val scannerCameraError = "No se pudo iniciar la cámara en este dispositivo."
    override val scannedCardPassBadge = "FICHA PVS"
    override val scannedCardLiveScan = "Escaneado"
    override val scannedCardVerified = "Verificado"
    override val scannedCardCodeLabel = "Código"
    override val scannedCardPriceLabel = "Precio"
    override val scannedCardBiweeklyQuotaLabel = "Cuota quincenal est."
    override val scannedCardPlanDetail: (Int, String) -> String = { quotas, initial -> "$quotas quincenas · Inicial $initial" }
    override val scannedCardRescanBtn = "Escanear otro"
    override val scannedCardQuickAddBtn = "Agregar cuota"
    override val shareProductBtn = "Compartir producto"
    override val shareInstallmentBtn = "Compartir plan"
    override val shareChooserTitle = "Compartir detalles con..."
    override val shareSubjectProduct: (String) -> String = { name -> "PVS Cuotas: $name" }
    override val shareCardHeader = "PVS Cuotas · Ficha de Producto"
    override val shareLineCode: (String) -> String = { code -> "Código: $code" }
    override val shareLinePrice: (String, String, String) -> String = { store, avg, diff -> "Precio: $store (Prom. mercado: $avg · $diff)" }
    override val shareLineVerdict: (String) -> String = { verdict -> "Semáforo PVS: $verdict" }
    override val shareLineInstallments: (String, String) -> String = { biweekly, planSummary -> "Cuota estimada: $biweekly/qna · $planSummary" }
    override val shareInstallmentHeader = "PVS Cuotas · Plan de Pago"
    override val shareInstallmentTotalLine: (String, String) -> String = { total, initial -> "Total: $total · Inicial: $initial" }
    override val shareInstallmentScheduleLine: (String, String, String) -> String = { biweekly, rem, date -> "Cuota: $biweekly/qna · Restantes: $rem · Próx. cobro: $date" }
    override val targetPriceTitle = "Rastreo de precio objetivo"
    override val targetPriceSubtitle = "Indicador visual si el precio está debajo de tu meta"
    override val targetPriceNoTargetBadge = "Sin meta"
    override val targetPriceBelowBadge: (String) -> String = { diff -> "¡Bajo meta! -$diff" }
    override val targetPriceAboveBadge: (String) -> String = { diff -> "+$diff sobre meta" }
    override val targetPriceAtBadge = "En tu meta exacta"
    override val targetPriceCurrentLabel = "Precio actual"
    override val targetPriceYourGoalLabel = "Tu precio objetivo"
    override val targetPriceSetBtn = "Definir precio objetivo"
    override val targetPriceEditBtn = "Editar"
    override val targetPriceClearBtn = "Quitar meta"
    override val targetPriceDialogTitle = "Definir precio objetivo"
    override val targetPriceDialogField = "Monto objetivo ($)"
    override val targetPriceBelowBanner: (String, String, String) -> String = { current, savings, target ->
        "¡Alerta de compra! El precio actual ($current) está $savings debajo de tu meta ($target)."
    }
    override val targetPriceAboveHint: (String, String) -> String = { needed, target ->
        "Aún supera tu meta por $needed (objetivo: $target)."
    }
    override val targetPriceSliderLabel = "Ajuste rápido de precio objetivo"
    override val targetPriceWatchlistLabel = "Metas activas"
    override val targetPriceShowcaseBelowChip: (String) -> String = { target -> "Bajo tu meta ($target)" }
    override val historyModeScansChip = "5 Escaneos"
    override val historyScannedSubtitle: (Int) -> String = { count -> "Últimos $count/5 precios escaneados (DB local)" }
    override val historyScannedLogBtn = "Registrar escaneo"
    override val historyScannedEmptyHint = "Escanea este producto o registra precios vistos en tienda para ver tu tendencia local."
    override val historyScannedDialogTitle = "Registrar precio escaneado"
    override val historyScannedDialogField = "Precio observado ($)"
    override val historyScannedPointLabel: (Int) -> String = { n -> "Esc. #$n" }
    override val productNotesTitle = "Notas del producto"
    override val productNotesSubtitle = "Apuntes personales guardados en Room"
    override val productNotesCountBadge: (Int) -> String = { count -> if (count == 1) "1 nota" else "$count notas" }
    override val productNotesInputHint = "Ej. Garantía 2 años en tienda centro..."
    override val productNotesSaveBtn = "Guardar"
    override val productNotesUpdateBtn = "Actualizar"
    override val productNotesEditCd = "Editar nota"
    override val productNotesDeleteCd = "Eliminar nota"
    override val productNotesEmptyTitle = "Sin notas todavía"
    override val productNotesEmptyBody = "Anota detalles de garantía, envío o promociones de este producto para tenerlos siempre a mano."
    override val productNotesQuickChips = listOf("Garantía extendida", "Envío gratis", "Descuento en caja")
    override val verifierManualLabel = "Búsqueda manual de código"
    override val verifierManualSubtitle = "¿No puedes escanear? Escribe código, nombre o EAN"
    override val verifierAutocompleteHeader: (Int) -> String = { count -> "Sugerencias ($count)" }
    override val verifierAutocompleteCustomAction: (String) -> String = { query -> "Analizar código \"$query\" con IA" }
    override val verifierClearSearchCd = "Limpiar búsqueda"
    override val quotaCalcTitle = "Calculadora rápida de cuotas"
    override val quotaCalcSubtitle = "Ingresa el precio total y elige el número de pagos"
    override val quotaCalcTotalPriceLabel = "Precio total ($)"
    override val quotaCalcPaymentsLabel = "Número de pagos quincenales"
    override val quotaCalcPaymentChip: (Int) -> String = { count -> "$count pagos" }
    override val quotaCalcResultLabel = "Valor de cada cuota quincenal"
    override val quotaCalcSummaryLine: (Int, String) -> String = { count, total -> "$count cuotas quincenales · Total $total" }
    override val quotaCalcResetPrice = "Precio tienda"
    override val themeSectionLabel = "Configuración de la interfaz"
    override val themeToggleTitle = "Modo oscuro nocturno"
    override val themeToggleSubtitle = "Reduce el deslumbramiento en turnos nocturnos o poca luz"
    override val themeDarkBadge = "Oscuro"
    override val themeLightBadge = "Claro"
    override val settingsTitle = "Configuración y Alertas"
    override val settingsSubtitle = "Apariencia nocturna, recordatorios y cuenta en la nube"
    override val settingsOpenCd = "Abrir configuración"
    override val accountSectionTitle = "Cuenta y respaldo en la nube"
    override val accountSignedInAs: (String) -> String = { name -> "Sesión activa: $name" }
    override val accountSyncActiveBadge = "Firestore activo"
    override val accountSignOutBtn = "Cerrar sesión"
    override val authHeroSubtitle = "Tu aliado inteligente en cuotas y precios reales"
    override val authCardTitle = "Inicia sesión para sincronizar tus cuotas"
    override val authCardBody = "Respalda tus planes quincenales, precios objetivo, notas y conversaciones con IA de forma segura con Google."
    override val authBenefitSync = "Nube Firestore"
    override val authBenefitPrivate = "Cuenta privada"
    override val authSignInButton = "Continuar con Google"
    override val authSigningInBtn = "Conectando..."
    override val authFooterNote = "Tus datos se guardan aislados por usuario en Cloud Firestore."
    override val assistantTitle = "Asistente PVS IA"
    override val assistantSubtitle = "Chat experto, voz y transcripción"
    override val assistantClearChatCd = "Limpiar conversación"
    override val assistantGroundingChip = "Búsqueda Google"
    override val assistantVoiceTtsChip = "Leer en voz"
    override val assistantTranscribeOnlyChip = "Solo transcribir"
    override val assistantThinkingLabel = "Analizando con Gemini..."
    override val assistantInputHint = "Pregunta sobre un producto, cuotas o graba tu voz..."
    override val assistantMicCd = "Grabar audio o voz"
    override val assistantSendCd = "Enviar mensaje"
    override val assistantStopSendCd = "Detener y procesar audio"
    override val assistantRecordingVoiceLabel = "Grabando pregunta de voz..."
    override val assistantRecordingTranscribeLabel = "Grabando para transcribir..."
    override val assistantAudioTranscriptBadge = "Transcripción de audio"
    override val assistantSpeakMessageCd = "Escuchar respuesta"
    override val assistantEmptyTitle = "Asistente conversacional PVS"
    override val assistantEmptyBody = "Pregúntame sobre cuotas quincenales, compara productos o toca el micrófono para transcribir audio y conversar por voz."
    override val assistantStarterPrompts = listOf(
        "¿Conviene comprar el Smart TV TV-50 según mi quincena libre?",
        "Compara TV-50 vs CEL-128 para pagarlo en 6 quincenas",
        "¿Cómo organizar mis pagos quincenales para ahorrar más?"
    )
    override val assistantMicPermissionDenied = "Permiso de micrófono denegado. Actívalo para grabar y transcribir audio."
    override val assistantMicStartError = "No se pudo iniciar la grabación de audio en este dispositivo."
    override val assistantMicEmptyError = "No se capturó audio suficiente. Intenta hablar más cerca del micrófono."
    override val coachMarkStepCounter: (Int, Int) -> String = { current, total -> "Paso $current de $total" }
    override val coachMarkSkip = "Saltar"
    override val coachMarkBack = "Anterior"
    override val coachMarkNext = "Siguiente"
    override val coachMarkFinish = "¡Listo!"
    override val coachMarkReplayBtn = "Ver guía interactiva en pantalla"
    override val coachStep1Title = "Escanea o verifica productos"
    override val coachStep1Body = "Toca esta tarjeta o la pestaña Escanear para revisar el precio real de un producto antes de comprarlo a cuotas."
    override val coachStep2Title = "Escanea el código de barras o QR"
    override val coachStep2Body = "Pulsa «Escanear código» para usar la cámara, o escribe el código arriba. Verás el semáforo PVS, cuotas estimadas y ficha IA."
    override val coachStep3Title = "Añade una nueva cuota"
    override val coachStep3Body = "Toca «Nueva cuota» para registrar un plan manual con monto total, inicial pagada, número de quincenas y fecha límite."
    override val coachStep4Title = "Pasa directo del escaneo a tus cuotas"
    override val coachStep4Body = "También puedes usar este acceso rápido cuando encuentres un buen precio y añadirlo al carrito en un toque."
    override val dialogFieldCategory = "Categoría o etiqueta"
    override val dialogCategoryHint = "Ej. Tecnología, Hogar, Salud"
    override val dialogCategoryPresetsLabel = "Sugerencias rápidas"
    override val categoryPickerTitle = "Etiquetar categoría"
    override val categoryPickerSubtitle: (String) -> String = { title -> "Elige o escribe una etiqueta para «$title»" }
    override val categoryEditAction = "Categoría"
    override val dashboardPieTitle = "Gastos por categoría"
    override val dashboardPieSubtitle: (Int) -> String = { count -> if (count == 1) "1 categoría activa" else "$count categorías activas" }
    override val dashboardPieMetricBiweekly = "Quincenal"
    override val dashboardPieMetricTotal = "Total"
    override val dashboardPieEmptyTitle = "Sin gastos por categoría"
    override val dashboardPieEmptyBody = "Añade o etiqueta tus cuotas en Mis Cuotas para visualizar aquí la distribución."
    override val dashboardPieManageAction = "Etiquetar"
    override val dashboardPiePlanCount: (Int) -> String = { n -> if (n == 1) "1 plan" else "$n planes" }
    override val dashboardPieSelectedLabel = "Seleccionada"
}

object EnglishStrings : AppStrings {
    override val language = AppLanguage.EN
    override val navInicio = "Home"
    override val navMisCuotas = "My Quotas"
    override val navVerificador = "Scan"
    override val navAsistente = "Assistant"
    override val brandTagline = "Smart biweekly control"
    override val quincenaTotalLabel = "Biweekly total due"
    override val perQuincena = "/ biweekly"
    override val activePlans: (Int) -> String = { count -> "$count active plans" }
    override val nextDueSummary: (String) -> String = { date -> "Next due: $date" }
    override val noPendingDue = "No pending payments"
    override val yourPlansHeader = "Your payment plans"
    override val tapOrDragHint = "Tap or drag ≡"
    override val addQuotaBtn = "New plan"
    override val paidBadge = "Paid"
    override val allQuotasCovered = "All installments covered"
    override val quotasRemainingShort: (Int, String) -> String = { rem, date -> "$rem quotas left · $date" }
    override val quincenaUnit = "biweekly"
    override val fieldInicial = "Down pmt"
    override val fieldCuotasRestantes = "Remaining"
    override val quotasOfTotal: (Int, Int) -> String = { rem, total -> "$rem of $total" }
    override val fieldProximaFecha = "Next due date"
    override val fieldProgreso = "Payment progress"
    override val quotasProgressCount: (Int, Int, Int) -> String = { paid, total, pct -> "$paid/$total quotas ($pct%)" }
    override val actionPayQuota = "Log payment"
    override val actionCompleted = "Completed!"
    override val actionMoveUp = "Move up"
    override val actionMoveDown = "Move down"
    override val actionDelete = "Delete"
    override val emptyCuotasTitle = "No active installment plans"
    override val emptyCuotasBody = "Add a new purchase or restore the preloaded examples to track your biweekly payments."
    override val loadExamples = "Load examples"
    override val dialogAddTitle = "Add new installment"
    override val dialogFieldName = "Product or description"
    override val dialogFieldCode = "Code (optional)"
    override val dialogFieldTotal = "Total price ($)"
    override val dialogFieldInitial = "Down payment ($)"
    override val dialogFieldQuotas = "Quotas"
    override val dialogFieldDate = "Next date (e.g. 22 Nov 2026)"
    override val actionSave = "Save plan"
    override val actionCancel = "Cancel"
    override val verifierTitle = "Code Verifier"
    override val verifierSubtitle = "Compare real market price first"
    override val verifierSearchHint = "Enter code (e.g. TV-50, CEL-128)"
    override val verifierExamplesLabel = "Preloaded examples"
    override val verifierRecentSearchesLabel = "Recent searches"
    override val verifierClearRecent = "Clear"
    override val verifierStorePrice = "Store price"
    override val verifierAvgPrice = "Average price"
    override val verifierMarketRef = "Market benchmark"
    override val verifierSavingsPct: (Int) -> String = { pct -> "$pct% savings" }
    override val verifierMarkupPct: (Int) -> String = { pct -> "+$pct% vs avg" }
    override val verifierQuincenaImpact = "Biweekly impact"
    override val verifierPlanSummary: (Int, String) -> String = { q, init -> "Plan: $q quotas · Initial $init" }
    override val verifierTotalProjected: (String) -> String = { total -> "Total: $total" }
    override val verifierAddToCuotas = "Add to My Quotas"
    override val verifierNotFoundTitle = "Code not found"
    override val verifierSearchPromptTitle = "Search a product code"
    override val verifierNotFoundBody = "Try one of the preloaded codes like TV-50, CEL-128, ZAP-01, or AUD-05."
    override val addedToCuotasSnackbar: (String) -> String = { name -> "$name added to My Quotas" }
    override val snackbarViewAction = "View"
    override val notifUpcomingTitle = "Upcoming payment due"
    override val notifMoreCount: (Int) -> String = { extra -> "+$extra more" }
    override val notifDueDetail: (String, String, String) -> String = { amount, urgency, date -> "$amount · $urgency ($date)" }
    override val notifCenterTitle = "PVS Reminders"
    override val notifCenterSubtitle = "Gentle alerts ahead of due dates"
    override val notifThresholdLabel = "Notify in advance"
    override val notifDaysChip: (Int) -> String = { d -> if (d == 1) "1 day" else "$d days" }
    override val notifEmptyTitle = "All clear"
    override val notifEmptyBody: (Int) -> String = { d -> "No installments due within the next $d days." }
    override val notifActionReview = "Review"
    override val notifActionPay = "Pay quota"
    override val notifDismissCd = "Dismiss alert"
    override val urgencyLabel: (Int) -> String = { days ->
        when {
            days < 0 -> "Overdue by ${-days} d"
            days == 0 -> "Due today"
            days == 1 -> "Due tomorrow"
            else -> "Due in $days days"
        }
    }
    override val verdictHeadline: (PriceVerdict) -> String = { verdict ->
        when (verdict) {
            PriceVerdict.GREEN -> "Worth it!"
            PriceVerdict.YELLOW -> "Think twice"
            PriceVerdict.RED -> "Too pricey, look elsewhere"
        }
    }
    override val verdictReason: (String, String) -> String = { code, _ ->
        when (code) {
            "TV-50" -> "15% below market average. A smart buy to split into comfortable biweekly quotas."
            "CEL-128" -> "8% above typical market price. Worth comparing another store before committing."
            "ZAP-01" -> "38% higher than average market price. This markup adds unnecessary strain to your budget."
            "AUD-05" -> "Real 21% savings vs market price with a very light impact on your biweekly total."
            "LAP-16" -> "Slightly above average (+7%) with a moderately high biweekly installment."
            "CAF-02" -> "34% markup compared to official retailers. Look for a better deal."
            else -> "Market comparison calculated against recent retail averages."
        }
    }
    override val replayTutorialCd = "Watch tutorial"
    override val tutorialSkip = "Skip"
    override val tutorialNext = "Next"
    override val tutorialStart = "Get started!"
    override val tutorialStep1Title = "Organize Your Quotas"
    override val tutorialStep1Body = "Track your biweekly total at the top. Tap any card to expand down payment, remaining quotas, and animated progress, or drag ≡ to reorder."
    override val tutorialStep2Title = "Verify by Code"
    override val tutorialStep2Body = "Search codes like TV-50, CEL-128, ZAP-01, or AUD-05. The PVS traffic light warns if it's worth it or overpriced, saving your last 5 searches."
    override val tutorialStep3Title = "Gentle PVS Alerts"
    override val tutorialStep3Body = "Get discreet in-app reminders 3 days before a due date and switch between Spanish or English anytime."
    override val quickBuyPromptCta = "What do you want to buy today? Search or enter code"
    override val verifierHeroTitle = "What do you want to buy today?"
    override val verifierHeroSubtitle = "Scan, search, or type a code to instantly see if it's worth buying."
    override val verifierSearchAction = "Analyze"
    override val budgetTitle = "How much money do you have?"
    override val budgetTotalLabel = "Available money"
    override val budgetCommittedLabel = "In active quotas"
    override val budgetFreeLabel = "Free to spend"
    override val budgetFitsGreat: (String) -> String = { left -> "It fits! You'll still have $left free this biweekly cycle." }
    override val budgetTightWarning: (String) -> String = { over -> "Too tight: you are $over short of free budget for this quota." }
    override val budgetEditHint = "Tap amount to edit"
    override val aiSectionTitle = "Smart product breakdown (AI)"
    override val aiLoadingText = "Analyzing power, lifespan & summary with AI..."
    override val aiPowerLabel = "Power & specifications"
    override val aiDurabilityLabel = "Estimated durability"
    override val aiSummaryLabel = "Product summary"
    override val aiRetryBtn = "Retry AI analysis"
    override val addToCartCta = "Add to Cart"
    override val addedToCartSnackbar: (String) -> String = { name -> "$name added to Cart" }
    override val homeWelcomeTitle = "PVS Main Hub"
    override val homeWelcomeSubtitle = "What would you like to do today?"
    override val homeMainOptionsLabel = "Main options"
    override val homeScanCardTitle = "Scan or Verify"
    override val homeScanCardDesc = "Check any code or product, view the PVS traffic light, calculate your budget, and get AI insights."
    override val homeScanCardBadge = "Traffic Light + AI"
    override val homeQuotasCardTitle = "Manage Quotas"
    override val homeQuotasCardDesc = "Organize biweekly plans, reorder cards smoothly, log payments, and track upcoming due dates."
    override val homeQuotasCardBadge: (Int) -> String = { count -> "$count active" }
    override val homeQuickCodesTitle = "Quick code check"
    override val homeSummaryQuincena = "Current biweekly"
    override val homeSummaryAvailable = "Available money"
    override val notifChannelName = "PVS Quota Reminders"
    override val notifChannelDesc = "Automatic alerts when a quota payment date is approaching"
    override val notifSystemMultiTitle: (Int) -> String = { count -> "$count quotas due soon" }
    override val notifSystemToggleTitle = "Phone system notifications"
    override val notifSystemToggleSubtitle = "Automatic alerts even when the app is closed"
    override val notifPermissionNeededTitle = "Notification permission needed"
    override val notifPermissionNeededBody = "Allow notifications so PVS Cuotas can remind you in your status bar before due dates."
    override val notifPermissionGrantBtn = "Allow notifications"
    override val notifPermissionSettingsBtn = "Open settings"
    override val notifTestReminderBtn = "Send test notification"
    override val notifTestSentFeedback = "Test reminder sent to your notification tray!"
    override val notifTestFailedFeedback = "Enable system notification permission on your phone first."
    override val historyTitle = "Price History Trend"
    override val historySubtitle = "Store price vs. market benchmark"
    override val historyMinLabel = "Lowest"
    override val historyMaxLabel = "Highest"
    override val historyTrendLabel = "Trend"
    override val historyLegendStore = "Store price"
    override val historyLegendMarket = "Market avg"
    override val historyTouchHint = "Tap or drag across the chart to inspect any month"
    override val historyDeltaVsMarket: (String) -> String = { diff -> "$diff vs. avg" }
    override val scannerOpenBtn = "Scan barcode"
    override val scannerTitle = "PVS Barcode Scanner"
    override val scannerSubtitle = "Point at any product barcode or QR code"
    override val scannerFrameHint = "Align the barcode inside the green viewfinder"
    override val scannerPermissionTitle = "Camera permission needed"
    override val scannerPermissionBody = "Allow camera access to scan retail barcodes (EAN, UPC, QR) directly in store."
    override val scannerPermissionGrantBtn = "Enable camera"
    override val scannerPermissionSettingsBtn = "Open settings"
    override val scannerTorchCd = "Toggle flashlight"
    override val scannerSwitchCameraCd = "Switch camera"
    override val scannerCloseCd = "Close scanner"
    override val scannerCameraError = "Unable to start camera preview on this device."
    override val scannedCardPassBadge = "PVS PASS"
    override val scannedCardLiveScan = "Scanned"
    override val scannedCardVerified = "Verified"
    override val scannedCardCodeLabel = "Product code"
    override val scannedCardPriceLabel = "Price"
    override val scannedCardBiweeklyQuotaLabel = "Est. biweekly quota"
    override val scannedCardPlanDetail: (Int, String) -> String = { quotas, initial -> "$quotas biweekly · Initial $initial" }
    override val scannedCardRescanBtn = "Scan another"
    override val scannedCardQuickAddBtn = "Add plan"
    override val shareProductBtn = "Share product"
    override val shareInstallmentBtn = "Share plan"
    override val shareChooserTitle = "Share product details via..."
    override val shareSubjectProduct: (String) -> String = { name -> "PVS Cuotas: $name" }
    override val shareCardHeader = "PVS Cuotas · Product Summary"
    override val shareLineCode: (String) -> String = { code -> "Code: $code" }
    override val shareLinePrice: (String, String, String) -> String = { store, avg, diff -> "Price: $store (Market avg: $avg · $diff)" }
    override val shareLineVerdict: (String) -> String = { verdict -> "PVS Verdict: $verdict" }
    override val shareLineInstallments: (String, String) -> String = { biweekly, planSummary -> "Est. quota: $biweekly/biweekly · $planSummary" }
    override val shareInstallmentHeader = "PVS Cuotas · Installment Plan"
    override val shareInstallmentTotalLine: (String, String) -> String = { total, initial -> "Total: $total · Down pmt: $initial" }
    override val shareInstallmentScheduleLine: (String, String, String) -> String = { biweekly, rem, date -> "Quota: $biweekly/biweekly · Remaining: $rem · Next due: $date" }
    override val targetPriceTitle = "Price target tracker"
    override val targetPriceSubtitle = "Visual indicator when scanned price is below target"
    override val targetPriceNoTargetBadge = "No target"
    override val targetPriceBelowBadge: (String) -> String = { diff -> "Below target! -$diff" }
    override val targetPriceAboveBadge: (String) -> String = { diff -> "+$diff above" }
    override val targetPriceAtBadge = "At exact target"
    override val targetPriceCurrentLabel = "Current price"
    override val targetPriceYourGoalLabel = "Your target price"
    override val targetPriceSetBtn = "Set target price"
    override val targetPriceEditBtn = "Edit"
    override val targetPriceClearBtn = "Remove target"
    override val targetPriceDialogTitle = "Set target price"
    override val targetPriceDialogField = "Target price ($)"
    override val targetPriceBelowBanner: (String, String, String) -> String = { current, savings, target ->
        "Deal indicator! Current price ($current) is $savings below your target ($target)."
    }
    override val targetPriceAboveHint: (String, String) -> String = { needed, target ->
        "Still $needed above your $target target."
    }
    override val targetPriceSliderLabel = "Quick target adjustment"
    override val targetPriceWatchlistLabel = "Tracked targets"
    override val targetPriceShowcaseBelowChip: (String) -> String = { target -> "Below target ($target)" }
    override val historyModeScansChip = "5 Scans"
    override val historyScannedSubtitle: (Int) -> String = { count -> "Last $count/5 scanned prices (Local Room DB)" }
    override val historyScannedLogBtn = "Log scan price"
    override val historyScannedEmptyHint = "Scan this product or log store prices to build your local 5-scan trend."
    override val historyScannedDialogTitle = "Log scanned price"
    override val historyScannedDialogField = "Observed price ($)"
    override val historyScannedPointLabel: (Int) -> String = { n -> "Scan #$n" }
    override val productNotesTitle = "Product notes"
    override val productNotesSubtitle = "Personal reminders saved locally in Room"
    override val productNotesCountBadge: (Int) -> String = { count -> if (count == 1) "1 note" else "$count notes" }
    override val productNotesInputHint = "E.g. 2-year warranty at downtown store..."
    override val productNotesSaveBtn = "Save"
    override val productNotesUpdateBtn = "Update"
    override val productNotesEditCd = "Edit note"
    override val productNotesDeleteCd = "Delete note"
    override val productNotesEmptyTitle = "No notes yet"
    override val productNotesEmptyBody = "Jot down warranty coverage, shipping promos, or store comparisons for this product."
    override val productNotesQuickChips = listOf("Extended warranty", "Free shipping", "Cash discount")
    override val verifierManualLabel = "Manual code search"
    override val verifierManualSubtitle = "Can't scan? Type code, name, or barcode"
    override val verifierAutocompleteHeader: (Int) -> String = { count -> "Suggestions ($count)" }
    override val verifierAutocompleteCustomAction: (String) -> String = { query -> "Look up \"$query\" with AI" }
    override val verifierClearSearchCd = "Clear search"
    override val quotaCalcTitle = "Quick installment calculator"
    override val quotaCalcSubtitle = "Enter total price and pick number of biweekly payments"
    override val quotaCalcTotalPriceLabel = "Total price ($)"
    override val quotaCalcPaymentsLabel = "Number of biweekly payments"
    override val quotaCalcPaymentChip: (Int) -> String = { count -> "$count pays" }
    override val quotaCalcResultLabel = "Biweekly installment amount"
    override val quotaCalcSummaryLine: (Int, String) -> String = { count, total -> "$count biweekly payments · Total $total" }
    override val quotaCalcResetPrice = "Store price"
    override val themeSectionLabel = "Interface settings"
    override val themeToggleTitle = "Night dark mode"
    override val themeToggleSubtitle = "Improves visibility during night shifts or low-light environments"
    override val themeDarkBadge = "Dark"
    override val themeLightBadge = "Light"
    override val settingsTitle = "Settings & Alerts"
    override val settingsSubtitle = "Dark mode appearance, reminders, and cloud account"
    override val settingsOpenCd = "Open settings"
    override val accountSectionTitle = "Account & Cloud Sync"
    override val accountSignedInAs: (String) -> String = { name -> "Signed in: $name" }
    override val accountSyncActiveBadge = "Firestore synced"
    override val accountSignOutBtn = "Sign out"
    override val authHeroSubtitle = "Smart biweekly plans & real market price verification"
    override val authCardTitle = "Sign in to sync your installments"
    override val authCardBody = "Securely back up your biweekly plans, price targets, product notes, and AI chat history with Google."
    override val authBenefitSync = "Firestore Sync"
    override val authBenefitPrivate = "Private Account"
    override val authSignInButton = "Sign in with Google"
    override val authSigningInBtn = "Connecting..."
    override val authFooterNote = "Your records are privately isolated per account in Cloud Firestore."
    override val assistantTitle = "PVS AI Assistant"
    override val assistantSubtitle = "Multi-turn chat, voice & transcription"
    override val assistantClearChatCd = "Clear conversation"
    override val assistantGroundingChip = "Google Search"
    override val assistantVoiceTtsChip = "Read aloud"
    override val assistantTranscribeOnlyChip = "Transcribe only"
    override val assistantThinkingLabel = "Thinking with Gemini..."
    override val assistantInputHint = "Ask about a product, quotas, or tap mic to speak..."
    override val assistantMicCd = "Record voice or audio"
    override val assistantSendCd = "Send message"
    override val assistantStopSendCd = "Stop & process audio"
    override val assistantRecordingVoiceLabel = "Recording voice question..."
    override val assistantRecordingTranscribeLabel = "Recording for transcription..."
    override val assistantAudioTranscriptBadge = "Audio transcript"
    override val assistantSpeakMessageCd = "Listen to response"
    override val assistantEmptyTitle = "PVS Conversational Assistant"
    override val assistantEmptyBody = "Ask about biweekly installments, compare products, or tap the microphone to transcribe audio and chat by voice."
    override val assistantStarterPrompts = listOf(
        "Is the Smart TV TV-50 worth buying with my free biweekly budget?",
        "Compare TV-50 vs CEL-128 across 6 biweekly payments",
        "How should I prioritize my biweekly installment plans?"
    )
    override val assistantMicPermissionDenied = "Microphone permission denied. Enable it to record and transcribe audio."
    override val assistantMicStartError = "Could not start audio recording on this device."
    override val assistantMicEmptyError = "Not enough audio captured. Try speaking closer to the microphone."
    override val coachMarkStepCounter: (Int, Int) -> String = { current, total -> "Step $current of $total" }
    override val coachMarkSkip = "Skip"
    override val coachMarkBack = "Back"
    override val coachMarkNext = "Next"
    override val coachMarkFinish = "Got it!"
    override val coachMarkReplayBtn = "Launch interactive spotlight guide"
    override val coachStep1Title = "Scan or verify a product"
    override val coachStep1Body = "Tap here or the Scan tab to check real retail prices and biweekly impact before committing to a purchase."
    override val coachStep2Title = "Scan barcode or QR code"
    override val coachStep2Body = "Tap «Scan barcode» to open the camera, or type a code above to get the PVS traffic light, quota breakdown, and AI summary."
    override val coachStep3Title = "Add a new installment plan"
    override val coachStep3Body = "Tap «New plan» to log a purchase with its total price, down payment, biweekly count, and next due date."
    override val coachStep4Title = "Jump from scan to your quotas"
    override val coachStep4Body = "Use this shortcut anytime to check a product and add it straight into your biweekly schedule."
    override val dialogFieldCategory = "Category or tag"
    override val dialogCategoryHint = "E.g. Tech, Home, Health"
    override val dialogCategoryPresetsLabel = "Quick presets"
    override val categoryPickerTitle = "Tag category"
    override val categoryPickerSubtitle: (String) -> String = { title -> "Choose or type a tag for \"$title\"" }
    override val categoryEditAction = "Category"
    override val dashboardPieTitle = "Spending by category"
    override val dashboardPieSubtitle: (Int) -> String = { count -> if (count == 1) "1 active category" else "$count active categories" }
    override val dashboardPieMetricBiweekly = "Biweekly"
    override val dashboardPieMetricTotal = "Total"
    override val dashboardPieEmptyTitle = "No categorized spending yet"
    override val dashboardPieEmptyBody = "Add or tag your plans in My Quotas to see your category breakdown here."
    override val dashboardPieManageAction = "Edit tags"
    override val dashboardPiePlanCount: (Int) -> String = { n -> if (n == 1) "1 plan" else "$n plans" }
    override val dashboardPieSelectedLabel = "Selected"
}

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { SpanishStrings }
