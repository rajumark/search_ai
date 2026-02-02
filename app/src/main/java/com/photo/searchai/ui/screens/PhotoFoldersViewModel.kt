package com.photo.searchai.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photo.searchai.core.data.repository.MediaRepository
import com.photo.searchai.core.database.entity.AlbumSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PhotoFoldersViewModel
@Inject
constructor(
        private val mediaRepository: MediaRepository
) : ViewModel() {

    data class PhotoFoldersUiState(
            val albums: List<AlbumSummary> = emptyList()
    )

    private val _uiState = MutableStateFlow(PhotoFoldersUiState())
    val uiState: StateFlow<PhotoFoldersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            mediaRepository.getAlbumSummaries().collectLatest { albums ->
                _uiState.update { it.copy(albums = albums) }
            }
        }
    }
}
