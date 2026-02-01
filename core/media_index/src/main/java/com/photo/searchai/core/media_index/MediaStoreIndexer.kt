package com.photo.searchai.core.media_index

import com.photo.searchai.core.media_index.model.MediaItem
import kotlinx.coroutines.flow.Flow

/**
 * Interface for indexing and observing media from MediaStore.
 */
interface MediaStoreIndexer {
    /**
     * Retrieves all media items from MediaStore.
     */
    suspend fun getAllMedia(): List<MediaItem>

    /**
     * Observes changes in MediaStore and emits the full list or updates.
     */
    fun observeMedia(): Flow<List<MediaItem>>

    /**
     * Retrieves a single media item by its MediaStore ID.
     */
    suspend fun getMediaById(id: Long): MediaItem?
}
