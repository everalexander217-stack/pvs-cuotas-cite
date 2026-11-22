package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AddCard
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.PvsForestDark
import com.example.ui.theme.PvsForestGreen
import com.example.ui.theme.PvsLeafBright
import com.example.ui.theme.PvsLeafVibrant
import kotlin.math.roundToInt

enum class CoachMarkStepId {
    SCAN_ENTRY,
    SCAN_BUTTON,
    ADD_QUOTA_BUTTON,
    QUICK_SCAN_TO_QUOTA
}

@Stable
class CoachMarkRegistry {
    private val boundsMap = mutableStateMapOf<CoachMarkStepId, Rect>()

    fun updateBounds(id: CoachMarkStepId, rect: Rect) {
        if (rect.width > 0f && rect.height > 0f && boundsMap[id] != rect) {
            boundsMap[id] = rect
        }
    }

    fun clearBounds(id: CoachMarkStepId) {
        boundsMap.remove(id)
    }

    fun boundsFor(id: CoachMarkStepId): Rect? = boundsMap[id]
}

val LocalCoachMarkRegistry = compositionLocalOf<CoachMarkRegistry?> { null }

fun Modifier.coachMarkTarget(id: CoachMarkStepId): Modifier = composed {
    val registry = LocalCoachMarkRegistry.current
    DisposableEffect(id, registry) {
        onDispose {
            registry?.clearBounds(id)
        }
    }
    if (registry == null) {
        this
    } else {
        this.onGloballyPositioned { coordinates ->
            if (coordinates.isAttached) {
                registry.updateBounds(id, coordinates.boundsInRoot())
            }
        }
    }
}

/**
 * Interactive coach mark spotlight layer that softly highlights live UI targets
 * to teach new users how to scan a product and add an installment quota.
 */
