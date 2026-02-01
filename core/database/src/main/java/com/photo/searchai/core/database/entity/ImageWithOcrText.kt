package com.photo.searchai.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Data class representing an image with its associated OCR text.
 * Uses Room's @Relation annotation for joined queries.
 */
data class ImageWithOcrText(
    @Embedded val image: ImageEntity,
    @Relation(
        parentColumn = "mediaStoreId",
        entityColumn = "mediaStoreId"
    )
    val ocrText: OcrTextEntity?
)
