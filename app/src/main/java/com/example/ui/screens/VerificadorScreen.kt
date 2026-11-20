package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ai.AiAnalysisUiState
import com.example.data.PriceVerdict
import com.example.data.ProductCodeInfo
import com.example.data.ProductVerifierCatalog
import com.example.ui.PvsUiState
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.CodePillBadge
import com.example.ui.components.PriceHistoryDashboardCard
import com.example.ui.components.PriceTargetTrackerCard
import com.example.ui.components.PvsCurvedWaveOverlay
import com.example.ui.components.PvsHeaderBar
import com.example.ui.components.ScannedProductShowcaseCard
import com.example.ui.components.TrafficLightSemaphore
import com.example.ui.components.formatUsd
import com.example.ui.components.pvsBouncyScale
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.PvsForestDark
import com.example.ui.theme.PvsForestGreen
import com.example.ui.theme.PvsLeafBright
import com.example.ui.theme.PvsLeafVibrant
import com.example.ui.theme.PvsMintContainer
import com.example.ui.theme.PvsMintSurface
import com.example.ui.theme.SemaphoreGreen
import com.example.ui.theme.SemaphoreGreenBg
import com.example.ui.theme.SemaphoreGreenBorder
import com.example.ui.theme.SemaphoreRed
import com.example.ui.theme.SemaphoreRedBg
import com.example.ui.theme.SemaphoreRedBorder
import com.example.ui.theme.SemaphoreRedGlow
import com.example.ui.theme.SemaphoreYellow
import com.example.ui.theme.SemaphoreYellowBg
import com.example.ui.theme.SemaphoreYellowBorder
import com.example.ui.theme.SemaphoreYellowGlow
import kotlin.math.abs

