package com.photo.searchai.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.photo.searchai.core.data.repository.MediaRepository
import com.photo.searchai.core.database.entity.ImageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed class SearchItem {
    data class Image(val entity: ImageEntity) : SearchItem()
    data class Header(val date: String) : SearchItem()
}

@HiltViewModel
class SearchByTextViewModel @Inject constructor(private val mediaRepository: MediaRepository) :
        ViewModel() {

    private val dateFormatter = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

    val imagesPagingData: Flow<PagingData<SearchItem>> =
            mediaRepository
                    .getAllImagesPager()
                    .map { pagingData -> pagingData.map { SearchItem.Image(it) as SearchItem } }
                    .map { pagingData ->
                        pagingData.insertSeparators { before, after ->
                            if (after == null) return@insertSeparators null

                            val currentImage =
                                    (after as? SearchItem.Image)?.entity
                                            ?: return@insertSeparators null
                            val currentDate =
                                    dateFormatter.format(Date(currentImage.dateAdded * 1000L))

                            if (before == null) {
                                return@insertSeparators SearchItem.Header(currentDate)
                            }

                            val prevImage =
                                    (before as? SearchItem.Image)?.entity
                                            ?: return@insertSeparators null
                            val prevDate = dateFormatter.format(Date(prevImage.dateAdded * 1000L))

                            if (currentDate != prevDate) {
                                SearchItem.Header(currentDate)
                            } else {
                                null
                            }
                        }
                    }
                    .cachedIn(viewModelScope)
}
