package com.photo.searchai.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.photo.searchai.domain.model.FeatureType

@Entity(tableName = "processing_snapshots")
data class ProcessingSnapshotEntity(
        @PrimaryKey(autoGenerate = true) val snapshotId: Long = 0,
        val featureType: FeatureType,
        val totalPending: Int,
        val processedCount: Int,
        val createdAt: Long
)
