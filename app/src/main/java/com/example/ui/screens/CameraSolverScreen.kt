package com.example.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.localization.ZeluStrings
import com.example.model.SolvedQuestion
import com.example.ui.components.MathSolutionCard
import com.example.ui.theme.DeepNavy900
import com.example.ui.theme.ElectricBlue
import com.example.viewmodel.FollowUpUiState
import com.example.viewmodel.SolverUiState
import kotlin.math.min

private enum class CropAspect { FULL, SQUARE, WIDE }

@Composable
fun CameraSolverScreen(
    strings: ZeluStrings,
    pendingImageUri: Uri?,
    onPendingUriConsumed: () -> Unit,
    solverState: SolverUiState,
    followUpState: FollowUpUiState,
    onSolveImage: (bitmap: Bitmap, hint: String, difficulty: String) -> Unit,
    onToggleBookmark: (SolvedQuestion) -> Unit,
    onListenVoice: (SolvedQuestion) -> Unit,
    onAskFollowUp: (SolvedQuestion, Int?, String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var activeCrop by remember { mutableStateOf(CropAspect.FULL) }
    var extraHint by rememberSaveable { mutableStateOf("") }
    var difficultyLevel by rememberSaveable { mutableStateOf("detailed") }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    LaunchedEffect(pendingImageUri) {
        if (pendingImageUri != null) {
            decodeUriBitmap(context, pendingImageUri)?.let { bmp ->
                capturedBitmap = bmp
                activeCrop = CropAspect.FULL
            }
            onPendingUriConsumed()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            decodeUriBitmap(context, uri)?.let { bmp ->
                capturedBitmap = bmp
                activeCrop = CropAspect.FULL
                cameraError = null
            } ?: run {
                cameraError = "Could not load selected image."
            }
        }
    }

    val systemCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            capturedBitmap = bmp
            activeCrop = CropAspect.FULL
            cameraError = null
        }
    }

    val previewBitmap = remember(capturedBitmap, activeCrop) {
        capturedBitmap?.let { applyCropPreset(it, activeCrop) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 760.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = strings.cameraTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (previewBitmap == null) {
                // Live CameraX or Photo Picker view
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (hasCameraPermission) {
                            val previewView = remember(context) {
                                PreviewView(context).apply {
                                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                }
                            }

                            DisposableEffect(previewView, lifecycleOwner) {
                                var disposed = false
                                var boundProvider: ProcessCameraProvider? = null
                                var boundCamera: Camera? = null

                                val cameraStateObserver = Observer<CameraState> { state ->
                                    val error = state.error
                                    if (error != null) {
                                        Log.w("CameraSolver", "Camera state error: ${error.code}")
                                        cameraError = "Camera preview unavailable on this device. Use Pick Photo below."
                                    } else if (state.type == CameraState.Type.OPEN) {
                                        cameraError = null
                                    }
                                }

                                previewView.post {
                                    if (disposed) return@post
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                                    cameraProviderFuture.addListener({
                                        if (disposed) return@addListener
                                        try {
                                            val cameraProvider = cameraProviderFuture.get()
                                            boundProvider = cameraProvider
                                            val selector = when {
                                                cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ->
                                                    CameraSelector.DEFAULT_BACK_CAMERA
                                                cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ->
                                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                                else -> null
                                            }
                                            if (selector == null) {
                                                cameraError = "No camera sensor detected. Use Pick Photo below."
                                                return@addListener
                                            }
                                            val resolutionSelector = ResolutionSelector.Builder()
                                                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                                                .build()
                                            val preview = Preview.Builder()
                                                .setResolutionSelector(resolutionSelector)
                                                .build()
                                                .also { it.surfaceProvider = previewView.surfaceProvider }
                                            val capture = ImageCapture.Builder()
                                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                                .setResolutionSelector(resolutionSelector)
                                                .build()
                                            imageCapture = capture
                                            cameraProvider.unbindAll()
                                            val camera = cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                selector,
                                                preview,
                                                capture
                                            )
                                            boundCamera = camera
                                            camera.cameraInfo.cameraState.observe(lifecycleOwner, cameraStateObserver)
                                            cameraError = null
                                        } catch (e: Exception) {
                                            Log.w("CameraSolver", "Camera bind failed", e)
                                            cameraError = "Camera preview unavailable on this device. Use Pick Photo below."
                                        }
                                    }, ContextCompat.getMainExecutor(context))
                                }

                                onDispose {
                                    disposed = true
                                    imageCapture = null
                                    boundCamera?.cameraInfo?.cameraState?.removeObserver(cameraStateObserver)
                                    runCatching { boundProvider?.unbindAll() }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(DeepNavy900)
                            ) {
                                AndroidView(
                                    factory = { previewView },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(36.dp))
                                    Text(strings.cameraPermissionRequired, style = MaterialTheme.typography.bodyMedium)
                                    Button(
                                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                        modifier = Modifier.testTag("grant_camera_button")
                                    ) {
                                        Text(strings.grantCameraPermission)
                                    }
                                }
                            }
                        }

                        if (cameraError != null) {
                            Text(
                                text = cameraError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (hasCameraPermission) {
                                Button(
                                    onClick = {
                                        val capture = imageCapture
                                        if (capture == null || cameraError != null) {
                                            try {
                                                systemCameraLauncher.launch(null)
                                            } catch (_: ActivityNotFoundException) {
                                                cameraError = "Camera unavailable. Use Pick Photo instead."
                                            }
                                            return@Button
                                        }
                                        capture.takePicture(
                                            ContextCompat.getMainExecutor(context),
                                            object : ImageCapture.OnImageCapturedCallback() {
                                                override fun onCaptureSuccess(image: ImageProxy) {
                                                    val bmp = image.toBitmap()
                                                    val rotation = image.imageInfo.rotationDegrees.toFloat()
                                                    image.close()
                                                    capturedBitmap = if (rotation != 0f) rotateBitmap(bmp, rotation) else bmp
                                                    activeCrop = CropAspect.FULL
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    cameraError = exception.localizedMessage ?: "Capture failed."
                                                }
                                            }
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("capture_math_photo_button")
                                ) {
                                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.capturePhoto)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("pick_math_photo_button")
                            ) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.pickFromGallery)
                            }
                        }
                    }
                }
            } else {
                // Interactive Crop, Rotate & Retake Editor
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = strings.cameraTitle,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DeepNavy900)
                        )

                        // Rotate & Crop Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { capturedBitmap = capturedBitmap?.let { rotateBitmap(it, -90f) } },
                                modifier = Modifier.weight(1f).testTag("rotate_left_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = strings.rotateLeft)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.rotateLeft, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { capturedBitmap = capturedBitmap?.let { rotateBitmap(it, 90f) } },
                                modifier = Modifier.weight(1f).testTag("rotate_right_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = strings.rotateRight)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.rotateRight, maxLines = 1)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = activeCrop == CropAspect.FULL,
                                onClick = { activeCrop = CropAspect.FULL },
                                label = { Text(strings.cropFree) },
                                leadingIcon = { Icon(Icons.Filled.Crop, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            FilterChip(
                                selected = activeCrop == CropAspect.SQUARE,
                                onClick = { activeCrop = CropAspect.SQUARE },
                                label = { Text(strings.cropSquare) }
                            )
                            FilterChip(
                                selected = activeCrop == CropAspect.WIDE,
                                onClick = { activeCrop = CropAspect.WIDE },
                                label = { Text(strings.cropWide) }
                            )
                        }

                        OutlinedTextField(
                            value = extraHint,
                            onValueChange = { extraHint = it },
                            label = { Text("Optional instruction (e.g., solve question #2)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = difficultyLevel == "easy",
                                onClick = { difficultyLevel = "easy" },
                                shape = SegmentedButtonDefaults.itemShape(0, 2)
                            ) { Text(strings.easyMode, maxLines = 1) }
                            SegmentedButton(
                                selected = difficultyLevel == "detailed",
                                onClick = { difficultyLevel = "detailed" },
                                shape = SegmentedButtonDefaults.itemShape(1, 2)
                            ) { Text(strings.detailedMode, maxLines = 1) }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { capturedBitmap = null },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("retake_photo_button")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.retake)
                            }
                            Button(
                                onClick = { onSolveImage(previewBitmap, extraHint, difficultyLevel) },
                                enabled = solverState !is SolverUiState.Solving,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(50.dp)
                                    .testTag("analyze_photo_button")
                            ) {
                                if (solverState is SolverUiState.Solving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.analyzeAndSolve, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Output card
            when (solverState) {
                is SolverUiState.UnclearImage -> {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.WarningAmber, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.unclearImageTitle, fontWeight = FontWeight.Bold)
                            }
                            Text(solverState.message, style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(onClick = { capturedBitmap = null }) {
                                Text(strings.retake)
                            }
                        }
                    }
                }
                is SolverUiState.Solved -> {
                    MathSolutionCard(
                        question = solverState.question,
                        strings = strings,
                        uncertaintyNote = solverState.uncertaintyNote,
                        followUpState = followUpState,
                        onToggleBookmark = { onToggleBookmark(solverState.question) },
                        onListenVoice = { onListenVoice(solverState.question) },
                        onAskFollowUp = { stepIdx, text -> onAskFollowUp(solverState.question, stepIdx, text) }
                    )
                }
                is SolverUiState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${strings.errorPrefix}: ${solverState.message}",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                else -> Unit
            }
        }
    }
}

private fun decodeUriBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    } catch (_: Exception) {
        null
    }
}

private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

private fun applyCropPreset(source: Bitmap, aspect: CropAspect): Bitmap {
    if (aspect == CropAspect.FULL) return source
    val w = source.width
    val h = source.height
    return when (aspect) {
        CropAspect.SQUARE -> {
            val side = min(w, h)
            Bitmap.createBitmap(source, (w - side) / 2, (h - side) / 2, side, side)
        }
        CropAspect.WIDE -> {
            val targetH = min(h, (w * 9) / 16).coerceAtLeast(1)
            val targetW = min(w, (targetH * 16) / 9).coerceAtLeast(1)
            Bitmap.createBitmap(source, (w - targetW) / 2, (h - targetH) / 2, targetW, targetH)
        }
        CropAspect.FULL -> source
    }
}
