package com.photo.searchai.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photo.searchai.core.data.repository.MediaRepository
import com.photo.searchai.core.database.entity.SearchResultWithOcr
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

data class SearchUiState(
        val query: String = "",
        val results: List<SearchResultWithOcr> = emptyList(),
        val resultsCount: Int = 0,
        val suggestedChips: List<String> = emptyList(),
        val isSearching: Boolean = false,
        val isActive: Boolean = false
)

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchByTextViewModel
@Inject
constructor(
        private val mediaRepository: MediaRepository,
        private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val QUERY_KEY = "search_query"

    private val _uiState =
            MutableStateFlow(
                    SearchUiState(
                            query = savedStateHandle.get<String>(QUERY_KEY) ?: "",
                            isActive = (savedStateHandle.get<String>(QUERY_KEY) ?: "").isNotEmpty()
                    )
            )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        _uiState
                .map { it.query }
                .distinctUntilChanged()
                .debounce(300)
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        // When query is empty, show recent searches as chips AND all photos
                        combine(
                                mediaRepository.getRecentSearches(),
                                mediaRepository.getAllImages()
                        ) { recent, images ->
                            _uiState.update { it.copy(suggestedChips = recent) }
                            images.map { SearchResultWithOcr(it, null) }
                        }
                    } else {
                        // Perform search and fetch suggestions
                        flow {
                            _uiState.update { it.copy(isSearching = true) }

                            // Save to recent searches
                            mediaRepository.saveRecentSearch(query)

                            // Fetch suggestions in background
                            val suggestions = mediaRepository.getSuggestions(query)
                            _uiState.update { it.copy(suggestedChips = suggestions) }

                            // Emit search results
                            emitAll(mediaRepository.searchImages(query))
                        }
                    }
                }
                .flowOn(Dispatchers.IO)
                .onEach { results ->
                    _uiState.update {
                        it.copy(results = results, resultsCount = results.size, isSearching = false)
                    }
                }
                .launchIn(viewModelScope)
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        savedStateHandle[QUERY_KEY] = newQuery
    }

    fun onChipClick(chipText: String) {
        val currentQuery = _uiState.value.query.trim()
        val newQuery = if (currentQuery.isEmpty()) chipText else "$currentQuery $chipText"
        onQueryChange(newQuery)
    }

    fun onActiveChange(isActive: Boolean) {
        _uiState.update { it.copy(isActive = isActive) }
    }

    fun onClearQuery() {
        onQueryChange("")
    }
}
