package com.photo.searchai.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.photo.searchai.core.database.entity.AlbumSummary

private enum class AlbumLayoutMode {
    List,
    Grid
}

@ExperimentalFoundationApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoFoldersScreen(
        onNavigateBack: () -> Unit,
        onAlbumClick: (AlbumSummary) -> Unit,
        viewModel: PhotoFoldersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var layoutMode by remember { mutableStateOf(AlbumLayoutMode.List) }

    Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                        title = { Text("Photo Folders") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                )
            }
    ) { paddingValues ->
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(paddingValues)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                        selected = layoutMode == AlbumLayoutMode.List,
                        onClick = { layoutMode = AlbumLayoutMode.List },
                        leadingIcon = {
                            Icon(Icons.Default.ViewList, contentDescription = null)
                        },
                        label = { Text("List") }
                )
                FilterChip(
                        selected = layoutMode == AlbumLayoutMode.Grid,
                        onClick = { layoutMode = AlbumLayoutMode.Grid },
                        leadingIcon = {
                            Icon(Icons.Default.GridView, contentDescription = null)
                        },
                        label = { Text("Grid") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (layoutMode == AlbumLayoutMode.List) {
                LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(items = uiState.albums, key = { it.bucketId }) { album ->
                        AlbumListItem(album = album, onClick = { onAlbumClick(album) })
                    }
                }
            } else {
                LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(items = uiState.albums, key = { it.bucketId }) { album ->
                        AlbumGridItem(album = album, onClick = { onAlbumClick(album) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumListItem(album: AlbumSummary, onClick: () -> Unit) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(onClick = onClick)
                            .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
                model = album.thumbnailUri,
                contentDescription = album.bucketName,
                modifier =
                        Modifier.size(72.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surface),
                contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = album.bucketName.ifBlank { "Unknown" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
            Text(
                    text = "${album.imageCount} photos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlbumGridItem(album: AlbumSummary, onClick: () -> Unit) {
    Column(
            modifier =
                    Modifier.fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(onClick = onClick)
                            .padding(8.dp)
    ) {
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(140.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surface)
        ) {
            AsyncImage(
                    model = album.thumbnailUri,
                    contentDescription = album.bucketName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
                text = album.bucketName.ifBlank { "Unknown" },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
        )
        Text(
                text = "${album.imageCount} photos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
