package com.photo.searchai.domain.repository

import com.photo.searchai.domain.model.FeatureType
import com.photo.searchai.domain.model.ProcessingSnapshot
import kotlinx.coroutines.flow.Flow

interface SnapshotRepository {
    suspend fun createSnapshot()
    fun getSnapshotProgress(featureType: FeatureType): Flow<ProcessingSnapshot?>
    suspend fun updateSnapshotProgress(featureType: FeatureType, processedCount: Int)
    suspend fun getLatestSnapshotSync(featureType: FeatureType): ProcessingSnapshot?
}
