package com.photo.searchai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchByTextScreen(
        onNavigateBack: () -> Unit,
        viewModel: SearchByTextViewModel = hiltViewModel()
) {
    val pagingItems = viewModel.imagesPagingData.collectAsLazyPagingItems()

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Search by Text") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                )
            }
    ) { paddingValues ->
        LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(
                    count = pagingItems.itemCount,
                    span = { index ->
                        val item = pagingItems[index]
                        if (item is SearchItem.Header) {
                            GridItemSpan(maxLineSpan)
                        } else {
                            GridItemSpan(1)
                        }
                    },
                    key = { index ->
                        val item = pagingItems[index]
                        when (item) {
                            is SearchItem.Header -> "header_${item.date}"
                            is SearchItem.Image -> item.entity.id
                            null -> "loading_$index"
                        }
                    }
            ) { index ->
                val item = pagingItems[index]
                if (item != null) {
                    when (item) {
                        is SearchItem.Header -> {
                            Text(
                                    text = item.date,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .padding(vertical = 8.dp, horizontal = 16.dp)
                            )
                        }
                        is SearchItem.Image -> {
                            AsyncImage(
                                    model = item.entity.uri,
                                    contentDescription = item.entity.name,
                                    modifier =
                                            Modifier.aspectRatio(1f)
                                                    .fillMaxWidth()
                                                    .background(
                                                            MaterialTheme.colorScheme.surfaceVariant
                                                    ),
                                    contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}
