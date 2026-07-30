package com.example.yomu.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey val url: String,
    val mangaUrl: String,
    val name: String,
    val chapterNumber: Float,
    val dateUpload: String,
    val isRead: Boolean = false,
    val totalPages: Int = 0,
    val lastReadPage: Int = 0,
    val lastReadPanel: Int = 0
)
