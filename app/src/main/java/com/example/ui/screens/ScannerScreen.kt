package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.plate.PlateImageAnalyzer
import com.example.ui.components.LicensePlateBadge
import com.example.ui.components.ManualCheckDialog
import com.example.ui.components.PlateBadgeStyle
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.IceWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBase
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SlateMuted
import com.example.viewmodel.ScannerUiState
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Composable
fun ScannerScreen(
    scannerState: ScannerUiState,
    hasCameraPermission: Boolean,
    activeStolenCount: Int,
    onRequestCameraPermission: () -> Unit,
    onToggleScan: () -> Unit,
    onToggleTorch: () -> Unit,
    onToggleSound: () -> Unit,
    onSwitchCameraLens: () -> Unit,
    onSelectZoom: (Float) -> Unit,
    onFrameAnalyzed: () -> Unit,
    onCameraTextRecognized: (String) -> Unit,
    onCaptureStarted: () -> Unit,
    onCaptureRecognized: (String) -> Unit,
    onCaptureFailed: (String) -> Unit,
    onScanPhotoUri: (Uri) -> Unit,
    onCheckPlateManually: (String) -> Unit,
    onOpenDatabase: () -> Unit
) {
    val context = LocalContext.current
    var cameraErrorText by remember { mutableStateOf<String?>(null) }
    var isStreamLive by remember { mutableStateOf(false) }
    var hasMultipleCameras by remember { mutableStateOf(false) }
    var captureTrigger by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showManualCheckDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onScanPhotoUri(uri)
        }
    }

    if (showManualCheckDialog) {
        ManualCheckDialog(
            onDismiss = { showManualCheckDialog = false },
            onCheckPlate = onCheckPlateManually
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBase)
    ) {
        if (hasCameraPermission && cameraErrorText == null) {
            LiveCameraPreviewLayer(
                isScanningActive = scannerState.isScanningActive,
                isTorchEnabled = scannerState.isTorchEnabled,
                lensFacing = scannerState.lensFacing,
                zoomRatio = scannerState.zoomRatio,
                onFrameAnalyzed = onFrameAnalyzed,
                onTextRecognized = onCameraTextRecognized,
                onStreamStateChanged = { isStreamLive = it },
                onMultipleCamerasDetected = { hasMultipleCameras = it },
                onCaptureReady = { trigger -> captureTrigger = trigger },
                onCaptureStarted = onCaptureStarted,
                onCaptureRecognized = onCaptureRecognized,
                onCaptureFailed = onCaptureFailed,
                onCameraError = { cameraErrorText = it }
            )

            // Tactical Radar HUD overlay
            RadarReticleOverlay(
                isActive = scannerState.isScanningActive,
                isMatch = scannerState.lastScanMatched,
                isStreamLive = isStreamLive,
                modifier = Modifier.fillMaxSize()
            )

            // Top HUD bar
            TopScannerHudBar(
                isScanningActive = scannerState.isScanningActive,
                isStreamLive = isStreamLive,
                isTorchEnabled = scannerState.isTorchEnabled,
                isSoundEnabled = scannerState.isSoundEnabled,
                lensFacing = scannerState.lensFacing,
                hasMultipleCameras = hasMultipleCameras,
                activeStolenCount = activeStolenCount,
                framesAnalyzed = scannerState.framesAnalyzed,
                onToggleTorch = onToggleTorch,
                onToggleSound = onToggleSound,
                onSwitchCamera = onSwitchCameraLens,
                onOpenDatabase = onOpenDatabase,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // Bottom telemetry, zoom & controls dock
            BottomTelemetryDock(
                scannerState = scannerState,
                canCapture = captureTrigger != null && !scannerState.isCapturingFrame,
                onSelectZoom = onSelectZoom,
                onCaptureNow = { captureTrigger?.invoke() },
                onToggleScan = onToggleScan,
                onPickPhoto = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onOpenManualCheck = { showManualCheckDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        } else {
            CameraOnboardingView(
                cameraError = cameraErrorText,
                activeStolenCount = activeStolenCount,
                isAnalyzingPhoto = scannerState.isAnalyzingPhoto,
                lastDetectedPlate = scannerState.lastDetectedPlate,
                lastScanMatched = scannerState.lastScanMatched,
                onRequestCamera = {
                    cameraErrorText = null
                    onRequestCameraPermission()
                },
                onOpenSettings = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                },
                onPickPhoto = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onOpenManualCheck = { showManualCheckDialog = true }
            )
        }

        // Photo or shutter frame analyzing indicator overlay
        AnimatedVisibility(
            visible = scannerState.isAnalyzingPhoto || scannerState.isCapturingFrame,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianSurface.copy(alpha = 0.94f),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = RadarCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.scanner_status_analyzing),
                        style = MaterialTheme.typography.titleSmall,
                        color = IceWhite
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveCameraPreviewLayer(
    isScanningActive: Boolean,
    isTorchEnabled: Boolean,
    lensFacing: Int,
    zoomRatio: Float,
    onFrameAnalyzed: () -> Unit,
    onTextRecognized: (String) -> Unit,
    onStreamStateChanged: (Boolean) -> Unit,
    onMultipleCamerasDetected: (Boolean) -> Unit,
    onCaptureReady: ((() -> Unit)?) -> Unit,
    onCaptureStarted: () -> Unit,
    onCaptureRecognized: (String) -> Unit,
    onCaptureFailed: (String) -> Unit,
    onCameraError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentIsActive by rememberUpdatedState(isScanningActive)
    val currentOnFrame by rememberUpdatedState(onFrameAnalyzed)
    val currentOnText by rememberUpdatedState(onTextRecognized)
    val currentOnCaptureStarted by rememberUpdatedState(onCaptureStarted)
    val currentOnCaptureRecognized by rememberUpdatedState(onCaptureRecognized)
    val currentOnCaptureFailed by rememberUpdatedState(onCaptureFailed)

    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Tap-to-focus on the real camera viewfinder
    LaunchedEffect(boundCamera) {
        previewView.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val camera = boundCamera ?: return@setOnTouchListener false
                val point = previewView.meteringPointFactory.createPoint(event.x, event.y)
                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                    .build()
                runCatching { camera.cameraControl.startFocusAndMetering(action) }
                view.performClick()
                true
            } else {
                event.action == MotionEvent.ACTION_DOWN
            }
        }
    }

    // Apply live flash torch changes
    LaunchedEffect(isTorchEnabled, boundCamera) {
        runCatching {
            val cam = boundCamera ?: return@runCatching
            if (cam.cameraInfo.hasFlashUnit()) {
                cam.cameraControl.enableTorch(isTorchEnabled)
            }
        }
    }

    // Apply live zoom ratio changes
    LaunchedEffect(zoomRatio, boundCamera) {
        runCatching {
            val cam = boundCamera ?: return@runCatching
            val zoomState = cam.cameraInfo.zoomState.value
            val minZoom = zoomState?.minZoomRatio ?: 1.0f
            val maxZoom = zoomState?.maxZoomRatio ?: 4.0f
            cam.cameraControl.setZoomRatio(zoomRatio.coerceIn(minZoom, maxZoom))
        }
    }

    // Observe real CameraX preview stream state (IDLE vs STREAMING)
    DisposableEffect(previewView, lifecycleOwner) {
        val observer = Observer<PreviewView.StreamState> { state ->
            onStreamStateChanged(state == PreviewView.StreamState.STREAMING)
        }
        previewView.previewStreamState.observe(lifecycleOwner, observer)
        onDispose {
            previewView.previewStreamState.removeObserver(observer)
            onStreamStateChanged(false)
        }
    }

    // Bind real CameraX hardware whenever lifecycleOwner or selected camera lens changes
    DisposableEffect(lifecycleOwner, lensFacing) {
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val analyzer = PlateImageAnalyzer(
            isActiveProvider = { currentIsActive },
            onFrameAnalyzed = { mainExecutor.execute { currentOnFrame() } },
            onTextRecognized = { text -> mainExecutor.execute { currentOnText(text) } }
        )

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var isDisposed = false

        val bindRunnable = Runnable {
            if (isDisposed) return@Runnable
            previewView.post {
                if (isDisposed) return@post
                runCatching {
                    val cameraProvider = cameraProviderFuture.get()
                    val availableCameras = cameraProvider.availableCameraInfos
                    onMultipleCamerasDetected(availableCameras.size > 1)

                    if (availableCameras.isEmpty()) {
                        onCameraError("لا توجد كاميرا متاحة على هذا الجهاز.")
                        return@runCatching
                    }

                    val desiredSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()
                    val fallbackOpposite = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    val selector = when {
                        cameraProvider.hasCamera(desiredSelector) -> desiredSelector
                        cameraProvider.hasCamera(fallbackOpposite) -> fallbackOpposite
                        else -> availableCameras.first().cameraSelector
                    }

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(analysisExecutor, analyzer)
                        }

                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    cameraProvider.unbindAll()

                    val camera = runCatching {
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            imageAnalysis,
                            imageCapture
                        )
                    }.getOrElse {
                        // Fallback for constrained/legacy cameras that support 2 concurrent streams
                        Log.w("ScannerScreen", "3-stream bind failed, falling back to Preview + Analysis", it)
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            imageAnalysis
                        )
                    }

                    boundCamera = camera

                    // Register live shutter capture action
                    onCaptureReady {
                        currentOnCaptureStarted()
                        imageCapture.takePicture(
                            analysisExecutor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    PlateImageAnalyzer.recognizeTextFromCapturedProxy(
                                        imageProxy = image,
                                        onResult = { recognized ->
                                            mainExecutor.execute {
                                                currentOnCaptureRecognized(recognized)
                                            }
                                        },
                                        onError = { err ->
                                            Log.e("ScannerScreen", "Single-shot OCR failed", err)
                                            mainExecutor.execute {
                                                currentOnCaptureFailed("تعذر قراءة النص من اللقطة")
                                            }
                                        }
                                    )
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Log.e("ScannerScreen", "ImageCapture failed", exception)
                                    mainExecutor.execute {
                                        currentOnCaptureFailed("تعذر التقاط إطار من الكاميرا")
                                    }
                                }
                            }
                        )
                    }
                }.onFailure { e ->
                    Log.e("ScannerScreen", "Failed to bind CameraX", e)
                    onCameraError(e.localizedMessage ?: "تعذر تشغيل كاميرا الهاتف")
                }
            }
        }

        cameraProviderFuture.addListener(bindRunnable, mainExecutor)

        onDispose {
            isDisposed = true
            onCaptureReady(null)
            boundCamera = null
            runCatching {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            }
            analyzer.close()
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier
            .fillMaxSize()
            .testTag("live_camera_preview")
    )
}

