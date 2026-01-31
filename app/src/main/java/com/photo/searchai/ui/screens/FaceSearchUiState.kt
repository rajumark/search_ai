package com.photo.searchai.ui.screens

import com.photo.searchai.data.local.entity.FaceEntity

enum class FaceDisplayMode {
    FULL_IMAGE,
    CROPPED_FACE
}

data class FaceSearchUiState(
        val displayMode: FaceDisplayMode = FaceDisplayMode.CROPPED_FACE,
        val selectedFaces: Set<Long> = emptySet(),
        val isInSelectionMode: Boolean = false,
        val isLoading: Boolean = false
)

data class FaceWithImage(val face: FaceEntity, val originalImagePath: String)
