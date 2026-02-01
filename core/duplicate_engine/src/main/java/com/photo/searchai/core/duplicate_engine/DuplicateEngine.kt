package com.photo.searchai.core.duplicate_engine

import com.photo.searchai.core.media_index.model.MediaItem

/**
 * Interface for detecting duplicate and near-duplicate media.
 */
interface DuplicateEngine {
    /**
     * Computes a unique hash for a media file for exact duplicate detection.
     */
    suspend fun computeHash(path: String): String

    /**
     * Identifies potential near-duplicates based on metadata (resolution, size, date).
     */
    fun isNearDuplicate(media1: MediaItem, media2: MediaItem): Boolean

    /**
     * Ranks a list of images in a duplicate group and suggests the "Original" to keep.
     */
    fun rankDuplicates(mediaList: List<MediaItem>): Pair<MediaItem, List<MediaItem>>
}
