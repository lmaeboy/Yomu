package com.example.yomu.domain.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.example.yomu.domain.model.PanelCoordinate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import java.util.Collections

class AIPanelDetector(context: Context) {
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        val modelBytes = context.assets.open("model.onnx").readBytes()
        session = env.createSession(modelBytes, OrtSession.SessionOptions())
    }

    suspend fun detectPanels(originalBitmap: Bitmap, confidenceThreshold: Float = 0.25f): List<PanelCoordinate> = withContext(Dispatchers.Default) {
        val inputSize = 320
        
        val scale = minOf(inputSize.toFloat() / originalBitmap.width, inputSize.toFloat() / originalBitmap.height)
        val newWidth = (originalBitmap.width * scale).toInt()
        val newHeight = (originalBitmap.height * scale).toInt()
        
        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        
        // Create letterbox padded bitmap
        val paddedBitmap = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(paddedBitmap)
        // Fill background with YOLO standard gray padding
        canvas.drawColor(Color.rgb(114, 114, 114))
        
        // Center the image
        val xOffset = (inputSize - newWidth) / 2f
        val yOffset = (inputSize - newHeight) / 2f
        canvas.drawBitmap(resizedBitmap, xOffset, yOffset, null)

        val floatBuffer = FloatBuffer.allocate(1 * 3 * inputSize * inputSize)
        val pixels = IntArray(inputSize * inputSize)
        paddedBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (i in 0 until inputSize * inputSize) {
            val pixel = pixels[i]
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            floatBuffer.put(i, r)
            floatBuffer.put(inputSize * inputSize + i, g)
            floatBuffer.put(2 * inputSize * inputSize + i, b)
        }

        floatBuffer.rewind()
        
        val inputName = session.inputNames.iterator().next()
        val shape = longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong())
        val tensor = OnnxTensor.createTensor(env, floatBuffer, shape)
        
        val output = session.run(Collections.singletonMap(inputName, tensor))
        val outputTensor = output[0] as OnnxTensor
        
        val floatBufferOut = outputTensor.floatBuffer
        val floatArray = FloatArray(floatBufferOut.remaining())
        floatBufferOut.get(floatArray)
        
        val numBoxes = 2100
        val rawPanels = mutableListOf<PanelCoordinate>()
        
        for (i in 0 until numBoxes) {
            val conf = floatArray[4 * numBoxes + i]
            
            if (conf >= confidenceThreshold) {
                val cx = floatArray[0 * numBoxes + i]
                val cy = floatArray[1 * numBoxes + i]
                val w = floatArray[2 * numBoxes + i]
                val h = floatArray[3 * numBoxes + i]
                
                // Remove padding and scaling to get original coords
                val cx_orig = (cx - xOffset) / scale
                val cy_orig = (cy - yOffset) / scale
                val w_orig = w / scale
                val h_orig = h / scale
                
                val left = (cx_orig - w_orig / 2) / originalBitmap.width
                val top = (cy_orig - h_orig / 2) / originalBitmap.height
                val right = (cx_orig + w_orig / 2) / originalBitmap.width
                val bottom = (cy_orig + h_orig / 2) / originalBitmap.height
                
                if (left > 1.0f || top > 1.0f || right < 0.0f || bottom < 0.0f) {
                    continue
                }
                
                val cLeft = left.coerceIn(0f, 1f)
                val cTop = top.coerceIn(0f, 1f)
                val cRight = right.coerceIn(0f, 1f)
                val cBottom = bottom.coerceIn(0f, 1f)
                
                rawPanels.add(
                    PanelCoordinate(
                        rect = RectF(cLeft, cTop, cRight, cBottom),
                        confidence = conf
                    )
                )
            }
        }
        
        tensor.close()
        output.close()
        
        val nmsPanels = applyNMS(rawPanels, 0.45f)
        XYCutAlgorithm.sortPanels(nmsPanels)
    }
    
    private fun applyNMS(boxes: List<PanelCoordinate>, iouThreshold: Float): List<PanelCoordinate> {
        val sorted = boxes.sortedByDescending { it.confidence }.toMutableList()
        val selected = mutableListOf<PanelCoordinate>()
        
        while (sorted.isNotEmpty()) {
            val bestBox = sorted.first()
            selected.add(bestBox)
            sorted.removeAt(0)
            
            val it = sorted.iterator()
            while (it.hasNext()) {
                val box = it.next()
                val iou = calculateIoU(bestBox.rect, box.rect)
                val containment = calculateContainment(bestBox.rect, box.rect)
                if (iou > iouThreshold || containment > 0.50f) {
                    it.remove()
                }
            }
        }
        return selected
    }
    
    private fun calculateContainment(rect1: RectF, rect2: RectF): Float {
        val x1 = maxOf(rect1.left, rect2.left)
        val y1 = maxOf(rect1.top, rect2.top)
        val x2 = minOf(rect1.right, rect2.right)
        val y2 = minOf(rect1.bottom, rect2.bottom)
        
        val intersectionArea = maxOf(0f, x2 - x1) * maxOf(0f, y2 - y1)
        val area1 = (rect1.right - rect1.left) * (rect1.bottom - rect1.top)
        val area2 = (rect2.right - rect2.left) * (rect2.bottom - rect2.top)
        val minArea = minOf(area1, area2)
        return if (minArea > 0) intersectionArea / minArea else 0f
    }

    private fun calculateIoU(rect1: RectF, rect2: RectF): Float {
        val x1 = maxOf(rect1.left, rect2.left)
        val y1 = maxOf(rect1.top, rect2.top)
        val x2 = minOf(rect1.right, rect2.right)
        val y2 = minOf(rect1.bottom, rect2.bottom)
        
        val intersectionArea = maxOf(0f, x2 - x1) * maxOf(0f, y2 - y1)
        val area1 = (rect1.right - rect1.left) * (rect1.bottom - rect1.top)
        val area2 = (rect2.right - rect2.left) * (rect2.bottom - rect2.top)
        
        val unionArea = area1 + area2 - intersectionArea
        return if (unionArea > 0) intersectionArea / unionArea else 0f
    }
}
