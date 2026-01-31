package com.photo.searchai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photo.searchai.data.local.entity.ImageEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for image-related database operations.
 */
@Dao
interface ImageDao {
    
    /**
     * Insert images, ignoring conflicts (already existing images).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImages(images: List<ImageEntity>): List<Long>
    
    /**
     * Get IDs of unparsed images (OCR not done), limited to batch size.
     */
    @Query("SELECT mediaStoreId FROM images WHERE parsed = 0 LIMIT :limit")
    suspend fun getUnparsedImageIds(limit: Int): List<Long>
    
    /**
     * Get IDs of images where barcode scanning is not done, limited to batch size.
     */
    @Query("SELECT mediaStoreId FROM images WHERE barcodeParsed = 0 LIMIT :limit")
    suspend fun getUnparsedBarcodeImageIds(limit: Int): List<Long>
    
    /**
     * Get IDs of images where labeling is not done, limited to batch size.
     */
    @Query("SELECT mediaStoreId FROM images WHERE labelParsed = 0 LIMIT :limit")
    suspend fun getUnparsedLabelImageIds(limit: Int): List<Long>
    
    /**
     * Get image by mediaStoreId.
     */
    @Query("SELECT * FROM images WHERE mediaStoreId = :id")
    suspend fun getImageById(id: Long): ImageEntity?
    
    /**
     * Mark an image as OCR parsed.
     */
    @Query("UPDATE images SET parsed = 1 WHERE mediaStoreId = :id")
    suspend fun markAsParsed(id: Long): Int
    
    /**
     * Mark an image as barcode parsed.
     */
    @Query("UPDATE images SET barcodeParsed = 1 WHERE mediaStoreId = :id")
    suspend fun markAsBarcodeParsed(id: Long): Int
    
    /**
     * Mark an image as label parsed.
     */
    @Query("UPDATE images SET labelParsed = 1 WHERE mediaStoreId = :id")
    suspend fun markAsLabelParsed(id: Long): Int
    
    /**
     * Get count of OCR parsed images as Flow for live updates.
     */
    @Query("SELECT COUNT(*) FROM images WHERE parsed = 1")
    fun getParsedCountFlow(): Flow<Int>
    
    /**
     * Get count of barcode parsed images as Flow.
     */
    @Query("SELECT COUNT(*) FROM images WHERE barcodeParsed = 1")
    fun getBarcodeParsedCountFlow(): Flow<Int>
    
    /**
     * Get count of label parsed images as Flow.
     */
    @Query("SELECT COUNT(*) FROM images WHERE labelParsed = 1")
    fun getLabelParsedCountFlow(): Flow<Int>
    
    /**
     * Get total image count as Flow for live updates.
     */
    @Query("SELECT COUNT(*) FROM images")
    fun getTotalCountFlow(): Flow<Int>
    
    /**
     * Get count of OCR unparsed images as Flow.
     */
    @Query("SELECT COUNT(*) FROM images WHERE parsed = 0")
    fun getPendingCountFlow(): Flow<Int>
    
    /**
     * Get count of barcode unparsed images as Flow.
     */
    @Query("SELECT COUNT(*) FROM images WHERE barcodeParsed = 0")
    fun getBarcodePendingCountFlow(): Flow<Int>
    
    /**
     * Get count of label unparsed images as Flow.
     */
    @Query("SELECT COUNT(*) FROM images WHERE labelParsed = 0")
    fun getLabelPendingCountFlow(): Flow<Int>
    
    /**
     * Get all parsed image IDs for sync check.
     */
    @Query("SELECT mediaStoreId FROM images")
    suspend fun getAllImageIds(): List<Long>
    
    /**
     * Delete images that no longer exist in MediaStore.
     */
    @Query("DELETE FROM images WHERE mediaStoreId NOT IN (:validIds)")
    suspend fun deleteStaleImages(validIds: List<Long>): Int
    
    /**
     * Get images by list of mediaStoreIds.
     */
    @Query("SELECT * FROM images WHERE mediaStoreId IN (:ids)")
    suspend fun getImagesByIds(ids: List<Long>): List<ImageEntity>
    
    /**
     * Delete image by mediaStoreId.
     */
    @Query("DELETE FROM images WHERE mediaStoreId = :id")
    suspend fun deleteImageById(id: Long): Int
    
    /**
     * Delete images by list of mediaStoreIds.
     */
    @Query("DELETE FROM images WHERE mediaStoreId IN (:ids)")
    suspend fun deleteImagesByIds(ids: List<Long>): Int
    
    /**
     * Get count of fully processed images (all stages done).
     */
    @Query("SELECT COUNT(*) FROM images WHERE parsed = 1 AND barcodeParsed = 1 AND labelParsed = 1")
    fun getFullyProcessedCountFlow(): Flow<Int>
}


