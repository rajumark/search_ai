package com.photo.searchai.ui.screens

import androidx.activity.compose.BackHandler
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photo.searchai.ui.components.SearchResults
import com.photo.searchai.ui.components.FullscreenImageViewer

@ExperimentalFoundationApi
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchByLabelsScreen(
        onNavigateBack: () -> Unit,
        viewModel: SearchByLabelsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showViewer by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.selectedLabels) {
        if (uiState.selectedLabels.isEmpty()) {
            onNavigateBack()
        }
    }

    BackHandler { onNavigateBack() }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Search by labels") },
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
                                .padding(horizontal = 16.dp)
        ) {
            if (uiState.selectedLabels.isNotEmpty()) {
                Text(
                        text = "Selected",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.selectedLabels.forEach { label ->
                        AssistChip(
                                onClick = { viewModel.removeLabel(label) },
                                label = { Text(label) },
                                leadingIcon = {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Remove")
                                }
                        )
                    }
                }
            }

            if (uiState.relatedLabels.isNotEmpty()) {
                Text(
                        text = "Related",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                val relatedColumns = uiState.relatedLabels.chunked(2)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(relatedColumns) { columnLabels ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            columnLabels.forEach { label ->
                                FilterChip(
                                        selected = false,
                                        onClick = { viewModel.addLabel(label) },
                                        label = { Text(label) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Add, contentDescription = "Add")
                                        }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SearchResults(
                    results = uiState.results,
                    query = uiState.selectedLabels.joinToString(" "),
                    modifier = Modifier.fillMaxSize(),
                    onItemClick = {
                        selectedIndex = it
                        showViewer = true
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
}
