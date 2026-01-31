package com.photo.searchai.ui.search

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import java.io.File

/**
 * SearchByText screen with gallery-like features.
 * Features:
 * - Material3 SearchBar for filtering images by OCR text
 * - 3×3 grid of images loaded from the database with paging
 * - Multi-select functionality
 * - Action icons for delete, share, and favorites
 * 
 * Performance optimizations:
 * - Stable keys for grid items prevent unnecessary recomposition
 * - Aggressive image caching with Coil
 * - Optimized thumbnail loading with size hints
 * - collectAsStateWithLifecycle for proper lifecycle handling
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchByTextScreen(
    viewModel: SearchViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToFullScreen: (Long, Int) -> Unit
) {
    val context = LocalContext.current
    // Use collectAsStateWithLifecycle for better lifecycle handling and stability
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagingItems = viewModel.searchResults.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()
    
    // Keep track of all loaded items for select all functionality
    // Use remember with stable reference to prevent unnecessary recomposition
    val allLoadedIds = remember { mutableStateListOf<Long>() }
    
    // Update the list of all loaded IDs when paging items change
    // Only update when item count actually changes
    LaunchedEffect(pagingItems.itemCount) {
        val newIds = mutableListOf<Long>()
        for (i in 0 until pagingItems.itemCount) {
            pagingItems.peek(i)?.let { newIds.add(it.mediaStoreId) }
        }
        if (newIds != allLoadedIds.toList()) {
            allLoadedIds.clear()
            allLoadedIds.addAll(newIds)
        }
    }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SearchEvent.NavigateToFullScreen -> {
                    onNavigateToFullScreen(event.mediaStoreId, event.index)
                }
                is SearchEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                SearchEvent.ClearSelection -> {
                    viewModel.clearSelection()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            if (uiState.isInSelectionMode) {
                // Selection mode toolbar
                SelectionToolbar(
                    selectedCount = uiState.selectedImages.size,
                    onClearSelection = { viewModel.clearSelection() },
                    onSelectAll = { viewModel.selectAll(allLoadedIds.toList()) },
                    onDelete = { viewModel.deleteSelectedImages() },
                    onShare = { viewModel.shareSelectedImages() },
                    onFavorite = { viewModel.addToFavorites() }
                )
            } else {
                // Normal mode - Search bar
                SearchTopBar(
                    query = uiState.searchQuery,
                    isActive = uiState.isSearchActive,
                    suggestions = uiState.suggestions,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onActiveChange = { viewModel.onSearchActiveChanged(it) },
                    onClear = { viewModel.clearSearch() },
                    onNavigateBack = onNavigateBack,
                    onSuggestionClick = { viewModel.onSuggestionClicked(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                pagingItems.loadState.refresh is LoadState.Loading -> {
                    // Initial loading
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                pagingItems.loadState.refresh is LoadState.Error -> {
                    // Error state
                    val error = (pagingItems.loadState.refresh as LoadState.Error).error
                    Text(
                        text = "Error: ${error.localizedMessage}",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
                pagingItems.itemCount == 0 && pagingItems.loadState.refresh !is LoadState.Loading -> {
                    // Empty state
                    EmptyState(
                        query = uiState.searchQuery,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    // Grid content
                    ImageGrid(
                        pagingItems = pagingItems,
                        selectedImages = uiState.selectedImages,
                        isInSelectionMode = uiState.isInSelectionMode,
                        gridState = gridState,
                        onImageClick = { mediaStoreId, index ->
                            viewModel.onImageClicked(mediaStoreId, index)
                        },
                        onImageLongPress = { mediaStoreId ->
                            viewModel.onImageLongPressed(mediaStoreId)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    query: String,
    isActive: Boolean,
    suggestions: List<String>,
    onQueryChange: (String) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onClear: () -> Unit,
    onNavigateBack: () -> Unit,
    onSuggestionClick: (String) -> Unit
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.animateContentSize()
    ) {
        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = { onActiveChange(false) },
            active = false, // We don't expand, just use as a text field
            onActiveChange = { },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isActive) 0.dp else 16.dp)
                .padding(top = 8.dp),
            placeholder = { Text("Search by text in images...") },
            leadingIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear"
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                }
            },
            colors = SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(28.dp)
        ) { }

        SuggestionsChips(
            suggestions = suggestions,
            onSuggestionClick = onSuggestionClick
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestionsChips(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    if (suggestions.isEmpty()) return

    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { suggestion ->
            androidx.compose.material3.AssistChip(
                onClick = { onSuggestionClick(suggestion) },
                label = { Text(suggestion) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionToolbar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onFavorite: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectedCount selected") },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear selection"
                )
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(
                    imageVector = Icons.Default.SelectAll,
                    contentDescription = "Select all"
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share"
                )
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Add to favorites"
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageGrid(
    pagingItems: LazyPagingItems<ImageWithText>,
    selectedImages: Set<Long>,
    isInSelectionMode: Boolean,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onImageClick: (Long, Int) -> Unit,
    onImageLongPress: (Long) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            count = pagingItems.itemCount,
            // Use stable keys to prevent item recomposition
            key = { index -> 
                pagingItems.peek(index)?.mediaStoreId ?: "placeholder_$index"
            }
        ) { index ->
            // Use peek instead of get to avoid triggering reloads
            val item = pagingItems[index]
            if (item != null) {
                val isSelected = item.mediaStoreId in selectedImages
                PerformantImageGridItem(
                    mediaStoreId = item.mediaStoreId,
                    imagePath = item.imagePath,
                    isSelected = isSelected,
                    isInSelectionMode = isInSelectionMode,
                    onClick = { onImageClick(item.mediaStoreId, index) },
                    onLongClick = { onImageLongPress(item.mediaStoreId) }
                )
            }
        }

        // Loading more indicator
        if (pagingItems.loadState.append is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

/**
 * Highly performant image grid item with minimal recomposition.
 * Uses:
 * - SubcomposeAsyncImage for efficient loading
 * - Aggressive caching policies
 * - Minimal UI layers for faster rendering
 * - No gradient overlays (removed for performance)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PerformantImageGridItem(
    mediaStoreId: Long,
    imagePath: String,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    
    // Only animate scale when selection state changes
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.92f else 1f,
        label = "selection_scale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(4.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // Use SubcomposeAsyncImage for efficient loading with placeholder
        SubcomposeAsyncImage(
            model = remember(imagePath) {
                ImageRequest.Builder(context)
                    .data(File(imagePath))
                    // Use thumbnail size for grid - much faster loading
                    .size(Size(300, 300))
                    // Aggressive caching
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    // Use cache key based on path for stability
                    .memoryCacheKey("thumb_$mediaStoreId")
                    .diskCacheKey("thumb_$mediaStoreId")
                    // Crossfade disabled for faster rendering
                    .crossfade(false)
                    .build()
            },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                // Minimal placeholder - just a solid background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer)
                )
            }
        )

        // Selection overlay - only show when selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            )
        }

        // Selection indicator - only visible in selection mode
        AnimatedVisibility(
            visible = isInSelectionMode,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.White.copy(alpha = 0.7f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (isSelected) "Selected" else "Not selected",
                    tint = if (isSelected) Color.White else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    query: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        if (query.isEmpty()) {
            Text(
                text = "No images with text found.\nImages will appear here once OCR processing is complete.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = "No images found matching \"$query\"",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
