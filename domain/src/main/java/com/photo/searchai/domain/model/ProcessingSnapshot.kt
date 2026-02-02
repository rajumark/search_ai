package com.photo.searchai.domain.model

enum class FeatureType {
    OCR,
    LABELING,
    MEDIA_PROCESSING
}

data class ProcessingSnapshot(
        val snapshotId: Long,
        val featureType: FeatureType,
        val totalPending: Int,
        val processedCount: Int,
        val createdAt: Long
)
