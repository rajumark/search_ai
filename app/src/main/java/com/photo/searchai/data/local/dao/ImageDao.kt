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
     * Get IDs of unparsed images, limited to batch size.
     */
    @Query("SELECT mediaStoreId FROM images WHERE parsed = 0 LIMIT :limit")
    suspend fun getUnparsedImageIds(limit: Int): List<Long>
    
    /**
     * Get image by mediaStoreId.
     */
    @Query("SELECT * FROM images WHERE mediaStoreId = :id")
    suspend fun getImageById(id: Long): ImageEntity?
    
    /**
     * Mark an image as parsed.
     */
    @Query("UPDATE images SET parsed = 1 WHERE mediaStoreId = :id")
    suspend fun markAsParsed(id: Long): Int
    
    /**
     * Get count of parsed images as Flow for live updates.
     */
    @Query("SELECT COUNT(*) FROM images WHERE parsed = 1")
    fun getParsedCountFlow(): Flow<Int>
    
    /**
     * Get total image count as Flow for live updates.
     */
    @Query("SELECT COUNT(*) FROM images")
    fun getTotalCountFlow(): Flow<Int>
    
    /**
     * Get count of unparsed images as Flow.
     */
    @Query("SELECT COUNT(*) FROM images WHERE parsed = 0")
    fun getPendingCountFlow(): Flow<Int>
    
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
}

