package com.photo.searchai.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photo.searchai.data.local.entity.WorkerStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshHistoryScreen(
        viewModel: RefreshHistoryViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Refresh History") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Back"
                                )
                            }
                        },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                )
                )
            }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Schedule Status Card
                item {
                    ScheduleStatusCard(
                            isScheduled = uiState.isPeriodicScheduled,
                            nextScheduledTime = uiState.nextScheduledTime,
                            totalRuns = uiState.totalRuns,
                            successfulRuns = uiState.successfulRuns,
                            onSchedule = viewModel::schedulePeriodicProcessing,
                            onCancel = viewModel::cancelPeriodicProcessing,
                            onTriggerNow = viewModel::triggerManualRefresh,
                            onReschedule = viewModel::rescheduleWork
                    )
                }

                // History Section Header
                item {
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                                text = "PROCESSING HISTORY",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing =
                                        MaterialTheme.typography.labelMedium.letterSpacing * 1.5f
                        )
                    }
                }

                // History Items
                if (uiState.historyItems.isEmpty()) {
                    item { EmptyHistoryCard() }
                } else {
                    items(items = uiState.historyItems, key = { it.id }) { item ->
                        HistoryItemCard(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleStatusCard(
        isScheduled: Boolean,
        nextScheduledTime: Long,
        totalRuns: Int,
        successfulRuns: Int,
        onSchedule: () -> Unit,
        onCancel: () -> Unit,
        onTriggerNow: () -> Unit,
        onReschedule: () -> Unit
) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
            shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                        color =
                                if (isScheduled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint =
                                        if (isScheduled) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            text = "Background Processing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                            text = if (isScheduled) "Scheduled every 6 hours" else "Not scheduled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                        color =
                                if (isScheduled) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                            text = if (isScheduled) "ACTIVE" else "INACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color =
                                    if (isScheduled) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (nextScheduledTime > 0 && isScheduled) {
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                                imageVector = Icons.Rounded.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                                text = "Next run: ${formatNextRunTime(nextScheduledTime)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Stats Row
            if (totalRuns > 0) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatBadge(
                            label = "Total Runs",
                            value = "$totalRuns",
                            color = MaterialTheme.colorScheme.primary
                    )
                    StatBadge(
                            label = "Successful",
                            value = "$successfulRuns",
                            color = MaterialTheme.colorScheme.tertiary
                    )
                    StatBadge(
                            label = "Success Rate",
                            value =
                                    if (totalRuns > 0) "${(successfulRuns * 100 / totalRuns)}%"
                                    else "—",
                            color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isScheduled) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop")
                    }

                    FilledTonalButton(onClick = onTriggerNow, modifier = Modifier.weight(1f)) {
                        Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Run Now")
                    }
                } else {
                    Button(onClick = onSchedule, modifier = Modifier.fillMaxWidth()) {
                        Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enable 6-Hour Schedule")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
        )
        Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HistoryItemCard(item: WorkerHistoryItem) {
    val statusColor by
            animateColorAsState(
                    targetValue =
                            when (item.status) {
                                WorkerStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                                WorkerStatus.RUNNING -> MaterialTheme.colorScheme.tertiary
                                WorkerStatus.FAILED -> MaterialTheme.colorScheme.error
                                WorkerStatus.CANCELLED -> MaterialTheme.colorScheme.outline
                            },
                    animationSpec = tween(300),
                    label = "statusColor"
            )

    val statusIcon =
            when (item.status) {
                WorkerStatus.COMPLETED -> Icons.Rounded.Check
                WorkerStatus.RUNNING -> Icons.Rounded.Sync
                WorkerStatus.FAILED -> Icons.Rounded.Error
                WorkerStatus.CANCELLED -> Icons.Rounded.Cancel
            }

    Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
            shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Header Row
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (item.status == WorkerStatus.RUNNING) {
                            CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = statusColor
                            )
                        } else {
                            Icon(
                                    imageVector = statusIcon,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            text = item.formattedDate,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                            text = item.formattedTimeRange,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                            color = statusColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                                text = item.statusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (item.isScheduledRun) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                                text = "Scheduled",
                                style = MaterialTheme.typography.labelSmall,
                                color =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.7f
                                        )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Row
            Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HistoryStatItem(
                            icon = Icons.Rounded.Timer,
                            label = "Duration",
                            value = item.formattedDuration
                    )
                    HistoryStatItem(
                            icon = Icons.Rounded.Refresh,
                            label = "Processed",
                            value = "${item.totalImagesProcessed}"
                    )
                }
            }

            // Processing breakdown
            if (item.totalImagesProcessed > 0) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProcessingChip(label = "OCR", count = item.imagesProcessedOcr)
                    ProcessingChip(label = "Barcode", count = item.imagesProcessedBarcode)
                    ProcessingChip(label = "Label", count = item.imagesProcessedLabel)
                }
            }

            // Error message
            AnimatedVisibility(
                    visible = item.errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
            ) {
                item.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryStatItem(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProcessingChip(label: String, count: Int) {
    Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp)
    ) {
        Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun EmptyHistoryCard() {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
            shape = RoundedCornerShape(16.dp)
    ) {
        Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                    text = "No Processing History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                    text =
                            "Background processing runs will appear here.\nEnable the 6-hour schedule to automatically process your photos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatNextRunTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = timestamp - now

    return if (diff <= 0) {
        "Soon"
    } else {
        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff % (1000 * 60 * 60)) / (1000 * 60)

        when {
            hours > 0 -> "in ${hours}h ${minutes}m"
            minutes > 0 -> "in ${minutes}m"
            else -> "Soon"
        }
    }
}
