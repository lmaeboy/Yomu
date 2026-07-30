import kotlinx.coroutines.runBlocking
import com.example.yomu.data.source.WeebcentralSource

fun main() = runBlocking {
    val source = WeebcentralSource()
    val search = source.searchManga("naruto", 1)
    if (search.isNotEmpty()) {
        println("URL: ${search[0].url}")
        println("TITLE: ${search[0].title}")
        println("COVER: ${search[0].coverUrl}")
    }
}
