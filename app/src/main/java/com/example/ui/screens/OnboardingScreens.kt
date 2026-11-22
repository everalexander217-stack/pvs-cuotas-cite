package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.ReceiptLong
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PriceVerdict
import com.example.ui.components.AnimatedQuotaProgressBar
import com.example.ui.components.CodePillBadge
import com.example.ui.components.PvsLanguageTogglePill
import com.example.ui.components.PvsLogoMark
import com.example.ui.components.TrafficLightSemaphore
import com.example.ui.components.pvsBouncyScale
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Fluid, choreographed PVS Intro Splash Screen utilizing official PVS palette
 * (Forest Dark, Forest Green, Vibrant Leaf, Bright Leaf, Mint) before transitioning
 * smoothly into the main application.
 */
@Composable
fun PvsIntroSplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val logoScale = remember { Animatable(0.54f) }
    val logoAlpha = remember { Animatable(0f) }
    val rippleExpand = remember { Animatable(0.35f) }
    val arcSweep = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }
    val exitScale = remember { Animatable(1f) }
    var showWordmark by remember { mutableStateOf(false) }
    var showBadgeChip by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun triggerSmoothExit() {
        if (isExiting) return
        isExiting = true
        scope.launch {
            launch {
                exitScale.animateTo(
                    targetValue = 1.05f,
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                )
            }
            exitAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
            onFinished()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "intro_glow")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_radius"
    )
    val orbitalRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(11000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbital_rotation"
    )
    val waveShift by infiniteTransition.animateFloat(
        initialValue = -0.04f,
        targetValue = 0.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_shift"
    )

    LaunchedEffect(Unit) {
        launch {
            logoAlpha.animateTo(1f, animationSpec = tween(420, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            rippleExpand.animateTo(
                targetValue = 1f,
                animationSpec = tween(900, easing = FastOutSlowInEasing)
            )
        }
        launch {
            delay(110)
            arcSweep.animateTo(
                targetValue = 1f,
                animationSpec = tween(1150, easing = FastOutSlowInEasing)
            )
        }
        delay(340)
        showBadgeChip = true
        delay(140)
        showWordmark = true
        delay(1500)
        triggerSmoothExit()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = exitAlpha.value
                scaleX = exitScale.value
                scaleY = exitScale.value
            }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PvsForestDark,
                        Color(0xFF13482B),
                        PvsForestGreen,
                        Color(0xFF113F26)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { triggerSmoothExit() }
            )
            .testTag("intro_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Layered animated PVS curves, radial emerald glow, and orbital sweep arcs
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.42f)
            val baseRadius = size.minDimension * 0.34f

            // Ambient radial leaf glow behind logo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PvsLeafBright.copy(alpha = 0.28f * rippleExpand.value),
                        PvsLeafVibrant.copy(alpha = 0.12f * rippleExpand.value),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 2.1f
                ),
                radius = baseRadius * 2.1f,
                center = center
            )

            // Top flowing botanical curve
            val topWave = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height * (0.14f + waveShift * 0.4f))
                cubicTo(
                    size.width * 0.68f, size.height * (0.22f - waveShift * 0.5f),
                    size.width * 0.28f, size.height * (0.07f + waveShift * 0.4f),
                    0f, size.height * (0.17f - waveShift * 0.3f)
                )
                close()
            }
            drawPath(topWave, color = PvsLeafVibrant.copy(alpha = 0.16f * arcSweep.value))

            // Expanding soft concentric ripple rings
            drawCircle(
                color = PvsLeafBright.copy(alpha = 0.16f * rippleExpand.value),
                radius = baseRadius * pulseRadius,
                center = center
            )
            drawCircle(
                color = PvsMintContainer.copy(alpha = 0.22f * rippleExpand.value),
                radius = baseRadius * 0.87f,
                center = center
            )

            // Primary orbital gradient arc (Bright Leaf -> Mint -> Vibrant Leaf -> Forest)
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        PvsLeafBright,
                        PvsMintSurface,
                        PvsLeafVibrant,
                        PvsForestGreen,
                        PvsLeafBright
                    ),
                    center = center
                ),
                startAngle = -90f + orbitalRotation * 0.45f,
                sweepAngle = 310f * arcSweep.value,
                useCenter = false,
                topLeft = Offset(center.x - baseRadius * 1.13f, center.y - baseRadius * 1.13f),
                size = Size(baseRadius * 2.26f, baseRadius * 2.26f),
                style = Stroke(width = 5.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Counter-rotating outer PVS mint accent arc
            drawArc(
                color = PvsMintContainer.copy(alpha = 0.45f),
                startAngle = 30f - orbitalRotation * 0.28f,
                sweepAngle = 145f * arcSweep.value,
                useCenter = false,
                topLeft = Offset(center.x - baseRadius * 1.26f, center.y - baseRadius * 1.26f),
                size = Size(baseRadius * 2.52f, baseRadius * 2.52f),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Dual bottom botanical waves
            val backWave = Path().apply {
                moveTo(0f, size.height * (0.80f + waveShift * 0.4f))
                cubicTo(
                    size.width * 0.28f, size.height * (0.73f - waveShift * 0.5f),
                    size.width * 0.68f, size.height * (0.90f + waveShift * 0.3f),
                    size.width, size.height * (0.79f - waveShift * 0.4f)
                )
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(backWave, color = PvsLeafVibrant.copy(alpha = 0.18f * arcSweep.value))

            val frontWave = Path().apply {
                moveTo(0f, size.height * (0.86f - waveShift * 0.3f))
                cubicTo(
                    size.width * 0.34f, size.height * (0.94f + waveShift * 0.4f),
                    size.width * 0.68f, size.height * (0.79f - waveShift * 0.4f),
                    size.width, size.height * (0.85f + waveShift * 0.3f)
                )
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(frontWave, color = PvsLeafBright.copy(alpha = 0.14f * arcSweep.value))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 32.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(204.dp)
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        alpha = logoAlpha.value
                    },
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 18.dp,
                border = BorderStroke(3.dp, PvsLeafBright.copy(alpha = 0.65f))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.pvs_logo_official),
                    contentDescription = "PVS Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            AnimatedVisibility(
                visible = showBadgeChip,
                enter = fadeIn(tween(340)) + slideInVertically(tween(360)) { it / 2 }
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = PvsLeafVibrant.copy(alpha = 0.24f),
                    border = BorderStroke(1.dp, PvsLeafBright.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = "PVS · FINANZAS",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = PvsLeafBright,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedVisibility(
                visible = showWordmark,
                enter = fadeIn(tween(420)) + slideInVertically(tween(440)) { it / 2 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PVS Cuotas",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.brandTagline,
                        style = MaterialTheme.typography.bodyLarge,
                        color = PvsMintSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    // Smooth animated loading pill bar in PVS bright green & mint colors
                    Box(
                        modifier = Modifier
                            .width(104.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.18f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(arcSweep.value.coerceIn(0.14f, 1f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(PvsLeafVibrant, PvsLeafBright, PvsMintSurface)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive 3-step PVS Onboarding Tutorial with curved green design,
 * animated interactive mini-previews, and bouncy tactile buttons.
 */
@Composable
fun PvsTutorialScreen(
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val haptics = LocalHapticFeedback.current
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val skipInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("tutorial_screen")
    ) {
        // Subtle organic background curves
        val curveColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val topCurve = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height * 0.22f)
                cubicTo(
                    size.width * 0.68f, size.height * 0.29f,
                    size.width * 0.30f, size.height * 0.14f,
                    0f, size.height * 0.24f
                )
                close()
            }
            drawPath(topCurve, color = curveColor)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.Center)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: PVS Logo + Language Toggle + Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PvsLogoMark(size = 40.dp)
                    Text(
                        text = "PVS Cuotas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PvsLanguageTogglePill(
                        language = language,
                        onToggle = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleLanguage()
                        }
                    )
                    TextButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onFinish()
                        },
                        interactionSource = skipInteraction,
                        modifier = Modifier
                            .pvsBouncyScale(skipInteraction)
                            .testTag("tutorial_skip_button")
                    ) {
                        Text(
                            text = strings.tutorialSkip,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Interactive Horizontal Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                TutorialStepPage(
                    page = page,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Animated Page Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                repeat(3) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 28.dp else 9.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "dot_width"
                    )
                    Box(
                        modifier = Modifier
                            .height(9.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(if (isSelected) PvsLeafVibrant else PvsMintContainer)
                    )
                }
            }

            // Primary Bouncy CTA Button
            val isLastPage = pagerState.currentPage == 2
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isLastPage) {
                        onFinish()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                interactionSource = nextInteraction,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PvsForestGreen,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .pvsBouncyScale(nextInteraction, pressedScale = 0.95f)
                    .testTag("tutorial_next_button")
            ) {
                AnimatedContent(
                    targetState = isLastPage,
                    transitionSpec = { fadeIn(tween(200)).togetherWith(fadeOut(tween(150))) },
                    label = "tutorial_btn_label"
                ) { last ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (last) strings.tutorialStart else strings.tutorialNext,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (last) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun TutorialStepPage(
    page: Int,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val (title, body, icon) = when (page) {
        0 -> Triple(strings.tutorialStep1Title, strings.tutorialStep1Body, Icons.Rounded.ReceiptLong)
        1 -> Triple(strings.tutorialStep2Title, strings.tutorialStep2Body, Icons.Rounded.QrCodeScanner)
        else -> Triple(strings.tutorialStep3Title, strings.tutorialStep3Body, Icons.Rounded.NotificationsActive)
    }

    Column(
        modifier = modifier.padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated Live Visual Preview per step
        when (page) {
            0 -> TutorialPreviewInstallments()
            1 -> TutorialPreviewVerifier()
            else -> TutorialPreviewAlerts()
        }

        Spacer(modifier = Modifier.height(28.dp))

        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "0${page + 1} / 03",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun TutorialPreviewInstallments() {
    var progress by remember { mutableStateOf(0.35f) }
    LaunchedEffect(Unit) {
        delay(250)
        progress = 0.68f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                    Icon(
                        imageVector = Icons.Rounded.DragIndicator,
                        contentDescription = null,
                        tint = PvsLeafVibrant,
                        modifier = Modifier.size(20.dp)
                    )
                    CodePillBadge(code = "TV-50")
                    Text(
                        text = "Smart TV 50\" 4K",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "$48.63",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedQuotaProgressBar(progress = progress, height = 10.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Inicial: $97.25",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "4/6 cuotas",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun TutorialPreviewVerifier() {
    val strings = LocalAppStrings.current
    var activeVerdict by remember { mutableStateOf(PriceVerdict.GREEN) }
    LaunchedEffect(Unit) {
        val cycle = listOf(PriceVerdict.GREEN, PriceVerdict.YELLOW, PriceVerdict.RED)
        var i = 0
        while (true) {
            delay(1400)
            i = (i + 1) % cycle.size
            activeVerdict = cycle[i]
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("TV-50", "CEL-128", "ZAP-01").forEach { code ->
                        CodePillBadge(code = code)
                    }
                }
                TrafficLightSemaphore(activeVerdict = activeVerdict)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SemaphoreGreenBg
            ) {
                AnimatedContent(
                    targetState = activeVerdict,
                    transitionSpec = { fadeIn(tween(250)).togetherWith(fadeOut(tween(150))) },
                    label = "tutorial_verdict_cycle"
                ) { verdict ->
                    Text(
                        text = strings.verdictHeadline(verdict),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SemaphoreGreen,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialPreviewAlerts() {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.2.dp, PvsLeafVibrant.copy(alpha = 0.5f))
    ) {
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
                    .background(
                        Brush.linearGradient(colors = listOf(PvsForestDark, PvsForestGreen))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = PvsLeafBright,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${strings.notifUpcomingTitle} · TV-50",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Smart TV 50\" 4K UHD",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$48.63 · ${strings.urgencyLabel(2)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
