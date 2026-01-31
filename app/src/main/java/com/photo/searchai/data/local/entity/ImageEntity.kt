package com.photo.searchai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing an image from MediaStore.
 * Tracks OCR parsing state for each image.
 */
@Entity(
    tableName = "images",
    indices = [
        Index(value = ["parsed"]),
        Index(value = ["mediaStoreId"])
    ]
)
data class ImageEntity(
    @PrimaryKey
    val mediaStoreId: Long,
    val path: String,
    val dateAdded: Long,
    val parsed: Boolean = false
)
