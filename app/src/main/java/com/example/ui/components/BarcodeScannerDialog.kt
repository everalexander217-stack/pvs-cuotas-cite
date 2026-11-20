package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.PvsForestDark
import com.example.ui.theme.PvsForestGreen
import com.example.ui.theme.PvsLeafBright
import com.example.ui.theme.PvsLeafVibrant
import com.example.ui.theme.PvsMintContainer
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode

/**
 * Full-screen CameraX + ML Kit barcode/QR scanner dialog with runtime CAMERA permission
 * handling, PVS viewfinder overlay, torch toggle, and single-shot barcode capture.
 */
@Composable
fun BarcodeScannerDialog(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val haptics = LocalHapticFeedback.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequested by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionRequested = true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission && !permissionRequested) {
            permissionRequested = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF071E12))
                .testTag("barcode_scanner_dialog")
        ) {
            if (hasCameraPermission) {
                CameraXBarcodePreview(
                    onBarcodeDetected = { value ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBarcodeScanned(value)
                    },
                    onDismiss = onDismiss
                )
            } else {
                CameraPermissionFallback(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onOpenSettings = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun CameraXBarcodePreview(
    onBarcodeDetected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val strings = LocalAppStrings.current
    val haptics = LocalHapticFeedback.current

    var isTorchEnabled by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var hasDetectedBarcode by remember { mutableStateOf(false) }

    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        BarcodeScanning.getClient(options)
    }

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        }
    }

    LaunchedEffect(useFrontCamera) {
        runCatching {
            cameraController.cameraSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            isTorchEnabled = false
            cameraController.enableTorch(false)
        }.onFailure {
            cameraError = strings.scannerCameraError
        }
    }

    DisposableEffect(lifecycleOwner, cameraController, barcodeScanner) {
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val analyzer = MlKitAnalyzer(
            listOf(barcodeScanner),
            CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
            mainExecutor
        ) { result ->
            if (hasDetectedBarcode) return@MlKitAnalyzer
            val barcodes = result?.getValue(barcodeScanner)
            val rawValue = barcodes
                ?.firstOrNull { !it.rawValue.isNullOrBlank() }
                ?.rawValue
                ?.trim()
            if (!rawValue.isNullOrEmpty()) {
                hasDetectedBarcode = true
                onBarcodeDetected(rawValue)
            }
        }

        runCatching {
            cameraController.setImageAnalysisAnalyzer(mainExecutor, analyzer)
            cameraController.bindToLifecycle(lifecycleOwner)
        }.onFailure {
            cameraError = strings.scannerCameraError
        }

        onDispose {
            runCatching {
                cameraController.enableTorch(false)
                cameraController.clearImageAnalysisAnalyzer()
                cameraController.unbind()
                barcodeScanner.close()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    controller = cameraController
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("camerax_preview_view")
        )

        // PVS Viewfinder Overlay with Cutout + Animated Green Scan Line
        ScannerViewfinderOverlay(modifier = Modifier.fillMaxSize())

        // Top Bar Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                PvsLogoMark(size = 38.dp)
                Column {
                    Text(
                        text = strings.scannerTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = strings.scannerSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = PvsMintContainer
                    )
                }
            }

            IconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDismiss()
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .testTag("close_scanner_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = strings.scannerCloseCd,
                    tint = Color.White
                )
            }
        }

        // Bottom Hint + Torch / Camera Switch Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 22.dp)
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (cameraError != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFB3261E).copy(alpha = 0.9f)
                ) {
                    Text(
                        text = cameraError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = PvsForestDark.copy(alpha = 0.82f),
                    border = BorderStroke(1.dp, PvsLeafVibrant.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = strings.scannerFrameHint,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val nextTorch = !isTorchEnabled
                        isTorchEnabled = nextTorch
                        runCatching { cameraController.enableTorch(nextTorch) }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            if (isTorchEnabled) PvsLeafVibrant else Color.Black.copy(alpha = 0.6f)
                        )
                        .testTag("scanner_torch_button")
                ) {
                    Icon(
                        imageVector = if (isTorchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                        contentDescription = strings.scannerTorchCd,
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        useFrontCamera = !useFrontCamera
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .testTag("scanner_switch_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Cameraswitch,
                        contentDescription = strings.scannerSwitchCameraCd,
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ScannerViewfinderOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y_progress"
    )

    Canvas(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
    ) {
        val boxW = (size.width * 0.76f).coerceAtMost(320.dp.toPx())
        val boxH = (boxW * 0.62f).coerceAtMost(size.height * 0.40f)
        val left = (size.width - boxW) / 2f
        val top = (size.height - boxH) / 2f
        val radius = 24.dp.toPx()

        // Dim surrounding screen
        drawRect(color = Color.Black.copy(alpha = 0.54f))

        // Clear transparent cutout window
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(radius, radius),
            blendMode = BlendMode.Clear
        )

        // Subtle outer border
        drawRoundRect(
            color = Color.White.copy(alpha = 0.30f),
            topLeft = Offset(left, top),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Animated PVS bright green scan beam inside viewfinder
        val laserY = top + boxH * laserProgress
        drawLine(
            color = PvsLeafBright,
            start = Offset(left + 16.dp.toPx(), laserY),
            end = Offset(left + boxW - 16.dp.toPx(), laserY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Corner brackets in PVS Leaf Vibrant green
        val cornerLen = 28.dp.toPx()
        val stroke = 4.dp.toPx()
        // Top-left
        drawLine(PvsLeafVibrant, Offset(left, top + cornerLen), Offset(left, top + radius / 2f), stroke, StrokeCap.Round)
        drawLine(PvsLeafVibrant, Offset(left + radius / 2f, top), Offset(left + cornerLen, top), stroke, StrokeCap.Round)
        // Top-right
        drawLine(PvsLeafVibrant, Offset(left + boxW - cornerLen, top), Offset(left + boxW - radius / 2f, top), stroke, StrokeCap.Round)
        drawLine(PvsLeafVibrant, Offset(left + boxW, top + radius / 2f), Offset(left + boxW, top + cornerLen), stroke, StrokeCap.Round)
        // Bottom-left
        drawLine(PvsLeafVibrant, Offset(left, top + boxH - cornerLen), Offset(left, top + boxH - radius / 2f), stroke, StrokeCap.Round)
        drawLine(PvsLeafVibrant, Offset(left + radius / 2f, top + boxH), Offset(left + cornerLen, top + boxH), stroke, StrokeCap.Round)
        // Bottom-right
        drawLine(PvsLeafVibrant, Offset(left + boxW - cornerLen, top + boxH), Offset(left + boxW - radius / 2f, top + boxH), stroke, StrokeCap.Round)
        drawLine(PvsLeafVibrant, Offset(left + boxW, top + boxH - radius / 2f), Offset(left + boxW, top + boxH - cornerLen), stroke, StrokeCap.Round)
    }
}

@Composable
private fun CameraPermissionFallback(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
                .testTag("close_scanner_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = strings.scannerCloseCd,
                tint = Color.White
            )
        }

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(PvsMintContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = null,
                        tint = PvsForestGreen,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Text(
                    text = strings.scannerPermissionTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PvsForestDark,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = strings.scannerPermissionBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PvsForestGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("grant_camera_permission_button")
                ) {
                    Text(strings.scannerPermissionGrantBtn)
                }
                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(strings.scannerPermissionSettingsBtn)
                }
            }
        }
    }
}
