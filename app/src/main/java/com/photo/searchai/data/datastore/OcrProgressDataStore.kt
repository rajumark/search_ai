package com.photo.searchai.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "image_processing_progress"
)

/**
 * Processing stage enumeration.
 */
enum class ProcessingStage {
    IDLE,
    OCR,
    BARCODE,
    LABELING,
    COMPLETE
}

/**
 * Data class representing image processing progress for all stages.
 */
data class ProcessingProgress(
    val totalImages: Int = 0,
    // OCR stage
    val ocrParsed: Int = 0,
    val ocrPending: Int = 0,
    // Barcode stage
    val barcodeParsed: Int = 0,
    val barcodePending: Int = 0,
    // Labeling stage
    val labelParsed: Int = 0,
    val labelPending: Int = 0,
    // Current processing state
    val currentStage: ProcessingStage = ProcessingStage.IDLE,
    val lastUpdated: Long = 0
) {
    // Overall progress (0 to 1) across all stages
    val overallProgress: Float
        get() {
            val totalWork = totalImages * 3 // 3 stages per image
            if (totalWork <= 0) return 0f
            val completedWork = ocrParsed + barcodeParsed + labelParsed
            return completedWork.toFloat() / totalWork
        }
    
    // Stage-specific progress
    val ocrProgress: Float
        get() = if (totalImages > 0) ocrParsed.toFloat() / totalImages else 0f
    
    val barcodeProgress: Float
        get() = if (totalImages > 0) barcodeParsed.toFloat() / totalImages else 0f
    
    val labelProgress: Float
        get() = if (totalImages > 0) labelParsed.toFloat() / totalImages else 0f
    
    val isComplete: Boolean
        get() = totalImages > 0 && ocrPending == 0 && barcodePending == 0 && labelPending == 0
    
    val isProcessing: Boolean
        get() = currentStage != ProcessingStage.IDLE && currentStage != ProcessingStage.COMPLETE
}

// Legacy alias for backward compatibility
typealias OcrProgress = ProcessingProgress

/**
 * DataStore for persisting image processing progress state.
 * Tracks OCR, barcode scanning, and image labeling progress.
 */
class OcrProgressDataStore @Inject constructor(
    private val context: Context
) {
    private object Keys {
        val TOTAL_IMAGES = intPreferencesKey("total_images")
        // OCR
        val OCR_PARSED = intPreferencesKey("ocr_parsed")
        val OCR_PENDING = intPreferencesKey("ocr_pending")
        // Barcode
        val BARCODE_PARSED = intPreferencesKey("barcode_parsed")
        val BARCODE_PENDING = intPreferencesKey("barcode_pending")
        // Label
        val LABEL_PARSED = intPreferencesKey("label_parsed")
        val LABEL_PENDING = intPreferencesKey("label_pending")
        // State
        val CURRENT_STAGE = stringPreferencesKey("current_stage")
        val LAST_UPDATED = longPreferencesKey("last_updated")
        
        // Legacy keys for backward compatibility
        val PARSED_IMAGES = intPreferencesKey("parsed_images")
        val PENDING_IMAGES = intPreferencesKey("pending_images")
    }
    
    /**
     * Flow of current processing progress.
     */
    val progressFlow: Flow<ProcessingProgress> = context.progressDataStore.data.map { preferences ->
        ProcessingProgress(
            totalImages = preferences[Keys.TOTAL_IMAGES] ?: 0,
            ocrParsed = preferences[Keys.OCR_PARSED] ?: preferences[Keys.PARSED_IMAGES] ?: 0,
            ocrPending = preferences[Keys.OCR_PENDING] ?: preferences[Keys.PENDING_IMAGES] ?: 0,
            barcodeParsed = preferences[Keys.BARCODE_PARSED] ?: 0,
            barcodePending = preferences[Keys.BARCODE_PENDING] ?: 0,
            labelParsed = preferences[Keys.LABEL_PARSED] ?: 0,
            labelPending = preferences[Keys.LABEL_PENDING] ?: 0,
            currentStage = try {
                ProcessingStage.valueOf(preferences[Keys.CURRENT_STAGE] ?: "IDLE")
            } catch (e: Exception) {
                ProcessingStage.IDLE
            },
            lastUpdated = preferences[Keys.LAST_UPDATED] ?: 0
        )
    }
    
    /**
     * Legacy method for backward compatibility.
     * Updates OCR progress only.
     */
    suspend fun updateProgress(total: Int, parsed: Int, pending: Int) {
        updateOcrProgress(total, parsed, pending)
    }
    
    /**
     * Update OCR processing progress.
     */
    suspend fun updateOcrProgress(total: Int, parsed: Int, pending: Int) {
        context.progressDataStore.edit { preferences ->
            preferences[Keys.TOTAL_IMAGES] = total
            preferences[Keys.OCR_PARSED] = parsed
            preferences[Keys.OCR_PENDING] = pending
            preferences[Keys.LAST_UPDATED] = System.currentTimeMillis()
        }
    }
    
    /**
     * Update barcode processing progress.
     */
    suspend fun updateBarcodeProgress(total: Int, parsed: Int, pending: Int) {
        context.progressDataStore.edit { preferences ->
            preferences[Keys.TOTAL_IMAGES] = total
            preferences[Keys.BARCODE_PARSED] = parsed
            preferences[Keys.BARCODE_PENDING] = pending
            preferences[Keys.LAST_UPDATED] = System.currentTimeMillis()
        }
    }
    
    /**
     * Update label processing progress.
     */
    suspend fun updateLabelProgress(total: Int, parsed: Int, pending: Int) {
        context.progressDataStore.edit { preferences ->
            preferences[Keys.TOTAL_IMAGES] = total
            preferences[Keys.LABEL_PARSED] = parsed
            preferences[Keys.LABEL_PENDING] = pending
            preferences[Keys.LAST_UPDATED] = System.currentTimeMillis()
        }
    }
    
    /**
     * Update current processing stage.
     */
    suspend fun updateCurrentStage(stage: ProcessingStage) {
        context.progressDataStore.edit { preferences ->
            preferences[Keys.CURRENT_STAGE] = stage.name
            preferences[Keys.LAST_UPDATED] = System.currentTimeMillis()
        }
    }
    
    /**
     * Update all progress at once.
     */
    suspend fun updateAllProgress(
        total: Int,
        ocrParsed: Int, ocrPending: Int,
        barcodeParsed: Int, barcodePending: Int,
        labelParsed: Int, labelPending: Int,
        currentStage: ProcessingStage
    ) {
        context.progressDataStore.edit { preferences ->
            preferences[Keys.TOTAL_IMAGES] = total
            preferences[Keys.OCR_PARSED] = ocrParsed
            preferences[Keys.OCR_PENDING] = ocrPending
            preferences[Keys.BARCODE_PARSED] = barcodeParsed
            preferences[Keys.BARCODE_PENDING] = barcodePending
            preferences[Keys.LABEL_PARSED] = labelParsed
            preferences[Keys.LABEL_PENDING] = labelPending
            preferences[Keys.CURRENT_STAGE] = currentStage.name
            preferences[Keys.LAST_UPDATED] = System.currentTimeMillis()
        }
    }
    
    /**
     * Clear progress data.
     */
    suspend fun clearProgress() {
        context.progressDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

