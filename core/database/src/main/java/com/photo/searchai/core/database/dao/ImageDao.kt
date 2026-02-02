package com.photo.searchai.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import com.photo.searchai.core.database.entity.ImageEntity
import com.photo.searchai.core.database.entity.ProcessingStatus

@Dao
interface ImageDao {
    @Query("SELECT COUNT(*) FROM images WHERE ocrStatus = :status")
    suspend fun getCountByOcrStatus(status: ProcessingStatus): Int

    @Query("SELECT * FROM images WHERE ocrStatus = :status LIMIT :limit")
    suspend fun getPendingOcrImages(
            status: ProcessingStatus = ProcessingStatus.PENDING,
            limit: Int
    ): List<ImageEntity>

    @Update suspend fun updateImage(image: ImageEntity)
}
