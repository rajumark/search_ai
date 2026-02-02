package com.photo.searchai.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photo.searchai.core.data.repository.MediaRepository
import com.photo.searchai.core.work.MediaWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel
@Inject
constructor(
        private val mediaRepository: MediaRepository,
        private val mediaWorkScheduler: MediaWorkScheduler
) : ViewModel() {

    val imageCount: StateFlow<Int> =
            mediaRepository
                    .getImageCountFlow()
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = 0
                    )

    val ocrCount: StateFlow<Int> =
            mediaRepository
                    .getOcrProcessedCountFlow()
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = 0
                    )

    init {
        // Run one-time sync and schedule periodic sync when ViewModel is initialized
        mediaWorkScheduler.runOneTimeSync()
        mediaWorkScheduler.schedulePeriodicSync()
    }

    fun refreshImages() {
        mediaWorkScheduler.runOneTimeSync()
    }
}
