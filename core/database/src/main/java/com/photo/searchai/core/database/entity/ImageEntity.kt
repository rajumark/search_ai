package com.photo.searchai.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ProcessingStatus {
    PENDING,
    DONE
}

@Entity(tableName = "images")
data class ImageEntity(
        @PrimaryKey val imageId: String,
        val path: String,
        val dateAdded: Long,
        val ocrStatus: ProcessingStatus = ProcessingStatus.PENDING,
        val ocrText: String? = null,
        val labelingStatus: ProcessingStatus = ProcessingStatus.PENDING,
        val labels: String? = null, // Storing as comma separated string for simplicity or JSON
        val lastProcessedAt: Long? = null
)
