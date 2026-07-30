package com.example.yomu.data.source

import com.example.yomu.domain.Chapter
import com.example.yomu.domain.Manga
import com.example.yomu.domain.Page
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder

class WeebcentralSource : Source {
    override val name: String = "Weebcentral"
    private val baseUrl = "https://weebcentral.com"
    
    private val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
        "Accept-Language" to "en-US,en;q=0.5"
    )

    override suspend fun getPopularManga(page: Int): List<Manga> = withContext(Dispatchers.IO) {
        searchManga("", page)
    }

    override suspend fun searchManga(query: String, page: Int): List<Manga> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
            val offset = (page - 1) * 32
            
            val url = if (query.isBlank()) {
                "$baseUrl/search/data?sort=Popularity&order=Descending&limit=32&offset=$offset&display_mode=Full%20Display"
            } else {
                "$baseUrl/search/data?text=$encodedQuery&limit=32&offset=$offset&display_mode=Full%20Display"
            }

            val doc = Jsoup.connect(url).headers(headers).get()
            val mangaList = mutableListOf<Manga>()
            val seriesElements = doc.select("article > section > a")
            
            for (element in seriesElements) {
                var seriesUrl = element.attr("href")
                if (seriesUrl.startsWith("/")) seriesUrl = baseUrl + seriesUrl
                val img = element.selectFirst("img") ?: element.selectFirst("source")
                var coverUrl = img?.attr("src") ?: img?.attr("srcset") ?: ""
                if (coverUrl.startsWith("/")) coverUrl = baseUrl + coverUrl
                val title = element.selectFirst("div:not([class]):last-child")?.text() ?: element.text()
                if (seriesUrl.isNotBlank() && title.isNotBlank()) {
                    mangaList.add(Manga(url = seriesUrl, title = title, coverUrl = coverUrl))
                }
            }
            return@withContext mangaList.distinctBy { it.url }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    override suspend fun getMangaDetails(manga: Manga): Manga = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(manga.url).headers(headers).get()
            var author = "Unknown"
            var description = "No description available."
            var status = "Unknown"
            var parsedTitle = manga.title
            var parsedCover = manga.coverUrl
            
            var tags = ""
            var releaseDate = ""
            var hasAnime = false
            var isAdult = false

            val sections = doc.select("section[x-data] > section")
            if (sections.size > 0) {
                val metadataSection = sections[0]
                val img = metadataSection.selectFirst("img") ?: metadataSection.selectFirst("source")
                parsedCover = img?.attr("src") ?: img?.attr("srcset") ?: parsedCover
                if (parsedCover.startsWith("/")) parsedCover = baseUrl + parsedCover

                metadataSection.select("strong").forEach { strong ->
                    val text = strong.text().lowercase()
                    val parentText = strong.parent()?.text()?.replace(strong.text(), "")?.trim() ?: ""
                    
                    if (text.contains("author")) {
                        author = strong.parent()?.select("a")?.joinToString(", ") { it.text() } ?: parentText
                    }
                    if (text.contains("status")) {
                        status = strong.parent()?.select("a")?.text() ?: parentText
                    }
                    if (text.contains("released")) {
                        releaseDate = parentText
                    }
                    if (text.contains("anime adaptation")) {
                        hasAnime = parentText.equals("yes", ignoreCase = true)
                    }
                    if (text.contains("adult content")) {
                        isAdult = parentText.equals("yes", ignoreCase = true)
                    }
                }
            }
            if (sections.size > 1) {
                val detailsSection = sections[1]
                parsedTitle = detailsSection.selectFirst("h1")?.text() ?: parsedTitle
                val descP = detailsSection.selectFirst("li:has(strong:contains(Description)) > p")
                if (descP != null) {
                    description = descP.text()
                } else {
                    val pTags = detailsSection.select("p")
                    description = pTags.maxByOrNull { it.text().length }?.text() ?: description
                }
                
                // Parse Tags
                val tagElements = detailsSection.select("a[href*=\"/search/data\"]")
                tags = tagElements.joinToString(", ") { it.text() }
            }

            return@withContext manga.copy(
                title = if (parsedTitle == "Loading...") "Unknown Title" else parsedTitle,
                coverUrl = parsedCover,
                author = author,
                description = description,
                status = status,
                tags = tags,
                releaseDate = releaseDate,
                hasAnime = hasAnime,
                isAdult = isAdult
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext manga
        }
    }

    override suspend fun getChapterList(manga: Manga): List<Chapter> = withContext(Dispatchers.IO) {
        try {
            val pathSegments = manga.url.removePrefix("$baseUrl/").split("/")
            if (pathSegments.size < 2) return@withContext emptyList()
            val chapterListUrl = "$baseUrl/series/${pathSegments[1]}/full-chapter-list"
            val chapterDoc = Jsoup.connect(chapterListUrl).headers(headers).get()
            val chapterElements = chapterDoc.select("div[x-data]")
            val chapters = mutableListOf<Chapter>()
            for (element in chapterElements) {
                val a = element.selectFirst("a")
                val time = element.selectFirst("time")
                if (a != null) {
                    var url = a.attr("href")
                    if (url.startsWith("/")) url = baseUrl + url
                    val name = a.selectFirst("span.flex > span")?.text() ?: a.text()
                    val numMatch = Regex("""\b(\d+(\.\d+)?)\b""").find(name)
                    val chapterNum = numMatch?.groupValues?.get(1)?.toFloatOrNull() ?: -1f
                    var dateUpload = ""
                    if (time != null) {
                        dateUpload = time.attr("datetime")
                    }
                    if (url.isNotBlank()) {
                        chapters.add(Chapter(url = url, name = name, dateUpload = dateUpload, chapterNumber = chapterNum, mangaUrl = manga.url))
                    }
                }
            }
            return@withContext chapters.distinctBy { it.url }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    override suspend fun getPageList(chapter: Chapter): List<Page> = withContext(Dispatchers.IO) {
        try {
            val pageUrl = "${chapter.url}/images?is_prev=False&reading_style=long_strip"
            val doc = Jsoup.connect(pageUrl).headers(headers).get()
            val imgElements = doc.select("section[x-data~=scroll] > img").ifEmpty {
                doc.select("img[src*=/images/]") 
            }
            val pages = mutableListOf<Page>()
            var index = 0
            for (element in imgElements) {
                val src = element.attr("src").takeIf { it.isNotBlank() } ?: element.attr("data-src")
                if (!src.isNullOrBlank()) {
                    pages.add(Page(index = index++, url = chapter.url, imageUrl = src))
                }
            }
            return@withContext pages
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
}
