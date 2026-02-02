package com.photo.searchai.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

enum class HomeTab {
        Home,
        Menu
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
        onNavigateToSearch: () -> Unit,
        onNavigateToFavorites: () -> Unit,
        onNavigateToGrouping: () -> Unit,
        onNavigateToLabels: () -> Unit,
        viewModel: HomeViewModel = hiltViewModel()
) {
        var selectedTab by remember { mutableStateOf(HomeTab.Home) }

        Scaffold(
                topBar = {
                        TopAppBar(
                                title = {
                                        Text(
                                                text =
                                                        when (selectedTab) {
                                                                HomeTab.Home -> "Photo Search AI"
                                                                HomeTab.Menu -> "Menu"
                                                        }
                                        )
                                },
                                actions = {
                                        if (selectedTab == HomeTab.Home) {
                                                IconButton(
                                                        onClick = { viewModel.refreshImages() }
                                                ) {
                                                        Icon(
                                                                Icons.Default.Refresh,
                                                                contentDescription = "Refresh"
                                                        )
                                                }
                                        }
                                }
                        )
                },
                bottomBar = {
                        NavigationBar {
                                NavigationBarItem(
                                        selected = selectedTab == HomeTab.Home,
                                        onClick = { selectedTab = HomeTab.Home },
                                        label = { Text("Home") },
                                        icon = {
                                                Icon(
                                                        Icons.Default.Home,
                                                        contentDescription = "Home"
                                                )
                                        }
                                )
                                NavigationBarItem(
                                        selected = selectedTab == HomeTab.Menu,
                                        onClick = { selectedTab = HomeTab.Menu },
                                        label = { Text("Menu") },
                                        icon = {
                                                Icon(
                                                        Icons.Default.Menu,
                                                        contentDescription = "Menu"
                                                )
                                        }
                                )
                        }
                }
        ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        when (selectedTab) {
                                HomeTab.Home -> {
                                        HomeTabContent(viewModel)
                                }
                                HomeTab.Menu -> {
                                        MenuTabContent(
                                                onNavigateToSearch = onNavigateToSearch,
                                                onNavigateToFavorites = onNavigateToFavorites,
                                                onNavigateToGrouping = onNavigateToGrouping,
                                                onNavigateToLabels = onNavigateToLabels
                                        )
                                }
                        }
                }
        }
}

@Composable
private fun HomeTabContent(viewModel: HomeViewModel) {
        val imageCount by viewModel.imageCount.collectAsState()
        val ocrCount by viewModel.ocrCount.collectAsState()
        val labelingCount by viewModel.labelingCount.collectAsState()

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                                text = "Labels Indexed: $labelingCount",
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

@Composable
private fun MenuTabContent(
        onNavigateToSearch: () -> Unit,
        onNavigateToFavorites: () -> Unit,
        onNavigateToGrouping: () -> Unit,
        onNavigateToLabels: () -> Unit
) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                                onClick = onNavigateToSearch,
                                modifier = Modifier.padding(16.dp).fillMaxWidth(0.8f)
                        ) { Text("Search by text") }
                        Button(
                                onClick = onNavigateToGrouping,
                                modifier = Modifier.padding(16.dp).fillMaxWidth(0.8f)
                        ) { Text("Grouping by text") }
                        Button(
                                onClick = onNavigateToLabels,
                                modifier = Modifier.padding(16.dp).fillMaxWidth(0.8f)
                        ) { Text("Explore by labels") }
                        Button(
                                onClick = onNavigateToFavorites,
                                modifier = Modifier.padding(16.dp).fillMaxWidth(0.8f)
                        ) { Text("Favorite images") }
                }
        }
}
