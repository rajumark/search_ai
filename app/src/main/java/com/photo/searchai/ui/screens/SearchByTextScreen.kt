package com.photo.searchai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
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

@OptIn(ExperimentalMaterial3Api::class)
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
            ) { SearchResults(results = uiState.results) }

            if (!uiState.isActive) {
                SearchResults(results = uiState.results)
            }
        }
    }
}
