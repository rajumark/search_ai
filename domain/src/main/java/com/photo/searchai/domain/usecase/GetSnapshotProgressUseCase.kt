package com.photo.searchai.domain.usecase

import com.photo.searchai.domain.model.FeatureType
import com.photo.searchai.domain.model.ProcessingSnapshot
import com.photo.searchai.domain.repository.SnapshotRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetSnapshotProgressUseCase @Inject constructor(private val repository: SnapshotRepository) {
    operator fun invoke(featureType: FeatureType): Flow<ProcessingSnapshot?> {
        return repository.getSnapshotProgress(featureType)
    }
}
