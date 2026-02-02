package com.photo.searchai.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

@Composable
fun SearchResults(
        results: List<SearchResultWithOcr>,
        query: String = "",
        modifier: Modifier = Modifier
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
        LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items = results, key = { it.image.id }) { item ->
                    AsyncImage(
                            model = item.image.uri,
                            contentDescription = item.image.name,
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
