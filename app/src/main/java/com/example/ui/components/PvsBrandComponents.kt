package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.notifications.QuotaNotificationHelper
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.InstallmentEntity
import com.example.data.PaymentAlert
import com.example.data.PriceVerdict
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
import com.example.ui.theme.SemaphoreRedGlow
import com.example.ui.theme.SemaphoreYellowGlow
import java.util.Locale

fun formatUsd(amount: Double): String = String.format(Locale.US, "$%.2f", amount)

/**
 * Reusable spring-bounce press modifier for crisp, playful touch feedback on buttons, chips, and cards.
 */
fun Modifier.pvsBouncyScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.94f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pvs_bounce_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Official PVS Brand Emblem rendered inside a clean rounded botanical card.
 */
@Composable
fun PvsLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(13.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, PvsMintContainer)
    ) {
        Image(
            painter = painterResource(id = R.drawable.pvs_logo_official),
            contentDescription = "PVS Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
        )
    }
}

/**
 * Language Switcher Pill (ES / EN) with bouncy press animation.
 */
@Composable
fun PvsLanguageTogglePill(
    language: AppLanguage,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .pvsBouncyScale(interactionSource)
            .clip(RoundedCornerShape(50))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggle()
                }
            )
            .testTag("language_toggle_pill"),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Language,
                contentDescription = "Cambiar idioma / Change language",
                tint = PvsForestGreen,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = language.shortCode,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = PvsForestDark
            )
        }
    }
}

/**
 * Notification Bell button with live count badge and bouncy press animation.
 */
