package com.example.yomu.domain.model

import android.graphics.RectF

data class PanelCoordinate(
    val rect: RectF,
    val confidence: Float,
    val isText: Boolean = false
)
