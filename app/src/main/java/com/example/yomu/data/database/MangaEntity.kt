package com.example.yomu.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manga")
data class MangaEntity(
    @PrimaryKey val url: String,
    val title: String,
    val coverUrl: String,
    val author: String,
    val description: String,
    val status: String,
    val isFavorite: Boolean,
    
    val tags: String = "",
    val releaseDate: String = "",
    val hasAnime: Boolean = false,
    val isAdult: Boolean = false
)
