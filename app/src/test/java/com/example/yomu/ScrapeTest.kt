package com.example.yomu

import org.jsoup.Jsoup
import org.junit.Test
import java.io.File

class ScrapeTest {
    @Test
    fun test() {
        val file = File("weeb.html")
        val doc = Jsoup.parse(file, "UTF-8", "https://weebcentral.com")
        
        // Try to find the manga items
        val elements = doc.select("a[href^=https://weebcentral.com/series/]")
        println("Found ${elements.size} series links.")
        elements.take(5).forEach { el ->
            println("Link: ${el.attr("href")}")
            println("Title: ${el.text()}")
            
            // Try to find image within the parent
            val img = el.parent()?.parent()?.selectFirst("img") ?: el.selectFirst("img")
            println("Image: ${img?.attr("src")}")
            println("---")
        }
    }
}
