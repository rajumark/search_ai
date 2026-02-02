package com.photo.searchai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.photo.searchai.core.database.entity.ImageEntity

@Composable
fun SearchResults(results: List<ImageEntity>, modifier: Modifier = Modifier) {
    if (results.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(
                    text = "No photos found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items = results, key = { it.id }) { image ->
                AsyncImage(
                        model = image.uri,
                        contentDescription = image.name,
                        modifier =
                                Modifier.aspectRatio(1f)
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                )
            }
        }
    }
}
