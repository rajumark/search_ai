package com.photo.searchai.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photo.searchai.core.data.repository.MediaRepository
import com.photo.searchai.core.database.entity.SearchResultWithOcr
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SearchByLabelsViewModel
@Inject
constructor(
        private val mediaRepository: MediaRepository,
        savedStateHandle: SavedStateHandle
) : ViewModel() {

    data class LabelSearchUiState(
            val selectedLabels: List<String> = emptyList(),
            val relatedLabels: List<String> = emptyList(),
            val results: List<SearchResultWithOcr> = emptyList(),
            val isLoading: Boolean = false
    )

    private val _uiState = MutableStateFlow(LabelSearchUiState())
    val uiState: StateFlow<LabelSearchUiState> = _uiState.asStateFlow()

    init {
        val initialLabel = savedStateHandle.get<String>(ARG_LABEL).orEmpty()
        if (initialLabel.isNotBlank()) {
            addLabel(initialLabel)
        }

        viewModelScope.launch {
            _uiState
                    .map { it.selectedLabels }
                    .distinctUntilChanged()
                    .collect { labels ->
                        if (labels.isEmpty()) {
                            _uiState.update {
                                it.copy(relatedLabels = emptyList(), results = emptyList())
                            }
                            return@collect
                        }

                        _uiState.update { it.copy(isLoading = true) }
                        val related = mediaRepository.getRelatedLabels(labels)
                        val results =
                                mediaRepository.getImagesForLabels(labels).map { image ->
                                    SearchResultWithOcr(image, null)
                                }
                        _uiState.update {
                            it.copy(
                                    relatedLabels = related.map { label -> label.label },
                                    results = results,
                                    isLoading = false
                            )
                        }
                    }
        }
    }

    fun addLabel(label: String) {
        if (label.isBlank()) return
        _uiState.update {
            if (label in it.selectedLabels) it
            else it.copy(selectedLabels = it.selectedLabels + label)
        }
    }

    fun removeLabel(label: String) {
        _uiState.update { it.copy(selectedLabels = it.selectedLabels - label) }
    }

    companion object {
        const val ARG_LABEL = "label"
    }
}
