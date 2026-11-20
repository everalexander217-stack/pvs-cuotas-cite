package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.PriceVerdict
import com.example.data.ProductCodeInfo
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
import kotlin.math.abs

/**
 * Stylized PVS Digital Product Pass component displaying scanned/verified product details:
 * Name, Price, Code (with generated barcode pattern), and Estimated Biweekly Quota.
 */
@Composable
fun ScannedProductShowcaseCard(
    product: ProductCodeInfo,
    scannedBarcode: String?,
    onRescanClick: () -> Unit,
    onQuickAddClick: () -> Unit,
    targetPrice: Double? = null,
    onShareClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    var revealed by remember(product.code, scannedBarcode) { mutableStateOf(false) }
    LaunchedEffect(product.code, scannedBarcode) {
        revealed = true
    }

    val cardScale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.96f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scanned_showcase_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pvs_scan_pulse")
    val livePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_scan_dot"
    )

    val verdictDotColor = when (product.verdict) {
        PriceVerdict.GREEN -> SemaphoreGreen
        PriceVerdict.YELLOW -> SemaphoreYellowGlow
        PriceVerdict.RED -> SemaphoreRedGlow
    }
    val diffPct = product.percentageDifference
    val isCameraScan = !scannedBarcode.isNullOrBlank()
    val rawDiffersFromCode = isCameraScan && !scannedBarcode.equals(product.code, ignoreCase = true)
    val isBelowTarget = targetPrice != null && product.storePrice < targetPrice

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .testTag("scanned_product_showcase_card_${product.code}"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.2.dp, PvsLeafBright.copy(alpha = 0.55f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PvsForestDark,
                            Color(0xFF13482B),
                            PvsForestGreen
                        )
                    )
                )
        ) {
            // Signature PVS curved waves & radial emerald luminescence
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PvsLeafBright.copy(alpha = 0.22f),
                            PvsLeafVibrant.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.85f, h * 0.16f),
                        radius = w * 0.68f
                    ),
                    center = Offset(w * 0.85f, h * 0.16f),
                    radius = w * 0.68f
                )

                val ribbon = Path().apply {
                    moveTo(0f, h * 0.18f)
                    cubicTo(w * 0.34f, h * 0.04f, w * 0.68f, h * 0.34f, w, h * 0.12f)
                }
                drawPath(
                    path = ribbon,
                    color = PvsLeafBright.copy(alpha = 0.14f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                val bottomWave = Path().apply {
                    moveTo(0f, h * 0.82f)
                    cubicTo(w * 0.32f, h * 0.72f, w * 0.70f, h * 0.94f, w, h * 0.78f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(bottomWave, color = PvsLeafVibrant.copy(alpha = 0.13f))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Brand Strip: PVS Logo + FICHA PVS + Live Scan / Verified Status + Category
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PvsLogoMark(size = 30.dp)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = PvsLeafVibrant.copy(alpha = 0.28f),
                            border = BorderStroke(1.dp, PvsLeafBright.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = strings.scannedCardPassBadge,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = PvsLeafBright,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(
                                1.dp,
                                if (isCameraScan) PvsLeafBright.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.22f)
                            ),
                            modifier = Modifier
                                .semantics(mergeDescendants = true) {}
                                .testTag("scanned_product_status_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isCameraScan) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(PvsLeafBright.copy(alpha = livePulseAlpha))
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Verified,
                                        contentDescription = null,
                                        tint = PvsLeafBright,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = if (isCameraScan) strings.scannedCardLiveScan else strings.scannedCardVerified,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        val shareInt = remember { MutableInteractionSource() }
                        Surface(
                            shape = CircleShape,
                            color = PvsLeafVibrant.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, PvsLeafBright.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .size(48.dp)
                                .pvsBouncyScale(shareInt, pressedScale = 0.9f)
                        ) {
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (onShareClick != null) {
                                        onShareClick()
                                    } else {
                                        shareProductViaChooser(
                                            context = context,
                                            product = product,
                                            scannedBarcode = scannedBarcode,
                                            strings = strings
                                        )
                                    }
                                },
                                interactionSource = shareInt,
                                modifier = Modifier.testTag("share_product_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = strings.shareProductBtn,
                                    tint = PvsLeafBright,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Product Identity Block: Name + Category + Stylised PVS Barcode & Code Pill
                AnimatedVisibility(
                    visible = revealed,
                    enter = fadeIn(tween(240)) + slideInVertically(tween(260)) { it / 6 }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(verdictDotColor)
                                    )
                                    Text(
                                        text = product.category.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PvsMintContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("scanned_product_name")
                                )
                                if (isBelowTarget && targetPrice != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = PvsLeafBright.copy(alpha = 0.22f),
                                        border = BorderStroke(1.dp, PvsLeafBright),
                                        modifier = Modifier
                                            .semantics(mergeDescendants = true) {}
                                            .testTag("showcase_below_target_badge")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.TrendingDown,
                                                contentDescription = null,
                                                tint = PvsLeafBright,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = strings.targetPriceShowcaseBelowChip(formatUsd(targetPrice)),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = PvsLeafBright
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Code Bar Strip with Deterministic Barcode Pattern
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.QrCode2,
                                        contentDescription = null,
                                        tint = PvsLeafBright,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = strings.scannedCardCodeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PvsMintContainer.copy(alpha = 0.85f)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = product.code,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.testTag("scanned_product_code")
                                            )
                                            if (rawDiffersFromCode) {
                                                Text(
                                                    text = "· $scannedBarcode",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    color = PvsLeafBright,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                PvsStylizedBarcodeGlyph(
                                    seedKey = product.code,
                                    modifier = Modifier
                                        .width(74.dp)
                                        .height(26.dp)
                                )
                            }
                        }
                    }
                }

                // Ticket Perforation Divider
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                ) {
                    val cy = size.height / 2f
                    drawLine(
                        color = PvsLeafBright.copy(alpha = 0.4f),
                        start = Offset(10.dp.toPx(), cy),
                        end = Offset(size.width - 10.dp.toPx(), cy),
                        strokeWidth = 1.4.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = PvsForestDark,
                        radius = 5.dp.toPx(),
                        center = Offset(0f, cy)
                    )
                    drawCircle(
                        color = PvsForestDark,
                        radius = 5.dp.toPx(),
                        center = Offset(size.width, cy)
                    )
                }

                // Price + Estimated Biweekly Quota Highlight Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Product Store Price
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                    ) {
                        Column(
                            modifier = Modifier.padding(13.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = strings.scannedCardPriceLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = PvsMintContainer
                            )
                            Text(
                                text = formatUsd(product.storePrice),
                                style = MaterialTheme.typography.headlineSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.testTag("scanned_product_price")
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = if (diffPct <= 0) {
                                        strings.verifierSavingsPct(abs(diffPct))
                                    } else {
                                        strings.verifierMarkupPct(diffPct)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (diffPct <= 0) PvsLeafBright else verdictDotColor,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // 2. Estimated Biweekly Quota (Bright PVS Leaf Accent Tile)
                    Surface(
                        modifier = Modifier.weight(1.12f),
                        shape = RoundedCornerShape(18.dp),
                        color = PvsMintSurface,
                        border = BorderStroke(1.2.dp, PvsLeafBright)
                    ) {
                        Column(
                            modifier = Modifier.padding(13.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Payments,
                                    contentDescription = null,
                                    tint = PvsForestGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = strings.scannedCardBiweeklyQuotaLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PvsForestGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = formatUsd(product.biweeklyImpact),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = PvsForestDark,
                                    modifier = Modifier.testTag("scanned_product_biweekly_quota")
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = strings.perQuincena,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PvsForestGreen,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    tint = PvsForestGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = strings.scannedCardPlanDetail(
                                        product.quotasCount,
                                        formatUsd(product.suggestedInitial)
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PvsForestDark.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Quick Actions Strip: Re-scan & Quick Add to Quotas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val rescanInt = remember { MutableInteractionSource() }
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onRescanClick()
                        },
                        interactionSource = rescanInt,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.14f),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .pvsBouncyScale(rescanInt)
                            .testTag("scanned_card_rescan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = null,
                            tint = PvsLeafBright,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.scannedCardRescanBtn,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    val quickAddInt = remember { MutableInteractionSource() }
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onQuickAddClick()
                        },
                        interactionSource = quickAddInt,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PvsLeafBright,
                            contentColor = PvsForestDark
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .pvsBouncyScale(quickAddInt)
                            .testTag("scanned_card_quick_add_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.scannedCardQuickAddBtn,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PvsStylizedBarcodeGlyph(
    seedKey: String,
    modifier: Modifier = Modifier
) {
    val barWeights = remember(seedKey) {
        val hash = seedKey.hashCode()
        List(14) { index ->
            val bit = ((hash ushr (index % 24)) + index * 7) and 0x3
            when (bit) {
                0 -> 1.2f
                1 -> 2.4f
                2 -> 1.6f
                else -> 3.0f
            }
        }
    }

    Canvas(modifier = modifier) {
        val totalSlots = barWeights.size * 2 - 1
        val unitWidth = size.width / (totalSlots * 1.45f)
        var x = 0f
        barWeights.forEachIndexed { idx, weight ->
            val barWidth = (unitWidth * weight * 0.72f).coerceAtLeast(1.5.dp.toPx())
            val barHeight = if (idx == 0 || idx == barWeights.lastIndex) size.height else size.height * 0.84f
            if (x + barWidth <= size.width) {
                drawRoundRect(
                    color = PvsMintSurface.copy(alpha = 0.85f),
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                )
            }
            x += barWidth + (unitWidth * 1.25f)
        }
    }
}
