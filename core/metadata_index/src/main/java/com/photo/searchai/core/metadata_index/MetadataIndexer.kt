package com.photo.searchai.core.metadata_index

import com.photo.searchai.core.database.entity.ExifEntity

/**
 * Interface for extracting EXIF metadata from media files.
 */
interface MetadataIndexer {
    /**
     * Extracts EXIF metadata for the given image.
     */
    suspend fun extractMetadata(mediaStoreId: Long, path: String): ExifEntity
}
