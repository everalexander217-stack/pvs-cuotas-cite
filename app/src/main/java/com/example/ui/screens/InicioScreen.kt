package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.PriceVerdict
import com.example.data.ProductVerifierCatalog
import com.example.ui.components.CoachMarkStepId
import com.example.ui.components.coachMarkTarget
import com.example.ui.PvsUiState
import com.example.ui.components.CategoryPieChartCard
import com.example.ui.components.PvsCurvedWaveOverlay
import com.example.ui.components.PvsHeaderBar
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
import com.example.ui.theme.SemaphoreRedGlow
import com.example.ui.theme.SemaphoreYellowGlow
import kotlinx.coroutines.delay

@Composable
fun InicioScreen(
    state: PvsUiState,
    onGoToScan: () -> Unit,
    onGoToQuotas: () -> Unit,
    onQuickSelectCode: (String) -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenTutorial: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val haptics = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(60)
        showContent = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 28.dp)
            .testTag("inicio_hub_screen")
    ) {
        PvsHeaderBar(
            title = strings.homeWelcomeTitle,
            subtitle = strings.homeWelcomeSubtitle,
            language = state.language,
            alertCount = state.upcomingAlerts.size,
            onToggleLanguage = onToggleLanguage,
            onOpenNotifications = onOpenNotifications,
            onOpenTutorial = onOpenTutorial
        )

        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(340)) + slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) { it / 10 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Financial Overview Curved PVS Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.homeSummaryQuincena,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PvsMintContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatUsd(state.totalQuincena),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = strings.activePlans(state.activeInstallments.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PvsLeafBright
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(46.dp)
                                    .background(Color.White.copy(alpha = 0.2f))
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.homeSummaryAvailable,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PvsMintContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatUsd(state.freeMoney),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = PvsLeafBright
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = strings.budgetFreeLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PvsMintContainer.copy(alpha = 0.88f)
                                )
                            }
                        }
                    }
                }

                // Main Options Section Header
                Text(
                    text = strings.homeMainOptionsLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Primary Option 1: Escanear o Verificar Producto
                PrimaryHubOptionCard(
                    title = strings.homeScanCardTitle,
                    description = strings.homeScanCardDesc,
                    badge = strings.homeScanCardBadge,
                    icon = Icons.Rounded.QrCodeScanner,
                    trailingBadgeIcon = Icons.Rounded.AutoAwesome,
                    isHighlighted = true,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onGoToScan()
                    },
                    testTag = "home_option_scan",
                    modifier = Modifier.coachMarkTarget(CoachMarkStepId.SCAN_ENTRY)
                )

                // Primary Option 2: Gestión de Cuotas
                PrimaryHubOptionCard(
                    title = strings.homeQuotasCardTitle,
                    description = strings.homeQuotasCardDesc,
                    badge = strings.homeQuotasCardBadge(state.activeInstallments.size),
                    icon = Icons.Rounded.ReceiptLong,
                    trailingBadgeIcon = null,
                    isHighlighted = false,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onGoToQuotas()
                    },
                    testTag = "home_option_quotas"
                )

                // Spending Distribution Pie Chart by Category
                CategoryPieChartCard(
                    installments = state.installments,
                    onManageCategories = onGoToQuotas
                )

                // Quick Code Strip for direct 1-tap verification
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.homeQuickCodesTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProductVerifierCatalog.featuredCodes.forEach { code ->
                            val item = ProductVerifierCatalog.findByQuery(code)
                            val dotColor = when (item?.verdict ?: PriceVerdict.GREEN) {
                                PriceVerdict.GREEN -> SemaphoreGreen
                                PriceVerdict.YELLOW -> SemaphoreYellowGlow
                                PriceVerdict.RED -> SemaphoreRedGlow
                            }
                            val chipInt = remember { MutableInteractionSource() }
                            Surface(
                                modifier = Modifier
                                    .pvsBouncyScale(chipInt)
                                    .clip(RoundedCornerShape(50))
                                    .clickable(
                                        interactionSource = chipInt,
                                        indication = androidx.compose.material3.ripple()
                                    ) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onQuickSelectCode(code)
                                    }
                                    .testTag("home_quick_code_$code"),
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                    Text(
                                        text = code,
                                        style = MaterialTheme.typography.labelLarge,
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
    }
}

@Composable
private fun PrimaryHubOptionCard(
    title: String,
    description: String,
    badge: String,
    icon: ImageVector,
    trailingBadgeIcon: ImageVector?,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val cardBg = if (isHighlighted) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    val borderColor = if (isHighlighted) PvsLeafVibrant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
    val accentCircleTint = if (isHighlighted) {
        PvsLeafBright.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    }
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = modifier
            .fillMaxWidth()
            .pvsBouncyScale(interactionSource, pressedScale = 0.96f)
            .clip(RoundedCornerShape(26.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .testTag(testTag),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 3.dp else 2.dp),
        border = BorderStroke(1.2.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle curved botanical circle accent in corner
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = accentCircleTint,
                    radius = size.minDimension * 0.55f,
                    center = Offset(size.width * 0.94f, size.height * 0.16f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isHighlighted) {
                                Brush.linearGradient(listOf(PvsForestDark, PvsLeafVibrant))
                            } else {
                                Brush.linearGradient(listOf(primaryContainer, secondaryContainer))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isHighlighted) Color.White else onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (trailingBadgeIcon != null) {
                                Icon(
                                    imageVector = trailingBadgeIcon,
                                    contentDescription = null,
                                    tint = onPrimaryContainer,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = onPrimaryContainer
                            )
                        }
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isHighlighted) PvsForestGreen else primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = title,
                        tint = if (isHighlighted) Color.White else onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