@Composable
private fun RadarReticleOverlay(
    isActive: Boolean,
    isMatch: Boolean,
    isStreamLive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_sweep")
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sweep_progress"
    )

    val frameColor = when {
        isMatch -> AlertCrimson
        isActive -> RadarCyan
        else -> SignalAmber
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxWidth = (size.width * 0.82f).coerceAtMost(size.height * 0.72f)
            val boxHeight = (boxWidth * 0.34f).coerceIn(135f, size.height * 0.26f)
            val left = (size.width - boxWidth) / 2f
            val top = (size.height * 0.34f) - (boxHeight / 2f)

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.56f),
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.78f)
                    )
                )
            )

            drawRoundRect(
                color = frameColor.copy(alpha = 0.28f),
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 2.5f)
            )

            val bracketLen = 36f
            val stroke = 7f
            drawLine(frameColor, Offset(left, top + bracketLen), Offset(left, top), stroke, StrokeCap.Round)
            drawLine(frameColor, Offset(left, top), Offset(left + bracketLen, top), stroke, StrokeCap.Round)
            drawLine(frameColor, Offset(left + boxWidth - bracketLen, top), Offset(left + boxWidth, top), stroke, StrokeCap.Round)
            drawLine(frameColor, Offset(left + boxWidth, top), Offset(left + boxWidth, top + bracketLen), stroke, StrokeCap.Round)
            drawLine(frameColor, Offset(left, top + boxHeight - bracketLen), Offset(left, top + boxHeight), stroke, StrokeCap.Round)
            drawLine(frameColor, Offset(left, top + boxHeight), Offset(left + bracketLen, top + boxHeight), stroke, StrokeCap.Round)
            drawLine(frameColor, Offset(left + boxWidth - bracketLen, top + boxHeight), Offset(left + boxWidth, top + boxHeight), stroke, StrokeCap.Round)
            drawLine(frameColor, Offset(left + boxWidth, top + boxHeight - bracketLen), Offset(left + boxWidth, top + boxHeight), stroke, StrokeCap.Round)

            if (isActive && isStreamLive) {
                val laserY = top + 14f + (boxHeight - 28f) * sweepProgress
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            frameColor.copy(alpha = 0.9f),
                            Color.White,
                            frameColor.copy(alpha = 0.9f),
                            Color.Transparent
                        ),
                        startX = left + 12f,
                        endX = left + boxWidth - 12f
                    ),
                    start = Offset(left + 12f, laserY),
                    end = Offset(left + boxWidth - 12f, laserY),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }
        }

        if (!isStreamLive) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = RoundedCornerShape(18.dp),
                color = ObsidianSurface.copy(alpha = 0.9f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = RadarCyan,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.camera_connecting),
                        style = MaterialTheme.typography.titleSmall,
                        color = IceWhite
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 54.dp),
                shape = RoundedCornerShape(999.dp),
                color = ObsidianBase.copy(alpha = 0.72f)
            ) {
                Text(
                    text = stringResource(R.string.scanner_aim_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = IceWhite.copy(alpha = 0.92f),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun TopScannerHudBar(
    isScanningActive: Boolean,
    isStreamLive: Boolean,
    isTorchEnabled: Boolean,
    isSoundEnabled: Boolean,
    lensFacing: Int,
    hasMultipleCameras: Boolean,
    activeStolenCount: Int,
    framesAnalyzed: Long,
    onToggleTorch: () -> Unit,
    onToggleSound: () -> Unit,
    onSwitchCamera: () -> Unit,
    onOpenDatabase: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lensLabel = stringResource(
        if (lensFacing == CameraSelector.LENS_FACING_BACK) R.string.camera_back_lens
        else R.string.camera_front_lens
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = ObsidianSurface.copy(alpha = 0.86f),
                modifier = Modifier.border(
                    1.dp,
                    if (isStreamLive && isScanningActive) SafeEmerald.copy(alpha = 0.6f)
                    else SignalAmber.copy(alpha = 0.5f),
                    RoundedCornerShape(999.dp)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(
                                if (isStreamLive && isScanningActive) SafeEmerald
                                else SignalAmber
                            )
                    )
                    Text(
                        text = if (!isStreamLive) {
                            stringResource(R.string.camera_connecting)
                        } else if (isScanningActive) {
                            "${stringResource(R.string.camera_live_badge)} • $lensLabel"
                        } else {
                            stringResource(R.string.scanner_status_paused)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = IceWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Surface(
                onClick = onOpenDatabase,
                shape = RoundedCornerShape(999.dp),
                color = ObsidianBase.copy(alpha = 0.8f),
                modifier = Modifier.testTag("hud_stolen_count_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Radar,
                        contentDescription = null,
                        tint = if (activeStolenCount > 0) AlertCrimson else SignalAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (activeStolenCount > 0) {
                            "$activeStolenCount مركبة مسروقة • $framesAnalyzed إطار"
                        } else {
                            "أضف لوحات مسروقة للرصد"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (activeStolenCount > 0) IceWhite else SignalAmber
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hasMultipleCameras) {
                Surface(
                    shape = CircleShape,
                    color = ObsidianSurface.copy(alpha = 0.84f)
                ) {
                    IconButton(
                        onClick = onSwitchCamera,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("switch_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Cameraswitch,
                            contentDescription = stringResource(R.string.action_switch_camera),
                            tint = IceWhite
                        )
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = ObsidianSurface.copy(alpha = 0.84f)
            ) {
                IconButton(
                    onClick = onToggleSound,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("toggle_sound_button")
                ) {
                    Icon(
                        imageVector = if (isSoundEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                        contentDescription = stringResource(R.string.action_sound_toggle),
                        tint = if (isSoundEnabled) RadarCyan else SlateMuted
                    )
                }
            }

            if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                Surface(
                    shape = CircleShape,
                    color = if (isTorchEnabled) RadarCyan else ObsidianSurface.copy(alpha = 0.84f)
                ) {
                    IconButton(
                        onClick = onToggleTorch,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("toggle_torch_button")
                    ) {
                        Icon(
                            imageVector = if (isTorchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            contentDescription = stringResource(R.string.action_torch_toggle),
                            tint = if (isTorchEnabled) ObsidianBase else IceWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomTelemetryDock(
    scannerState: ScannerUiState,
    canCapture: Boolean,
    onSelectZoom: (Float) -> Unit,
    onCaptureNow: () -> Unit,
    onToggleScan: () -> Unit,
    onPickPhoto: () -> Unit,
    onOpenManualCheck: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.widthIn(max = 560.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Quick Zoom Bar (1x / 2x / 3x) for reading distant license plates
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = ObsidianBase.copy(alpha = 0.78f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(1.0f to "1x", 2.0f to "2x", 3.0f to "3x").forEach { (ratio, label) ->
                    val selected = kotlin.math.abs(scannerState.zoomRatio - ratio) < 0.25f
                    Surface(
                        onClick = { onSelectZoom(ratio) },
                        shape = RoundedCornerShape(999.dp),
                        color = if (selected) RadarCyan else Color.Transparent,
                        modifier = Modifier.testTag("zoom_${label}_button")
                    ) {
                        Text(
                            text = label,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) ObsidianBase else IceWhite,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = ObsidianSurface.copy(alpha = 0.93f),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianSurfaceVariant, RoundedCornerShape(24.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live OCR telemetry row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.scanner_last_plate_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateMuted
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        if (scannerState.lastDetectedPlate != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (scannerState.lastScanMatched) {
                                        Icons.Filled.GppMaybe
                                    } else {
                                        Icons.Filled.CheckCircle
                                    },
                                    contentDescription = null,
                                    tint = if (scannerState.lastScanMatched) AlertCrimson else SafeEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(
                                        if (scannerState.lastScanMatched) R.string.scanner_match_status
                                        else R.string.scanner_clear_status
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (scannerState.lastScanMatched) AlertCrimson else SafeEmerald,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.scanner_no_plate_yet),
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateMuted
                            )
                        }

                        if (!scannerState.lastRawOcrSnippet.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${stringResource(R.string.scanner_live_reading_prefix)} ${scannerState.lastRawOcrSnippet}",
                                style = MaterialTheme.typography.labelSmall,
                                color = RadarCyan.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (scannerState.lastDetectedPlate != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                        LicensePlateBadge(
                            plateText = scannerState.lastDetectedPlate,
                            style = if (scannerState.lastScanMatched) {
                                PlateBadgeStyle.STOLEN
                            } else {
                                PlateBadgeStyle.NEUTRAL
                            },
                            fontSize = 15.sp
                        )
                    }
                }

                // Primary Shutter Capture & Scan CTA
                Button(
                    onClick = onCaptureNow,
                    enabled = canCapture,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("capture_scan_now_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RadarCyan,
                        contentColor = ObsidianBase
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.CenterFocusStrong,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_capture_scan),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Secondary Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onToggleScan,
                        modifier = Modifier
                            .weight(1.1f)
                            .height(48.dp)
                            .testTag("toggle_scan_button"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (scannerState.isScanningActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                if (scannerState.isScanningActive) R.string.action_pause_scan
                                else R.string.action_resume_scan
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }

                    FilledTonalButton(
                        onClick = onPickPhoto,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pick_photo_scan_button"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ImageSearch,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.action_pick_photo),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenManualCheck,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("manual_check_button"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Keyboard,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.action_manual_check),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraOnboardingView(
    cameraError: String?,
    activeStolenCount: Int,
    isAnalyzingPhoto: Boolean,
    lastDetectedPlate: String?,
    lastScanMatched: Boolean,
    onRequestCamera: () -> Unit,
    onOpenSettings: () -> Unit,
    onPickPhoto: () -> Unit,
    onOpenManualCheck: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .height(190.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, RadarCyan.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_radar_patrol_1795251495890),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                shape = RoundedCornerShape(999.dp),
                color = ObsidianBase.copy(alpha = 0.85f)
            ) {
                Text(
                    text = "$activeStolenCount مركبة قيد الرصد",
                    style = MaterialTheme.typography.labelMedium,
                    color = RadarCyan,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = if (cameraError != null) "تعذر الاتصال بالكاميرا" else stringResource(R.string.camera_permission_title),
            style = MaterialTheme.typography.headlineSmall,
            color = IceWhite,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = cameraError ?: stringResource(R.string.camera_permission_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = SlateMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 440.dp)
        )

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = onRequestCamera,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .height(52.dp)
                .testTag("grant_camera_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.action_grant_camera),
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .height(48.dp)
                .testTag("open_app_settings_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_open_settings))
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.camera_unavailable_hint),
            style = MaterialTheme.typography.bodySmall,
            color = SlateMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = onPickPhoto,
                enabled = !isAnalyzingPhoto,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("onboarding_pick_photo_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.ImageSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.action_pick_photo))
            }

            OutlinedButton(
                onClick = onOpenManualCheck,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("onboarding_manual_check_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Keyboard, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.action_manual_check))
            }
        }

        if (lastDetectedPlate != null) {
            Spacer(modifier = Modifier.height(18.dp))
            LicensePlateBadge(
                plateText = lastDetectedPlate,
                style = if (lastScanMatched) PlateBadgeStyle.STOLEN else PlateBadgeStyle.NEUTRAL
            )
        }
    }
}
