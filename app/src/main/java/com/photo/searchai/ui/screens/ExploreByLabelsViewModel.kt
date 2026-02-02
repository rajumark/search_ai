package com.photo.searchai.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photo.searchai.core.data.repository.MediaRepository
import com.photo.searchai.core.database.entity.ImageEntity
import com.photo.searchai.core.database.entity.LabelCount
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ExploreByLabelsViewModel @Inject constructor(
        private val mediaRepository: MediaRepository
) : ViewModel() {

    data class LabelUiModel(val label: LabelCount, val previews: List<ImageEntity>)

    private val queryFlow = MutableStateFlow("")

    val query: StateFlow<String> = queryFlow

    val labels: StateFlow<List<LabelUiModel>> =
            combine(queryFlow, mediaRepository.getLabelCountsFlow()) { query, labels ->
                        if (query.isBlank()) {
                            labels
                        } else {
                            labels.filter { it.label.contains(query, ignoreCase = true) }
                        }
                    }
                    .mapLatest { filtered ->
                        val deferred =
                                filtered.map { label ->
                                    viewModelScope.async {
                                        LabelUiModel(
                                                label = label,
                                                previews =
                                                        mediaRepository.getLabelPreviewImages(
                                                                label.label
                                                        )
                                        )
                                    }
                                }
                        deferred.awaitAll()
                    }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = emptyList()
                    )

    fun onQueryChange(newQuery: String) {
        queryFlow.value = newQuery
    }
}
