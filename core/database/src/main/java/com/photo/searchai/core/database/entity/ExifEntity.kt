package com.photo.searchai.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity storing EXIF metadata for an image.
 */
@Entity(
    tableName = "exif_metadata",
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
data class ExifEntity(
    @PrimaryKey val mediaStoreId: Long,
    val make: String?,
    val model: String?,
    val flash: Int?, // 0: no flash, 1: flash fired
    val focalLength: Double?,
    val iso: Int?,
    val exposureTime: Double?,
    val aperture: Double?,
    val width: Int,
    val height: Int,
    val orientation: Int,
    val dateTaken: Long?,
    val latitude: Double?,
    val longitude: Double?,
    val software: String?,
    val isEdited: Boolean = false, // Derived: modified > created
    val hasExif: Boolean = true
)
