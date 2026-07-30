package com.example.yomu.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaDao {
    @Query("SELECT * FROM manga")
    fun getAllManga(): Flow<List<MangaEntity>>

    @Query("SELECT * FROM manga WHERE url = :url")
    fun getMangaByUrl(url: String): MangaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertManga(manga: MangaEntity): Long

    @Delete
    fun deleteManga(manga: MangaEntity): Int
}
