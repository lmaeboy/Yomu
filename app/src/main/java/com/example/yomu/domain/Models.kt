package com.example.yomu.domain

data class Manga(
    val url: String,
    val title: String,
    val coverUrl: String,
    val author: String = "",
    val description: String = "",
    val status: String = "Unknown",
    val isFavorite: Boolean = false,
    val tags: String = "",
    val releaseDate: String = "",
    val hasAnime: Boolean = false,
    val isAdult: Boolean = false
)

data class Chapter(
    val url: String,
    val name: String,
    val dateUpload: String = "",
    val chapterNumber: Float = -1f,
    val mangaUrl: String,
    val isRead: Boolean = false,
    val totalPages: Int = 0,
    val lastReadPage: Int = 0,
    val lastReadPanel: Int = 0
)

data class Page(
    val index: Int,
    val url: String,
    val imageUrl: String? = null
)