@Composable
fun PvsNotificationBellButton(
    alertCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    IconButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        interactionSource = interactionSource,
        modifier = modifier
            .pvsBouncyScale(interactionSource, pressedScale = 0.88f)
            .size(42.dp)
            .clip(CircleShape)
            .background(if (alertCount > 0) PvsMintSurface else MaterialTheme.colorScheme.surface)
            .testTag("notifications_bell_button")
    ) {
        BadgedBox(
            badge = {
                if (alertCount > 0) {
                    Badge(
                        containerColor = PvsLeafVibrant,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = alertCount.toString(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        ) {
            Icon(
                imageVector = if (alertCount > 0) {
                    Icons.Rounded.NotificationsActive
                } else {
                    Icons.Outlined.Notifications
                },
                contentDescription = LocalAppStrings.current.notifCenterTitle,
                tint = if (alertCount > 0) PvsForestGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

/**
 * Top PVS brand header with soft organic layout, language switcher, tutorial button, and notification bell.
 */
@Composable
fun PvsHeaderBar(
    title: String,
    subtitle: String,
    language: AppLanguage,
    alertCount: Int,
    onToggleLanguage: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenTutorial: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailingAction: @Composable (() -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            PvsLogoMark(size = 42.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "PVS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PvsLeafVibrant
                    )
                    Text(
                        text = " · $title",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            PvsLanguageTogglePill(
                language = language,
                onToggle = onToggleLanguage
            )
            if (onOpenTutorial != null) {
                val tutInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenTutorial()
                    },
                    interactionSource = tutInteraction,
                    modifier = Modifier
                        .pvsBouncyScale(tutInteraction, pressedScale = 0.88f)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .testTag("replay_tutorial_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = strings.replayTutorialCd,
                        tint = PvsForestGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            PvsNotificationBellButton(
                alertCount = alertCount,
                onClick = onOpenNotifications
            )
            if (trailingAction != null) {
                trailingAction()
            }
        }
    }
}

/**
 * Discreet, fluid in-app notification banner inspired by PVS curved orbital lines & green palette.
 */
@Composable
fun PvsInAppNotificationBanner(
    alerts: List<PaymentAlert>,
    visible: Boolean,
    onReviewClick: (PaymentAlert) -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val haptics = LocalHapticFeedback.current
    val primaryAlert = alerts.firstOrNull()

    AnimatedVisibility(
        visible = visible && primaryAlert != null,
        enter = slideInVertically(
            initialOffsetY = { -it / 2 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + expandVertically() + fadeIn(tween(260)),
        exit = slideOutVertically(
            targetOffsetY = { -it / 2 },
            animationSpec = tween(220)
        ) + shrinkVertically() + fadeOut(tween(180)),
        modifier = modifier
    ) {
        if (primaryAlert != null) {
            val infiniteTransition = rememberInfiniteTransition(label = "notif_pulse")
            val dotAlpha by infiniteTransition.animateFloat(
                initialValue = 0.45f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(950, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_alpha"
            )
            val cardInteraction = remember { MutableInteractionSource() }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp)
                    .pvsBouncyScale(cardInteraction, pressedScale = 0.97f)
                    .clickable(
                        interactionSource = cardInteraction,
                        indication = androidx.compose.material3.ripple(),
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onReviewClick(primaryAlert)
                        }
                    )
                    .testTag("in_app_payment_notification"),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 5.dp,
                border = BorderStroke(1.2.dp, PvsLeafVibrant.copy(alpha = 0.45f))
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val w = size.width
                        val h = size.height
                        val curve1 = Path().apply {
                            moveTo(0f, h * 0.25f)
                            cubicTo(w * 0.16f, 0f, w * 0.30f, h * 0.95f, w * 0.56f, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(curve1, color = PvsMintSurface.copy(alpha = 0.85f))

                        val arcAccent = Path().apply {
                            moveTo(w * 0.04f, h * 0.88f)
                            cubicTo(w * 0.34f, h * 1.05f, w * 0.72f, h * 0.78f, w * 0.97f, h * 0.16f)
                        }
                        drawPath(
                            path = arcAccent,
                            color = PvsLeafVibrant.copy(alpha = 0.22f),
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(PvsForestDark, PvsForestGreen)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = PvsLeafBright,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(PvsLeafVibrant.copy(alpha = dotAlpha))
                                )
                                Text(
                                    text = "${strings.notifUpcomingTitle} · ${primaryAlert.installment.code}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PvsForestGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (alerts.size > 1) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = PvsMintContainer
                                    ) {
                                        Text(
                                            text = strings.notifMoreCount(alerts.size - 1),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PvsForestDark,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = primaryAlert.installment.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = strings.notifDueDetail(
                                    formatUsd(primaryAlert.installment.biweeklyQuota),
                                    strings.urgencyLabel(primaryAlert.daysRemaining),
                                    primaryAlert.installment.nextPaymentDate
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        val reviewInteraction = remember { MutableInteractionSource() }
                        TextButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onReviewClick(primaryAlert)
                            },
                            interactionSource = reviewInteraction,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.pvsBouncyScale(reviewInteraction)
                        ) {
                            Text(
                                text = strings.notifActionReview,
                                style = MaterialTheme.typography.labelLarge,
                                color = PvsForestGreen
                            )
                        }

                        val dismissInteraction = remember { MutableInteractionSource() }
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismissClick()
                            },
                            interactionSource = dismissInteraction,
                            modifier = Modifier
                                .pvsBouncyScale(dismissInteraction, pressedScale = 0.85f)
                                .size(34.dp)
                                .testTag("dismiss_in_app_notification")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = strings.notifDismissCd,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Curved PVS Notification Center ModalBottomSheet allowing threshold tuning (1/3/5/7 days)
 * and direct payment or review actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PvsNotificationsBottomSheet(
    alerts: List<PaymentAlert>,
    thresholdDays: Int,
    notificationsEnabled: Boolean,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onPermissionGranted: () -> Unit,
    onSendTestReminder: () -> Boolean,
    onThresholdChange: (Int) -> Unit,
    onReviewInstallment: (Int) -> Unit,
    onPayInstallment: (InstallmentEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var hasSystemPermission by remember {
        mutableStateOf(QuotaNotificationHelper.canPostSystemNotifications(context))
    }
    var testFeedback by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasSystemPermission = granted && QuotaNotificationHelper.canPostSystemNotifications(context)
        if (granted) {
            onPermissionGranted()
            testFeedback = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PvsLogoMark(size = 40.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.notifCenterTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = strings.notifCenterSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // System background reminder toggle card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = if (notificationsEnabled) PvsMintSurface else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    1.dp,
                    if (notificationsEnabled) PvsLeafVibrant.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.notifSystemToggleTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = PvsForestDark
                        )
                        Text(
                            text = strings.notifSystemToggleSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { checked ->
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNotificationsEnabledChange(checked)
                            if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !QuotaNotificationHelper.hasNotificationPermission(context)
                            ) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PvsForestGreen
                        ),
                        modifier = Modifier.testTag("system_reminders_switch")
                    )
                }
            }

            // Runtime permission prompt for Android 13+ when system notifications aren't enabled yet
            if (notificationsEnabled && !hasSystemPermission) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, PvsForestGreen.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = strings.notifPermissionNeededTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = PvsForestDark
                        )
                        Text(
                            text = strings.notifPermissionNeededBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !QuotaNotificationHelper.hasNotificationPermission(context)
                            ) {
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PvsForestGreen),
                                    modifier = Modifier.testTag("grant_notification_permission_button")
                                ) {
                                    Text(strings.notifPermissionGrantBtn)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        val settingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(settingsIntent)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(strings.notifPermissionSettingsBtn)
                                }
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.notifThresholdLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 3, 5, 7).forEach { days ->
                        val selected = thresholdDays == days
                        val chipInteraction = remember { MutableInteractionSource() }
                        Surface(
                            modifier = Modifier
                                .pvsBouncyScale(chipInteraction)
                                .clip(RoundedCornerShape(50))
                                .clickable(
                                    interactionSource = chipInteraction,
                                    indication = androidx.compose.material3.ripple()
                                ) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onThresholdChange(days)
                                },
                            shape = RoundedCornerShape(50),
                            color = if (selected) PvsForestGreen else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (selected) PvsForestGreen else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Text(
                                text = strings.notifDaysChip(days),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            if (alerts.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = PvsMintSurface
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = strings.notifEmptyTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = PvsForestDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.notifEmptyBody(thresholdDays),
                            style = MaterialTheme.typography.bodySmall,
                            color = PvsForestGreen
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(alerts, key = { it.installment.id }) { alert ->
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = BorderStroke(1.dp, PvsLeafVibrant.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        CodePillBadge(code = alert.installment.code)
                                        Text(
                                            text = alert.installment.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = PvsForestDark
                                    ) {
                                        Text(
                                            text = strings.urgencyLabel(alert.daysRemaining),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PvsLeafBright,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${formatUsd(alert.installment.biweeklyQuota)} · ${alert.installment.nextPaymentDate}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val reviewInteraction = remember { MutableInteractionSource() }
                                        FilledTonalButton(
                                            onClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onReviewInstallment(alert.installment.id)
                                            },
                                            interactionSource = reviewInteraction,
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.pvsBouncyScale(reviewInteraction)
                                        ) {
                                            Text(strings.notifActionReview)
                                        }
                                        val payInteraction = remember { MutableInteractionSource() }
                                        Button(
                                            onClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onPayInstallment(alert.installment)
                                            },
                                            interactionSource = payInteraction,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = PvsForestGreen),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.pvsBouncyScale(payInteraction)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Payments,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(strings.notifActionPay)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick test reminder action for immediate verification on phone
            val testInteraction = remember { MutableInteractionSource() }
            FilledTonalButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (!QuotaNotificationHelper.hasNotificationPermission(context) &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        val sent = onSendTestReminder()
                        hasSystemPermission = QuotaNotificationHelper.canPostSystemNotifications(context)
                        testFeedback = if (sent) {
                            strings.notifTestSentFeedback
                        } else {
                            strings.notifTestFailedFeedback
                        }
                    }
                },
                interactionSource = testInteraction,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .pvsBouncyScale(testInteraction)
                    .testTag("test_notification_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsNone,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.notifTestReminderBtn)
            }

            if (testFeedback != null) {
                Text(
                    text = testFeedback!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = PvsForestGreen
                )
            }
        }
    }
}

/**
 * Organic multi-wave curved canvas decorator for PVS hero surfaces.
 */
@Composable
fun PvsCurvedWaveOverlay(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val backWave = Path().apply {
            moveTo(0f, h * 0.65f)
            cubicTo(w * 0.28f, h * 0.42f, w * 0.62f, h * 0.92f, w, h * 0.52f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = backWave,
            color = PvsLeafVibrant.copy(alpha = 0.22f)
        )

        val frontWave = Path().apply {
            moveTo(0f, h * 0.80f)
            cubicTo(w * 0.35f, h * 0.98f, w * 0.68f, h * 0.58f, w, h * 0.78f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = frontWave,
            color = PvsLeafBright.copy(alpha = 0.16f)
        )

        val accentCurve = Path().apply {
            moveTo(w * 0.52f, 0f)
            cubicTo(w * 0.65f, h * 0.35f, w * 0.85f, h * 0.18f, w, h * 0.42f)
        }
        drawPath(
            path = accentCurve,
            color = Color.White.copy(alpha = 0.10f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * Smooth animated curved progress bar in PVS Leaf & Forest green.
 */
@Composable
fun AnimatedQuotaProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    trackColor: Color = PvsMintSurface
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "quota_progress_anim"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val radius = size.height / 2f
        drawRoundRect(
            color = trackColor,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = CornerRadius(radius, radius)
        )
        if (animatedProgress > 0.005f) {
            val fillWidth = (size.width * animatedProgress).coerceAtLeast(size.height)
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(PvsForestGreen, PvsLeafVibrant, PvsLeafBright)
                ),
                topLeft = Offset.Zero,
                size = Size(width = fillWidth, height = size.height),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
    }
}

/**
 * Animated Semáforo indicator with pulsing halo on the active Verdict bulb.
 */
@Composable
fun TrafficLightSemaphore(
    activeVerdict: PriceVerdict,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "semaphore_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = PvsForestDark,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SemaphoreBulb(
                isActive = activeVerdict == PriceVerdict.GREEN,
                activeColor = SemaphoreGreen,
                inactiveColor = Color(0xFF1D422C),
                pulseAlpha = pulseAlpha
            )
            SemaphoreBulb(
                isActive = activeVerdict == PriceVerdict.YELLOW,
                activeColor = SemaphoreYellowGlow,
                inactiveColor = Color(0xFF3E351A),
                pulseAlpha = pulseAlpha
            )
            SemaphoreBulb(
                isActive = activeVerdict == PriceVerdict.RED,
                activeColor = SemaphoreRedGlow,
                inactiveColor = Color(0xFF421D1D),
                pulseAlpha = pulseAlpha
            )
        }
    }
}

@Composable
private fun SemaphoreBulb(
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    pulseAlpha: Float
) {
    val bulbColor by animateColorAsState(
        targetValue = if (isActive) activeColor else inactiveColor,
        animationSpec = tween(350),
        label = "bulb_color"
    )
    Box(
        modifier = Modifier.size(22.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(activeColor.copy(alpha = pulseAlpha))
            )
        }
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(bulbColor)
                .border(
                    width = if (isActive) 1.5.dp else 0.5.dp,
                    color = if (isActive) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.15f),
                    shape = CircleShape
                )
        )
    }
}

/**
 * Clean pill badge for codes like TV-50, CEL-128, etc.
 */
@Composable
fun CodePillBadge(
    code: String,
    modifier: Modifier = Modifier,
    containerColor: Color = PvsMintContainer,
    contentColor: Color = PvsForestDark
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = containerColor
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}
