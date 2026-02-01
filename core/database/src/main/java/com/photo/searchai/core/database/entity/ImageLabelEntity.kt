package com.photo.searchai.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity storing image labels detected by ML Kit.
 * Each image can have multiple labels (e.g., "dog", "beach", "sunset").
 */
@Entity(
    tableName = "image_labels",
    foreignKeys = [
        ForeignKey(
            entity = ImageEntity::class,
            parentColumns = ["mediaStoreId"],
            childColumns = ["mediaStoreId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mediaStoreId"]), Index(value = ["label"])]
)
data class ImageLabelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaStoreId: Long,
    val label: String,       // Label text (e.g., "dog", "beach")
    val confidence: Float,   // Confidence score 0-1
    val index: Int           // ML Kit index for this label
)
