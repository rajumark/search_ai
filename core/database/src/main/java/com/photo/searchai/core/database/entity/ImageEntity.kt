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
        val ocrStatus: ProcessingStatus = ProcessingStatus.PENDING,
        val labelingStatus: ProcessingStatus = ProcessingStatus.PENDING,
        val lastProcessedAt: Long? = null
)
