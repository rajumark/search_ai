package com.photo.searchai.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photo.searchai.core.data.repository.MediaRepository
import com.photo.searchai.core.database.entity.ImageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

data class SearchUiState(
        val query: String = "",
        val results: List<ImageEntity> = emptyList(),
        val isSearching: Boolean = false,
        val isActive: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchByTextViewModel
@Inject
constructor(
        private val mediaRepository: MediaRepository,
        private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val QUERY_KEY = "search_query"

    private val _uiState =
            MutableStateFlow(SearchUiState(query = savedStateHandle.get<String>(QUERY_KEY) ?: ""))
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        _uiState
                .map { it.query }
                .distinctUntilChanged()
                .debounce(300)
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        mediaRepository.getAllImages()
                    } else {
                        _uiState.update { it.copy(isSearching = true) }
                        mediaRepository.searchImages(query)
                    }
                }
                .flowOn(Dispatchers.IO)
                .onEach { results ->
                    _uiState.update { it.copy(results = results, isSearching = false) }
                }
                .launchIn(viewModelScope)
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        savedStateHandle[QUERY_KEY] = newQuery
    }

    fun onActiveChange(isActive: Boolean) {
        _uiState.update { it.copy(isActive = isActive) }
    }

    fun onClearQuery() {
        onQueryChange("")
    }
}
