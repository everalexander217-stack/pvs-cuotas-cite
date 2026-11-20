package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.ShowChart
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PriceHistoryPoint
import com.example.data.PriceVerdict
import com.example.data.ProductCodeInfo
import com.example.data.ProductVerifierCatalog
import com.example.data.ScannedPriceEntity
import com.example.ui.theme.AppLanguage
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
import com.example.ui.theme.SemaphoreRed
import com.example.ui.theme.SemaphoreRedBg
import com.example.ui.theme.SemaphoreYellow
import com.example.ui.theme.SemaphoreYellowBg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Interactive native Jetpack Compose price history trend dashboard combining:
 * - Room-persisted "Last 5 Scanned Prices" local trend mode (`selectedMonths == 0`)
 * - 3M / 6M / 12M market trend views with cubic Bezier splines, dual series, KPI strip,
 *   clickable scan chips, and touch/drag inspection scrubbing.
 */
@Composable
fun PriceHistoryDashboardCard(
    product: ProductCodeInfo,
    scannedHistory: List<ScannedPriceEntity> = emptyList(),
    onLogScannedPrice: ((Double) -> Unit)? = null,
    onClearScannedHistory: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val haptics = LocalHapticFeedback.current
    // 0 = Room local last-5 scanned prices; 3 / 6 / 12 = monthly benchmark trend
    var userPickedMode by remember(product.code) { mutableStateOf(false) }
    var selectedMonths by remember(product.code) {
        mutableIntStateOf(if (scannedHistory.isNotEmpty()) 0 else 6)
    }
    LaunchedEffect(product.code, scannedHistory.size) {
        if (!userPickedMode && scannedHistory.isNotEmpty()) {
            selectedMonths = 0
        }
    }
    var showLogPriceDialog by remember(product.code) { mutableStateOf(false) }

    val points = remember(
        product.code,
        product.storePrice,
        product.averagePrice,
        selectedMonths,
        scannedHistory
    ) {
        if (selectedMonths == 0) {
            if (scannedHistory.isEmpty()) {
                listOf(
                    PriceHistoryPoint(
                        labelEs = strings.historyScannedPointLabel(1),
                        labelEn = strings.historyScannedPointLabel(1),
                        storePrice = product.storePrice,
                        marketAvgPrice = product.averagePrice
                    )
                )
            } else {
                scannedHistory.mapIndexed { idx, entry ->
                    val shortLabel = "#${idx + 1}"
                    PriceHistoryPoint(
                        labelEs = shortLabel,
                        labelEn = shortLabel,
                        storePrice = entry.scannedPrice,
                        marketAvgPrice = entry.marketAvgPrice
                    )
                }
            }
        } else {
            ProductVerifierCatalog.priceHistoryFor(product, selectedMonths)
        }
    }

    var selectedIndex by remember(product.code, selectedMonths, points.size) {
        mutableIntStateOf(points.lastIndex.coerceAtLeast(0))
    }

    val drawProgress = remember { Animatable(0f) }
    LaunchedEffect(product.code, selectedMonths, scannedHistory.size) {
        drawProgress.snapTo(0f)
        drawProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    val minStore = points.minOfOrNull { it.storePrice } ?: product.storePrice
    val maxStore = points.maxOfOrNull { it.storePrice } ?: product.storePrice
    val firstPrice = points.firstOrNull()?.storePrice ?: product.storePrice
    val lastPrice = points.lastOrNull()?.storePrice ?: product.storePrice
    val trendPct = if (firstPrice > 0.0) {
        (((lastPrice - firstPrice) / firstPrice) * 100.0).roundToInt()
    } else 0

    val verdictLineColor = when (product.verdict) {
        PriceVerdict.GREEN -> PvsLeafVibrant
        PriceVerdict.YELLOW -> SemaphoreYellow
        PriceVerdict.RED -> SemaphoreRed
    }
    val trendBadgeColors = when {
        trendPct <= 0 -> SemaphoreGreen to SemaphoreGreenBg
        trendPct <= 10 -> SemaphoreYellow to SemaphoreYellowBg
        else -> SemaphoreRed to SemaphoreRedBg
    }

    val activePoint = points.getOrElse(selectedIndex.coerceIn(0, points.lastIndex.coerceAtLeast(0))) {
        PriceHistoryPoint("Hoy", "Now", product.storePrice, product.averagePrice)
    }
    val activePointLabel = if (selectedMonths == 0) {
        strings.historyScannedPointLabel(selectedIndex.coerceIn(0, points.lastIndex.coerceAtLeast(0)) + 1)
    } else if (strings.language == AppLanguage.ES) {
        activePoint.labelEs
    } else {
        activePoint.labelEn
    }
    val pointDiff = activePoint.storePrice - activePoint.marketAvgPrice
    val formattedDiff = if (pointDiff <= 0.0) {
        "-${formatUsd(-pointDiff)}"
    } else {
        "+${formatUsd(pointDiff)}"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("price_history_dashboard_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, PvsLeafVibrant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header + Mode / Range Picker (5 Scans / 3M / 6M / 12M)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    Brush.linearGradient(listOf(PvsForestDark, PvsForestGreen))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ShowChart,
                                contentDescription = null,
                                tint = PvsLeafBright,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = strings.historyTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PvsForestDark
                            )
                            Text(
                                text = if (selectedMonths == 0) {
                                    strings.historyScannedSubtitle(scannedHistory.size)
                                } else {
                                    strings.historySubtitle
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("price_history_subtitle")
                            )
                        }
                    }

                    if (selectedMonths == 0 && scannedHistory.isNotEmpty() && onClearScannedHistory != null) {
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onClearScannedHistory()
                            },
                            modifier = Modifier.testTag("clear_scanned_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = strings.verifierClearRecent,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Segmented Selector: 5 Scans (Room DB) | 3M | 6M | 12M
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val scansSelected = selectedMonths == 0
                        val scansInt = remember { MutableInteractionSource() }
                        Surface(
                            modifier = Modifier
                                .weight(1.35f)
                                .pvsBouncyScale(scansInt)
                                .clip(RoundedCornerShape(11.dp))
                                .clickable(
                                    interactionSource = scansInt,
                                    indication = androidx.compose.material3.ripple()
                                ) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    userPickedMode = true
                                    selectedMonths = 0
                                }
                                .semantics(mergeDescendants = true) {}
                                .testTag("history_mode_scans"),
                            shape = RoundedCornerShape(11.dp),
                            color = if (scansSelected) PvsForestGreen else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = if (scansSelected) PvsLeafBright else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.historyModeScansChip,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (scansSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        listOf(3, 6, 12).forEach { months ->
                            val isSelected = selectedMonths == months
                            val chipInt = remember { MutableInteractionSource() }
                            Surface(
                                modifier = Modifier
                                    .weight(0.75f)
                                    .pvsBouncyScale(chipInt)
                                    .clip(RoundedCornerShape(11.dp))
                                    .clickable(
                                        interactionSource = chipInt,
                                        indication = androidx.compose.material3.ripple()
                                    ) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        userPickedMode = true
                                        selectedMonths = months
                                    }
                                    .semantics(mergeDescendants = true) {}
                                    .testTag("history_range_$months"),
                                shape = RoundedCornerShape(11.dp),
                                color = if (isSelected) PvsForestGreen else Color.Transparent
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = "${months}M",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // KPI Summary Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DashboardKpiTile(
                    label = strings.historyMinLabel,
                    value = formatUsd(minStore),
                    valueColor = PvsForestGreen,
                    bgColor = PvsMintSurface,
                    modifier = Modifier.weight(1f)
                )
                DashboardKpiTile(
                    label = strings.historyMaxLabel,
                    value = formatUsd(maxStore),
                    valueColor = PvsForestDark,
                    bgColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = trendBadgeColors.second
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = strings.historyTrendLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = if (trendPct <= 0) {
                                    Icons.Rounded.TrendingDown
                                } else {
                                    Icons.Rounded.TrendingUp
                                },
                                contentDescription = null,
                                tint = trendBadgeColors.first,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${if (trendPct > 0) "+" else ""}$trendPct%",
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = trendBadgeColors.first
                            )
                        }
                    }
                }
            }

            // Active Scrubbed Point Tooltip Pill
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = PvsForestDark
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CodePillBadge(
                            code = activePointLabel,
                            containerColor = PvsForestGreen,
                            contentColor = Color.White
                        )
                        Column {
                            Text(
                                text = "${strings.historyLegendStore}: ${formatUsd(activePoint.storePrice)}",
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${strings.historyLegendMarket}: ${formatUsd(activePoint.marketAvgPrice)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = PvsMintContainer
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (pointDiff <= 0.0) {
                            PvsLeafVibrant.copy(alpha = 0.25f)
                        } else {
                            SemaphoreRed.copy(alpha = 0.28f)
                        }
                    ) {
                        Text(
                            text = strings.historyDeltaVsMarket(formattedDiff),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (pointDiff <= 0.0) PvsLeafBright else Color(0xFFFFB4AB),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Interactive Area + Dashed Benchmark Line Chart Canvas
            val textMeasurer = rememberTextMeasurer()
            val labelStyle = TextStyle(
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val activeLabelStyle = labelStyle.copy(
                color = PvsForestDark,
                fontWeight = FontWeight.Bold
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(216.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PvsMintSurface.copy(alpha = 0.45f))
                    .padding(top = 10.dp, bottom = 8.dp, start = 8.dp, end = 12.dp)
            ) {
                if (selectedMonths == 0) {
                    VicoScannedPriceLineChart(
                        points = points,
                        primaryColor = verdictLineColor,
                        contentDesc = strings.historyScannedSubtitle(scannedHistory.size),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .testTag("price_history_canvas")
                            .pointerInput(points.size) {
                                val leftMarginPx = 46.dp.toPx()
                                val rightPadPx = 8.dp.toPx()
                                fun updateIndexAt(x: Float) {
                                    if (points.size <= 1) return
                                    val chartWidth = (size.width - leftMarginPx - rightPadPx).coerceAtLeast(1f)
                                    val fraction = ((x - leftMarginPx) / chartWidth).coerceIn(0f, 1f)
                                    val idx = (fraction * (points.size - 1)).roundToInt().coerceIn(0, points.lastIndex)
                                    if (idx != selectedIndex) {
                                        selectedIndex = idx
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                                detectTapGestures { offset ->
                                    updateIndexAt(offset.x)
                                }
                            }
                            .pointerInput(points.size) {
                                val leftMarginPx = 46.dp.toPx()
                                val rightPadPx = 8.dp.toPx()
                                fun updateIndexAt(x: Float) {
                                    if (points.size <= 1) return
                                    val chartWidth = (size.width - leftMarginPx - rightPadPx).coerceAtLeast(1f)
                                    val fraction = ((x - leftMarginPx) / chartWidth).coerceIn(0f, 1f)
                                    val idx = (fraction * (points.size - 1)).roundToInt().coerceIn(0, points.lastIndex)
                                    if (idx != selectedIndex) {
                                        selectedIndex = idx
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    updateIndexAt(change.position.x)
                                }
                            }
                    ) {
                        if (points.isEmpty()) return@Canvas

                    val allPrices = points.flatMap { listOf(it.storePrice, it.marketAvgPrice) }
                    val rawMin = allPrices.minOrNull() ?: 0.0
                    val rawMax = allPrices.maxOrNull() ?: 100.0
                    val span = (rawMax - rawMin).coerceAtLeast(10.0)
                    val yMin = (rawMin - span * 0.16).coerceAtLeast(0.0)
                    val yMax = rawMax + span * 0.16
                    val yRange = (yMax - yMin).coerceAtLeast(1.0)

                    val leftPad = 46.dp.toPx()
                    val rightPad = 8.dp.toPx()
                    val topPad = 10.dp.toPx()
                    val bottomPad = 24.dp.toPx()
                    val chartW = (size.width - leftPad - rightPad).coerceAtLeast(10f)
                    val chartH = (size.height - topPad - bottomPad).coerceAtLeast(10f)

                    // Horizontal dashed grid + Y axis labels (3 levels)
                    val gridColor = PvsForestGreen.copy(alpha = 0.15f)
                    val gridDash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    for (step in 0..2) {
                        val ratio = step / 2f
                        val y = topPad + chartH * ratio
                        val priceAtY = yMax - yRange * ratio
                        drawLine(
                            color = gridColor,
                            start = Offset(leftPad, y),
                            end = Offset(leftPad + chartW, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = gridDash
                        )
                        val tickText = "$${priceAtY.roundToInt()}"
                        val measured = textMeasurer.measure(tickText, style = labelStyle)
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(
                                (leftPad - measured.size.width - 6.dp.toPx()).coerceAtLeast(0f),
                                y - measured.size.height / 2f
                            )
                        )
                    }

                    fun xFor(index: Int): Float {
                        return if (points.size == 1) {
                            leftPad + chartW / 2f
                        } else {
                            leftPad + chartW * (index.toFloat() / (points.size - 1).toFloat())
                        }
                    }

                    fun yFor(price: Double): Float {
                        val norm = ((price - yMin) / yRange).toFloat().coerceIn(0f, 1f)
                        return topPad + chartH * (1f - norm)
                    }

                    val storeCoords = points.mapIndexed { idx, pt -> Offset(xFor(idx), yFor(pt.storePrice)) }
                    val marketCoords = points.mapIndexed { idx, pt -> Offset(xFor(idx), yFor(pt.marketAvgPrice)) }

                    val storeLinePath = buildSmoothSplinePath(storeCoords)
                    val marketLinePath = if (marketCoords.size == 1) {
                        Path().apply {
                            val cy = marketCoords.first().y
                            moveTo(leftPad, cy)
                            lineTo(leftPad + chartW, cy)
                        }
                    } else {
                        buildSmoothSplinePath(marketCoords)
                    }

                    val areaPath = Path().apply {
                        addPath(storeLinePath)
                        if (storeCoords.size > 1) {
                            lineTo(storeCoords.last().x, topPad + chartH)
                            lineTo(storeCoords.first().x, topPad + chartH)
                            close()
                        }
                    }

                    // Animated horizontal reveal for curves & area gradient
                    val revealRight = leftPad + chartW * drawProgress.value + 12.dp.toPx()
                    clipRect(left = leftPad - 4.dp.toPx(), top = 0f, right = revealRight, bottom = topPad + chartH + 4.dp.toPx()) {
                        if (storeCoords.size > 1) {
                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        verdictLineColor.copy(alpha = 0.30f),
                                        verdictLineColor.copy(alpha = 0.02f)
                                    ),
                                    startY = topPad,
                                    endY = topPad + chartH
                                )
                            )
                        }

                        // Market average dashed line
                        drawPath(
                            path = marketLinePath,
                            color = PvsForestDark.copy(alpha = 0.55f),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            )
                        )

                        // Store price main line
                        if (storeCoords.size > 1) {
                            drawPath(
                                path = storeLinePath,
                                color = verdictLineColor,
                                style = Stroke(
                                    width = 3.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    // Draw X-axis labels cleanly
                    val labelStep = when {
                        points.size <= 6 -> 1
                        else -> 2
                    }
                    points.forEachIndexed { idx, pt ->
                        val isLast = idx == points.lastIndex
                        val isSelected = idx == selectedIndex
                        if (idx % labelStep == 0 || isLast || isSelected) {
                            val monthText = if (strings.language == AppLanguage.ES) pt.labelEs else pt.labelEn
                            val styleToUse = if (isSelected) activeLabelStyle else labelStyle
                            val measured = textMeasurer.measure(monthText, style = styleToUse)
                            val cx = xFor(idx) - measured.size.width / 2f
                            drawText(
                                textLayoutResult = measured,
                                topLeft = Offset(
                                    cx.coerceIn(leftPad - 4f, size.width - measured.size.width),
                                    topPad + chartH + 6.dp.toPx()
                                )
                            )
                        }
                    }

                    // Draw dots & crosshair on selected index
                    val clampedIdx = selectedIndex.coerceIn(0, points.lastIndex)
                    val selStore = storeCoords[clampedIdx]
                    val selMarket = marketCoords[clampedIdx]

                    drawLine(
                        color = PvsForestDark.copy(alpha = 0.32f),
                        start = Offset(selStore.x, topPad),
                        end = Offset(selStore.x, topPad + chartH),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )

                    storeCoords.forEachIndexed { idx, center ->
                        if (idx == clampedIdx) {
                            drawCircle(
                                color = verdictLineColor.copy(alpha = 0.25f),
                                radius = 11.dp.toPx(),
                                center = center
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 6.dp.toPx(),
                                center = center
                            )
                            drawCircle(
                                color = verdictLineColor,
                                radius = 4.dp.toPx(),
                                center = center
                            )
                        } else if (points.size <= 6) {
                            drawCircle(
                                color = verdictLineColor.copy(alpha = 0.7f),
                                radius = 3.dp.toPx(),
                                center = center
                            )
                        }
                    }

                    drawCircle(
                        color = PvsForestDark,
                        radius = 3.5.dp.toPx(),
                        center = selMarket
                    )
                    }
                }
            }

            // Room Local Last-5 Scanned Price Pills + Quick Log Scan Button
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(verdictLineColor)
                            )
                            Text(
                                text = strings.historyLegendStore,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .height(2.dp)
                                    .background(PvsForestDark.copy(alpha = 0.6f))
                            )
                            Text(
                                text = strings.historyLegendMarket,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (onLogScannedPrice != null) {
                        OutlinedButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showLogPriceDialog = true
                            },
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, PvsForestGreen.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("log_scanned_price_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = PvsForestGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.historyScannedLogBtn,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PvsForestGreen
                            )
                        }
                    }
                }

                if (scannedHistory.isNotEmpty()) {
                    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("scanned_history_pills_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        scannedHistory.forEachIndexed { idx, entry ->
                            val isSelected = selectedMonths == 0 && selectedIndex == idx
                            val isBelowAvg = entry.scannedPrice <= entry.marketAvgPrice
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedMonths = 0
                                        selectedIndex = idx
                                    }
                                    .semantics(mergeDescendants = true) {}
                                    .testTag("scanned_history_pill_$idx"),
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    isSelected -> PvsForestGreen
                                    isBelowAvg -> SemaphoreGreenBg
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) PvsForestGreen else PvsLeafVibrant.copy(alpha = 0.35f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Text(
                                        text = "#${idx + 1} · ${timeFormatter.format(Date(entry.scannedAt))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) PvsMintContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatUsd(entry.scannedPrice),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else PvsForestDark
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = strings.historyScannedEmptyHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("scanned_history_empty_hint")
                    )
                }

                Text(
                    text = strings.historyTouchHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showLogPriceDialog && onLogScannedPrice != null) {
        LogScannedPriceDialog(
            initialPrice = product.storePrice,
            onDismiss = { showLogPriceDialog = false },
            onConfirm = { newPrice ->
                onLogScannedPrice(newPrice)
                selectedMonths = 0
                showLogPriceDialog = false
            }
        )
    }
}

@Composable
private fun LogScannedPriceDialog(
    initialPrice: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val strings = LocalAppStrings.current
    var text by remember {
        mutableStateOf(String.format(Locale.US, "%.2f", initialPrice))
    }
    val parsed = text.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.historyScannedDialogTitle) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(strings.historyScannedDialogField) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("log_scanned_price_dialog_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { if (parsed != null && parsed >= 0.5) onConfirm(parsed) },
                enabled = parsed != null && parsed >= 0.5,
                colors = ButtonDefaults.buttonColors(containerColor = PvsForestGreen),
                modifier = Modifier.testTag("log_scanned_price_dialog_confirm")
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
private fun DashboardKpiTile(
    label: String,
    value: String,
    valueColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = bgColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

private fun buildSmoothSplinePath(coords: List<Offset>): Path {
    val path = Path()
    if (coords.isEmpty()) return path
    path.moveTo(coords.first().x, coords.first().y)
    if (coords.size == 1) return path
    for (i in 0 until coords.lastIndex) {
        val p0 = coords[i]
        val p1 = coords[i + 1]
        val midX = (p0.x + p1.x) / 2f
        path.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
    }
    return path
}
