package com.photo.searchai.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.photo.searchai.ui.component.ImageGridView

/** Face Search screen - Coming Soon placeholder. Will allow users to search photos by faces. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceSearchScreen(
        viewModel: FaceSearchViewModel,
        onNavigateBack: () -> Unit,
        onNavigateToFullScreen: (Long, Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagingItems = viewModel.facePagingData.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FaceSearchEvent.NavigateToFullScreen -> {
                    onNavigateToFullScreen(event.mediaStoreId, event.index)
                }
            }
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Face Search") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.toggleDisplayMode() }) {
                                Icon(
                                        imageVector =
                                                if (uiState.displayMode ==
                                                                FaceDisplayMode.CROPPED_FACE
                                                )
                                                        Icons.Default.Image
                                                else Icons.Default.CropFree,
                                        contentDescription = "Toggle View Mode"
                                )
                            }
                        },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                )
                )
            }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                pagingItems.loadState.refresh is LoadState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                pagingItems.itemCount == 0 &&
                        pagingItems.loadState.refresh !is LoadState.Loading -> {
                    // Empty state
                    Box(
                            modifier = Modifier.align(Alignment.Center).padding(32.dp),
                            contentAlignment = Alignment.Center
                    ) {
                        Text(
                                text = "No faces detected yet.\nProcessing images...",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    ImageGridView(
                            pagingItems = pagingItems,
                            extractId = { it.face.id },
                            extractImagePath = {
                                if (uiState.displayMode == FaceDisplayMode.CROPPED_FACE)
                                        it.face.croppedFacePath
                                else it.originalImagePath
                            },
                            selectedItems = uiState.selectedFaces,
                            isInSelectionMode = uiState.isInSelectionMode,
                            gridState = gridState,
                            onItemClick = { _, index ->
                                // We pass mediaStoreId for full screen navigation
                                val item = pagingItems[index]
                                if (item != null) {
                                    viewModel.onImageClicked(item.face.mediaStoreId, index)
                                }
                            },
                            onItemLongPress = { _ ->
                                // viewModel.onItemLongPressed(it) // Not fully implemented yet
                            }
                    )
                }
            }
        }
    }
}
