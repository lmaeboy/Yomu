import kotlinx.coroutines.runBlocking
import com.example.yomu.data.source.WeebcentralSource
import com.example.yomu.domain.Chapter

fun main() = runBlocking {
    val source = WeebcentralSource()
    val chapter = Chapter(url = "https://weebcentral.com/chapters/01J76YX1W6E24H3QKDE1T1566H", name = "Chapter 1", mangaUrl = "")
    val pages = source.getPageList(chapter)
    println("PAGES FOUND: ${pages.size}")
    if (pages.isNotEmpty()) {
        println("FIRST PAGE URL: ${pages[0].imageUrl}")
    }
}
