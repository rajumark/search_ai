package com.photo.searchai.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.photo.searchai.core.database.entity.ImageEntity
import com.photo.searchai.core.database.entity.OcrEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @Query("SELECT * FROM images ORDER BY dateAdded DESC")
    fun getAllImages(): Flow<List<ImageEntity>>

    @Query("SELECT * FROM images ORDER BY dateAdded DESC")
    fun getAllImagesPagingSource(): PagingSource<Int, ImageEntity>

    @Query("SELECT COUNT(*) FROM images") fun getImageCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImages(images: List<ImageEntity>)

    @Query("SELECT id FROM images") suspend fun getAllImageIds(): List<Long>

    @Query("DELETE FROM images WHERE id IN (:ids)") suspend fun deleteImagesByIds(ids: List<Long>)

    @Query("UPDATE images SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query(
            """
        SELECT i.*, o.ocrText FROM images i
        LEFT JOIN ocr_results o ON i.id = o.imageId
        WHERE i.isFavorite = 1
        ORDER BY i.dateAdded DESC
    """
    )
    fun getFavoriteImagesWithOcr(): Flow<List<com.photo.searchai.core.database.entity.SearchResultWithOcr>>

    @Query(
            """
        SELECT * FROM images 
        WHERE id NOT IN (SELECT imageId FROM ocr_results WHERE isOcrProcessed = 1)
    """
    )
    suspend fun getPendingOcrImages(): List<ImageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOcrResult(ocrEntity: OcrEntity)

    @Query("SELECT COUNT(*) FROM ocr_results WHERE isOcrProcessed = 1")
    fun getOcrProcessedCount(): Flow<Int>

    @Query("DELETE FROM images") suspend fun deleteAll()

    @RawQuery(observedEntities = [ImageEntity::class, OcrEntity::class])
    fun searchImagesRaw(
            query: SupportSQLiteQuery
    ): Flow<List<com.photo.searchai.core.database.entity.SearchResultWithOcr>>
}
