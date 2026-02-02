package com.photo.searchai.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.photo.searchai.core.database.entity.SearchResultWithOcr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenImageViewer(
        results: List<SearchResultWithOcr>,
        startIndex: Int,
        onDismiss: () -> Unit,
        onDelete: (SearchResultWithOcr) -> Unit,
        onShareImage: (SearchResultWithOcr) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { results.size })
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTextSheet by remember { mutableStateOf(false) }
    var ocrText by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val item = results[page]
            ZoomableImage(uri = item.image.uri, contentDescription = item.image.name)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                    title = {
                        Text(
                                text = "${pagerState.currentPage + 1} / ${results.size}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        IconButton(onClick = { onShareImage(results[pagerState.currentPage]) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
            )

            Spacer(modifier = Modifier.weight(1f))

            val currentItem = results.getOrNull(pagerState.currentPage)
            if (currentItem?.ocrText?.isNotBlank() == true) {
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                            onClick = {
                                ocrText = currentItem.ocrText.orEmpty()
                                showTextSheet = true
                            }
                    ) {
                        Icon(
                                imageVector = Icons.Default.TextSnippet,
                                contentDescription = "Text"
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(text = "Text")
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete image?") },
                text = { Text("This will remove the photo from your device.") },
                confirmButton = {
                    TextButton(
                            onClick = {
                                showDeleteDialog = false
                                results.getOrNull(pagerState.currentPage)?.let(onDelete)
                            }
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                }
        )
    }

    if (showTextSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
                onDismissRequest = { showTextSheet = false },
                sheetState = sheetState
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                        text = "Recognized text",
                        style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                SelectionContainer {
                    Text(
                            text = ocrText,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Start
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(ocrText))
                            }
                    ) { Text("Copy") }
                    TextButton(
                            onClick = {
                                val intent =
                                        Intent(Intent.ACTION_SEND)
                                                .setType("text/plain")
                                                .putExtra(Intent.EXTRA_TEXT, ocrText)
                                context.startActivity(Intent.createChooser(intent, "Share text"))
                            }
                    ) { Text("Share") }
                    TextButton(onClick = { showTextSheet = false }) { Text("Close") }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ZoomableImage(
        uri: String,
        contentDescription: String?
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val transformState: TransformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = newScale
        val maxX = (newScale - 1f) * 600f
        val maxY = (newScale - 1f) * 900f
        offsetX = (offsetX + panChange.x).coerceIn(-maxX, maxX)
        offsetY = (offsetY + panChange.y).coerceIn(-maxY, maxY)
    }

    AsyncImage(
            model = uri,
            contentDescription = contentDescription,
            modifier =
                    Modifier.fillMaxSize()
                            .background(Color.Black)
                            .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                            )
                            .transformable(state = transformState),
            contentScale = ContentScale.Fit
    )
}
