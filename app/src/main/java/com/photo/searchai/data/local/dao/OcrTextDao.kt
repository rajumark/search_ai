package com.photo.searchai.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photo.searchai.data.local.entity.OcrTextEntity

/**
 * DAO for OCR text database operations.
 */
@Dao
interface OcrTextDao {
    
    /**
     * Insert OCR text, replacing if exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOcrText(ocrText: OcrTextEntity): Long
    
    /**
     * Get OCR text by image ID.
     */
    @Query("SELECT * FROM ocr_text WHERE mediaStoreId = :id")
    suspend fun getOcrTextByImageId(id: Long): OcrTextEntity?
    
    /**
     * Search OCR text containing the query string.
     */
    @Query("SELECT * FROM ocr_text WHERE fullText LIKE '%' || :query || '%' OR indexedTokens LIKE '%' || :query || '%'")
    suspend fun searchOcrText(query: String): List<OcrTextEntity>
    
    /**
     * Search OCR text with paging support.
     * Returns a PagingSource for efficient loading of large result sets.
     */
    @Query("SELECT * FROM ocr_text WHERE fullText LIKE '%' || :query || '%' OR indexedTokens LIKE '%' || :query || '%' ORDER BY mediaStoreId DESC")
    fun searchOcrTextPaging(query: String): PagingSource<Int, OcrTextEntity>
    
    /**
     * Get all OCR text with paging (for empty query).
     */
    @Query("SELECT * FROM ocr_text ORDER BY mediaStoreId DESC")
    fun getAllOcrTextPaging(): PagingSource<Int, OcrTextEntity>
    
    /**
     * Get count of images with OCR text.
     */
    @Query("SELECT COUNT(*) FROM ocr_text")
    suspend fun getCount(): Int

    /**
     * Get sample of OCR texts for global frequency analysis.
     */
    @Query("SELECT * FROM ocr_text LIMIT :limit")
    suspend fun getAllOcrTexts(limit: Int): List<OcrTextEntity>

    /**
     * Search OCR text with limit (for co-occurrence analysis).
     */
    @Query("SELECT * FROM ocr_text WHERE fullText LIKE '%' || :query || '%' OR indexedTokens LIKE '%' || :query || '%' LIMIT :limit")
    suspend fun searchOcrTextsLimited(query: String, limit: Int): List<OcrTextEntity>
}

