package com.example.yomu.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yomu.data.database.ChapterEntity
import com.example.yomu.data.database.YomuDatabase
import com.example.yomu.data.source.WeebcentralSource
import com.example.yomu.domain.Chapter
import com.example.yomu.ui.reader.ReaderScreen
import com.example.yomu.ui.reader.ReaderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ViewerScreen(
    mangaUrl: String,
    chapterUrl: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val database = remember { YomuDatabase.getDatabase(context) }
    val source = remember { WeebcentralSource() }
    val readerViewModel: ReaderViewModel = viewModel()

    var chapters by remember { mutableStateOf<List<ChapterEntity>>(emptyList()) }
    var currentChapterIndex by remember { mutableIntStateOf(-1) }
    var pages by remember { mutableStateOf<List<com.example.yomu.domain.Page>>(emptyList()) }
    var currentChapterPageIndex by remember { mutableIntStateOf(0) }
    
    var showTransition by remember { mutableStateOf(false) }
    var transitionMessage by remember { mutableStateOf("") }
    
    var isLoadingPages by remember { mutableStateOf(true) }
    var startAtLastPanel by remember { mutableStateOf(false) }

    LaunchedEffect(mangaUrl) {
        readerViewModel.initialize(mangaUrl)
        
        // Fetch chapters from DB. Note: we order by chapterNumber DESC in DB
        database.chapterDao().getChapters(mangaUrl).collect { dbChapters ->
            // Re-sort ASC for reading
            chapters = dbChapters.sortedBy { it.chapterNumber }
            if (currentChapterIndex == -1 && chapters.isNotEmpty()) {
                val idx = chapters.indexOfFirst { it.url == chapterUrl }
                if (idx != -1) {
                    currentChapterIndex = idx
                }
            }
        }
    }

    LaunchedEffect(currentChapterIndex) {
        if (currentChapterIndex in chapters.indices) {
            val chapter = chapters[currentChapterIndex]
            isLoadingPages = true
            showTransition = false
            val fetchedPages = withContext(Dispatchers.IO) {
                source.getPageList(Chapter(
                    url = chapter.url,
                    name = chapter.name,
                    mangaUrl = chapter.mangaUrl
                ))
            }
            pages = fetchedPages
            
            // Resume from lastReadPage if we are starting fresh (not navigating from previous/next)
            if (!startAtLastPanel && currentChapterPageIndex == 0 && chapter.lastReadPage in fetchedPages.indices) {
                currentChapterPageIndex = chapter.lastReadPage
            } else if (startAtLastPanel && fetchedPages.isNotEmpty()) {
                currentChapterPageIndex = fetchedPages.size - 1
            } else {
                currentChapterPageIndex = 0
            }
            
            isLoadingPages = false
            
            // Update totalPages in DB
            withContext(Dispatchers.IO) {
                database.chapterDao().updateChapter(chapter.copy(totalPages = fetchedPages.size))
            }
        }
    }

    LaunchedEffect(pages, currentChapterPageIndex) {
        if (pages.isNotEmpty() && currentChapterPageIndex in pages.indices) {
            val page = pages[currentChapterPageIndex]
            if (page.imageUrl != null) {
                readerViewModel.loadPageAndDetect(context, page.imageUrl!!, startAtLastPanel)
                startAtLastPanel = false // Reset
            }
            
            // Update progress in DB
            if (currentChapterIndex in chapters.indices) {
                val chapter = chapters[currentChapterIndex]
                coroutineScope.launch(Dispatchers.IO) {
                    val isRead = (currentChapterPageIndex == pages.size - 1)
                    database.chapterDao().updateChapter(
                        chapter.copy(
                            lastReadPage = currentChapterPageIndex,
                            isRead = chapter.isRead || isRead // Don't un-read if already read
                        )
                    )
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoadingPages) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (showTransition) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { 
                        // Proceed to next chapter immediately
                        if (currentChapterIndex < chapters.size - 1) {
                            currentChapterIndex += 1
                        } else {
                            onBackClick() // End of manga
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = transitionMessage, color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Tap to continue", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            val currentImageUrl = if (pages.isNotEmpty() && currentChapterPageIndex in pages.indices) {
                pages[currentChapterPageIndex].imageUrl
            } else {
                null
            }
            
            ReaderScreen(
                imageUrl = currentImageUrl,
                viewModel = readerViewModel,
                currentPage = currentChapterPageIndex + 1,
                totalPages = pages.size,
                onBackClick = onBackClick,
                onPreviousPage = { startAtLast ->
                    if (currentChapterPageIndex > 0) {
                        startAtLastPanel = startAtLast
                        currentChapterPageIndex -= 1
                    } else if (currentChapterIndex > 0) {
                        // Go to previous chapter
                        startAtLastPanel = startAtLast
                        currentChapterIndex -= 1
                    }
                },
                onNextPage = {
                    if (currentChapterPageIndex < pages.size - 1) {
                        currentChapterPageIndex += 1
                    } else {
                        // Reached end of chapter
                        if (currentChapterIndex < chapters.size - 1) {
                            val nextChapter = chapters[currentChapterIndex + 1]
                            transitionMessage = "Next: ${nextChapter.name}"
                            showTransition = true
                        } else {
                            transitionMessage = "End of Manga"
                            showTransition = true
                        }
                    }
                }
            )
        }
    }
}
