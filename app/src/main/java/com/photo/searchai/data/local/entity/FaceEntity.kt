package com.photo.searchai.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a detected face from an image. Links back to the original image via
 * mediaStoreId. Stores the cropped face image path and bounding box coordinates.
 */
@Entity(
        tableName = "faces",
        foreignKeys =
                [
                        ForeignKey(
                                entity = ImageEntity::class,
                                parentColumns = ["mediaStoreId"],
                                childColumns = ["mediaStoreId"],
                                onDelete = ForeignKey.CASCADE
                        )],
        indices = [Index(value = ["mediaStoreId"]), Index(value = ["croppedFacePath"])]
)
data class FaceEntity(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,

        // Reference to the original image in MediaStore
        val mediaStoreId: Long,

        // Path to the cropped face image in private storage
        val croppedFacePath: String,

        // Bounding box coordinates from original image
        val boundingBoxLeft: Int,
        val boundingBoxTop: Int,
        val boundingBoxRight: Int,
        val boundingBoxBottom: Int,

        // Face dimensions
        val faceWidth: Int,
        val faceHeight: Int,

        // Index of this face within the image (0, 1, 2, etc.)
        val faceIndex: Int,

        // Timestamp when this face was detected
        val detectedAt: Long = System.currentTimeMillis()
)
