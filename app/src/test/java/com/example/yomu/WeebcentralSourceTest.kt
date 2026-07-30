package com.example.yomu

import com.example.yomu.data.source.WeebcentralSource
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class WeebcentralSourceTest {
    @Test
    fun testGetPageList() = runBlocking {
        val source = WeebcentralSource()
        val search = source.searchManga("naruto", 1)
        assertTrue("Search should return results", search.isNotEmpty())
        
        val chapters = source.getChapterList(search[0])
        assertTrue("Manga should have chapters", chapters.isNotEmpty())
        
        val pages = source.getPageList(chapters.last())
        println("PAGES FOUND: ${pages.size}")
        if (pages.isNotEmpty()) {
            println("FIRST PAGE URL: ${pages[0].imageUrl}")
        }
        assertTrue("Chapter should have pages", pages.isNotEmpty())
    }
}