@Composable
fun VerificadorScreen(
    state: PvsUiState,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: () -> Unit,
    onSelectExample: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onUpdateBudget: (Double) -> Unit,
    onAdjustBudget: (Double) -> Unit,
    onRetryAi: () -> Unit,
    onAddToCart: (ProductCodeInfo) -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenTutorial: (() -> Unit)? = null,
    onEnsureAiLoaded: (() -> Unit)? = null,
    onBarcodeScanned: (String) -> Unit = {},
    onSetPriceTarget: (ProductCodeInfo, Double) -> Unit = { _, _ -> },
    onClearPriceTarget: (String) -> Unit = {},
    onLogScannedPrice: (ProductCodeInfo, Double) -> Unit = { _, _ -> },
    onClearScannedHistory: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val focusManager = LocalFocusManager.current
    val haptics = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }

    LaunchedEffect(state.verifiedProduct?.code, state.aiAnalysisState is AiAnalysisUiState.Idle) {
        if (state.verifiedProduct != null && state.aiAnalysisState is AiAnalysisUiState.Idle) {
            onEnsureAiLoaded?.invoke()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 28.dp)
    ) {
        PvsHeaderBar(
            title = strings.verifierTitle,
            subtitle = strings.verifierSubtitle,
            language = state.language,
            alertCount = state.upcomingAlerts.size,
            onToggleLanguage = onToggleLanguage,
            onOpenNotifications = onOpenNotifications,
            onOpenTutorial = onOpenTutorial
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Step 1: "¿Qué quieres comprar hoy?" Warm Hero Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PvsForestDark, PvsForestGreen)
                            )
                        )
                ) {
                    PvsCurvedWaveOverlay(modifier = Modifier.matchParentSize())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCodeScanner,
                                contentDescription = null,
                                tint = PvsLeafBright,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.verifierHeroTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = strings.verifierHeroSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = PvsMintContainer
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            val heroScanInt = remember { MutableInteractionSource() }
                            Surface(
                                modifier = Modifier
                                    .pvsBouncyScale(heroScanInt)
                                    .clip(RoundedCornerShape(50))
                                    .clickable(
                                        interactionSource = heroScanInt,
                                        indication = androidx.compose.material3.ripple()
                                    ) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        focusManager.clearFocus()
                                        showScanner = true
                                    }
                                    .testTag("open_barcode_scanner_hero_button"),
                                shape = RoundedCornerShape(50),
                                color = PvsLeafBright
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.QrCodeScanner,
                                        contentDescription = null,
                                        tint = PvsForestDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = strings.scannerOpenBtn,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = PvsForestDark
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search Input + CameraX Scan IconButton + "Analizar" Bouncy Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            text = strings.verifierSearchHint,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = PvsForestGreen
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Limpiar búsqueda",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = PvsLeafVibrant,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onSubmitSearch()
                            focusManager.clearFocus()
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("verifier_search_input")
                )

                val scanIconInteraction = remember { MutableInteractionSource() }
                Surface(
                    modifier = Modifier
                        .size(52.dp)
                        .pvsBouncyScale(scanIconInteraction)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = scanIconInteraction,
                            indication = androidx.compose.material3.ripple()
                        ) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            focusManager.clearFocus()
                            showScanner = true
                        }
                        .testTag("open_barcode_scanner_button"),
                    shape = RoundedCornerShape(18.dp),
                    color = PvsMintContainer,
                    border = BorderStroke(1.dp, PvsLeafVibrant.copy(alpha = 0.5f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = strings.scannerOpenBtn,
                            tint = PvsForestDark,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }

                val searchBtnInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        focusManager.clearFocus()
                        onSubmitSearch()
                    },
                    enabled = state.searchQuery.isNotBlank(),
                    interactionSource = searchBtnInteraction,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PvsForestGreen),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 15.dp),
                    modifier = Modifier
                        .pvsBouncyScale(searchBtnInteraction)
                        .testTag("verifier_analyze_button")
                ) {
                    Text(
                        text = strings.verifierSearchAction,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Preloaded Examples
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.verifierExamplesLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProductVerifierCatalog.featuredCodes.forEach { code ->
                        val item = ProductVerifierCatalog.findByQuery(code)
                        val isSelected = state.verifiedProduct?.code.equals(code, ignoreCase = true)
                        ExampleCodeChip(
                            code = code,
                            verdict = item?.verdict ?: PriceVerdict.GREEN,
                            selected = isSelected,
                            onClick = {
                                focusManager.clearFocus()
                                onSelectExample(code)
                            }
                        )
                    }
                }
            }

            // Recent Searches (last 5)
            AnimatedVisibility(
                visible = state.recentSearches.isNotEmpty(),
                enter = fadeIn(tween(220)) + expandVertically(),
                exit = fadeOut(tween(160)) + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recent_searches_section"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = PvsForestGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = strings.verifierRecentSearchesLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = onClearRecentSearches,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("clear_recent_searches_button")
                        ) {
                            Text(
                                text = strings.verifierClearRecent,
                                style = MaterialTheme.typography.labelMedium,
                                color = PvsForestGreen
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.recentSearches.forEach { recent ->
                            val isSelected = state.verifiedProduct?.code.equals(recent.code, ignoreCase = true)
                            val recentInteraction = remember { MutableInteractionSource() }
                            Surface(
                                modifier = Modifier
                                    .pvsBouncyScale(recentInteraction)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(
                                        interactionSource = recentInteraction,
                                        indication = androidx.compose.material3.ripple()
                                    ) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        focusManager.clearFocus()
                                        onSelectExample(recent.code)
                                    }
                                    .testTag("recent_chip_${recent.code}"),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) PvsMintContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) PvsForestGreen else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = recent.code,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = PvsForestDark
                                    )
                                    Text(
                                        text = recent.productName.substringBefore(" "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Step 2 -> 3 -> 4 -> 5 Progressive Result Flow
            AnimatedContent(
                targetState = state.verifiedProduct,
                transitionSpec = {
                    (fadeIn(tween(280)) +
                        scaleIn(
                            initialScale = 0.95f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) +
                        slideInVertically(tween(260)) { it / 12 })
                        .togetherWith(fadeOut(tween(150)))
                },
                label = "verifier_result_card_anim"
            ) { product ->
                if (product != null) {
                    val activeTarget = state.targetForProduct(product.code)
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // PVS Digital Product Pass: Stylized post-scan summary (Name, Price, Code & Est. Biweekly Quota)
                        ScannedProductShowcaseCard(
                            product = product,
                            scannedBarcode = state.lastScannedBarcode,
                            targetPrice = activeTarget?.targetPrice,
                            onRescanClick = {
                                focusManager.clearFocus()
                                showScanner = true
                            },
                            onQuickAddClick = { onAddToCart(product) }
                        )

                        // Step 2: Semáforo + Market Price Comparison
                        VerifiedProductVerdictCard(
                            product = product,
                            currentQuincena = state.totalQuincena
                        )

                        // Step 2a: Interactive Price Target Tracking UI + Below-Target Indicator
                        PriceTargetTrackerCard(
                            product = product,
                            activeTarget = activeTarget,
                            savedTargets = state.priceTargets.values.sortedByDescending { it.updatedAt },
                            onSaveTarget = { newTarget -> onSetPriceTarget(product, newTarget) },
                            onClearTarget = { onClearPriceTarget(product.code) },
                            onSelectTrackedCode = { code ->
                                focusManager.clearFocus()
                                onSelectExample(code)
                            }
                        )

                        // Step 2b: Price History Visual Dashboard (Last 5 Room Scans + 3M / 6M / 12M)
                        PriceHistoryDashboardCard(
                            product = product,
                            scannedHistory = state.scannedHistoryFor(product.code),
                            onLogScannedPrice = { observedPrice ->
                                onLogScannedPrice(product, observedPrice)
                            },
                            onClearScannedHistory = {
                                onClearScannedHistory(product.code)
                            }
                        )

                        // Step 3: "¿Cuánto dinero tienes?" + Affordability Calculator
                        BudgetAffordabilityCard(
                            availableMoney = state.availableMoney,
                            committedQuincena = state.totalQuincena,
                            freeMoney = state.freeMoney,
                            productImpact = product.biweeklyImpact,
                            onEditClick = { showBudgetDialog = true },
                            onAdjustBudget = onAdjustBudget
                        )

                        // Step 4: AI Specs (Potencia), Durability (Cuánto dura), and Summary
                        AiProductBreakdownCard(
                            aiState = state.aiAnalysisState,
                            onRetry = onRetryAi
                        )

                        // Step 5: Final Single CTA -> "Añadir al Carrito"
                        val cartInteraction = remember { MutableInteractionSource() }
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAddToCart(product)
                            },
                            interactionSource = cartInteraction,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PvsForestGreen,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .pvsBouncyScale(cartInteraction, pressedScale = 0.95f)
                                .testTag("add_verified_to_cuotas_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(21.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.addToCartCta,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (state.aiAnalysisState is AiAnalysisUiState.Loading) {
                    AiProductBreakdownCard(
                        aiState = state.aiAnalysisState,
                        onRetry = onRetryAi
                    )
                } else {
                    NotFoundProductCard(
                        query = state.searchQuery,
                        onSelectExample = onSelectExample
                    )
                }
            }
        }
    }

    if (showBudgetDialog) {
        EditBudgetDialog(
            currentMoney = state.availableMoney,
            onDismiss = { showBudgetDialog = false },
            onConfirm = { newMoney ->
                onUpdateBudget(newMoney)
                showBudgetDialog = false
            }
        )
    }

    if (showScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = { scannedValue ->
                showScanner = false
                onBarcodeScanned(scannedValue)
            },
            onDismiss = { showScanner = false }
        )
    }
}

