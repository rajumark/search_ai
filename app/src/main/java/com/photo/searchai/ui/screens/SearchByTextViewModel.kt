package com.photo.searchai.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photo.searchai.core.data.repository.MediaRepository
import com.photo.searchai.core.database.entity.ImageEntity
import com.photo.searchai.core.database.entity.SearchResultWithOcr
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val NO_BUCKET = -1L

data class SearchUiState(
        val query: String = "",
        val results: List<SearchResultWithOcr> = emptyList(),
        val resultsCount: Int = 0,
        val suggestedChips: List<String> = emptyList(),
        val isSearching: Boolean = false,
        val bucketId: Long = NO_BUCKET,
        val bucketName: String = "",
        val searchPurpose: SearchPurpose = SearchPurpose.GeneralSearch
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
    private val BUCKET_ID_KEY = "bucket_id"
    private val BUCKET_NAME_KEY = "bucket_name"

    private val searchPurpose: SearchPurpose = run {
        val query = savedStateHandle.get<String>(QUERY_KEY).orEmpty()
        val bucketId = savedStateHandle.get<Long>(BUCKET_ID_KEY) ?: NO_BUCKET
        val bucketName = savedStateHandle.get<String>(BUCKET_NAME_KEY).orEmpty()
        
        when {
            query.trim().lowercase() == "is favorite" || 
            query.trim().lowercase() == "favorite" || 
            query.trim().lowercase() == "favorite images" -> SearchPurpose.Favorites
            
            bucketId != NO_BUCKET -> SearchPurpose.AlbumSearch(bucketId, bucketName)
            
            query.isNotEmpty() -> SearchPurpose.GroupingSearch(query)
            
            else -> SearchPurpose.GeneralSearch
        }
    }

    private val _uiState =
            MutableStateFlow(
                    SearchUiState(
                            query = searchPurpose.getInitialQuery(),
                            bucketId = searchPurpose.getBucketIdForSearch(),
                            bucketName = searchPurpose.getBucketNameForDisplay(),
                            searchPurpose = searchPurpose
                    )
            )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        _uiState
                .map { it.query }
                .distinctUntilChanged()
                .debounce(300)
                .flatMapLatest { query ->
                    val bucketId = _uiState.value.bucketId.takeIf { it >= 0 }
                    if (query.isBlank()) {
                        // When query is empty, show recent searches as chips AND all photos
                        combine(
                                mediaRepository.getRecentSearches(),
                                if (bucketId == null) {
                                    mediaRepository.getAllImages()
                                } else {
                                    mediaRepository.getImagesByBucket(bucketId)
                                }
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
                            emitAll(mediaRepository.searchImages(query, bucketId))
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
        // No longer needed with simplified search
    }

    fun onClearQuery() {
        onQueryChange("")
    }

    fun deleteImage(image: ImageEntity) {
        viewModelScope.launch(Dispatchers.IO) { mediaRepository.deleteImage(image) }
    }

    fun deleteImages(images: List<ImageEntity>) {
        if (images.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            images.forEach { mediaRepository.deleteImage(it) }
        }
    }

    fun setFavorite(imageId: Long, isFavorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { mediaRepository.setFavorite(imageId, isFavorite) }
    }
}
