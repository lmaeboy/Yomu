package com.example.yomu.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.yomu.data.database.ChapterEntity
import com.example.yomu.data.database.MangaEntity
import com.example.yomu.data.database.YomuDatabase
import com.example.yomu.data.source.WeebcentralSource
import com.example.yomu.domain.Chapter
import com.example.yomu.domain.Manga
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import java.time.Instant
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatRelativeDate(dateString: String): String {
    if (dateString.isBlank()) return ""
    try {
        val instant = Instant.parse(dateString)
        val now = Instant.now()
        val duration = Duration.between(instant, now)
        
        val hours = duration.toHours()
        val days = duration.toDays()
        
        return when {
            hours < 24 -> "${maxOf(1, hours)} hours ago"
            days < 7 -> "$days days ago"
            days < 28 -> "${days / 7} weeks ago"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("yyyy MM dd")
                    .withZone(ZoneId.systemDefault())
                formatter.format(instant)
            }
        }
    } catch (e: Exception) {
        return dateString
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    mangaUrl: String,
    onBackClick: () -> Unit,
    onChapterClick: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val database = remember { YomuDatabase.getDatabase(context) }
    val source = remember { WeebcentralSource() }

    var manga by remember { mutableStateOf<Manga?>(null) }
    var chapters by remember { mutableStateOf<List<ChapterEntity>>(emptyList()) }
    var isSaved by remember { mutableStateOf(false) }
    var lastReadChapter by remember { mutableStateOf<ChapterEntity?>(null) }
    
    var showFullCover by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(mangaUrl) {
        val localManga = withContext(Dispatchers.IO) {
            database.mangaDao().getMangaByUrl(mangaUrl)
        }
        if (localManga != null) {
            isSaved = true
            manga = Manga(
                url = localManga.url,
                title = localManga.title,
                coverUrl = localManga.coverUrl,
                author = localManga.author,
                description = localManga.description,
                status = localManga.status,
                isFavorite = localManga.isFavorite,
                tags = localManga.tags,
                releaseDate = localManga.releaseDate,
                hasAnime = localManga.hasAnime,
                isAdult = localManga.isAdult
            )
        } else {
            val tempManga = Manga(url = mangaUrl, title = "Loading...", coverUrl = "")
            manga = source.getMangaDetails(tempManga)
        }

        manga?.let { m ->
            // Fetch chapters from source and save them
            val fetchedChapters = source.getChapterList(m)
            withContext(Dispatchers.IO) {
                // Insert chapters avoiding conflict to keep isRead state
                val chapterEntities = fetchedChapters.map { 
                    ChapterEntity(
                        url = it.url,
                        mangaUrl = it.mangaUrl,
                        name = it.name,
                        chapterNumber = it.chapterNumber,
                        dateUpload = it.dateUpload
                    ) 
                }
                database.chapterDao().insertChapters(chapterEntities)
                
                // Read from DB to get the latest state
                database.chapterDao().getChapters(mangaUrl).collect { dbChapters ->
                    chapters = dbChapters
                    lastReadChapter = database.chapterDao().getLastReadChapter(mangaUrl)
                }
            }
        }
    }

    if (showFullCover && manga != null) {
        Dialog(onDismissRequest = { showFullCover = false }) {
            Box(modifier = Modifier.fillMaxSize().clickable { showFullCover = false }) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(manga!!.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Full Cover",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(manga?.title ?: "Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mark as unread") },
                            onClick = {
                                showMenu = false
                                coroutineScope.launch(Dispatchers.IO) {
                                    database.chapterDao().clearReadingProgress(mangaUrl)
                                }
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (manga == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                item {
                    Row(modifier = Modifier.padding(16.dp)) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(manga!!.coverUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = manga!!.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.width(120.dp).aspectRatio(0.7f).clickable { showFullCover = true }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = manga!!.title, style = MaterialTheme.typography.titleLarge)
                            Text(text = manga!!.author, style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Status: ${manga!!.status}", style = MaterialTheme.typography.bodyMedium)
                            if (manga!!.releaseDate.isNotBlank()) {
                                Text(text = "Released: ${manga!!.releaseDate}", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (manga!!.hasAnime) {
                                Text(text = "Anime Adaptation: Yes", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (manga!!.isAdult) {
                                Text(text = "18+", color = Color.Red, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            if (manga!!.tags.isNotBlank()) {
                                Text(text = "Tags: ${manga!!.tags}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }

                    // Add to Library Button
                    manga?.let { currentManga ->
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val entity = MangaEntity(
                                        url = currentManga.url,
                                        title = currentManga.title,
                                        coverUrl = currentManga.coverUrl,
                                        author = currentManga.author,
                                        description = currentManga.description,
                                        status = currentManga.status,
                                        isFavorite = true,
                                        tags = currentManga.tags,
                                        releaseDate = currentManga.releaseDate,
                                        hasAnime = currentManga.hasAnime,
                                        isAdult = currentManga.isAdult
                                    )
                                    if (isSaved) {
                                        withContext(Dispatchers.IO) {
                                            database.mangaDao().deleteManga(entity)
                                        }
                                        isSaved = false
                                    } else {
                                        withContext(Dispatchers.IO) {
                                            database.mangaDao().insertManga(entity)
                                        }
                                        isSaved = true
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            Text(if (isSaved) "Remove from Library" else "Add to Library")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = manga!!.description,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Start/Continue Button
                    if (chapters.isNotEmpty()) {
                        val nextChapterToRead = remember(chapters, manga) {
                            val isRealAccount = manga?.title?.contains("Real Account", ignoreCase = true) == true
                            if (isRealAccount) {
                                val (accountChs, prequelChs) = chapters.partition { it.name.contains("Account", ignoreCase = true) }
                                val allAsc = accountChs.sortedBy { it.chapterNumber } + prequelChs.sortedBy { it.chapterNumber }

                                val inProgress = allAsc.filter { it.lastReadPage > 0 && !it.isRead }.maxByOrNull { it.lastReadPage }
                                if (inProgress != null) return@remember inProgress

                                val lastCompleted = allAsc.filter { it.isRead }.lastOrNull()
                                if (lastCompleted != null) {
                                    val idx = allAsc.indexOf(lastCompleted)
                                    if (idx != -1 && idx + 1 < allAsc.size) {
                                        return@remember allAsc[idx + 1]
                                    }
                                    return@remember lastCompleted
                                }

                                // Default for Real Account: Account 1
                                accountChs.sortedBy { it.chapterNumber }.firstOrNull() ?: chapters.firstOrNull()
                            } else {
                                val chaptersAsc = chapters.sortedBy { it.chapterNumber }
                                val inProgress = chaptersAsc.filter { it.lastReadPage > 0 && !it.isRead }.maxByOrNull { it.chapterNumber }
                                if (inProgress != null) return@remember inProgress

                                val lastCompleted = chaptersAsc.filter { it.isRead }.maxByOrNull { it.chapterNumber }
                                if (lastCompleted != null) {
                                    val nextAfterCompleted = chaptersAsc.firstOrNull { it.chapterNumber > lastCompleted.chapterNumber && !it.isRead }
                                    if (nextAfterCompleted != null) return@remember nextAfterCompleted
                                    return@remember lastCompleted
                                }

                                chaptersAsc.firstOrNull()
                            }
                        }
                        
                        if (nextChapterToRead != null) {
                            Button(
                                onClick = {
                                    onChapterClick(nextChapterToRead.url)
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                            ) {
                                if (nextChapterToRead.lastReadPage > 0 && !nextChapterToRead.isRead) {
                                    Text("Continue ${nextChapterToRead.name}")
                                } else {
                                    Text("Start ${nextChapterToRead.name}")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    HorizontalDivider()
                }

                val isRealAccount = manga?.title?.contains("Real Account", ignoreCase = true) == true
                if (isRealAccount) {
                    val (accountChs, prequelChs) = chapters.partition { it.name.contains("Account", ignoreCase = true) }
                    val sortedAccounts = accountChs.sortedByDescending { it.chapterNumber }
                    val sortedPrequels = prequelChs.sortedByDescending { it.chapterNumber }

                    items(sortedAccounts) { chapter ->
                        ChapterListItem(chapter = chapter, onChapterClick = onChapterClick)
                    }

                    if (sortedPrequels.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 2.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Prequel Chapters (1 - 10)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        items(sortedPrequels) { chapter ->
                            ChapterListItem(chapter = chapter, onChapterClick = onChapterClick)
                        }
                    }
                } else {
                    items(chapters) { chapter ->
                        ChapterListItem(chapter = chapter, onChapterClick = onChapterClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterListItem(
    chapter: ChapterEntity,
    onChapterClick: (String) -> Unit
) {
    val isPartiallyRead = chapter.lastReadPage > 0 && !chapter.isRead
    val pagesLeft = if (chapter.totalPages > 0) chapter.totalPages - chapter.lastReadPage else 0
    
    ListItem(
        headlineContent = { 
            Text(
                text = chapter.name,
                color = if (chapter.isRead) Color.Gray else Color.Unspecified
            ) 
        },
        supportingContent = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = formatRelativeDate(chapter.dateUpload), style = MaterialTheme.typography.bodySmall)
                if (isPartiallyRead) {
                    Text(text = "$pagesLeft pages left", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        modifier = Modifier.clickable { onChapterClick(chapter.url) }
    )
    HorizontalDivider()
}