@Composable
fun CoachMarkOverlay(
    visible: Boolean,
    currentStepIndex: Int,
    activeStep: CoachMarkStepId,
    registry: CoachMarkRegistry,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val totalSteps = CoachMarkStepId.entries.size

    BackHandler(enabled = visible) {
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(240)),
        exit = fadeOut(tween(180)),
        modifier = modifier
    ) {
        val targetBounds = registry.boundsFor(activeStep)

        val infinitePulse = rememberInfiniteTransition(label = "coach_mark_halo")
        val haloPulse by infinitePulse.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "coach_mark_halo_alpha"
        )

        val (title, body, stepIcon) = when (activeStep) {
            CoachMarkStepId.SCAN_ENTRY -> Triple(
                strings.coachStep1Title,
                strings.coachStep1Body,
                Icons.Rounded.QrCodeScanner
            )
            CoachMarkStepId.SCAN_BUTTON -> Triple(
                strings.coachStep2Title,
                strings.coachStep2Body,
                Icons.Rounded.QrCodeScanner
            )
            CoachMarkStepId.ADD_QUOTA_BUTTON -> Triple(
                strings.coachStep3Title,
                strings.coachStep3Body,
                Icons.Rounded.AddCard
            )
            CoachMarkStepId.QUICK_SCAN_TO_QUOTA -> Triple(
                strings.coachStep4Title,
                strings.coachStep4Body,
                Icons.Rounded.TouchApp
            )
        }

        val safeInsets = WindowInsets.safeDrawing.asPaddingValues()

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .testTag("coach_mark_overlay")
        ) {
            val containerWidthPx = with(density) { maxWidth.toPx() }
            val containerHeightPx = with(density) { maxHeight.toPx() }
            val spotlightPaddingPx = with(density) { 10.dp.toPx() }
            val outerMarginPx = with(density) { 16.dp.toPx() }
            val topSafePx = with(density) { safeInsets.calculateTopPadding().toPx() } + outerMarginPx
            val bottomSafePx = containerHeightPx - with(density) { safeInsets.calculateBottomPadding().toPx() } - outerMarginPx

            val rawRect = remember(targetBounds, containerWidthPx, containerHeightPx) {
                if (targetBounds != null && targetBounds.width > 2f && targetBounds.height > 2f) {
                    Rect(
                        left = (targetBounds.left - spotlightPaddingPx).coerceAtLeast(8f),
                        top = (targetBounds.top - spotlightPaddingPx).coerceAtLeast(8f),
                        right = (targetBounds.right + spotlightPaddingPx).coerceAtMost(containerWidthPx - 8f),
                        bottom = (targetBounds.bottom + spotlightPaddingPx).coerceAtMost(containerHeightPx - 8f)
                    )
                } else {
                    null
                }
            }

            val animLeft by animateFloatAsState(
                targetValue = rawRect?.left ?: (containerWidthPx * 0.15f),
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "spotlight_left"
            )
            val animTop by animateFloatAsState(
                targetValue = rawRect?.top ?: (containerHeightPx * 0.25f),
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "spotlight_top"
            )
            val animRight by animateFloatAsState(
                targetValue = rawRect?.right ?: (containerWidthPx * 0.85f),
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "spotlight_right"
            )
            val animBottom by animateFloatAsState(
                targetValue = rawRect?.bottom ?: (containerHeightPx * 0.38f),
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "spotlight_bottom"
            )

            val hasTargetRect = rawRect != null
            val activeSpotlightRect = if (hasTargetRect) {
                Rect(animLeft, animTop, animRight, animBottom)
            } else {
                null
            }

            val currentSpotlightRect by rememberUpdatedState(activeSpotlightRect)

            // Soft spotlight scrim & glowing halo around target
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            val rect = currentSpotlightRect
                            if (rect != null && rect.contains(tapOffset)) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNext()
                            }
                        }
                    }
            ) {
                // Soft dark forest scrim
                drawRect(
                    color = Color(0xFF05130B).copy(alpha = 0.72f)
                )

                if (activeSpotlightRect != null) {
                    val cornerRadiusPx = 22.dp.toPx()
                    val rectTopLeft = Offset(activeSpotlightRect.left, activeSpotlightRect.top)
                    val rectSize = Size(activeSpotlightRect.width, activeSpotlightRect.height)

                    // Outer soft ambient glow
                    val glowExpand = 5.dp.toPx()
                    drawRoundRect(
                        color = PvsLeafBright.copy(alpha = haloPulse * 0.28f),
                        topLeft = Offset(activeSpotlightRect.left - glowExpand, activeSpotlightRect.top - glowExpand),
                        size = Size(activeSpotlightRect.width + glowExpand * 2, activeSpotlightRect.height + glowExpand * 2),
                        cornerRadius = CornerRadius(cornerRadiusPx + glowExpand, cornerRadiusPx + glowExpand),
                        style = Stroke(width = 12.dp.toPx())
                    )

                    // Clear cutout so the actual UI control shines through cleanly
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = rectTopLeft,
                        size = rectSize,
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        blendMode = BlendMode.Clear
                    )

                    // Crisp soft accent ring framing the target
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(PvsLeafBright, PvsLeafVibrant)
                        ),
                        topLeft = rectTopLeft,
                        size = rectSize,
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        style = Stroke(width = 2.5.dp.toPx()),
                        alpha = 0.92f
                    )
                }
            }

            // Adaptive positioned floating tooltip card
            var tooltipSize by remember { mutableStateOf(IntSize.Zero) }
            val gapPx = with(density) { 14.dp.toPx() }
            val cardSurfaceColor = MaterialTheme.colorScheme.surface
            val isLastStep = currentStepIndex >= totalSteps - 1

            val showBelow = if (activeSpotlightRect != null) {
                val spaceBelow = bottomSafePx - activeSpotlightRect.bottom
                val spaceAbove = activeSpotlightRect.top - topSafePx
                spaceBelow >= tooltipSize.height + gapPx || spaceBelow >= spaceAbove
            } else {
                true
            }

            val cardTopPx = if (activeSpotlightRect != null) {
                val desiredY = if (showBelow) {
                    activeSpotlightRect.bottom + gapPx
                } else {
                    activeSpotlightRect.top - tooltipSize.height - gapPx
                }
                desiredY.coerceIn(
                    topSafePx,
                    (bottomSafePx - tooltipSize.height).coerceAtLeast(topSafePx)
                )
            } else {
                ((containerHeightPx - tooltipSize.height) / 2f).coerceAtLeast(topSafePx)
            }

            val horizontalMaxWidthPx = with(density) { 420.dp.toPx() }
            val cardWidthEstimatePx = tooltipSize.width.takeIf { it > 0 }?.toFloat()
                ?: (containerWidthPx - outerMarginPx * 2).coerceAtMost(horizontalMaxWidthPx)
            val spotlightCenterX = activeSpotlightRect?.center?.x ?: (containerWidthPx / 2f)
            val cardLeftPx = (spotlightCenterX - cardWidthEstimatePx / 2f).coerceIn(
                outerMarginPx,
                (containerWidthPx - cardWidthEstimatePx - outerMarginPx).coerceAtLeast(outerMarginPx)
            )

            Box(
                modifier = Modifier
                    .offset { IntOffset(cardLeftPx.roundToInt(), cardTopPx.roundToInt()) }
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .onSizeChanged { tooltipSize = it }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (activeSpotlightRect != null && showBelow) {
                        CoachMarkBeaconPointer(
                            pointerUp = true,
                            pointerCenterXInParent = (spotlightCenterX - cardLeftPx).coerceIn(36f, cardWidthEstimatePx - 36f),
                            color = cardSurfaceColor
                        )
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("coach_mark_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = cardSurfaceColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                        border = BorderStroke(1.2.dp, PvsLeafVibrant.copy(alpha = 0.55f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = strings.coachMarkStepCounter(currentStepIndex + 1, totalSteps),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onDismiss()
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("coach_mark_skip_button")
                                ) {
                                    Text(
                                        text = strings.coachMarkSkip,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            AnimatedContent(
                                targetState = activeStep,
                                transitionSpec = { fadeIn(tween(180)).togetherWith(fadeOut(tween(140))) },
                                label = "coach_mark_content_anim"
                            ) { _ ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(13.dp))
                                            .background(
                                                Brush.linearGradient(listOf(PvsForestDark, PvsForestGreen))
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = stepIcon,
                                            contentDescription = null,
                                            tint = PvsLeafBright,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.testTag("coach_mark_title")
                                        )
                                        Text(
                                            text = body,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.testTag("coach_mark_body")
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Animated Step Dots
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(totalSteps) { index ->
                                        val isCurrent = index == currentStepIndex
                                        val dotWidth by animateDpAsState(
                                            targetValue = if (isCurrent) 20.dp else 7.dp,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            ),
                                            label = "coach_dot_width"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .height(7.dp)
                                                .width(dotWidth)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isCurrent) {
                                                        PvsLeafVibrant
                                                    } else {
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    }
                                                )
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (currentStepIndex > 0) {
                                        TextButton(
                                            onClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onPrevious()
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                            modifier = Modifier.testTag("coach_mark_back_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = strings.coachMarkBack,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onNext()
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PvsForestGreen,
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                        modifier = Modifier.testTag("coach_mark_next_button")
                                    ) {
                                        Text(
                                            text = if (isLastStep) strings.coachMarkFinish else strings.coachMarkNext,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = if (isLastStep) {
                                                Icons.Rounded.Check
                                            } else {
                                                Icons.AutoMirrored.Rounded.ArrowForward
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (activeSpotlightRect != null && !showBelow) {
                        CoachMarkBeaconPointer(
                            pointerUp = false,
                            pointerCenterXInParent = (spotlightCenterX - cardLeftPx).coerceIn(36f, cardWidthEstimatePx - 36f),
                            color = cardSurfaceColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoachMarkBeaconPointer(
    pointerUp: Boolean,
    pointerCenterXInParent: Float,
    color: Color
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val halfW = 10.dp.toPx()
        val cx = pointerCenterXInParent.coerceIn(halfW + 8f, (size.width - halfW - 8f).coerceAtLeast(halfW + 8f))
        val path = Path().apply {
            if (pointerUp) {
                moveTo(cx - halfW, size.height)
                lineTo(cx, 0f)
                lineTo(cx + halfW, size.height)
                close()
            } else {
                moveTo(cx - halfW, 0f)
                lineTo(cx + halfW, 0f)
                lineTo(cx, size.height)
                close()
            }
        }
        drawPath(path = path, color = color)
    }
}
