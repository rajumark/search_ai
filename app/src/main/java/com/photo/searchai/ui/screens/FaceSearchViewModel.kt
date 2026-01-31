package com.photo.searchai.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.photo.searchai.data.local.dao.FaceDao
import com.photo.searchai.data.local.dao.ImageDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FaceSearchViewModel
@Inject
constructor(
        private val faceDao: FaceDao,
        private val imageDao: ImageDao,
        @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(FaceSearchUiState())
    val uiState: StateFlow<FaceSearchUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<FaceSearchEvent>()
    val events: Flow<FaceSearchEvent> = _events.asSharedFlow()

    val facePagingData: Flow<PagingData<FaceWithImage>> =
            Pager(config = PagingConfig(pageSize = 20, enablePlaceholders = false)) {
                        faceDao.getAllFacesPaging()
                    }
                    .flow
                    .map { pagingData ->
                        pagingData.map { face ->
                            val image = imageDao.getImageById(face.mediaStoreId)
                            FaceWithImage(face, image?.path ?: "")
                        }
                    }
                    .cachedIn(viewModelScope)

    fun toggleDisplayMode() {
        _uiState.update { currentState ->
            val newMode =
                    if (currentState.displayMode == FaceDisplayMode.CROPPED_FACE) {
                        FaceDisplayMode.FULL_IMAGE
                    } else {
                        FaceDisplayMode.CROPPED_FACE
                    }
            currentState.copy(displayMode = newMode)
        }
    }

    fun onImageClicked(mediaStoreId: Long, index: Int) {
        if (_uiState.value.isInSelectionMode) {
            // For now, face selection just selects key based on face ID?
            // But the grid returns mediaStoreId/index if we use re-use logic.
            // Wait, ImageGridView calls onItemClick(id, index).
            // For FaceWithImage, id is expected to be extracted by extractId.
            // If we use face.id as ID, then we are selecting faces.
            // If we use mediaStoreId, we are selecting images.
        } else {
            // Navigate to full screen
            viewModelScope.launch {
                _events.emit(FaceSearchEvent.NavigateToFullScreen(mediaStoreId, index))
            }
        }
    }

    // Selection logic would be similar to SearchViewModel, simplified for now
    // as requirements focus on "reusable UI" and "mode toggle"
}

sealed class FaceSearchEvent {
    data class NavigateToFullScreen(val mediaStoreId: Long, val index: Int) : FaceSearchEvent()
}
