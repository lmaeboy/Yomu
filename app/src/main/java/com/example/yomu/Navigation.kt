package com.example.yomu

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.yomu.ui.browse.BrowseScreen
import com.example.yomu.ui.detail.DetailScreen
import com.example.yomu.ui.library.LibraryScreen
import com.example.yomu.ui.viewer.ViewerScreen
import coil3.asDrawable

@Composable
fun AnimatedGifIcon(
    iconRes: Int,
    contentDescription: String,
    trigger: Int,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val painter = coil3.compose.rememberAsyncImagePainter(
        model = coil3.request.ImageRequest.Builder(context)
            .data(iconRes)
            .memoryCacheKey("$iconRes-$trigger")
            .build()
    )

    androidx.compose.runtime.LaunchedEffect(painter.state, trigger) {
        val state = painter.state
        if (state is coil3.compose.AsyncImagePainter.State.Success) {
            val drawable = state.result.image.asDrawable(context.resources)
            
            // Safely set repeatCount = 0 if AnimatedImageDrawable (API 28+)
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                val animImageDrawable = drawable as? android.graphics.drawable.AnimatedImageDrawable
                animImageDrawable?.repeatCount = 0
            }

            val animatable = drawable as? android.graphics.drawable.Animatable
            if (animatable != null) {
                if (trigger > 0) {
                    animatable.start()
                } else {
                    animatable.stop()
                }
            }
        }
    }

    androidx.compose.foundation.Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier.size(24.dp),
        colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(
            androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }
        )
    )
}

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Library)
    var selectedTab by remember { mutableStateOf(0) }
    var libraryTrigger by remember { mutableStateOf(1) }
    var browseTrigger by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            if (backStack.last() is Library || backStack.last() is Browse) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            libraryTrigger++
                            backStack.clear()
                            backStack.add(Library)
                        },
                        icon = { AnimatedGifIcon(R.drawable.books, "Library", libraryTrigger) },
                        label = { Text("Library") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            browseTrigger++
                            backStack.clear()
                            backStack.add(Browse)
                        },
                        icon = { AnimatedGifIcon(R.drawable.compass, "Browse", browseTrigger) },
                        label = { Text("Browse") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.padding(innerPadding),
            entryProvider = entryProvider {
                entry<Library> {
                    LibraryScreen(
                        onMangaClick = { url -> backStack.add(Detail(url)) },
                        onTestClick = { backStack.add(Viewer("dummyUrl", "https://weebcentral.com/chapters/01J76YX1W6E24H3QKDE1T1566H")) }
                    )
                }
                entry<Browse> {
                    BrowseScreen(onMangaClick = { url -> backStack.add(Detail(url)) })
                }
                entry<Detail> {
                    DetailScreen(
                        mangaUrl = it.mangaUrl,
                        onBackClick = { backStack.removeLast() },
                        onChapterClick = { chapterUrl -> backStack.add(Viewer(it.mangaUrl, chapterUrl)) }
                    )
                }
                entry<Viewer> {
                    ViewerScreen(
                        mangaUrl = it.mangaUrl,
                        chapterUrl = it.chapterUrl,
                        onBackClick = { backStack.removeLast() }
                    )
                }
            }
        )
    }
}
