package com.example.plate

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class PlateImageAnalyzer(
    private val isActiveProvider: () -> Boolean,
    private val onFrameAnalyzed: () -> Unit,
    private val onTextRecognized: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val lastProcessedMs = AtomicLong(0L)
    private val isProcessing = AtomicBoolean(false)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (!isActiveProvider()) {
            imageProxy.close()
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastProcessedMs.get() < SCAN_INTERVAL_MS || !isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            isProcessing.set(false)
            imageProxy.close()
            return
        }

        lastProcessedMs.set(now)
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                onFrameAnalyzed()
                val text = visionText.text
                if (text.isNotBlank()) {
                    onTextRecognized(text)
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "ML Kit OCR frame failed", e)
            }
            .addOnCompleteListener {
                isProcessing.set(false)
                imageProxy.close()
            }
    }

    fun close() {
        runCatching { recognizer.close() }
    }

    companion object {
        private const val TAG = "PlateImageAnalyzer"
        private const val SCAN_INTERVAL_MS = 450L

        @OptIn(ExperimentalGetImage::class)
        fun recognizeTextFromCapturedProxy(
            imageProxy: ImageProxy,
            onResult: (String) -> Unit,
            onError: (Throwable) -> Unit
        ) {
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                onError(IllegalStateException("Captured frame has no image buffer"))
                return
            }

            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(inputImage)
                .addOnSuccessListener { result ->
                    onResult(result.text)
                }
                .addOnFailureListener { error ->
                    onError(error)
                }
                .addOnCompleteListener {
                    runCatching { recognizer.close() }
                    imageProxy.close()
                }
        }

        suspend fun recognizeTextFromUri(context: Context, uri: Uri): String {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            return try {
                suspendCancellableCoroutine { continuation ->
                    recognizer.process(image)
                        .addOnSuccessListener { result ->
                            if (continuation.isActive) {
                                continuation.resume(result.text)
                            }
                        }
                        .addOnFailureListener { error ->
                            if (continuation.isActive) {
                                continuation.resumeWithException(error)
                            }
                        }
                }
            } finally {
                runCatching { recognizer.close() }
            }
        }
    }
}
