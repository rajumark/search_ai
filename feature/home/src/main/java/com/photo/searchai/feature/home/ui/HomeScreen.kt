package com.photo.searchai.feature.home.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.photo.searchai.domain.model.ProcessingSnapshot
import com.photo.searchai.feature.home.HomeViewModel

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val ocrState by viewModel.ocrProgress.collectAsState()
    val labelingState by viewModel.labelingProgress.collectAsState()
    val totalImages by viewModel.totalImages.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Background Processing", style = MaterialTheme.typography.headlineMedium)
                Text(
                        text = "$totalImages images in DB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                )
            }

            val context = LocalContext.current
            IconButton(
                    onClick = {
                        val intent =
                                Intent(
                                        Intent.ACTION_DELETE,
                                        Uri.parse("package:" + context.packageName)
                                )
                        context.startActivity(intent)
                    }
            ) {
                Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Uninstall App",
                        tint = MaterialTheme.colorScheme.error
                )
            }
        }
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
