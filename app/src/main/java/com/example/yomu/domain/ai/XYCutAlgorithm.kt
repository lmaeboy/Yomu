package com.example.yomu.domain.ai

import android.graphics.RectF
import com.example.yomu.domain.model.PanelCoordinate

object XYCutAlgorithm {

    fun sortPanels(panels: List<PanelCoordinate>): List<PanelCoordinate> {
        if (panels.isEmpty()) return emptyList()
        return xyCut(panels)
    }

    private fun xyCut(panels: List<PanelCoordinate>): List<PanelCoordinate> {
        if (panels.size <= 1) return panels

        // Find the best horizontal cut (splits into top and bottom)
        val hCut = findBestCut(panels, isHorizontal = true)
        
        // Find the best vertical cut (splits into left and right)
        val vCut = findBestCut(panels, isHorizontal = false)

        // Prioritize horizontal cuts if a clean gap exists
        val isHorizontalFirst = if (hCut.gap > 0 && vCut.gap <= 0) {
            true
        } else if (vCut.gap > 0 && hCut.gap <= 0) {
            false
        } else if (hCut.gap > 0 && vCut.gap > 0) {
            true // Prioritize horizontal if both have clean gaps
        } else {
            // Neither has a clean gap, pick the one with the *least negative* overlap (i.e. highest value since gap is negative)
            hCut.gap >= vCut.gap
        }

        val bestCut = if (isHorizontalFirst) hCut else vCut

        if (bestCut.index == -1 || bestCut.index == 0 || bestCut.index == panels.size) {
            // Fallback sort if we can't split properly
            return panels.sortedWith(Comparator { p1, p2 ->
                val r1 = p1.rect
                val r2 = p2.rect
                if (Math.abs(r1.top - r2.top) < (r1.bottom - r1.top) * 0.3f) {
                    r2.right.compareTo(r1.right)
                } else {
                    r1.top.compareTo(r2.top)
                }
            })
        }

        // Split panels based on best cut
        val firstHalf = bestCut.sortedPanels.subList(0, bestCut.index)
        val secondHalf = bestCut.sortedPanels.subList(bestCut.index, bestCut.sortedPanels.size)

        val sortedFirst = xyCut(firstHalf)
        val sortedSecond = xyCut(secondHalf)

        return sortedFirst + sortedSecond
    }

    private data class CutResult(val index: Int, val gap: Float, val sortedPanels: List<PanelCoordinate>)

    private fun findBestCut(panels: List<PanelCoordinate>, isHorizontal: Boolean): CutResult {
        // For horizontal cuts, sort by top coordinate ascending.
        // For vertical cuts, sort by right coordinate descending (since we read Right-to-Left).
        val sorted = if (isHorizontal) {
            panels.sortedBy { it.rect.top }
        } else {
            panels.sortedByDescending { it.rect.right }
        }

        var bestIndex = -1
        var maxGap = -Float.MAX_VALUE

        for (i in 1 until sorted.size) {
            val gap = if (isHorizontal) {
                val previousMaxBottom = sorted.subList(0, i).maxOf { it.rect.bottom }
                val currentMinTop = sorted.subList(i, sorted.size).minOf { it.rect.top }
                currentMinTop - previousMaxBottom
            } else {
                // Since we read RTL, first part is Right, second part is Left.
                val previousMinLeft = sorted.subList(0, i).minOf { it.rect.left }
                val currentMaxRight = sorted.subList(i, sorted.size).maxOf { it.rect.right }
                previousMinLeft - currentMaxRight
            }

            if (gap > maxGap) {
                maxGap = gap
                bestIndex = i
            }
        }

        return CutResult(bestIndex, maxGap, sorted)
    }
}
