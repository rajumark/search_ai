package com.photo.searchai.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.photo.searchai.core.database.entity.SearchResultWithOcr

@ExperimentalFoundationApi
@Composable
fun SearchResults(
        results: List<SearchResultWithOcr>,
        query: String = "",
        modifier: Modifier = Modifier,
        selectionMode: Boolean = false,
        selectedIds: Set<Long> = emptySet(),
        onItemClick: (Int) -> Unit = {},
        onItemLongPress: (Int) -> Unit = {},
        onToggleSelection: (Long) -> Unit = {},
        onClearSelection: () -> Unit = {},
        onShareSelected: () -> Unit = {},
        onDeleteSelected: () -> Unit = {},
        onFavoriteSelected: () -> Unit = {}
) {
//    /android
    if (results.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(
                    text =
                            if (query.isEmpty()) "No photos found"
                            else "No photos found for \"$query\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            AnimatedVisibility(
                    visible = selectionMode,
                    enter = fadeIn() + slideInVertically { -it / 2 },
                    exit = fadeOut() + slideOutVertically { -it / 2 }
            ) {
                Surface(shadowElevation = 2.dp) {
                    Row(
                            modifier =
                                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onClearSelection) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }
                            Text(
                                    text = "${selectedIds.size} selected",
                                    style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = onFavoriteSelected) {
                                Icon(Icons.Default.Favorite, contentDescription = "Favorite")
                            }
                            IconButton(onClick = onShareSelected) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                            IconButton(onClick = onDeleteSelected) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }

            LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(items = results, key = { _, item -> item.image.id }) { index, item ->
                    val isSelected = item.image.id in selectedIds
                    Box(
                            modifier =
                                    Modifier.aspectRatio(1f)
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .combinedClickable(
                                                    onClick = {
                                                        if (selectionMode) {
                                                            onToggleSelection(item.image.id)
                                                        } else {
                                                            onItemClick(index)
                                                        }
                                                    },
                                                    onLongClick = { onItemLongPress(index) }
                                            )
                    ) {
                        AsyncImage(
                                model = item.image.uri,
                                contentDescription = item.image.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                        )

                        if (item.image.isFavorite) {
                            Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Favorite",
                                    tint = Color.White,
                                    modifier =
                                            Modifier.align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .size(18.dp)
                                            )
                        }

                        if (selectionMode) {
                            Box(
                                    modifier =
                                            Modifier.matchParentSize()
                                                    .background(
                                                            if (isSelected) {
                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                                            } else {
                                                                Color.Black.copy(alpha = 0.2f)
                                                            }
                                                    )
                            )
                            Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint =
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                Color.White.copy(alpha = 0.6f)
                                            },
                                    modifier =
                                            Modifier.align(Alignment.TopStart)
                                                    .padding(6.dp)
                                                    .size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun highlightMatches(text: String, query: String): AnnotatedString {
    val tokens = query.split(Regex("\\s+")).filter { it.length > 1 }
    if (tokens.isEmpty()) return AnnotatedString(text)

    // Find a snippet that contains at least one token
    val firstMatchIndex =
            tokens.map { text.indexOf(it, ignoreCase = true) }.filter { it >= 0 }.minOrNull() ?: 0

    val start = (firstMatchIndex - 20).coerceAtLeast(0)
    val end = (start + 100).coerceAtMost(text.length)
    val snippet = text.substring(start, end)

    return buildAnnotatedString {
        if (start > 0) append("...")
        val snippetLower = snippet.lowercase()
        var lastAddedIndex = 0

        // This is a simple highlighting. For production, use better regex or multiple passes.
        val matches = mutableListOf<Pair<Int, Int>>()
        tokens.forEach { token ->
            var index = snippetLower.indexOf(token.lowercase())
            while (index >= 0) {
                matches.add(index to index + token.length)
                index = snippetLower.indexOf(token.lowercase(), index + token.length)
            }
        }

        matches.sortBy { it.first }

        // Merge overlapping matches
        val mergedMatches = mutableListOf<Pair<Int, Int>>()
        if (matches.isNotEmpty()) {
            var current = matches[0]
            for (i in 1 until matches.size) {
                val next = matches[i]
                if (next.first <= current.second) {
                    current = current.first to maxOf(current.second, next.second)
                } else {
                    mergedMatches.add(current)
                    current = next
                }
            }
            mergedMatches.add(current)
        }

        mergedMatches.forEach { (mStart, mEnd) ->
            append(snippet.substring(lastAddedIndex, mStart))
            withStyle(
                    style =
                            SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Unspecified,
                                    background = Color.Yellow.copy(alpha = 0.4f)
                            )
            ) { append(snippet.substring(mStart, mEnd)) }
            lastAddedIndex = mEnd
        }
        append(snippet.substring(lastAddedIndex))
        if (end < text.length) append("...")
    }
}
