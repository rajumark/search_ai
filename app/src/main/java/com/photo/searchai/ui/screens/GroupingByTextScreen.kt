package com.photo.searchai.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.photo.searchai.core.database.entity.GroupEntity
import com.photo.searchai.core.database.entity.ImageEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupingByTextScreen(
        onNavigateBack: () -> Unit,
        onGroupClick: (String) -> Unit, // Pass query string
        viewModel: GroupingViewModel = hiltViewModel()
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Smart Collections") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                )
            }
    ) { paddingValues ->
        if (groups.isEmpty()) {
            Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                        "No collections found yet. Try adding images with text.",
                        style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(groups) { uiModel ->
                    GroupCard(
                            group = uiModel.group,
                            previews = uiModel.previews,
                            onClick = { onGroupClick(uiModel.group.groupKey.replace(" • ", " ")) }
                    )
                }
            }
        }
    }
}

@Composable
fun GroupCard(group: GroupEntity, previews: List<ImageEntity>, onClick: () -> Unit) {
    Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title and Subtitle
            Text(
                    text = group.groupKey,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                    text = "${group.imageCount} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Preview Image Grid (Row of up to 4 images)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                previews.take(4).forEach { image ->
                    AsyncImage(
                            model = image.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}
