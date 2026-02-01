package com.photo.searchai.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Image quality metrics for a specific photo.
 * Stores raw scores from OpenCV analysis.
 */
@Entity(
    tableName = "image_quality",
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
data class ImageQualityEntity(
    @PrimaryKey val mediaStoreId: Long,
    val blurScore: Double,
    val brightnessScore: Double,
    val contrastScore: Double,
    val overexposedRatio: Double,
    val width: Int,
    val height: Int,
    val imageHash: String,
    val analyzedAt: Long = System.currentTimeMillis()
)
