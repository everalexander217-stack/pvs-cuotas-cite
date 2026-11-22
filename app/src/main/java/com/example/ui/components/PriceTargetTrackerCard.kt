package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAlert
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.PriceTargetEntity
import com.example.data.ProductCodeInfo
import com.example.data.ProductVerifierCatalog
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
import com.example.ui.theme.SemaphoreYellow
import com.example.ui.theme.SemaphoreYellowBg
import com.example.ui.theme.SemaphoreYellowBorder
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Interactive Price Target Tracking Card allowing users to set/adjust a target price
 * for any scanned or selected product, complete with a visual gauge and clear alert indicator
 * when the current scanned price drops below their target.
 */
@Composable
fun PriceTargetTrackerCard(
    product: ProductCodeInfo,
    activeTarget: PriceTargetEntity?,
    savedTargets: List<PriceTargetEntity>,
    onSaveTarget: (Double) -> Unit,
    onClearTarget: () -> Unit,
    onSelectTrackedCode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val haptics = LocalHapticFeedback.current
    var showTargetDialog by remember(product.code) { mutableStateOf(false) }

    val targetPrice = activeTarget?.targetPrice
    val isBelowTarget = targetPrice != null && product.storePrice < targetPrice
    val isExactTarget = targetPrice != null && abs(product.storePrice - targetPrice) < 0.01
    val diffAmount = targetPrice?.let { abs(it - product.storePrice) } ?: 0.0

    val cardBorderColor by animateColorAsState(
        targetValue = when {
            isBelowTarget -> SemaphoreGreen
            targetPrice != null -> PvsLeafVibrant.copy(alpha = 0.55f)
            else -> MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(260),
        label = "tracker_border_color"
    )

    val badgeBgColor by animateColorAsState(
        targetValue = when {
            isBelowTarget || isExactTarget -> SemaphoreGreenBg
            targetPrice != null -> SemaphoreYellowBg
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "tracker_badge_bg"
    )

    val badgeTextColor by animateColorAsState(
        targetValue = when {
            isBelowTarget || isExactTarget -> SemaphoreGreen
            targetPrice != null -> SemaphoreYellow
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "tracker_badge_text"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "target_beacon_pulse")
    val beaconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("price_target_tracker_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isBelowTarget) 4.dp else 2.dp),
        border = BorderStroke(if (isBelowTarget) 1.5.dp else 1.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row + Animated Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = if (isBelowTarget) {
                                        listOf(PvsForestDark, SemaphoreGreen)
                                    } else {
                                        listOf(PvsForestDark, PvsForestGreen)
                                    }
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBelowTarget) {
                                Icons.Rounded.NotificationsActive
                            } else {
                                Icons.Rounded.TrackChanges
                            },
                            contentDescription = null,
                            tint = PvsLeafBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = strings.targetPriceTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.targetPriceSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeBgColor,
                    border = BorderStroke(
                        1.dp,
                        when {
                            isBelowTarget || isExactTarget -> SemaphoreGreenBorder
                            targetPrice != null -> SemaphoreYellowBorder
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    modifier = Modifier
                        .semantics(mergeDescendants = true) {}
                        .testTag("target_price_status_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (isBelowTarget) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SemaphoreGreen.copy(alpha = beaconAlpha))
                            )
                        }
                        Text(
                            text = when {
                                targetPrice == null -> strings.targetPriceNoTargetBadge
                                isBelowTarget -> strings.targetPriceBelowBadge(formatUsd(diffAmount))
                                isExactTarget -> strings.targetPriceAtBadge
                                else -> strings.targetPriceAboveBadge(formatUsd(diffAmount))
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor
                        )
                    }
                }
            }

            // Prominent Below-Target Visual Indicator Banner
            AnimatedVisibility(
                visible = isBelowTarget,
                enter = fadeIn(tween(240)) + expandVertically(),
                exit = fadeOut(tween(160)) + shrinkVertically()
            ) {
                if (targetPrice != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("below_target_indicator_banner"),
                        shape = RoundedCornerShape(16.dp),
                        color = SemaphoreGreenBg,
                        border = BorderStroke(1.2.dp, SemaphoreGreenBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = SemaphoreGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = strings.targetPriceBelowBanner(
                                    formatUsd(product.storePrice),
                                    formatUsd(diffAmount),
                                    formatUsd(targetPrice)
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = SemaphoreGreen
                            )
                        }
                    }
                }
            }

            // Dual Price Comparison Strip + Visual Threshold Gauge when a target is set
            if (targetPrice != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = strings.targetPriceCurrentLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatUsd(product.storePrice),
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (isBelowTarget) SemaphoreGreen else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    val editTargetInt = remember { MutableInteractionSource() }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .pvsBouncyScale(editTargetInt)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = editTargetInt,
                                indication = androidx.compose.material3.ripple()
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showTargetDialog = true
                            }
                            .testTag("target_price_value_tile"),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        border = BorderStroke(1.dp, PvsLeafVibrant.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.targetPriceYourGoalLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = formatUsd(targetPrice),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("active_target_price_text")
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = strings.targetPriceEditBtn,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Visual Target Threshold Bar
                PriceTargetVisualGauge(
                    currentPrice = product.storePrice,
                    targetPrice = targetPrice,
                    isBelowTarget = isBelowTarget
                )

                if (!isBelowTarget && !isExactTarget) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TrendingUp,
                            contentDescription = null,
                            tint = SemaphoreYellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = strings.targetPriceAboveHint(
                                formatUsd(diffAmount),
                                formatUsd(targetPrice)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = SemaphoreYellow,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Fine-tune Slider
                val sliderMin = (product.storePrice * 0.50).toFloat().coerceAtLeast(1f)
                val sliderMax = (max(product.storePrice, targetPrice) * 1.35).toFloat().coerceAtLeast(sliderMin + 10f)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.targetPriceSliderLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onClearTarget()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("clear_target_price_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.targetPriceClearBtn,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Slider(
                        value = targetPrice.toFloat().coerceIn(sliderMin, sliderMax),
                        onValueChange = { value ->
                            val rounded = (value * 1.0).roundToInt().toDouble().coerceAtLeast(1.0)
                            onSaveTarget(rounded)
                        },
                        valueRange = sliderMin..sliderMax,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = PvsLeafVibrant,
                            inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("target_price_slider")
                    )
                }
            } else {
                // CTA to set first target price
                val setBtnInt = remember { MutableInteractionSource() }
                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showTargetDialog = true
                    },
                    interactionSource = setBtnInt,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, PvsLeafVibrant.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .pvsBouncyScale(setBtnInt)
                        .testTag("set_target_price_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddAlert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.targetPriceSetBtn,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quick Percentage Target Presets (-5%, -10%, -15%, Custom)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(5, 10, 15).forEach { discountPct ->
                    val presetPrice = ((product.storePrice * (1.0 - discountPct / 100.0)) * 100.0).roundToInt() / 100.0
                    val isPresetSelected = targetPrice != null && abs(targetPrice - presetPrice) < 0.05
                    val presetInt = remember { MutableInteractionSource() }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .pvsBouncyScale(presetInt)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = presetInt,
                                indication = androidx.compose.material3.ripple()
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSaveTarget(presetPrice)
                            }
                            .testTag("target_preset_${discountPct}_pct"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPresetSelected) PvsForestGreen else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (isPresetSelected) PvsForestGreen else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "-$discountPct%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isPresetSelected) Color.White else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = formatUsd(presetPrice),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = if (isPresetSelected) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Saved Targets Quick Switcher (when multiple products have tracked targets)
            val otherSaved = savedTargets.filter { !it.code.equals(product.code, ignoreCase = true) }
            if (otherSaved.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = strings.targetPriceWatchlistLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        otherSaved.forEach { saved ->
                            val catalogProduct = ProductVerifierCatalog.findByQuery(saved.code)
                            val savedIsBelow = catalogProduct != null && catalogProduct.storePrice < saved.targetPrice
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectTrackedCode(saved.code)
                                    },
                                shape = RoundedCornerShape(50),
                                color = if (savedIsBelow) SemaphoreGreenBg else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (savedIsBelow) SemaphoreGreenBorder else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (savedIsBelow) {
                                        Icon(
                                            imageVector = Icons.Rounded.TrendingDown,
                                            contentDescription = null,
                                            tint = SemaphoreGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = "${saved.code} · ${formatUsd(saved.targetPrice)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = if (savedIsBelow) SemaphoreGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTargetDialog) {
        EditTargetPriceDialog(
            initialTarget = targetPrice ?: ((product.storePrice * 0.90 * 100.0).roundToInt() / 100.0),
            onDismiss = { showTargetDialog = false },
            onConfirm = { newTarget ->
                onSaveTarget(newTarget)
                showTargetDialog = false
            }
        )
    }
}

@Composable
private fun PriceTargetVisualGauge(
    currentPrice: Double,
    targetPrice: Double,
    isBelowTarget: Boolean
) {
    val minVal = min(currentPrice, targetPrice) * 0.65
    val maxVal = max(currentPrice, targetPrice) * 1.15
    val span = (maxVal - minVal).coerceAtLeast(1.0)

    val rawCurrentFraction = ((currentPrice - minVal) / span).toFloat().coerceIn(0.05f, 0.95f)
    val rawTargetFraction = ((targetPrice - minVal) / span).toFloat().coerceIn(0.05f, 0.95f)

    val animatedCurrent by animateFloatAsState(
        targetValue = rawCurrentFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "gauge_current_pos"
    )
    val animatedTarget by animateFloatAsState(
        targetValue = rawTargetFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "gauge_target_pos"
    )

    val trackColor = MaterialTheme.colorScheme.primaryContainer
    val pinColor = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
    ) {
        val barHeight = 8.dp.toPx()
        val cy = size.height / 2f
        val barTop = cy - barHeight / 2f

        // Base track
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, barTop),
            size = Size(size.width, barHeight),
            cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
        )

        // Safe green target zone up to targetPrice marker
        val targetX = size.width * animatedTarget
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(PvsForestGreen, SemaphoreGreen)),
            topLeft = Offset(0f, barTop),
            size = Size(targetX.coerceAtLeast(barHeight), barHeight),
            cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
        )

        // Vertical Target Price pin line
        drawLine(
            color = pinColor,
            start = Offset(targetX, 2.dp.toPx()),
            end = Offset(targetX, size.height - 2.dp.toPx()),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Current Scanned Price dot
        val currentX = size.width * animatedCurrent
        val dotColor = if (isBelowTarget) SemaphoreGreen else SemaphoreYellow
        drawCircle(
            color = Color.White,
            radius = 8.dp.toPx(),
            center = Offset(currentX, cy)
        )
        drawCircle(
            color = dotColor,
            radius = 5.5.dp.toPx(),
            center = Offset(currentX, cy)
        )
    }
}

@Composable
private fun EditTargetPriceDialog(
    initialTarget: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val strings = LocalAppStrings.current
    var text by remember {
        mutableStateOf(String.format(Locale.US, "%.2f", initialTarget))
    }
    val parsed = text.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.targetPriceDialogTitle) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(strings.targetPriceDialogField) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("target_price_dialog_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { if (parsed != null && parsed >= 1.0) onConfirm(parsed) },
                enabled = parsed != null && parsed >= 1.0,
                colors = ButtonDefaults.buttonColors(containerColor = PvsForestGreen),
                modifier = Modifier.testTag("target_price_dialog_confirm")
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
