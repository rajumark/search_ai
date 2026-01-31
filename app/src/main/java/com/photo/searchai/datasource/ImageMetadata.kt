package com.photo.searchai.datasource

/**
 * Data class representing image metadata from MediaStore.
 */
data class ImageMetadata(
    val mediaStoreId: Long,
    val path: String,
    val dateAdded: Long
)
