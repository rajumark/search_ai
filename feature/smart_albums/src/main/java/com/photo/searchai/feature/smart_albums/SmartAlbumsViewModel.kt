package com.photo.searchai.feature.smart_albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photo.searchai.core.database.dao.ExifDao
import com.photo.searchai.core.database.dao.SmartAlbumDao
import com.photo.searchai.core.database.entity.SmartAlbumRuleEntity
import com.photo.searchai.core.media_index.MediaStoreIndexer
import com.photo.searchai.core.media_index.model.MediaItem
import com.photo.searchai.core.rules_engine.RulesEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SmartAlbumsState(
        val albums: List<SmartAlbumInfo> = emptyList(),
        val isLoading: Boolean = true
)

data class SmartAlbumInfo(
        val rule: SmartAlbumRuleEntity,
        val mediaCount: Int,
        val previewImage: MediaItem?
)

@HiltViewModel
class SmartAlbumsViewModel
@Inject
constructor(
        private val smartAlbumDao: SmartAlbumDao,
        private val rulesEngine: RulesEngine,
        private val mediaStoreIndexer: MediaStoreIndexer,
        private val exifDao: ExifDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmartAlbumsState())
    val uiState: StateFlow<SmartAlbumsState> = _uiState.asStateFlow()

    init {
        loadSmartAlbums()
    }

    private fun loadSmartAlbums() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Generate default rules if none exist
            val existingRules = smartAlbumDao.getAllRules()
            if (existingRules.isEmpty()) {
                createDefaultRules()
            }

            val rules = smartAlbumDao.getAllRules()
            val allMedia = mediaStoreIndexer.getAllMedia()

            val albumInfos =
                    rules.map { rule ->
                        val matchingMedia =
                                allMedia.filter { media ->
                                    val exif = exifDao.getExifForImage(media.id)
                                    rulesEngine.evaluate(rule, media, exif)
                                }
                        SmartAlbumInfo(
                                rule = rule,
                                mediaCount = matchingMedia.size,
                                previewImage = matchingMedia.firstOrNull()
                        )
                    }

            _uiState.value = SmartAlbumsState(albums = albumInfos, isLoading = false)
        }
    }

    private suspend fun createDefaultRules() {
        val defaultRules =
                listOf(
                        SmartAlbumRuleEntity(
                                name = "Screenshots",
                                ruleType = "SCREENSHOT",
                                configurationJson = "{}",
                                description = "Automatically group screenshots",
                                isEnabled = true
                        ),
                        SmartAlbumRuleEntity(
                                name = "Large Files (>10MB)",
                                ruleType = "LARGE_FILE",
                                configurationJson = (10 * 1024 * 1024L).toString(),
                                description = "Files larger than 10MB",
                                isEnabled = true
                        )
                )
        for (rule in defaultRules) {
            smartAlbumDao.insertRule(rule)
        }
    }
}
