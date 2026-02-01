package com.photo.searchai.ui.search

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.photo.searchai.ui.component.ImageGridView

/**
 * SearchByText screen with gallery-like features. Features:
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
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                pagingItems.loadState.refresh is LoadState.Loading -> {
                    // Initial loading
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                pagingItems.loadState.refresh is LoadState.Error -> {
                    // Error state
                    val error = (pagingItems.loadState.refresh as LoadState.Error).error
                    Text(
                            text = "Error: ${error.localizedMessage}",
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                    )
                }
                pagingItems.itemCount == 0 &&
                        pagingItems.loadState.refresh !is LoadState.Loading -> {
                    // Empty state
                    EmptyState(
                            query = uiState.searchQuery,
                            modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    // Grid content
                    // Grid content
                    ImageGridView(
                            pagingItems = pagingItems,
                            extractId = { it.mediaStoreId },
                            extractImagePath = { it.imagePath },
                            selectedItems = uiState.selectedImages,
                            isInSelectionMode = uiState.isInSelectionMode,
                            gridState = gridState,
                            onItemClick = { mediaStoreId, index ->
                                viewModel.onImageClicked(mediaStoreId, index)
                            },
                            onItemLongPress = { mediaStoreId ->
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
    androidx.compose.foundation.layout.Column(modifier = Modifier.animateContentSize()) {
        SearchBar(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { onActiveChange(false) },
                active = false, // We don't expand, just use as a text field
                onActiveChange = {},
                modifier =
                        Modifier.fillMaxWidth()
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
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                        }
                    } else {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    }
                },
                colors =
                        SearchBarDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                shape = RoundedCornerShape(28.dp)
        ) {}

        SuggestionsChips(suggestions = suggestions, onSuggestionClick = onSuggestionClick)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestionsChips(suggestions: List<String>, onSuggestionClick: (String) -> Unit) {
    if (suggestions.isEmpty()) return

    androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear selection")
                }
            },
            actions = {
                IconButton(onClick = onSelectAll) {
                    Icon(imageVector = Icons.Default.SelectAll, contentDescription = "Select all")
                }
                IconButton(onClick = onShare) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
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
            colors =
                    TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EmptyState(query: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        if (query.isEmpty()) {
            Text(
                    text =
                            "No images with text found.\nImages will appear here once OCR processing is complete.",
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
