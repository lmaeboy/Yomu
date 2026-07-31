package com.example.yomu.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
@JvmSuppressWildcards
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE mangaUrl = :mangaUrl ORDER BY chapterNumber DESC")
    fun getChapters(mangaUrl: String): Flow<List<ChapterEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChapters(chapters: List<ChapterEntity>): List<Long>

    @Update
    suspend fun updateChapter(chapter: ChapterEntity): Int

    @Query("SELECT * FROM chapters WHERE url = :chapterUrl")
    suspend fun getChapter(chapterUrl: String): ChapterEntity?
    
    @Query("SELECT * FROM chapters WHERE mangaUrl = :mangaUrl AND isRead = 1 ORDER BY chapterNumber DESC LIMIT 1")
    suspend fun getLastReadChapter(mangaUrl: String): ChapterEntity?

    @Query("UPDATE chapters SET isRead = 0, lastReadPage = 0 WHERE mangaUrl = :mangaUrl")
    suspend fun clearReadingProgress(mangaUrl: String): Int
}
