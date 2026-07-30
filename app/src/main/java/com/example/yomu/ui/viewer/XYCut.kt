package com.example.yomu.ui.viewer

import androidx.compose.ui.geometry.Rect

/**
 * A mock representation of the YOLOv8 model output for panel detection.
 * In the future, this will be replaced by the actual PyTorch Lite inference.
 */
object YoloMock {
    fun detectPanels(imageWidth: Float, imageHeight: Float): List<Rect> {
        // Mocking some panels:
        // Top right panel, top left panel
        // Bottom right, bottom left
        // Reading order: Top Right -> Top Left -> Bottom Right -> Bottom Left (Manga right-to-left)
        return listOf(
            Rect(left = imageWidth * 0.55f, top = 0f, right = imageWidth, bottom = imageHeight * 0.45f), // Top Right
            Rect(left = 0f, top = 0f, right = imageWidth * 0.45f, bottom = imageHeight * 0.45f), // Top Left
            Rect(left = imageWidth * 0.55f, top = imageHeight * 0.55f, right = imageWidth, bottom = imageHeight), // Bottom Right
            Rect(left = 0f, top = imageHeight * 0.55f, right = imageWidth * 0.45f, bottom = imageHeight) // Bottom Left
        )
    }
}

object XYCut {
    /**
     * Sorts bounding boxes into correct reading order (Right-to-Left, Top-to-Bottom).
     * This is a simplified version of the recursive XY-Cut algorithm.
     */
    fun sortPanels(panels: List<Rect>): List<Rect> {
        if (panels.isEmpty()) return emptyList()
        
        // Simplified sorting: primary sort by Y (Top-to-Bottom), secondary by X (Right-to-Left)
        // A true recursive XY-Cut would project profiles and split by largest gaps.
        return panels.sortedWith(Comparator { a, b ->
            // If they overlap significantly on the Y axis, they are on the same "row"
            val yOverlap = maxOf(0f, minOf(a.bottom, b.bottom) - maxOf(a.top, b.top))
            val aHeight = a.height
            val bHeight = b.height
            
            if (yOverlap > (minOf(aHeight, bHeight) * 0.5f)) {
                // Same row, sort right to left
                b.right.compareTo(a.right)
            } else {
                // Different row, sort top to bottom
                a.top.compareTo(b.top)
            }
        })
    }
}
