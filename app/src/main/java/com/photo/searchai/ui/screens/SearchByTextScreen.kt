package com.photo.searchai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photo.searchai.ui.components.FullscreenImageViewer
import com.photo.searchai.ui.components.SearchResults

@ExperimentalFoundationApi
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchByTextScreen(
        onNavigateBack: () -> Unit,
        viewModel: SearchByTextViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showViewer by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isFavoriteQuery = uiState.query.trim().lowercase().startsWith("is favorite") ||
            uiState.query.trim().lowercase() == "favorite" ||
            uiState.query.trim().lowercase() == "favorite images"

    BackHandler(enabled = selectionMode || uiState.isActive) {
        when {
            selectionMode -> {
                selectionMode = false
                selectedIds.clear()
            }
            uiState.isActive -> viewModel.onActiveChange(false)
        }
    }

    Scaffold(
            topBar = {
                Column(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    if (uiState.bucketName.isNotBlank()) {
                        Text(
                                text = uiState.bucketName,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                    SearchBar(
                            modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                            query = uiState.query,
                            onQueryChange = viewModel::onQueryChange,
                            onSearch = { /* IME search action handled by debouncing */ },
                            active = uiState.isActive,
                            onActiveChange = viewModel::onActiveChange,
                            placeholder = {
                                Text(
                                        when {
                                            isFavoriteQuery -> "Favorite images"
                                            uiState.bucketName.isNotBlank() ->
                                                    "Search ${uiState.bucketName}"
                                            else -> "Search photos by text"
                                        }
                                )
                            },
                            leadingIcon = {
                                IconButton(
                                        onClick = {
                                            if (uiState.isActive) {
                                                viewModel.onActiveChange(false)
                                            } else {
                                                onNavigateBack()
                                            }
                                        }
                                ) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                            },
                            trailingIcon = {
                                if (uiState.query.isNotEmpty()) {
                                    IconButton(onClick = viewModel::onClearQuery) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }
                            }
                    ) {
                        Column(
                                modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                        ) {
                            if (isFavoriteQuery) {
                                Text(
                                        text = "Favorite images",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            if (uiState.suggestedChips.isNotEmpty()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                            text =
                                                    if (uiState.query.isEmpty()) "Recent searches"
                                                    else "Suggestions",
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        uiState.suggestedChips.forEach { chipText ->
                                            SuggestionChip(
                                                    onClick = { viewModel.onChipClick(chipText) },
                                                    label = { Text(chipText) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) { paddingValues ->
        Column(
                modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
        ) {
            if (uiState.query.isNotEmpty()) {
                Text(
                        text = "${uiState.resultsCount} matches",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            SearchResults(
                    results = uiState.results,
                    query = uiState.query,
                    selectionMode = selectionMode,
                    selectedIds = selectedIds.toSet(),
                    onItemClick = {
                        selectedIndex = it
                        showViewer = true
                    },
                    onItemLongPress = {
                        selectionMode = true
                        val id = uiState.results.getOrNull(it)?.image?.id ?: return@SearchResults
                        if (id !in selectedIds) selectedIds.add(id)
                    },
                    onToggleSelection = { id ->
                        if (id in selectedIds) {
                            selectedIds.remove(id)
                            if (selectedIds.isEmpty()) selectionMode = false
                        } else {
                            selectedIds.add(id)
                        }
                    },
                    onClearSelection = {
                        selectionMode = false
                        selectedIds.clear()
                    },
                    onShareSelected = {
                        val selectedItems = uiState.results.filter { it.image.id in selectedIds }
                        if (selectedItems.isNotEmpty()) {
                            val uris =
                                    selectedItems.map { Uri.parse(it.image.uri) }
                            val intent =
                                    Intent(Intent.ACTION_SEND_MULTIPLE)
                                            .setType("image/*")
                                            .putParcelableArrayListExtra(
                                                    Intent.EXTRA_STREAM,
                                                    ArrayList(uris)
                                            )
                                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            context.startActivity(Intent.createChooser(intent, "Share images"))
                        }
                    },
                    onDeleteSelected = { showDeleteDialog = true },
                    onFavoriteSelected = {
                        val selectedItems = uiState.results.filter { it.image.id in selectedIds }
                        selectedItems.forEach {
                            viewModel.setFavorite(it.image.id, !it.image.isFavorite)
                        }
                    }
            )
        }
    }

    if (showViewer && uiState.results.isNotEmpty()) {
        FullscreenImageViewer(
                results = uiState.results,
                startIndex = selectedIndex,
                onDismiss = { showViewer = false },
                onDelete = {
                    viewModel.deleteImage(it.image)
                    showViewer = false
                },
                onShareImage = { item ->
                    val intent =
                            Intent(Intent.ACTION_SEND)
                                    .setType("image/*")
                                    .putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(item.image.uri))
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(Intent.createChooser(intent, "Share image"))
                },
                onToggleFavorite = { imageId, isFavorite ->
                    viewModel.setFavorite(imageId, isFavorite)
                }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete selected photos?") },
                text = { Text("This will remove the selected photos from your device.") },
                confirmButton = {
                    TextButton(
                            onClick = {
                                showDeleteDialog = false
                                val images = uiState.results.filter { it.image.id in selectedIds }
                                viewModel.deleteImages(images.map { it.image })
                                selectedIds.clear()
                                selectionMode = false
                            }
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                }
        )
    }
}
