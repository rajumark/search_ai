package com.photo.searchai.core.data.repository

import com.photo.searchai.core.database.dao.ImageDao
import com.photo.searchai.core.database.dao.SnapshotDao
import com.photo.searchai.core.database.entity.ProcessingSnapshotEntity
import com.photo.searchai.core.database.entity.ProcessingStatus
import com.photo.searchai.domain.model.FeatureType
import com.photo.searchai.domain.model.ProcessingSnapshot
import com.photo.searchai.domain.repository.SnapshotRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SnapshotRepositoryImpl
@Inject
constructor(private val snapshotDao: SnapshotDao, private val imageDao: ImageDao) :
        SnapshotRepository {

    override suspend fun createSnapshot() {
        val features = listOf(FeatureType.OCR, FeatureType.LABELING)
        features.forEach { feature ->
            val total =
                    imageDao.getCountByOcrStatus(
                            ProcessingStatus.PENDING
                    ) // Logic simplified for demo
            // In real app, query based on feature type

            val snapshot =
                    ProcessingSnapshotEntity(
                            featureType = feature,
                            totalPending = total,
                            processedCount = 0,
                            createdAt = System.currentTimeMillis()
                    )
            snapshotDao.insert(snapshot)
        }
    }

    override fun getSnapshotProgress(featureType: FeatureType): Flow<ProcessingSnapshot?> {
        return snapshotDao.getLatestSnapshot(featureType).map { entity ->
            entity?.let {
                ProcessingSnapshot(
                        snapshotId = it.snapshotId,
                        featureType = it.featureType,
                        totalPending = it.totalPending,
                        processedCount = it.processedCount,
                        createdAt = it.createdAt
                )
            }
        }
    }

    override suspend fun updateSnapshotProgress(featureType: FeatureType, processedCount: Int) {
        snapshotDao.updateProgress(featureType, processedCount)
    }

    override suspend fun getLatestSnapshotSync(featureType: FeatureType): ProcessingSnapshot? {
        val entity = snapshotDao.getLatestSnapshotSync(featureType) ?: return null
        return ProcessingSnapshot(
                snapshotId = entity.snapshotId,
                featureType = entity.featureType,
                totalPending = entity.totalPending,
                processedCount = entity.processedCount,
                createdAt = entity.createdAt
        )
    }
    override fun getTotalImageCount(): Flow<Int> = imageDao.getTotalCount()
}
