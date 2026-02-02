package com.photo.searchai.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photo.searchai.domain.model.FeatureType
import com.photo.searchai.domain.repository.SnapshotRepository
import com.photo.searchai.domain.usecase.GetSnapshotProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel
@Inject
constructor(
        private val getSnapshotProgressUseCase: GetSnapshotProgressUseCase,
        private val snapshotRepository: SnapshotRepository
) : ViewModel() {

        init {
                viewModelScope.launch {
                        // Create immutable snapshot for this session
                        snapshotRepository.createSnapshot()
                        // In a real app, check if snapshot is recent enough or if app was just
                        // opened
                }
        }

        val ocrProgress =
                getSnapshotProgressUseCase(FeatureType.OCR)
                        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        val labelingProgress =
                getSnapshotProgressUseCase(FeatureType.LABELING)
                        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        val totalImages =
                snapshotRepository
                        .getTotalImageCount()
                        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}
