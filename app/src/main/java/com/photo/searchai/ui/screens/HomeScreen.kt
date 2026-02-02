package com.photo.searchai.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
        val imageCount by viewModel.imageCount.collectAsState()
        val ocrCount by viewModel.ocrCount.collectAsState()

        Scaffold(
                topBar = {
                        TopAppBar(
                                title = { Text("Photo Search AI") },
                                actions = {
                                        IconButton(onClick = { viewModel.refreshImages() }) {
                                                Icon(
                                                        Icons.Default.Refresh,
                                                        contentDescription = "Refresh"
                                                )
                                        }
                                }
                        )
                }
        ) { paddingValues ->
                Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                        text = "Total Images Found",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                        text = "$imageCount",
                                        style =
                                                MaterialTheme.typography.displayLarge.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 72.sp
                                                ),
                                        color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.padding(16.dp))
                                Text(
                                        text = "OCR Indexed: $ocrCount",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.padding(8.dp))
                                Text(
                                        text = "Your library is being indexed in background",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                )
                        }
                }
        }
}
