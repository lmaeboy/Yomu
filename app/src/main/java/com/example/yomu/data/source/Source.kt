package com.example.yomu.data.source

import com.example.yomu.domain.Chapter
import com.example.yomu.domain.Manga
import com.example.yomu.domain.Page

interface Source {
    val name: String
    
    suspend fun getPopularManga(page: Int): List<Manga>
    suspend fun searchManga(query: String, page: Int): List<Manga>
    suspend fun getMangaDetails(manga: Manga): Manga
    suspend fun getChapterList(manga: Manga): List<Chapter>
    suspend fun getPageList(chapter: Chapter): List<Page>
}
