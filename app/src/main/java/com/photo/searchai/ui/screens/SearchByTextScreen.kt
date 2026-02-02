package com.photo.searchai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photo.searchai.ui.components.SearchResults

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchByTextScreen(
        onNavigateBack: () -> Unit,
        viewModel: SearchByTextViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.isActive) { viewModel.onActiveChange(false) }

    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            SearchBar(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .padding(horizontal = if (uiState.isActive) 0.dp else 16.dp),
                    query = uiState.query,
                    onQueryChange = viewModel::onQueryChange,
                    onSearch = { /* IME search action handled by debouncing */},
                    active = uiState.isActive,
                    onActiveChange = viewModel::onActiveChange,
                    placeholder = { Text("Search photos by text") },
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
                Column(modifier = Modifier.fillMaxSize()) {
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

                    if (uiState.query.isNotEmpty()) {
                        Text(
                                text = "${uiState.resultsCount} matches",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    SearchResults(results = uiState.results, query = uiState.query)
                }
            }

            if (!uiState.isActive) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (uiState.query.isNotEmpty()) {
                        Text(
                                text = "${uiState.resultsCount} matches",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    SearchResults(results = uiState.results, query = uiState.query)
                }
            }
        }
    }
}
