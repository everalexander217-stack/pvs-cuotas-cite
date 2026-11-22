package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CategoryCatalog
import com.example.data.CategoryMetricMode
import com.example.data.CategorySlice
import com.example.data.InstallmentEntity
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.PvsForestGreen
import com.example.ui.theme.PvsLeafVibrant
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun CategoryPieChartCard(
    installments: List<InstallmentEntity>,
    onManageCategories: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val haptics = LocalHapticFeedback.current

    var metricMode by remember { mutableStateOf(CategoryMetricMode.BIWEEKLY) }
    val biweeklySlices = remember(installments) {
        CategoryCatalog.computeDistribution(installments, CategoryMetricMode.BIWEEKLY)
    }
    val totalSlices = remember(installments) {
        CategoryCatalog.computeDistribution(installments, CategoryMetricMode.TOTAL)
    }

    val effectiveSlices = when (metricMode) {
        CategoryMetricMode.BIWEEKLY -> if (biweeklySlices.isNotEmpty()) biweeklySlices else totalSlices
        CategoryMetricMode.TOTAL -> totalSlices
    }
    val activeMetricLabel = if (metricMode == CategoryMetricMode.BIWEEKLY && biweeklySlices.isNotEmpty()) {
        strings.dashboardPieMetricBiweekly
    } else {
        strings.dashboardPieMetricTotal
    }

    var selectedCategory by remember(effectiveSlices) {
        mutableStateOf(effectiveSlices.firstOrNull()?.category)
    }

    val selectedSlice = effectiveSlices.firstOrNull {
        it.category.equals(selectedCategory, ignoreCase = true)
    } ?: effectiveSlices.firstOrNull()

    var chartPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(effectiveSlices.size) {
        chartPlayed = true
    }
    val sweepProgress by animateFloatAsState(
        targetValue = if (chartPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing),
        label = "pie_sweep_progress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_pie_chart_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header row with title + optional manage tags CTA
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = strings.dashboardPieTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.dashboardPieSubtitle(effectiveSlices.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (onManageCategories != null) {
                    TextButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onManageCategories()
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("pie_manage_categories_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.dashboardPieManageAction,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Segmented Metric Toggle: Quincenal vs Total
            if (biweeklySlices.isNotEmpty() || totalSlices.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MetricTogglePill(
                            label = strings.dashboardPieMetricBiweekly,
                            selected = metricMode == CategoryMetricMode.BIWEEKLY,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                metricMode = CategoryMetricMode.BIWEEKLY
                            },
                            testTag = "pie_metric_biweekly",
                            modifier = Modifier.weight(1f)
                        )
                        MetricTogglePill(
                            label = strings.dashboardPieMetricTotal,
                            selected = metricMode == CategoryMetricMode.TOTAL,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                metricMode = CategoryMetricMode.TOTAL
                            },
                            testTag = "pie_metric_total",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (effectiveSlices.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pie_empty_state")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = strings.dashboardPieEmptyTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.dashboardPieEmptyBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val grandTotal = remember(effectiveSlices) { effectiveSlices.sumOf { it.amount } }
                val cardSurfaceColor = MaterialTheme.colorScheme.surface

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val sideBySide = maxWidth >= 460.dp

                    if (sideBySide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(0.9f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PieChartCanvas(
                                    slices = effectiveSlices,
                                    selectedSlice = selectedSlice,
                                    sweepProgress = sweepProgress,
                                    separatorColor = cardSurfaceColor,
                                    onSelectSlice = { slice ->
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedCategory = slice.category
                                    }
                                )
                                SelectedSliceBanner(
                                    slice = selectedSlice,
                                    metricLabel = activeMetricLabel,
                                    grandTotal = grandTotal
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1.1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                effectiveSlices.forEach { slice ->
                                    CategoryLegendRow(
                                        slice = slice,
                                        isSelected = slice.category.equals(selectedSlice?.category, ignoreCase = true),
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedCategory = slice.category
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            PieChartCanvas(
                                slices = effectiveSlices,
                                selectedSlice = selectedSlice,
                                sweepProgress = sweepProgress,
                                separatorColor = cardSurfaceColor,
                                onSelectSlice = { slice ->
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedCategory = slice.category
                                }
                            )

                            SelectedSliceBanner(
                                slice = selectedSlice,
                                metricLabel = activeMetricLabel,
                                grandTotal = grandTotal
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                effectiveSlices.forEach { slice ->
                                    CategoryLegendRow(
                                        slice = slice,
                                        isSelected = slice.category.equals(selectedSlice?.category, ignoreCase = true),
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedCategory = slice.category
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

@Composable
private fun MetricTogglePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (selected) PvsForestGreen else Color.Transparent,
        label = "pie_metric_bg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "pie_metric_fg"
    )
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        color = bg
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = fg
            )
        }
    }
}

@Composable
private fun PieChartCanvas(
    slices: List<CategorySlice>,
    selectedSlice: CategorySlice?,
    sweepProgress: Float,
    separatorColor: Color,
    onSelectSlice: (CategorySlice) -> Unit
) {
    val latestSlices by rememberUpdatedState(slices)

    Box(
        modifier = Modifier
            .size(196.dp)
            .testTag("category_pie_canvas_box"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(188.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val currentSlices = latestSlices
                        if (currentSlices.isEmpty()) return@detectTapGestures
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val radius = min(size.width, size.height) / 2f
                        if (sqrt(dx * dx + dy * dy) > radius) return@detectTapGestures

                        val touchAngle = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f) + 360f) % 360f
                        var acc = 0f
                        for (slice in currentSlices) {
                            val sweep = slice.percentage * 360f
                            if (touchAngle >= acc && touchAngle <= acc + sweep) {
                                onSelectSlice(slice)
                                break
                            }
                            acc += sweep
                        }
                    }
                }
        ) {
            val baseDiameter = min(size.width, size.height) - 18.dp.toPx()
            val radius = baseDiameter / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val popOffsetPx = 7.dp.toPx()

            var startAngle = -90f
            slices.forEach { slice ->
                val fullSweep = slice.percentage * 360f
                val animatedSweep = (fullSweep * sweepProgress).coerceAtLeast(0.5f)
                val isSelected = slice.category.equals(selectedSlice?.category, ignoreCase = true)

                val midAngleRad = Math.toRadians((startAngle + fullSweep / 2f).toDouble())
                val shiftX = if (isSelected && slices.size > 1) (cos(midAngleRad) * popOffsetPx).toFloat() else 0f
                val shiftY = if (isSelected && slices.size > 1) (sin(midAngleRad) * popOffsetPx).toFloat() else 0f

                val arcTopLeft = Offset(center.x - radius + shiftX, center.y - radius + shiftY)
                val arcSize = Size(baseDiameter, baseDiameter)

                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = animatedSweep,
                    useCenter = true,
                    topLeft = arcTopLeft,
                    size = arcSize
                )

                if (slices.size > 1) {
                    drawArc(
                        color = separatorColor,
                        startAngle = startAngle,
                        sweepAngle = animatedSweep,
                        useCenter = true,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }

                startAngle += fullSweep
            }
        }
    }
}

@Composable
private fun SelectedSliceBanner(
    slice: CategorySlice?,
    metricLabel: String,
    grandTotal: Double
) {
    if (slice == null) return
    val strings = LocalAppStrings.current
    val pctText = "${(slice.percentage * 100f).roundToInt()}%"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .testTag("pie_selected_summary"),
        shape = RoundedCornerShape(16.dp),
        color = slice.color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, slice.color.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
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
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(slice.color),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CategoryCatalog.iconForCategory(slice.category),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "${slice.category} · $pctText",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${strings.dashboardPiePlanCount(slice.planCount)} · $metricLabel (${formatUsd(grandTotal)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = formatUsd(slice.amount),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CategoryLegendRow(
    slice: CategorySlice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val interactionSource = remember { MutableInteractionSource() }
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) slice.color.copy(alpha = 0.11f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        label = "legend_row_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) slice.color.copy(alpha = 0.65f) else Color.Transparent,
        label = "legend_row_border"
    )
    val pct = "${(slice.percentage * 100f).roundToInt()}%"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pvsBouncyScale(interactionSource, pressedScale = 0.98f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .testTag("pie_legend_item_${slice.category}"),
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(slice.color)
            )
            Icon(
                imageVector = CategoryCatalog.iconForCategory(slice.category),
                contentDescription = null,
                tint = slice.color,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slice.category,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = strings.dashboardPiePlanCount(slice.planCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatUsd(slice.amount),
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = pct,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = if (isSelected) slice.color else PvsLeafVibrant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
