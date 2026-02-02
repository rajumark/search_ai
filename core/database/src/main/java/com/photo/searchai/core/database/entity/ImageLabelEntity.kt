package com.photo.searchai.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
        tableName = "image_labels",
        foreignKeys =
                [
                        ForeignKey(
                                entity = ImageEntity::class,
                                parentColumns = ["id"],
                                childColumns = ["imageId"],
                                onDelete = ForeignKey.CASCADE
                        )],
        indices = [Index(value = ["imageId"])]
)
data class ImageLabelEntity(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val imageId: Long,
        val labelId: String, // Storing index or text hash as stable ID
        val labelText: String,
        val confidence: Float,
        val modelVersion: String,
        val createdAt: Long = System.currentTimeMillis()
)
