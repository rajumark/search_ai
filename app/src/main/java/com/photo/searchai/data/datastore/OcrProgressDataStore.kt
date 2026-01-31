package com.photo.searchai.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.ocrProgressDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ocr_progress"
)

/**
 * Data class representing OCR processing progress.
 */
data class OcrProgress(
    val totalImages: Int = 0,
    val parsedImages: Int = 0,
    val pendingImages: Int = 0,
    val lastUpdated: Long = 0
) {
    val progress: Float
        get() = if (totalImages > 0) parsedImages.toFloat() / totalImages else 0f
    
    val isComplete: Boolean
        get() = totalImages > 0 && pendingImages == 0
}

/**
 * DataStore for persisting OCR progress state.
 * Used by WorkManager to checkpoint progress between batches.
 */
class OcrProgressDataStore @Inject constructor(
    private val context: Context
) {
    private object Keys {
        val TOTAL_IMAGES = intPreferencesKey("total_images")
        val PARSED_IMAGES = intPreferencesKey("parsed_images")
        val PENDING_IMAGES = intPreferencesKey("pending_images")
        val LAST_UPDATED = longPreferencesKey("last_updated")
    }
    
    /**
     * Flow of current OCR progress.
     */
    val progressFlow: Flow<OcrProgress> = context.ocrProgressDataStore.data.map { preferences ->
        OcrProgress(
            totalImages = preferences[Keys.TOTAL_IMAGES] ?: 0,
            parsedImages = preferences[Keys.PARSED_IMAGES] ?: 0,
            pendingImages = preferences[Keys.PENDING_IMAGES] ?: 0,
            lastUpdated = preferences[Keys.LAST_UPDATED] ?: 0
        )
    }
    
    /**
     * Update OCR progress.
     */
    suspend fun updateProgress(total: Int, parsed: Int, pending: Int) {
        context.ocrProgressDataStore.edit { preferences ->
            preferences[Keys.TOTAL_IMAGES] = total
            preferences[Keys.PARSED_IMAGES] = parsed
            preferences[Keys.PENDING_IMAGES] = pending
            preferences[Keys.LAST_UPDATED] = System.currentTimeMillis()
        }
    }
    
    /**
     * Clear progress data.
     */
    suspend fun clearProgress() {
        context.ocrProgressDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
