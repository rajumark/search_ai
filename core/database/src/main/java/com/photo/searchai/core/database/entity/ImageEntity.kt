package com.photo.searchai.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing an image from MediaStore. Tracks processing state for OCR, barcode scanning,
 * image labeling, and face detection.
 */
@Entity(
        tableName = "images",
        indices =
                [
                        Index(value = ["parsed"]),
                        Index(value = ["barcodeParsed"]),
                        Index(value = ["labelParsed"]),
                        Index(value = ["faceParsed"]),
                        Index(value = ["qualityParsed"]),
                        Index(value = ["mediaStoreId"])]
)
data class ImageEntity(
        @PrimaryKey val mediaStoreId: Long,
        val path: String,
        val dateAdded: Long,
        // Legacy field for backward compatibility - now represents "ocrParsed"
        val parsed: Boolean = false,
        // Barcode scanning completed
        val barcodeParsed: Boolean = false,
        // Image labeling completed
        val labelParsed: Boolean = false,
        // Face detection completed
        val faceParsed: Boolean = false,
        // Quality analysis completed
        val qualityParsed: Boolean = false
) {
    // Helper property for checking if OCR is done (uses 'parsed' for backward compatibility)
    val ocrParsed: Boolean
        get() = parsed

    // Check if all processing is complete
    val fullyProcessed: Boolean
        get() = parsed && barcodeParsed && labelParsed && faceParsed && qualityParsed
}
