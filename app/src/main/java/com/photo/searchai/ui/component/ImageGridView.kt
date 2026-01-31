package com.photo.searchai.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import java.io.File

/**
 * Reusable Image Grid component that supports paging, multi-selection, and performance
 * optimizations.
 *
 * @param T The type of items in the paging list.
 * @param pagingItems The lazy paging items to display.
 * @param extractId Function to extract a unique ID (Long) from an item.
 * @param extractImagePath Function to extract the image path (String) from an item.
 * @param selectedItems Set of selected item IDs.
 * @param isInSelectionMode Whether the grid is in selection mode.
 * @param gridState The state of the lazy grid.
 * @param onItemClick Callback when an item is clicked (id, index).
 * @param onItemLongPress Callback when an item is long-pressed (id).
 */
@Composable
fun <T : Any> ImageGridView(
        pagingItems: LazyPagingItems<T>,
        extractId: (T) -> Long,
        extractImagePath: (T) -> String,
        selectedItems: Set<Long>,
        isInSelectionMode: Boolean,
        gridState: LazyGridState,
        onItemClick: (Long, Int) -> Unit,
        onItemLongPress: (Long) -> Unit
) {
    LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            contentPadding = PaddingValues(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize()
    ) {
        items(
                count = pagingItems.itemCount,
                // Use stable keys to prevent item recomposition
                key = { index ->
                    val item = pagingItems.peek(index)
                    if (item != null) extractId(item) else "placeholder_$index"
                }
        ) { index ->
            // Use peek instead of get to avoid triggering reloads
            val item = pagingItems[index]
            if (item != null) {
                val id = extractId(item)
                val path = extractImagePath(item)
                val isSelected = id in selectedItems

                PerformantImageGridItem(
                        id = id,
                        imagePath = path,
                        isSelected = isSelected,
                        isInSelectionMode = isInSelectionMode,
                        onClick = { onItemClick(id, index) },
                        onLongClick = { onItemLongPress(id) }
                )
            }
        }

        // Loading more indicator
        if (pagingItems.loadState.append is LoadState.Loading) {
            item {
                Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
            }
        }
    }
}

/** Highly performant image grid item with minimal recomposition. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PerformantImageGridItem(
        id: Long,
        imagePath: String,
        isSelected: Boolean,
        isInSelectionMode: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit
) {
    val context = LocalContext.current

    // Only animate scale when selection state changes
    val scale by
            animateFloatAsState(
                    targetValue = if (isSelected) 0.92f else 1f,
                    label = "selection_scale"
            )

    Box(
            modifier =
                    Modifier.aspectRatio(1f)
                            .scale(scale)
                            .clip(RoundedCornerShape(4.dp))
                            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        // Use SubcomposeAsyncImage for efficient loading with placeholder
        SubcomposeAsyncImage(
                model =
                        remember(imagePath) {
                            ImageRequest.Builder(context)
                                    .data(File(imagePath))
                                    // Use thumbnail size for grid - much faster loading
                                    .size(Size(300, 300))
                                    // Aggressive caching
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    // Use cache key based on path for stability
                                    .memoryCacheKey("thumb_$id")
                                    .diskCacheKey("thumb_$id")
                                    // Crossfade disabled for faster rendering
                                    .crossfade(false)
                                    .build()
                        },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    // Minimal placeholder - just a solid background
                    Box(
                            modifier =
                                    Modifier.fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                },
                error = {
                    Box(
                            modifier =
                                    Modifier.fillMaxSize()
                                            .background(MaterialTheme.colorScheme.errorContainer)
                    )
                }
        )

        // Selection overlay - only show when selected
        if (isSelected) {
            Box(
                    modifier =
                            Modifier.fillMaxSize()
                                    .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    )
            )
        }

        // Selection indicator - only visible in selection mode
        AnimatedVisibility(
                visible = isInSelectionMode,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
        ) {
            Box(
                    modifier =
                            Modifier.size(22.dp)
                                    .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else Color.White.copy(alpha = 0.7f),
                                            shape = CircleShape
                                    ),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        imageVector =
                                if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = if (isSelected) "Selected" else "Not selected",
                        tint = if (isSelected) Color.White else Color.Gray,
                        modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
