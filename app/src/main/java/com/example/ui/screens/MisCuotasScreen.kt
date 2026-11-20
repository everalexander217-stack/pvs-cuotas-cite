package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsScore
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.DueDateUtils
import com.example.data.InstallmentEntity
import com.example.ui.PvsUiState
import com.example.ui.components.AnimatedQuotaProgressBar
import com.example.ui.components.CodePillBadge
import com.example.ui.components.PvsCurvedWaveOverlay
import com.example.ui.components.PvsHeaderBar
import com.example.ui.components.formatUsd
import com.example.ui.components.pvsBouncyScale
import com.example.ui.components.shareInstallmentViaChooser
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
import kotlin.math.roundToInt

@Composable
fun MisCuotasScreen(
    state: PvsUiState,
    onToggleExpand: (Int) -> Unit,
    onMoveInMemory: (Int, Int) -> Unit,
    onCommitOrder: () -> Unit,
    onMoveStep: (Int, Int) -> Unit,
    onRegisterPayment: (InstallmentEntity) -> Unit,
    onDeleteInstallment: (Int) -> Unit,
    onShowAddDialog: (Boolean) -> Unit,
    onAddCustomInstallment: (
        title: String,
        code: String,
        category: String,
        initialPaid: Double,
        totalAmount: Double,
        totalQuotas: Int,
        nextPaymentDate: String
    ) -> Unit,
    onRestoreDefaults: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenTutorial: (() -> Unit)? = null,
    onGoToVerifier: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 104.dp.toPx() }

    var draggingItemId by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Column(modifier = modifier.fillMaxSize()) {
        PvsHeaderBar(
            title = strings.navMisCuotas,
            subtitle = strings.brandTagline,
            language = state.language,
            alertCount = state.upcomingAlerts.size,
            onToggleLanguage = onToggleLanguage,
            onOpenNotifications = onOpenNotifications,
            onOpenTutorial = onOpenTutorial
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "hero_quincena_summary") {
                QuincenaSummaryHeroCard(
                    totalQuincena = state.totalQuincena,
                    activeCount = state.activeInstallments.size,
                    nextDueDate = state.nextDueDateSummary.ifBlank { strings.noPendingDue },
                    onAddClick = { onShowAddDialog(true) }
                )
            }

            if (onGoToVerifier != null) {
                item(key = "quick_buy_prompt_pill") {
                    val promptInteraction = remember { MutableInteractionSource() }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pvsBouncyScale(promptInteraction, pressedScale = 0.97f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(
                                interactionSource = promptInteraction,
                                indication = androidx.compose.material3.ripple()
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onGoToVerifier()
                            }
                            .testTag("quick_buy_prompt_card"),
                        shape = RoundedCornerShape(18.dp),
                        color = PvsMintSurface,
                        border = BorderStroke(1.dp, PvsLeafVibrant.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.quickBuyPromptCta,
                                style = MaterialTheme.typography.labelLarge,
                                color = PvsForestDark,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = PvsForestGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            item(key = "reorder_hint_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.yourPlansHeader,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = strings.tapOrDragHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.installments.isEmpty() && !state.isLoading) {
                item(key = "empty_cuotas_state") {
                    EmptyCuotasCard(
                        onAddClick = { onShowAddDialog(true) },
                        onRestoreClick = onRestoreDefaults
                    )
                }
            } else {
                itemsIndexed(
                    items = state.installments,
                    key = { _, item -> item.id }
                ) { index, item ->
                    val isDragging = draggingItemId == item.id
                    val isExpanded = item.id in state.expandedIds
                    val daysUntil = DueDateUtils.daysUntilDue(item.nextPaymentDate)
                    val isDueSoon = !item.isCompleted && daysUntil != null && daysUntil <= state.alertThresholdDays

                    val handleDragStart: () -> Unit = {
                        draggingItemId = item.id
                        dragOffsetY = 0f
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }

                    val handleDragDelta: (Float) -> Unit = { deltaY ->
                        dragOffsetY += deltaY
                        val currentIndex = state.installments.indexOfFirst { it.id == item.id }
                        if (currentIndex != -1) {
                            if (dragOffsetY > itemHeightPx * 0.65f && currentIndex < state.installments.lastIndex) {
                                onMoveInMemory(currentIndex, currentIndex + 1)
                                dragOffsetY -= itemHeightPx
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else if (dragOffsetY < -itemHeightPx * 0.65f && currentIndex > 0) {
                                onMoveInMemory(currentIndex, currentIndex - 1)
                                dragOffsetY += itemHeightPx
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }

                    val handleDragEnd: () -> Unit = {
                        draggingItemId = null
                        dragOffsetY = 0f
                        onCommitOrder()
                    }

                    InstallmentReorderableCard(
                        item = item,
                        isExpanded = isExpanded,
                        isDragging = isDragging,
                        isDueSoon = isDueSoon,
                        daysUntil = daysUntil,
                        dragTranslationY = if (isDragging) dragOffsetY else 0f,
                        canMoveUp = index > 0,
                        canMoveDown = index < state.installments.lastIndex,
                        onToggleExpand = { onToggleExpand(item.id) },
                        onRegisterPayment = { onRegisterPayment(item) },
                        onMoveUp = { onMoveStep(item.id, -1) },
                        onMoveDown = { onMoveStep(item.id, 1) },
                        onDelete = { onDeleteInstallment(item.id) },
                        onDragStart = handleDragStart,
                        onDragDelta = handleDragDelta,
                        onDragEnd = handleDragEnd,
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(220),
                            fadeOutSpec = tween(180),
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    )
                }
            }
        }
    }

    if (state.showAddDialog) {
        AddInstallmentDialog(
            onDismiss = { onShowAddDialog(false) },
            onConfirm = onAddCustomInstallment
        )
    }
}

@Composable
private fun QuincenaSummaryHeroCard(
    totalQuincena: Double,
    activeCount: Int,
    nextDueDate: String,
    onAddClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val animatedTotal by animateFloatAsState(
        targetValue = totalQuincena.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "quincena_total_anim"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quincena_total_card"),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(PvsForestDark, PvsForestGreen, Color(0xFF23784A))
                    )
                )
        ) {
            PvsCurvedWaveOverlay(modifier = Modifier.matchParentSize())

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.quincenaTotalLabel.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = PvsMintContainer
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = strings.activePlans(activeCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatUsd(animatedTotal.toDouble()),
                            style = MaterialTheme.typography.displaySmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.perQuincena,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PvsMintContainer,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    val addInteraction = remember { MutableInteractionSource() }
                    val addHaptics = LocalHapticFeedback.current
                    FilledTonalButton(
                        onClick = {
                            addHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAddClick()
                        },
                        interactionSource = addInteraction,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = PvsLeafBright,
                            contentColor = PvsForestDark
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                        modifier = Modifier
                            .pvsBouncyScale(addInteraction)
                            .testTag("add_cuota_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = strings.addQuotaBtn,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.addQuotaBtn,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.13f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = PvsLeafBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.nextDueSummary(nextDueDate),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(PvsLeafBright)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallmentReorderableCard(
    item: InstallmentEntity,
    isExpanded: Boolean,
    isDragging: Boolean,
    isDueSoon: Boolean,
    daysUntil: Int?,
    dragTranslationY: Float,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggleExpand: () -> Unit,
    onRegisterPayment: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 14.dp else if (isExpanded) 4.dp else 1.dp,
        label = "card_elevation"
    )
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.025f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "card_scale"
    )
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chevron_rotation"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isDragging -> PvsLeafVibrant
            isExpanded -> PvsForestGreen.copy(alpha = 0.55f)
            isDueSoon -> PvsLeafVibrant.copy(alpha = 0.45f)
            else -> MaterialTheme.colorScheme.outline
        },
        label = "card_border_color"
    )

    val cardHaptics = LocalHapticFeedback.current
    val cardInteraction = remember { MutableInteractionSource() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 10f else 0f)
            .graphicsLayer {
                translationY = dragTranslationY
                scaleX = scale
                scaleY = scale
            }
            .pvsBouncyScale(cardInteraction, pressedScale = 0.98f)
            .pointerInput(item.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount.y)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .testTag("cuota_card_${item.code}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = BorderStroke(if (isDragging || isExpanded) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = cardInteraction,
                    indication = androidx.compose.material3.ripple(),
                    onClick = {
                        cardHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleExpand()
                    }
                )
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .pointerInput(item.id) {
                            detectVerticalDragGestures(
                                onDragStart = { onDragStart() },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    onDragDelta(dragAmount)
                                },
                                onDragEnd = { onDragEnd() },
                                onDragCancel = { onDragEnd() }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DragIndicator,
                        contentDescription = "Reordenar ${item.title}",
                        tint = if (isDragging) PvsLeafVibrant else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (item.isCompleted) SemaphoreGreenBg else PvsMintSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon(item.category),
                        contentDescription = null,
                        tint = if (item.isCompleted) SemaphoreGreen else PvsForestGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CodePillBadge(code = item.code)
                        if (item.isCompleted) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SemaphoreGreenBg
                            ) {
                                Text(
                                    text = strings.paidBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SemaphoreGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (isDueSoon && daysUntil != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PvsMintSurface
                            ) {
                                Text(
                                    text = strings.urgencyLabel(daysUntil),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PvsForestGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (item.isCompleted) {
                            strings.allQuotasCovered
                        } else {
                            strings.quotasRemainingShort(item.remainingQuotas, item.nextPaymentDate)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (item.isCompleted) "$0.00" else formatUsd(item.biweeklyQuota),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isCompleted) SemaphoreGreen else PvsForestDark
                    )
                    Text(
                        text = strings.quincenaUnit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = if (isExpanded) "Contraer" else "Expandir",
                    tint = PvsForestGreen,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(chevronRotation)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(220)) + expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailStatTile(
                            label = strings.fieldInicial,
                            value = formatUsd(item.initialPaid),
                            modifier = Modifier.weight(1f)
                        )
                        DetailStatTile(
                            label = strings.fieldCuotasRestantes,
                            value = strings.quotasOfTotal(item.remainingQuotas, item.totalQuotas),
                            modifier = Modifier.weight(1f)
                        )
                        DetailStatTile(
                            label = strings.fieldProximaFecha,
                            value = item.nextPaymentDate,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val pct = (item.progressFraction * 100).roundToInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.fieldProgreso,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = strings.quotasProgressCount(item.paidQuotas, item.totalQuotas, pct),
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = PvsForestGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    AnimatedQuotaProgressBar(
                        progress = item.progressFraction,
                        height = 11.dp
                    )

                    val cuotasContext = LocalContext.current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                cardHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                shareInstallmentViaChooser(
                                    context = cuotasContext,
                                    item = item,
                                    strings = strings
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("share_cuota_button_${item.code}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = strings.shareInstallmentBtn,
                                tint = PvsForestGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.shareInstallmentBtn,
                                style = MaterialTheme.typography.labelMedium,
                                color = PvsForestGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val payInteraction = remember { MutableInteractionSource() }
                        Button(
                            onClick = {
                                cardHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRegisterPayment()
                            },
                            enabled = !item.isCompleted,
                            interactionSource = payInteraction,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PvsForestGreen,
                                contentColor = Color.White,
                                disabledContainerColor = SemaphoreGreenBg,
                                disabledContentColor = SemaphoreGreen
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .pvsBouncyScale(payInteraction)
                                .testTag("pay_quota_btn_${item.code}")
                        ) {
                            Icon(
                                imageVector = if (item.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.Payments,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (item.isCompleted) strings.actionCompleted else strings.actionPayQuota,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onMoveUp,
                                enabled = canMoveUp,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowUpward,
                                    contentDescription = strings.actionMoveUp,
                                    tint = if (canMoveUp) PvsForestGreen else MaterialTheme.colorScheme.outline
                                )
                            }
                            IconButton(
                                onClick = onMoveDown,
                                enabled = canMoveDown,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDownward,
                                    contentDescription = strings.actionMoveDown,
                                    tint = if (canMoveDown) PvsForestGreen else MaterialTheme.colorScheme.outline
                                )
                            }
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = strings.actionDelete,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyCuotasCard(
    onAddClick: () -> Unit,
    onRestoreClick: () -> Unit
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
                    imageVector = Icons.Rounded.ShoppingBag,
                    contentDescription = null,
                    tint = PvsForestGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = strings.emptyCuotasTitle,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.emptyCuotasBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onRestoreClick) {
                    Text(strings.loadExamples)
                }
                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PvsForestGreen)
                ) {
                    Text(strings.addQuotaBtn)
                }
            }
        }
    }
}

@Composable
private fun AddInstallmentDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        code: String,
        category: String,
        initialPaid: Double,
        totalAmount: Double,
        totalQuotas: Int,
        nextPaymentDate: String
    ) -> Unit
) {
    val strings = LocalAppStrings.current
    var title by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var totalStr by remember { mutableStateOf("") }
    var initialStr by remember { mutableStateOf("") }
    var quotasStr by remember { mutableStateOf("4") }
    var dateStr by remember { mutableStateOf(DueDateUtils.formatDateOffsetFromToday(2)) }

    val total = totalStr.toDoubleOrNull() ?: 0.0
    val initial = initialStr.toDoubleOrNull() ?: 0.0
    val quotas = (quotasStr.toIntOrNull() ?: 4).coerceIn(1, 24)
    val isValid = title.isNotBlank() && total > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.dialogAddTitle,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.dialogFieldName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text(strings.dialogFieldCode) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = quotasStr,
                        onValueChange = { quotasStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text(strings.dialogFieldQuotas) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalStr,
                        onValueChange = { totalStr = it },
                        label = { Text(strings.dialogFieldTotal) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = initialStr,
                        onValueChange = { initialStr = it },
                        label = { Text(strings.dialogFieldInitial) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text(strings.dialogFieldDate) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title,
                        code,
                        "General",
                        initial,
                        total,
                        quotas,
                        dateStr
                    )
                },
                enabled = isValid,
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

private fun categoryIcon(category: String): ImageVector {
    return when (category) {
        "Tecnología" -> Icons.Rounded.Tv
        "Telefonía" -> Icons.Rounded.PhoneAndroid
        "Audio" -> Icons.Rounded.Headphones
        "Calzado" -> Icons.Rounded.SportsScore
        "Computación" -> Icons.Rounded.LaptopMac
        else -> Icons.Rounded.ShoppingBag
    }
}
