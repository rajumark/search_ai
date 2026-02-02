package com.photo.searchai.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.photo.searchai.core.database.entity.ImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @Query("SELECT * FROM images") fun getAllImages(): Flow<List<ImageEntity>>

    @Query("SELECT COUNT(*) FROM images") fun getImageCount(): Flow<Int>

    @Upsert suspend fun upsertImages(images: List<ImageEntity>)

    @Query("SELECT * FROM images WHERE isOcrProcessed = 0")
    suspend fun getPendingOcrImages(): List<ImageEntity>

    @Query("UPDATE images SET ocrText = :ocrText, isOcrProcessed = 1 WHERE id = :id")
    suspend fun updateOcrResult(id: Long, ocrText: String)

    @Query("SELECT COUNT(*) FROM images WHERE isOcrProcessed = 1")
    fun getOcrProcessedCount(): Flow<Int>

    @Query("DELETE FROM images") suspend fun deleteAll()
}
