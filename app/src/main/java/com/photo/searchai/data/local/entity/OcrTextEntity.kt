package com.photo.searchai.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity storing OCR text extracted from images.
 * References ImageEntity via foreign key.
 */
@Entity(
    tableName = "ocr_text",
    foreignKeys = [
        ForeignKey(
            entity = ImageEntity::class,
            parentColumns = ["mediaStoreId"],
            childColumns = ["mediaStoreId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mediaStoreId"])]
)
data class OcrTextEntity(
    @PrimaryKey
    val mediaStoreId: Long,
    val fullText: String,
    val indexedTokens: String
)
