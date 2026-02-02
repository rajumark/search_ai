package com.photo.searchai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photo.searchai.ui.components.SearchResults

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchByLabelsScreen(
        onNavigateBack: () -> Unit,
        viewModel: SearchByLabelsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                                label = { Text(label) }
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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.relatedLabels.forEach { label ->
                        FilterChip(
                                selected = false,
                                onClick = { viewModel.addLabel(label) },
                                label = { Text(label) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SearchResults(
                    results = uiState.results,
                    query = uiState.selectedLabels.joinToString(" "),
                    modifier = Modifier.fillMaxSize()
            )
        }
    }
}
