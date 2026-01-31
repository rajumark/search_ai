package com.photo.searchai.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photo.searchai.data.local.entity.BarcodeEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for barcode database operations.
 */
@Dao
interface BarcodeDao {
    
    /**
     * Insert barcodes, replacing if exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBarcodes(barcodes: List<BarcodeEntity>): List<Long>
    
    /**
     * Insert single barcode.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBarcode(barcode: BarcodeEntity): Long
    
    /**
     * Get all barcodes for an image.
     */
    @Query("SELECT * FROM barcodes WHERE mediaStoreId = :mediaStoreId")
    suspend fun getBarcodesForImage(mediaStoreId: Long): List<BarcodeEntity>
    
    /**
     * Search barcodes by raw value or display value.
     */
    @Query("SELECT * FROM barcodes WHERE rawValue LIKE '%' || :query || '%' OR displayValue LIKE '%' || :query || '%'")
    suspend fun searchBarcodes(query: String): List<BarcodeEntity>
    
    /**
     * Search barcodes with paging.
     */
    @Query("SELECT * FROM barcodes WHERE rawValue LIKE '%' || :query || '%' OR displayValue LIKE '%' || :query || '%' ORDER BY mediaStoreId DESC")
    fun searchBarcodesPaging(query: String): PagingSource<Int, BarcodeEntity>
    
    /**
     * Get all unique image IDs that have barcodes.
     */
    @Query("SELECT DISTINCT mediaStoreId FROM barcodes")
    suspend fun getImageIdsWithBarcodes(): List<Long>
    
    /**
     * Get all barcodes with paging.
     */
    @Query("SELECT * FROM barcodes ORDER BY mediaStoreId DESC")
    fun getAllBarcodesPaging(): PagingSource<Int, BarcodeEntity>
    
    /**
     * Get count of barcodes.
     */
    @Query("SELECT COUNT(*) FROM barcodes")
    fun getBarcodeCountFlow(): Flow<Int>
    
    /**
     * Delete barcodes for an image.
     */
    @Query("DELETE FROM barcodes WHERE mediaStoreId = :mediaStoreId")
    suspend fun deleteBarcodesForImage(mediaStoreId: Long): Int
    
    /**
     * Get count of images with barcodes.
     */
    @Query("SELECT COUNT(DISTINCT mediaStoreId) FROM barcodes")
    fun getImagesWithBarcodesCountFlow(): Flow<Int>
}