@Composable
private fun ExampleCodeChip(
    code: String,
    verdict: PriceVerdict,
    selected: Boolean,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val chipInteraction = remember { MutableInteractionSource() }
    val dotColor = when (verdict) {
        PriceVerdict.GREEN -> SemaphoreGreen
        PriceVerdict.YELLOW -> SemaphoreYellowGlow
        PriceVerdict.RED -> SemaphoreRedGlow
    }

    Surface(
        modifier = Modifier
            .pvsBouncyScale(chipInteraction)
            .clip(RoundedCornerShape(50))
            .clickable(
                interactionSource = chipInteraction,
                indication = androidx.compose.material3.ripple(),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .testTag("example_chip_$code"),
        shape = RoundedCornerShape(50),
        color = if (selected) PvsForestGreen else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) PvsForestGreen else MaterialTheme.colorScheme.outline
        ),
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (selected) Color.White else dotColor)
            )
            Text(
                text = code,
                style = MaterialTheme.typography.labelLarge,
                fontFamily = JetBrainsMonoFontFamily,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun VerifiedProductVerdictCard(
    product: ProductCodeInfo,
    currentQuincena: Double
) {
    val strings = LocalAppStrings.current
    val (verdictAccent, verdictBg, verdictBorder) = when (product.verdict) {
        PriceVerdict.GREEN -> Triple(SemaphoreGreen, SemaphoreGreenBg, SemaphoreGreenBorder)
        PriceVerdict.YELLOW -> Triple(SemaphoreYellow, SemaphoreYellowBg, SemaphoreYellowBorder)
        PriceVerdict.RED -> Triple(SemaphoreRed, SemaphoreRedBg, SemaphoreRedBorder)
    }

    val diffPct = product.percentageDifference
    val projectedQuincena = currentQuincena + product.biweeklyImpact
    val impactShare = if (projectedQuincena > 0) {
        (product.biweeklyImpact / projectedQuincena).toFloat().coerceIn(0.08f, 0.92f)
    } else 0.25f

    val animatedImpactShare by animateFloatAsState(
        targetValue = impactShare,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "impact_share_anim"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("verifier_result_card_${product.code}"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.2.dp, verdictBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(verdictBg)
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CodePillBadge(
                            code = product.code,
                            containerColor = Color.White,
                            contentColor = PvsForestDark
                        )
                        Text(
                            text = product.category,
                            style = MaterialTheme.typography.labelMedium,
                            color = verdictAccent
                        )
                    }

                    TrafficLightSemaphore(activeVerdict = product.verdict)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = PvsForestDark
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (diffPct <= 0) Icons.Rounded.TrendingDown else Icons.Rounded.TrendingUp,
                        contentDescription = null,
                        tint = verdictAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.verdictHeadline(product.verdict),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = verdictAccent
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = strings.verdictReason(product.code, product.verdictReason),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PvsForestDark.copy(alpha = 0.85f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PriceMetricBox(
                        title = strings.verifierStorePrice,
                        price = formatUsd(product.storePrice),
                        badgeText = if (diffPct <= 0) {
                            strings.verifierSavingsPct(abs(diffPct))
                        } else {
                            strings.verifierMarkupPct(diffPct)
                        },
                        badgeColor = verdictAccent,
                        badgeBg = verdictBg,
                        modifier = Modifier.weight(1f)
                    )
                    PriceMetricBox(
                        title = strings.verifierAvgPrice,
                        price = formatUsd(product.averagePrice),
                        badgeText = strings.verifierMarketRef,
                        badgeColor = PvsForestGreen,
                        badgeBg = PvsMintSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Storefront,
                                    contentDescription = null,
                                    tint = PvsForestGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.verifierQuincenaImpact,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "+${formatUsd(product.biweeklyImpact)} / qna",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = verdictAccent
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(PvsMintContainer)
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight((1f - animatedImpactShare).coerceAtLeast(0.05f))
                                        .background(PvsForestGreen)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(animatedImpactShare)
                                        .background(verdictAccent)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.verifierPlanSummary(
                                    product.quotasCount,
                                    formatUsd(product.suggestedInitial)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = strings.verifierTotalProjected(formatUsd(projectedQuincena)),
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetAffordabilityCard(
    availableMoney: Double,
    committedQuincena: Double,
    freeMoney: Double,
    productImpact: Double,
    onEditClick: () -> Unit,
    onAdjustBudget: (Double) -> Unit
) {
    val strings = LocalAppStrings.current
    val haptics = LocalHapticFeedback.current
    val remainingAfterPurchase = freeMoney - productImpact
    val canAfford = remainingAfterPurchase >= 0.0

    val statusColor = if (canAfford) SemaphoreGreen else SemaphoreRed
    val statusBg = if (canAfford) SemaphoreGreenBg else SemaphoreRedBg
    val statusBorder = if (canAfford) SemaphoreGreenBorder else SemaphoreRedBorder

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("budget_affordability_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(PvsMintSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = PvsForestGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = strings.budgetTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.budgetEditHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val editInteraction = remember { MutableInteractionSource() }
                Surface(
                    modifier = Modifier
                        .pvsBouncyScale(editInteraction)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = editInteraction,
                            indication = androidx.compose.material3.ripple(),
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEditClick()
                            }
                        )
                        .testTag("edit_budget_button"),
                    shape = RoundedCornerShape(14.dp),
                    color = PvsMintContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formatUsd(availableMoney),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = PvsForestDark
                        )
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = strings.budgetEditHint,
                            tint = PvsForestGreen,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Quick budget adjust pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(-25.0 to "-$25", 25.0 to "+$25", 50.0 to "+$50", 100.0 to "+$100").forEach { (delta, label) ->
                    val chipInt = remember { MutableInteractionSource() }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .pvsBouncyScale(chipInt)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = chipInt,
                                indication = androidx.compose.material3.ripple()
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAdjustBudget(delta)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = PvsForestDark
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = strings.budgetCommittedLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "-${formatUsd(committedQuincena)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = strings.budgetFreeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatUsd(freeMoney),
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (freeMoney >= 0) PvsForestGreen else SemaphoreRed
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = statusBg,
                border = BorderStroke(1.dp, statusBorder)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (canAfford) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (canAfford) {
                            strings.budgetFitsGreat(formatUsd(remainingAfterPurchase))
                        } else {
                            strings.budgetTightWarning(formatUsd(abs(remainingAfterPurchase)))
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }
        }
    }
}

@Composable
private fun AiProductBreakdownCard(
    aiState: AiAnalysisUiState,
    onRetry: () -> Unit
) {
    val strings = LocalAppStrings.current
    val haptics = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_product_breakdown_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, PvsLeafVibrant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(listOf(PvsForestDark, PvsLeafVibrant))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = strings.aiSectionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PvsForestDark
                )
            }

            when (aiState) {
                is AiAnalysisUiState.Idle,
                is AiAnalysisUiState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = PvsLeafVibrant,
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = strings.aiLoadingText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is AiAnalysisUiState.Error -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = SemaphoreRedBg,
                        border = BorderStroke(1.dp, SemaphoreRedBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = aiState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SemaphoreRed
                            )
                            val retryInteraction = remember { MutableInteractionSource() }
                            FilledTonalButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onRetry()
                                },
                                interactionSource = retryInteraction,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.pvsBouncyScale(retryInteraction)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.aiRetryBtn)
                            }
                        }
                    }
                }

                is AiAnalysisUiState.Success -> {
                    val analysis = aiState.analysis
                    AiInfoBlock(
                        icon = Icons.Rounded.Bolt,
                        label = strings.aiPowerLabel,
                        content = analysis.powerAndSpecs
                    )
                    AiInfoBlock(
                        icon = Icons.Rounded.Schedule,
                        label = strings.aiDurabilityLabel,
                        content = analysis.durability
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = PvsMintSurface
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = strings.aiSummaryLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = PvsForestGreen
                            )
                            Text(
                                text = analysis.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PvsForestDark
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiInfoBlock(
    icon: ImageVector,
    label: String,
    content: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PvsForestGreen,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun EditBudgetDialog(
    currentMoney: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val strings = LocalAppStrings.current
    var text by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", currentMoney)) }
    val parsed = text.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.budgetTitle) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(strings.budgetTotalLabel) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (parsed != null) onConfirm(parsed) },
                enabled = parsed != null && parsed >= 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = PvsForestGreen)
            ) {
                Text(strings.actionSave)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionCancel)
            }
        }
    )
}

@Composable
private fun PriceMetricBox(
    title: String,
    price: String,
    badgeText: String,
    badgeColor: Color,
    badgeBg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = price,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = badgeBg
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun NotFoundProductCard(
    query: String,
    onSelectExample: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(PvsMintSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCodeScanner,
                    contentDescription = null,
                    tint = PvsForestGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (query.isBlank()) {
                    strings.verifierSearchPromptTitle
                } else {
                    strings.verifierNotFoundTitle
                },
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.verifierNotFoundBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("TV-50", "CEL-128", "ZAP-01", "AUD-05").forEach { code ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onSelectExample(code) },
                        shape = RoundedCornerShape(50),
                        color = PvsMintContainer
                    ) {
                        Text(
                            text = code,
                            style = MaterialTheme.typography.labelSmall,
                            color = PvsForestDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
