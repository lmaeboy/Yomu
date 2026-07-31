package com.example.yomu.ui.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch
import com.example.yomu.data.database.ChapterEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    imageUrl: String?,
    viewModel: ReaderViewModel,
    onPreviousPage: (Boolean) -> Unit,
    onNextPage: () -> Unit,
    onBackClick: () -> Unit,
    currentPage: Int = 1,
    totalPages: Int = 1,
    chapters: List<ChapterEntity> = emptyList(),
    currentChapterIndex: Int = -1,
    onSelectChapter: ((Int) -> Unit)? = null
) {
    val panels by viewModel.panels.collectAsState()
    val currentPanelIndex by viewModel.currentPanelIndex.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val debugBitmap by viewModel.debugBitmap.collectAsState()
    
    val confidence by viewModel.confidenceThreshold.collectAsState()
    val minArea by viewModel.minAreaPercentage.collectAsState()
    val padding by viewModel.paddingPercentage.collectAsState()
    val isDebugMode by viewModel.isDebugMode.collectAsState()
    val isTextDetectionEnabled by viewModel.isTextDetectionEnabled.collectAsState()

    var showOverlay by remember { mutableStateOf(false) }
    var showSettingsOverlay by remember { mutableStateOf(false) }
    var showChapterMenu by remember { mutableStateOf(false) }

    // Manual Zoom & Pan
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Reset manual zoom when panel index changes
    LaunchedEffect(currentPanelIndex) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showOverlay = !showOverlay }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    offsetX += pan.x * scale
                    offsetY += pan.y * scale
                }
            }
    ) {
        if (imageUrl != null) {
            val contentScale = ContentScale.Fit
            val imageW by viewModel.imageWidth.collectAsState()
            val imageH by viewModel.imageHeight.collectAsState()

            val animLeft = remember { Animatable(0f) }
            val animTop = remember { Animatable(0f) }
            val animRight = remember { Animatable(1f) }
            val animBottom = remember { Animatable(1f) }
            
            var lastPage by remember { mutableIntStateOf(currentPage) }

            LaunchedEffect(currentPage, currentPanelIndex, panels) {
                val targetRect = if (currentPanelIndex >= 0 && panels.isNotEmpty() && currentPanelIndex < panels.size) {
                    panels[currentPanelIndex].rect
                } else {
                    android.graphics.RectF(0f, 0f, 1f, 1f)
                }
                
                if (lastPage != currentPage) {
                    // Page changed, snap immediately!
                    animLeft.snapTo(targetRect.left)
                    animTop.snapTo(targetRect.top)
                    animRight.snapTo(targetRect.right)
                    animBottom.snapTo(targetRect.bottom)
                    lastPage = currentPage
                } else {
                    // Same page, animate smoothly!
                    launch { animLeft.animateTo(targetRect.left, tween(400, easing = FastOutSlowInEasing)) }
                    launch { animTop.animateTo(targetRect.top, tween(400, easing = FastOutSlowInEasing)) }
                    launch { animRight.animateTo(targetRect.right, tween(400, easing = FastOutSlowInEasing)) }
                    launch { animBottom.animateTo(targetRect.bottom, tween(400, easing = FastOutSlowInEasing)) }
                }
            }

            val transformModifier = Modifier.graphicsLayer {
                val w = size.width
                val h = size.height
                
                val fitScale = minOf(w / imageW, h / imageH)
                val drawW = imageW * fitScale
                val drawH = imageH * fitScale
                
                val panelCenterX = (animLeft.value + animRight.value) / 2f
                val panelCenterY = (animTop.value + animBottom.value) / 2f
                val panelWidthFrac = animRight.value - animLeft.value
                val panelHeightFrac = animBottom.value - animTop.value
                
                val panelW = panelWidthFrac * drawW
                val panelH = panelHeightFrac * drawH
                
                val safePanelW = if (panelW > 0) panelW else 1f
                val safePanelH = if (panelH > 0) panelH else 1f
                
                val zoomScale = minOf(w / safePanelW, h / safePanelH).coerceAtMost(5f) * scale
                
                scaleX = zoomScale
                scaleY = zoomScale
                
                translationX = drawW * (0.5f - panelCenterX) * zoomScale + offsetX
                translationY = drawH * (0.5f - panelCenterY) * zoomScale + offsetY
            }

            if (isDebugMode && debugBitmap != null && currentPanelIndex == -1) {
                Image(
                    bitmap = debugBitmap!!.asImageBitmap(),
                    contentDescription = "Debug Page",
                    modifier = Modifier.fillMaxSize().then(transformModifier),
                    contentScale = contentScale
                )
            } else {
                val currentBitmap by viewModel.currentBitmap.collectAsState()
                if (currentBitmap != null) {
                    Image(
                        bitmap = currentBitmap!!.asImageBitmap(),
                        contentDescription = "Manga Page",
                        modifier = Modifier.fillMaxSize().then(transformModifier),
                        contentScale = contentScale
                    )
                }
            }
        }

        if (isProcessing) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // Invisible tap zones for navigation
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                        if (currentPanelIndex > -1) {
                            viewModel.previousPanel()
                        } else {
                            onPreviousPage(true)
                        }
                    }
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                        showOverlay = !showOverlay
                    }
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                        if (currentPanelIndex < panels.size - 1) {
                            viewModel.nextPanel()
                        } else {
                            onNextPage()
                        }
                    }
            )
        }

        // Tachiyomi-style Overlay
        if (showOverlay) {
            // Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(8.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    IconButton(onClick = { showSettingsOverlay = !showSettingsOverlay }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }
            }

            // Chapter Selection Menu (translucent, see-thru, ~50% screen height)
            if (showChapterMenu && chapters.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f)
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 90.dp, start = 12.dp, end = 12.dp)
                        .background(
                            Color.Black.copy(alpha = 0.85f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp)
                ) {
                    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

                    LaunchedEffect(showChapterMenu, currentChapterIndex) {
                        if (currentChapterIndex in chapters.indices) {
                            listState.scrollToItem(maxOf(0, currentChapterIndex - 2))
                        }
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Chapter (${currentChapterIndex + 1} / ${chapters.size})",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            IconButton(onClick = { showChapterMenu = false }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Collapse Menu", tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(4.dp))

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            itemsIndexed(chapters) { index, chapter ->
                                val isSelected = index == currentChapterIndex
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                            else Color.Transparent,
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            onSelectChapter?.invoke(index)
                                            showChapterMenu = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = chapter.name,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                    )
                                    if (chapter.isRead) {
                                        Text(
                                            text = "Read",
                                            color = Color.Gray,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onPreviousPage(false) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Page", tint = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { showChapterMenu = !showChapterMenu }) {
                            Icon(
                                imageVector = if (showChapterMenu) Icons.Default.KeyboardArrowDown else androidx.compose.material.icons.Icons.Default.KeyboardArrowUp,
                                contentDescription = "Chapter Browser",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = "$currentPage / $totalPages",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    IconButton(onClick = onNextPage) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Page", tint = Color.White)
                    }
                }
            }
        }

        // Settings Overlay
        if (showSettingsOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { showSettingsOverlay = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(16.dp).clickable { /* consume clicks */ },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("AI Settings (Saved locally for this Manga)", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Confidence Threshold: ${"%.2f".format(confidence)}")
                        Slider(
                            value = confidence,
                            onValueChange = { viewModel.applySettings(it, minArea, padding, isDebugMode, isTextDetectionEnabled) },
                            valueRange = 0.05f..0.95f
                        )

                        Text("Min Area Filter: ${"%.3f".format(minArea)}")
                        Slider(
                            value = minArea,
                            onValueChange = { viewModel.applySettings(confidence, it, padding, isDebugMode, isTextDetectionEnabled) },
                            valueRange = 0.005f..0.1f
                        )

                        Text("Zoom Padding: ${"%.2f".format(padding)}")
                        Slider(
                            value = padding,
                            onValueChange = { viewModel.applySettings(confidence, minArea, it, isDebugMode, isTextDetectionEnabled) },
                            valueRange = -0.3f..0.5f
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isTextDetectionEnabled,
                                onCheckedChange = { viewModel.applySettings(confidence, minArea, padding, isDebugMode, it) }
                            )
                            Text("Enable Text AI Detection")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isDebugMode,
                                onCheckedChange = { viewModel.applySettings(confidence, minArea, padding, it, isTextDetectionEnabled) }
                            )
                            Text("Debug Mode (Show Boxes)")
                        }
                        
                        Button(
                            onClick = { viewModel.resetSettingsToDefault() },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text("Reset to Defaults")
                        }
                    }
                }
            }
        }
    }
}
