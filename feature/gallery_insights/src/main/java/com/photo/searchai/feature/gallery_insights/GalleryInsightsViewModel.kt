package com.photo.searchai.feature.gallery_insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photo.searchai.core.database.dao.ExifDao
import com.photo.searchai.core.database.dao.ImageDao
import com.photo.searchai.core.media_index.MediaStoreIndexer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class GalleryInsightsState(
    val totalPhotos: Int = 0,
    val totalSizeMB: Long = 0,
    val topFolders: List<FolderStat> = emptyList(),
    val topCameras: List<CameraStat> = emptyList(),
    val mostActiveMonth: String = "N/A",
    val isLoading: Boolean = true
)

data class FolderStat(val name: String, val count: Int)
data class CameraStat(val model: String, val count: Int)

@HiltViewModel
class GalleryInsightsViewModel @Inject constructor(
    private val imageDao: ImageDao,
    private val exifDao: ExifDao,
    private val mediaStoreIndexer: MediaStoreIndexer
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryInsightsState())
    val uiState: StateFlow<GalleryInsightsState> = _uiState.asStateFlow()

    init {
        loadInsights()
    }

    private fun loadInsights() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val allMedia = mediaStoreIndexer.getAllMedia()
            val totalPhotos = allMedia.size
            val totalSize = allMedia.sumOf { it.size } / (1024 * 1024)

            val folderStats = allMedia.groupBy { 
                it.path.substringBeforeLast("/").substringAfterLast("/")
            }.map { FolderStat(it.key, it.value.size) }
             .sortedByDescending { it.count }
             .take(5)

            val allExif = exifDao.getAllExif() // Note: Need verify if this exists or add it
            val cameraStats = allExif.filter { it.model != null }
                .groupBy { it.model!! }
                .map { CameraStat(it.key, it.value.size) }
                .sortedByDescending { it.count }
                .take(3)

            // Calculate most active month
            val monthCounts = allMedia.groupBy {
                val date = Date(it.dateAdded * 1000)
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date)
            }.mapValues { it.value.size }
            
            val topMonth = monthCounts.maxByOrNull { it.value }?.key ?: "N/A"

            _uiState.value = GalleryInsightsState(
                totalPhotos = totalPhotos,
                totalSizeMB = totalSize,
                topFolders = folderStats,
                topCameras = cameraStats,
                mostActiveMonth = topMonth,
                isLoading = false
            )
        }
    }
}
