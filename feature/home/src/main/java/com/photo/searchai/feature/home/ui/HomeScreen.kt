package com.photo.searchai.feature.home.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.photo.searchai.domain.model.ProcessingSnapshot
import com.photo.searchai.feature.home.HomeViewModel

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val ocrState by viewModel.ocrProgress.collectAsState()
    val labelingState by viewModel.labelingProgress.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Background Processing", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        ProcessingCard("OCR Feature", ocrState)
        Spacer(modifier = Modifier.height(8.dp))
        ProcessingCard("Labeling Feature", labelingState)
    }
}

@Composable
fun ProcessingCard(title: String, snapshot: ProcessingSnapshot?) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)

            if (snapshot == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Initializing Snapshot...", style = MaterialTheme.typography.bodySmall)
            } else {
                val progress =
                        if (snapshot.totalPending > 0) {
                            snapshot.processedCount.toFloat() / snapshot.totalPending.toFloat()
                        } else 1f

                LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${(progress * 100).toInt()}%")
                    Text("${snapshot.processedCount} / ${snapshot.totalPending}")
                }
            }
        }
    }
}
