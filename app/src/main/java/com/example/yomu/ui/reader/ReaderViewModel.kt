package com.example.yomu.ui.reader

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil3.asDrawable
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.SingletonImageLoader
import com.example.yomu.domain.ai.AIPanelDetector
import com.example.yomu.domain.ai.AITextDetector
import com.example.yomu.domain.model.PanelCoordinate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    
    private val prefs = application.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
    private val panelDetector = AIPanelDetector(application)
    private val textDetector = AITextDetector()
    
    private val _panels = MutableStateFlow<List<PanelCoordinate>>(emptyList())
    val panels: StateFlow<List<PanelCoordinate>> = _panels

    private val _debugBitmap = MutableStateFlow<Bitmap?>(null)
    val debugBitmap: StateFlow<Bitmap?> = _debugBitmap
    
    private val _currentBitmap = MutableStateFlow<Bitmap?>(null)
    val currentBitmap: StateFlow<Bitmap?> = _currentBitmap
    
    private val _currentPanelIndex = MutableStateFlow(-1)
    val currentPanelIndex: StateFlow<Int> = _currentPanelIndex
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _imageWidth = MutableStateFlow(1f)
    val imageWidth: StateFlow<Float> = _imageWidth
    
    private val _imageHeight = MutableStateFlow(1f)
    val imageHeight: StateFlow<Float> = _imageHeight

    // Settings
    private val _confidenceThreshold = MutableStateFlow(0.25f)
    val confidenceThreshold: StateFlow<Float> = _confidenceThreshold

    private val _minAreaPercentage = MutableStateFlow(0.025f)
    val minAreaPercentage: StateFlow<Float> = _minAreaPercentage

    private val _paddingPercentage = MutableStateFlow(0.10f)
    val paddingPercentage: StateFlow<Float> = _paddingPercentage

    private val _isDebugMode = MutableStateFlow(false)
    val isDebugMode: StateFlow<Boolean> = _isDebugMode

    private val _isTextDetectionEnabled = MutableStateFlow(true)
    val isTextDetectionEnabled: StateFlow<Boolean> = _isTextDetectionEnabled

    private var currentMangaUrl: String = ""
    private var currentSoftwareBitmap: Bitmap? = null

    fun initialize(mangaUrl: String) {
        if (currentMangaUrl != mangaUrl) {
            currentMangaUrl = mangaUrl
            val safeUrl = mangaUrl.replace("/", "_")
            _confidenceThreshold.value = prefs.getFloat("confidence_$safeUrl", 0.25f)
            _minAreaPercentage.value = prefs.getFloat("minArea_$safeUrl", 0.025f)
            _paddingPercentage.value = prefs.getFloat("padding_$safeUrl", 0.10f)
            _isDebugMode.value = prefs.getBoolean("debugMode_$safeUrl", false)
            _isTextDetectionEnabled.value = prefs.getBoolean("enableTextAI_$safeUrl", true)
        }
    }

    fun loadPageAndDetect(context: Context, imageUrl: String, startAtLastPanel: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            _currentPanelIndex.value = -1
            _debugBitmap.value = null
            _currentBitmap.value = null
            
            try {
                val headers = NetworkHeaders.Builder()
                    .set("User-Agent", "Mozilla/5.0")
                    .build()

                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .httpHeaders(headers)
                    .build()
                
                val result = SingletonImageLoader.get(context).execute(request)
                if (result is SuccessResult) {
                    val hardwareBitmap = result.image.asDrawable(context.resources).toBitmap()
                    val softwareBitmap = hardwareBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    currentSoftwareBitmap = softwareBitmap
                    _currentBitmap.value = softwareBitmap
                    _imageWidth.value = softwareBitmap.width.toFloat()
                    _imageHeight.value = softwareBitmap.height.toFloat()
                    
                    processBitmapWithCurrentSettings(softwareBitmap, startAtLastPanel)
                } else {
                    _panels.value = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _panels.value = emptyList()
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    fun applySettings(confidence: Float, minArea: Float, padding: Float, debugMode: Boolean, enableTextAI: Boolean = _isTextDetectionEnabled.value) {
        val safeUrl = currentMangaUrl.replace("/", "_")
        prefs.edit().apply {
            putFloat("confidence_$safeUrl", confidence)
            putFloat("minArea_$safeUrl", minArea)
            putFloat("padding_$safeUrl", padding)
            putBoolean("debugMode_$safeUrl", debugMode)
            putBoolean("enableTextAI_$safeUrl", enableTextAI)
            apply()
        }
        
        _confidenceThreshold.value = confidence
        _minAreaPercentage.value = minArea
        _paddingPercentage.value = padding
        _isDebugMode.value = debugMode
        _isTextDetectionEnabled.value = enableTextAI
        
        currentSoftwareBitmap?.let {
            viewModelScope.launch(Dispatchers.IO) {
                _isProcessing.value = true
                processBitmapWithCurrentSettings(it, false)
                _isProcessing.value = false
            }
        }
    }
    
    fun resetSettingsToDefault() {
        applySettings(0.25f, 0.025f, 0.10f, false, true)
    }

    private suspend fun processBitmapWithCurrentSettings(bitmap: Bitmap, startAtLastPanel: Boolean) {
        val conf = _confidenceThreshold.value
        val minArea = _minAreaPercentage.value
        val padding = _paddingPercentage.value
        val enableTextAI = _isTextDetectionEnabled.value
        
        val (rawPanels, rawText) = coroutineScope {
            val panelsDeferred = async { panelDetector.detectPanels(bitmap, conf) }
            val textDeferred = if (enableTextAI) {
                async { textDetector.detectText(bitmap, conf) }
            } else {
                async { emptyList<PanelCoordinate>() }
            }
            Pair(panelsDeferred.await(), textDeferred.await())
        }
        
        val allDetected = rawPanels + rawText
        val sortedAll = com.example.yomu.domain.ai.XYCutAlgorithm.sortPanels(allDetected)
        
        // Filter panels by minimum area and apply padding to all coordinates
        val filteredPanels = sortedAll.filter { panel ->
            if (panel.isText) return@filter true
            val w = panel.rect.right - panel.rect.left
            val h = panel.rect.bottom - panel.rect.top
            val areaPercentage = w * h
            areaPercentage >= minArea
        }.map { panel ->
            val w = panel.rect.right - panel.rect.left
            val h = panel.rect.bottom - panel.rect.top
            val padX = w * padding
            val padY = h * padding
            
            val newLeft = (panel.rect.left - padX).coerceIn(0f, 1f)
            val newRight = (panel.rect.right + padX).coerceIn(0f, 1f)
            val newTop = (panel.rect.top - padY).coerceIn(0f, 1f)
            val newBottom = (panel.rect.bottom + padY).coerceIn(0f, 1f)
            
            panel.copy(rect = android.graphics.RectF(newLeft, newTop, newRight, newBottom))
        }
        
        _panels.value = filteredPanels
        
        if (_isDebugMode.value) {
            val debugBmp = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = android.graphics.Canvas(debugBmp)
            val paint = android.graphics.Paint().apply {
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 5f
            }
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.YELLOW
                textSize = 24f
            }
            
            filteredPanels.forEachIndexed { index, panel ->
                val left = panel.rect.left * debugBmp.width
                val top = panel.rect.top * debugBmp.height
                val right = panel.rect.right * debugBmp.width
                val bottom = panel.rect.bottom * debugBmp.height
                paint.color = if (panel.isText) android.graphics.Color.BLUE else android.graphics.Color.RED
                canvas.drawRect(left, top, right, bottom, paint)
                val labelText = "$index: ${if (panel.isText) "Text" else "Panel"} ${"%.2f".format(panel.confidence)}"
                canvas.drawText(labelText, left, top - 10, textPaint)
            }
            _debugBitmap.value = debugBmp
        } else {
            _debugBitmap.value = null
        }
        
        if (startAtLastPanel && filteredPanels.isNotEmpty()) {
            _currentPanelIndex.value = filteredPanels.size - 1
        } else {
            _currentPanelIndex.value = -1
        }
    }
    
    fun nextPanel() {
        if (_panels.value.isNotEmpty() && _currentPanelIndex.value < _panels.value.size - 1) {
            _currentPanelIndex.value += 1
        }
    }
    
    fun previousPanel() {
        if (_panels.value.isNotEmpty() && _currentPanelIndex.value > -1) {
            _currentPanelIndex.value -= 1
        }
    }
    
    fun viewFullPage() {
        _currentPanelIndex.value = -1
    }
}
