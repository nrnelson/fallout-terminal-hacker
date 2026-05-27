package com.n3network.falloutterminalhacker.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.n3network.falloutterminalhacker.camera.CameraPreview
import com.n3network.falloutterminalhacker.ocr.EnglishDictionary
import com.n3network.falloutterminalhacker.ocr.TextRecognizer
import com.n3network.falloutterminalhacker.ocr.WordExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

@Composable
fun CameraScreen(onWordsCaptured: (List<String>) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    val recognizer = remember { TextRecognizer() }
    DisposableEffect(recognizer) {
        onDispose { recognizer.close() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            CameraPreview(
                onImageCaptureReady = { imageCapture = it },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "CAMERA PERMISSION REQUIRED",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // Controls overlay — inset to avoid status bar / nav bar
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Text(
                "ROBCO TERMLINK SCANNER",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                status?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (isProcessing) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "PROCESSING...",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                } else {
                    TerminalButton(
                        text = "SCAN TERMINAL",
                        onClick = {
                            val capture = imageCapture ?: return@TerminalButton
                            isProcessing = true
                            status = null
                            scope.launch {
                                try {
                                    val bitmap = captureBitmap(capture, context)
                                        .downscaleToLongSide(OCR_MAX_LONG_SIDE_PX)
                                    try {
                                        val dict = EnglishDictionary.get(context)
                                        val words = withContext(Dispatchers.Default) {
                                            val raw = recognizer.recognize(bitmap)
                                            WordExtractor.extract(raw, dict::contains)
                                        }
                                        when {
                                            words.size < 2 ->
                                                status = "Found only ${words.size} words. Retry."
                                            else -> onWordsCaptured(words)
                                        }
                                    } finally {
                                        bitmap.recycle()
                                    }
                                } catch (e: Exception) {
                                    status = "Capture failed: ${e.message}"
                                } finally {
                                    isProcessing = false
                                }
                            }
                        },
                        enabled = imageCapture != null && hasPermission
                    )
                }
            }
        }
    }
}

/**
 * Long-side cap (px) we feed into ML Kit text recognition. Modern phone cameras
 * shoot ~4000 px on the long side; that's wasteful for terminal-text OCR and
 * makes each scan slower and hungrier on memory. ~1600 keeps glyphs crisp.
 */
private const val OCR_MAX_LONG_SIDE_PX = 1600

private fun Bitmap.downscaleToLongSide(maxPx: Int): Bitmap {
    val longSide = maxOf(width, height)
    if (longSide <= maxPx) return this
    val scale = maxPx.toFloat() / longSide
    val scaled = Bitmap.createScaledBitmap(
        this,
        (width * scale).toInt(),
        (height * scale).toInt(),
        true
    )
    if (scaled !== this) recycle()
    return scaled
}

private suspend fun captureBitmap(
    imageCapture: ImageCapture,
    context: Context
): Bitmap = suspendCancellableCoroutine { cont ->
    val executor = ContextCompat.getMainExecutor(context)
    imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            try {
                val raw = image.toBitmap()
                val rotation = image.imageInfo.rotationDegrees
                val rotated = if (rotation != 0) {
                    val m = Matrix().apply { postRotate(rotation.toFloat()) }
                    Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
                        .also { raw.recycle() }
                } else raw
                cont.resume(rotated)
            } catch (e: Exception) {
                cont.resumeWith(Result.failure(e))
            } finally {
                image.close()
            }
        }

        override fun onError(exception: ImageCaptureException) {
            cont.resumeWith(Result.failure(exception))
        }
    })
}
