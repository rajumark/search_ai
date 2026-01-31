package com.photo.searchai.data.local.dao

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
}
