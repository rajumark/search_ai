package com.photo.searchai.feature.storage_cleanup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photo.searchai.core.database.dao.CleanupDao
import com.photo.searchai.core.database.dao.ImageDao
import com.photo.searchai.core.database.entity.CleanupCandidateEntity
import com.photo.searchai.core.media_index.MediaStoreIndexer
import com.photo.searchai.core.media_index.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageCleanupState(
    val candidates: List<CleanupInfo> = emptyList(),
    val totalReclaimableMB: Long = 0,
    val isLoading: Boolean = true
)

data class CleanupInfo(
    val candidate: CleanupCandidateEntity,
    val media: MediaItem?
)

@HiltViewModel
class StorageCleanupViewModel @Inject constructor(
    private val cleanupDao: CleanupDao,
    private val imageDao: ImageDao,
    private val mediaStoreIndexer: MediaStoreIndexer
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageCleanupState())
    val uiState: StateFlow<StorageCleanupState> = _uiState.asStateFlow()

    init {
        loadCandidates()
    }

    private fun loadCandidates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val candidateEntities = cleanupDao.getAllCandidates()
            val allMedia = mediaStoreIndexer.getAllMedia()
            
            val cleanupInfos = candidateEntities.map { candidate ->
                CleanupInfo(
                    candidate = candidate,
                    media = allMedia.find { it.id == candidate.mediaStoreId }
                )
            }.filter { it.media != null }

            val totalSize = cleanupInfos.sumOf { it.candidate.reclaimableSize } / (1024 * 1024)

            _uiState.value = StorageCleanupState(
                candidates = cleanupInfos,
                totalReclaimableMB = totalSize,
                isLoading = false
            )
        }
    }

    fun deleteCandidate(candidateId: Long) {
        viewModelScope.launch {
            val candidate = uiState.value.candidates.find { it.candidate.mediaStoreId == candidateId }
            if (candidate != null) {
                // In a real app, you'd use ContentResolver.delete()
                // For now, we'll just remove from DB to simulate
                cleanupDao.deleteCandidate(candidateId)
                imageDao.deleteImageById(candidateId)
                loadCandidates()
            }
        }
    }
}
