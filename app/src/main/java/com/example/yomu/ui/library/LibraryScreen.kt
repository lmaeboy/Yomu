package com.example.yomu.ui.library

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.yomu.data.database.MangaEntity
import com.example.yomu.data.database.YomuDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onMangaClick: (String) -> Unit,
    onTestClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember { YomuDatabase.getDatabase(context) }
    val libraryManga by database.mangaDao().getAllManga().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Library") })
        }
    ) { paddingValues ->
        if (libraryManga.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Your library is empty. Go browse!")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.padding(paddingValues).fillMaxSize()
            ) {
                items(libraryManga) { manga ->
                    LibraryItem(manga, onClick = { onMangaClick(manga.url) })
                }
            }
        }
    }
}

@Composable
fun LibraryItem(manga: MangaEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .aspectRatio(0.7f),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(manga.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            ) {
                Text(
                    text = manga.title,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
            }
        }
    }
}
