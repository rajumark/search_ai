package com.photo.searchai.core.media_index.model

import android.net.Uri

/**
 * Domain model for a media item from MediaStore.
 */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val path: String,
    val size: Long,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val dateAdded: Long,
    val dateModified: Long,
    val orientation: Int,
    val isFavorite: Boolean,
    val isHidden: Boolean
)
