package com.photo.searchai.feature.media_vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photo.searchai.core.database.dao.ImageDao
import com.photo.searchai.core.database.dao.VaultDao
import com.photo.searchai.core.database.entity.VaultEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MediaVaultState(
        val vaultItems: List<VaultEntity> = emptyList(),
        val isLocked: Boolean = true,
        val isLoading: Boolean = false
)

@HiltViewModel
class MediaVaultViewModel
@Inject
constructor(private val vaultDao: VaultDao, private val imageDao: ImageDao) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaVaultState())
    val uiState: StateFlow<MediaVaultState> = _uiState.asStateFlow()

    init {
        // Vault items should only be loaded after unlock
    }

    fun unlock(pin: String) {
        if (pin == "1234") { // Simplified for demo
            loadVaultItems()
        }
    }

    private fun loadVaultItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val items = vaultDao.getAllVaultEntries()
            _uiState.value =
                    MediaVaultState(vaultItems = items, isLocked = false, isLoading = false)
        }
    }

    fun addToVault(mediaStoreId: Long, originalPath: String) {
        viewModelScope.launch {
            val vaultEntry =
                    VaultEntity(
                            mediaStoreId = mediaStoreId,
                            originalPath = originalPath,
                            vaultPath = "/vault/$mediaStoreId.hidden" // Simulated
                    )
            vaultDao.insertVaultEntry(vaultEntry)
            // Mark as hidden in MediaStore or delete from MediaStore list
            loadVaultItems()
        }
    }
}
