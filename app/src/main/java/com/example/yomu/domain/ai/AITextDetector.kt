package com.example.yomu.domain.ai

import android.graphics.Bitmap
import android.graphics.RectF
import com.example.yomu.domain.model.PanelCoordinate
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class AITextDetector {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun detectText(originalBitmap: Bitmap, confidenceThreshold: Float = 0.25f): List<PanelCoordinate> = withContext(Dispatchers.Default) {
        try {
            val maxDim = 1024
            val maxOriginalDim = maxOf(originalBitmap.width, originalBitmap.height)
            val scale = if (maxOriginalDim > maxDim) maxDim.toFloat() / maxOriginalDim else 1.0f

            val scaledBitmap = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (originalBitmap.width * scale).toInt(),
                    (originalBitmap.height * scale).toInt(),
                    true
                )
            } else {
                originalBitmap
            }

            val inputImage = InputImage.fromBitmap(scaledBitmap, 0)
            val visionText: Text? = suspendCancellableCoroutine { continuation ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { textResult ->
                        if (continuation.isActive) continuation.resume(textResult)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            }

            if (visionText == null) return@withContext emptyList()

            val bitmapWidth = scaledBitmap.width.toFloat()
            val bitmapHeight = scaledBitmap.height.toFloat()

            if (bitmapWidth <= 0f || bitmapHeight <= 0f) return@withContext emptyList()

            val textBlocks = mutableListOf<PanelCoordinate>()
            val blocksList: List<Text.TextBlock> = visionText.textBlocks

            for (block in blocksList) {
                val box = block.boundingBox ?: continue
                val left = (box.left.toFloat() / bitmapWidth).coerceIn(0f, 1f)
                val top = (box.top.toFloat() / bitmapHeight).coerceIn(0f, 1f)
                val right = (box.right.toFloat() / bitmapWidth).coerceIn(0f, 1f)
                val bottom = (box.bottom.toFloat() / bitmapHeight).coerceIn(0f, 1f)

                val widthFrac = right - left
                val heightFrac = bottom - top

                // Ignore tiny noise or full-page text boxes
                if (widthFrac > 0.015f && heightFrac > 0.01f && (widthFrac * heightFrac) < 0.85f) {
                    textBlocks.add(
                        PanelCoordinate(
                            rect = RectF(left, top, right, bottom),
                            confidence = 0.95f,
                            isText = true
                        )
                    )
                }
            }

            textBlocks
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
