package com.photo.searchai.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PhotoFilter
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photo.searchai.data.datastore.BenchmarkData
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
        viewModel: HomeViewModel,
        onNavigateToSearch: () -> Unit,
        onNavigateToFaceSearch: () -> Unit = {},
        onNavigateToBarcodePhotos: () -> Unit = {},
        onNavigateToScanner: () -> Unit = {},
        onNavigateToRefreshHistory: () -> Unit = {},
        onNavigateToBatterySettings: () -> Unit = {},
        onNavigateToGalleryInsights: () -> Unit = {},
        onNavigateToSmartAlbums: () -> Unit = {},
        onNavigateToStorageCleanup: () -> Unit = {},
        onNavigateToMediaVault: () -> Unit = {},
        onNavigateToOnboarding: () -> Unit = {}
) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                        NavigationDrawerContent(
                                totalImages = uiState.totalImages,
                                parsedImages = uiState.parsedImages,
                                onNavigateToSearch = {
                                        scope.launch { drawerState.close() }
                                        onNavigateToSearch()
                                },
                                onNavigateToFaceSearch = {
                                        scope.launch { drawerState.close() }
                                        onNavigateToFaceSearch()
                                },
                                onNavigateToBarcodePhotos = {
                                        scope.launch { drawerState.close() }
                                        onNavigateToBarcodePhotos()
                                },
                                onNavigateToScanner = {
                                        scope.launch { drawerState.close() }
                                        onNavigateToScanner()
                                },
                                onNavigateToRefreshHistory = {
                                        scope.launch { drawerState.close() }
                                        onNavigateToRefreshHistory()
                                },
                                onNavigateToBatterySettings = {
                                        scope.launch { drawerState.close() }
                                        onNavigateToBatterySettings()
                                },
                                onNavigateToGalleryInsights = {
                                        scope.launch { drawerState.close() }
                                        onNavigateToGalleryInsights()
                                },
                                onNavigateToSmartAlbums = {
                                        scope.launch { drawerState.close() }
                                        onNavigateToSmartAlbums()
                                },
                                onNavigateToStorageCleanup = {
                                        scope.launch { drawerState.close() }
                                        onNavigateToStorageCleanup()
                                },
                                onNavigateToMediaVault = {
                                        scope.launch { drawerState.close() }
                                        onNavigateToMediaVault()
                                },
                                onNavigateToOnboarding = {
                                        scope.launch { drawerState.close() }
                                        onNavigateToOnboarding()
                                }
                        )
                }
        ) {
                Scaffold(
                        topBar = {
                                TopAppBar(
                                        title = { Text("Photo Search AI") },
                                        navigationIcon = {
                                                IconButton(
                                                        onClick = {
                                                                scope.launch { drawerState.open() }
                                                        }
                                                ) {
                                                        Icon(
                                                                imageVector = Icons.Rounded.Menu,
                                                                contentDescription = "Menu"
                                                        )
                                                }
                                        },
                                        actions = {
                                                IconButton(onClick = { viewModel.refresh() }) {
                                                        Icon(
                                                                imageVector = Icons.Rounded.Refresh,
                                                                contentDescription = "Refresh"
                                                        )
                                                }
                                        },
                                        colors =
                                                TopAppBarDefaults.topAppBarColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme.surface
                                                )
                                )
                        }
                ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                                if (uiState.isLoading) {
                                        CircularProgressIndicator(
                                                modifier = Modifier.align(Alignment.Center)
                                        )
                                } else {
                                        Column(
                                                modifier =
                                                        Modifier.fillMaxSize()
                                                                .verticalScroll(
                                                                        rememberScrollState()
                                                                )
                                                                .padding(
                                                                        horizontal = 16.dp,
                                                                        vertical = 16.dp
                                                                ),
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                                // OCR Processing Card
                                                ProcessingCard(
                                                        icon =
                                                                Icons.AutoMirrored.Rounded
                                                                        .TextSnippet,
                                                        title = "Text Recognition",
                                                        subtitle = "OCR Processing",
                                                        stageProgress = uiState.ocrProgress,
                                                        averageTimePerImageMs =
                                                                uiState.averageTimePerImageMs
                                                )

                                                // Barcode Processing Card
                                                ProcessingCard(
                                                        icon = Icons.Rounded.QrCodeScanner,
                                                        title = "Barcode Scanning",
                                                        subtitle = "QR & Barcode Detection",
                                                        stageProgress = uiState.barcodeProgress,
                                                        averageTimePerImageMs =
                                                                uiState.averageTimePerImageMs
                                                )

                                                // Image Labeling Card
                                                ProcessingCard(
                                                        icon = Icons.Rounded.Face,
                                                        title = "Image Labeling",
                                                        subtitle = "Object & Scene Detection",
                                                        stageProgress = uiState.labelProgress,
                                                        averageTimePerImageMs =
                                                                uiState.averageTimePerImageMs
                                                )

                                                ProcessingCard(
                                                        icon = Icons.Rounded.PhotoFilter,
                                                        title = "Quality Analysis",
                                                        subtitle = "Blur, Brightness & More",
                                                        stageProgress = uiState.qualityProgress,
                                                        averageTimePerImageMs =
                                                                uiState.averageTimePerImageMs
                                                )

                                                Text(
                                                        text = "Insights & Tools",
                                                        style =
                                                                MaterialTheme.typography
                                                                        .titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(top = 8.dp)
                                                )

                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(12.dp)
                                                ) {
                                                        QuickActionCard(
                                                                title = "Insights",
                                                                icon = Icons.Rounded.Analytics,
                                                                onClick =
                                                                        onNavigateToGalleryInsights,
                                                                modifier = Modifier.weight(1f)
                                                        )
                                                        QuickActionCard(
                                                                title = "Smart Albums",
                                                                icon = Icons.Rounded.AutoAwesome,
                                                                onClick = onNavigateToSmartAlbums,
                                                                modifier = Modifier.weight(1f)
                                                        )
                                                }

                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(12.dp)
                                                ) {
                                                        QuickActionCard(
                                                                title = "Cleanup",
                                                                icon =
                                                                        Icons.Rounded
                                                                                .CleaningServices,
                                                                onClick =
                                                                        onNavigateToStorageCleanup,
                                                                modifier = Modifier.weight(1f)
                                                        )
                                                        QuickActionCard(
                                                                title = "Vault",
                                                                icon = Icons.Rounded.Lock,
                                                                onClick = onNavigateToMediaVault,
                                                                modifier = Modifier.weight(1f)
                                                        )
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))
                                        }
                                }
                        }
                }
        }
}

@Composable
private fun ProcessingCard(
        icon: ImageVector,
        title: String,
        subtitle: String,
        stageProgress: StageProgress,
        averageTimePerImageMs: Long,
        modifier: Modifier = Modifier
) {
        val animatedProgress by
                animateFloatAsState(
                        targetValue = stageProgress.progress,
                        animationSpec = tween(durationMillis = 300),
                        label = "cardProgress"
                )

        Card(
                modifier = modifier.fillMaxWidth(),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                        // Header Row with Icon and Title
                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                // Icon Container
                                Surface(
                                        color =
                                                when {
                                                        stageProgress.isActive ->
                                                                MaterialTheme.colorScheme
                                                                        .primaryContainer
                                                        stageProgress.isComplete ->
                                                                MaterialTheme.colorScheme
                                                                        .tertiaryContainer
                                                        else ->
                                                                MaterialTheme.colorScheme
                                                                        .surfaceVariant
                                                },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.size(56.dp)
                                ) {
                                        Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                        imageVector =
                                                                if (stageProgress.isComplete)
                                                                        Icons.Rounded.Check
                                                                else icon,
                                                        contentDescription = null,
                                                        tint =
                                                                when {
                                                                        stageProgress.isActive ->
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onPrimaryContainer
                                                                        stageProgress.isComplete ->
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onTertiaryContainer
                                                                        else ->
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                                },
                                                        modifier = Modifier.size(28.dp)
                                                )
                                        }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                                text = subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                }

                                // Status Badge
                                Surface(
                                        color =
                                                when {
                                                        stageProgress.isActive ->
                                                                MaterialTheme.colorScheme
                                                                        .primaryContainer
                                                        stageProgress.isComplete ->
                                                                MaterialTheme.colorScheme
                                                                        .tertiaryContainer
                                                        else ->
                                                                MaterialTheme.colorScheme
                                                                        .surfaceVariant
                                                },
                                        shape = RoundedCornerShape(10.dp)
                                ) {
                                        Text(
                                                text =
                                                        when {
                                                                stageProgress.isActive ->
                                                                        "${stageProgress.percentage}%"
                                                                stageProgress.isComplete -> "Done"
                                                                else -> "Pending"
                                                        },
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color =
                                                        when {
                                                                stageProgress.isActive ->
                                                                        MaterialTheme.colorScheme
                                                                                .onPrimaryContainer
                                                                stageProgress.isComplete ->
                                                                        MaterialTheme.colorScheme
                                                                                .onTertiaryContainer
                                                                else ->
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        },
                                                modifier =
                                                        Modifier.padding(
                                                                horizontal = 12.dp,
                                                                vertical = 8.dp
                                                        )
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Content based on status
                        if (stageProgress.isActive) {
                                // Progress Bar
                                LinearProgressIndicator(
                                        progress = { animatedProgress },
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .height(8.dp)
                                                        .clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        strokeCap = StrokeCap.Round
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Stats Row
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                        // Remaining Images
                                        Column {
                                                Text(
                                                        text = "${stageProgress.pending}",
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                        text = "Remaining",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                        }

                                        // Estimated Time - calculate based on average time per
                                        // image
                                        Column(horizontalAlignment = Alignment.End) {
                                                val estimatedMs =
                                                        averageTimePerImageMs *
                                                                stageProgress.pending
                                                val estimatedText =
                                                        when {
                                                                averageTimePerImageMs <= 0 ->
                                                                        "Calculating..."
                                                                estimatedMs < 60_000 ->
                                                                        "${estimatedMs / 1000}s"
                                                                estimatedMs < 3_600_000 ->
                                                                        "${estimatedMs / 60_000}m ${(estimatedMs % 60_000) / 1000}s"
                                                                else ->
                                                                        "${estimatedMs / 3_600_000}h ${(estimatedMs % 3_600_000) / 60_000}m"
                                                        }
                                                Text(
                                                        text = estimatedText,
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.tertiary
                                                )
                                                Text(
                                                        text = "Est. Time",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                        }
                                }
                        } else if (stageProgress.isComplete) {
                                // Completed stage - show "Images are up to date"
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                ) {
                                        Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                                text = "Images are up to date",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.tertiary
                                        )
                                }
                        } else {
                                // Pending stage - show "Waiting to start"
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                ) {
                                        Icon(
                                                imageVector = Icons.Rounded.Timer,
                                                contentDescription = null,
                                                tint =
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                                .copy(alpha = 0.6f),
                                                modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                                text = "Waiting to start",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                                .copy(alpha = 0.6f)
                                        )
                                }
                        }
                }
        }
}

@Composable
private fun BenchmarkCard(
        benchmarkData: BenchmarkData,
        isProcessing: Boolean,
        averageTimePerImageMs: Long,
        estimatedTimeRemaining: String,
        modifier: Modifier = Modifier
) {
        Card(
                modifier = modifier.fillMaxWidth(),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(20.dp)
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.size(44.dp)
                                ) {
                                        Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                        imageVector = Icons.Rounded.Analytics,
                                                        contentDescription = null,
                                                        tint =
                                                                MaterialTheme.colorScheme
                                                                        .onPrimaryContainer,
                                                        modifier = Modifier.size(24.dp)
                                                )
                                        }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = "OCR Benchmark",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                                text =
                                                        if (benchmarkData.isComplete)
                                                                "Processing Complete"
                                                        else "In Progress...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                }

                                if (benchmarkData.isComplete) {
                                        Surface(
                                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                                shape = RoundedCornerShape(8.dp)
                                        ) {
                                                Text(
                                                        text =
                                                                "${benchmarkData.totalImagesProcessed} images",
                                                        style =
                                                                MaterialTheme.typography
                                                                        .labelMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onTertiaryContainer,
                                                        modifier =
                                                                Modifier.padding(
                                                                        horizontal = 10.dp,
                                                                        vertical = 6.dp
                                                                )
                                                )
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                                text = "MILESTONE TIMINGS",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                letterSpacing =
                                                        MaterialTheme.typography
                                                                .labelSmall
                                                                .letterSpacing * 1.5f
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                benchmarkData.milestones.forEach { (percent, time)
                                                        ->
                                                        MilestoneItem(
                                                                percent = percent,
                                                                time = time,
                                                                isReached = time != "—"
                                                        )
                                                }
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                                StatCard(
                                        icon = Icons.Rounded.Speed,
                                        label = "Avg. per image",
                                        value =
                                                if (benchmarkData.isComplete) {
                                                        benchmarkData.formattedAverageTime
                                                } else if (averageTimePerImageMs > 0) {
                                                        formatMs(averageTimePerImageMs)
                                                } else {
                                                        "—"
                                                },
                                        modifier = Modifier.weight(1f),
                                        accentColor = MaterialTheme.colorScheme.primary
                                )

                                StatCard(
                                        icon = Icons.Rounded.Timer,
                                        label = "Est. remaining",
                                        value =
                                                if (benchmarkData.isComplete) "Done"
                                                else estimatedTimeRemaining,
                                        modifier = Modifier.weight(1f),
                                        accentColor = MaterialTheme.colorScheme.tertiary
                                )
                        }

                        if (benchmarkData.isComplete && benchmarkData.timeTo100Percent > 0) {
                                Spacer(modifier = Modifier.height(16.dp))

                                Surface(
                                        color =
                                                MaterialTheme.colorScheme.primaryContainer.copy(
                                                        alpha = 0.3f
                                                ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                ) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                        text =
                                                                "Total time: ${benchmarkData.formatMilestoneTime(benchmarkData.timeTo100Percent)}",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                )
                                        }
                                }
                        }
                }
        }
}

@Composable
private fun MilestoneItem(percent: String, time: String, isReached: Boolean) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                        color =
                                if (isReached) {
                                        MaterialTheme.colorScheme.primaryContainer
                                } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(width = 52.dp, height = 32.dp)
                ) {
                        Box(contentAlignment = Alignment.Center) {
                                Text(
                                        text = percent,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color =
                                                if (isReached) {
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                                .copy(alpha = 0.6f)
                                                }
                                )
                        }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                        text = time,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isReached) FontWeight.Medium else FontWeight.Normal,
                        color =
                                if (isReached) {
                                        MaterialTheme.colorScheme.onSurface
                                } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.5f
                                        )
                                },
                        textAlign = TextAlign.Center
                )
        }
}

@Composable
private fun StatCard(
        icon: ImageVector,
        label: String,
        value: String,
        modifier: Modifier = Modifier,
        accentColor: androidx.compose.ui.graphics.Color
) {
        Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
                modifier = modifier
        ) {
                Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                                text = value,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                        )
                }
        }
}

private fun formatMs(ms: Long): String {
        return when {
                ms < 1000 -> "${ms}ms"
                ms < 60000 -> String.format("%.1fs", ms / 1000.0)
                else -> String.format("%.1fmin", ms / 60000.0)
        }
}

@Composable
private fun CenterContent(totalImages: Int, parsedImages: Int, onOpenDrawer: () -> Unit) {
        Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
                Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                        text = "Photo Search AI",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                        text = "Find photos by what's in them",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (totalImages > 0) {
                        Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        CardDefaults.cardColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                                .copy(alpha = 0.5f)
                                        )
                        ) {
                                Row(
                                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                        text = "$totalImages",
                                                        style =
                                                                MaterialTheme.typography
                                                                        .headlineSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                        text = "Total Photos",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                        text = "$parsedImages",
                                                        style =
                                                                MaterialTheme.typography
                                                                        .headlineSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.secondary
                                                )
                                                Text(
                                                        text = "Indexed",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                        }
                                }
                        }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                                Modifier.clip(RoundedCornerShape(24.dp))
                                        .background(
                                                MaterialTheme.colorScheme.primaryContainer.copy(
                                                        alpha = 0.3f
                                                )
                                        )
                                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                        Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                                text = "Open menu to search",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                        )
                }
        }
}

@Composable
private fun NavigationDrawerContent(
        totalImages: Int,
        parsedImages: Int,
        onNavigateToSearch: () -> Unit,
        onNavigateToFaceSearch: () -> Unit,
        onNavigateToBarcodePhotos: () -> Unit,
        onNavigateToScanner: () -> Unit,
        onNavigateToRefreshHistory: () -> Unit,
        onNavigateToBatterySettings: () -> Unit,
        onNavigateToGalleryInsights: () -> Unit,
        onNavigateToSmartAlbums: () -> Unit,
        onNavigateToStorageCleanup: () -> Unit,
        onNavigateToMediaVault: () -> Unit,
        onNavigateToOnboarding: () -> Unit
) {
        ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Column(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(24.dp)
                ) {
                        Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                                text = "Photo Search AI",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                                text = "$parsedImages / $totalImages photos indexed",
                                style = MaterialTheme.typography.bodySmall,
                                color =
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                alpha = 0.7f
                                        )
                        )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                        text = "SEARCH",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold
                )

                DrawerMenuItem(
                        icon = Icons.AutoMirrored.Rounded.TextSnippet,
                        label = "Search by Text",
                        subtitle = "Find photos with text",
                        enabled = parsedImages > 0,
                        onClick = onNavigateToSearch
                )

                DrawerMenuItem(
                        icon = Icons.Rounded.Face,
                        label = "Search by Face",
                        subtitle = "Coming soon",
                        enabled = false,
                        onClick = onNavigateToFaceSearch
                )

                DrawerMenuItem(
                        icon = Icons.Rounded.QrCodeScanner,
                        label = "Barcode Photos",
                        subtitle = "Coming soon",
                        enabled = false,
                        onClick = onNavigateToBarcodePhotos
                )

                HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 28.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                )

                Text(
                        text = "TOOLS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold
                )

                DrawerMenuItem(
                        icon = Icons.Rounded.DocumentScanner,
                        label = "Document Scanner",
                        subtitle = "Coming soon",
                        enabled = false,
                        onClick = onNavigateToScanner
                )

                HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 28.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                )

                Text(
                        text = "NEW FEATURES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold
                )

                DrawerMenuItem(
                        icon = Icons.Rounded.Analytics,
                        label = "Gallery Insights",
                        subtitle = "Metadata analysis",
                        enabled = true,
                        onClick = onNavigateToGalleryInsights
                )

                DrawerMenuItem(
                        icon = Icons.Rounded.AutoAwesome,
                        label = "Smart Albums",
                        subtitle = "Rule-based albums",
                        enabled = true,
                        onClick = onNavigateToSmartAlbums
                )

                DrawerMenuItem(
                        icon = Icons.Rounded.CleaningServices,
                        label = "Storage Cleanup",
                        subtitle = "Free up space",
                        enabled = true,
                        onClick = onNavigateToStorageCleanup
                )

                DrawerMenuItem(
                        icon = Icons.Rounded.Lock,
                        label = "Media Vault",
                        subtitle = "Private photos",
                        enabled = true,
                        onClick = onNavigateToMediaVault
                )

                DrawerMenuItem(
                        icon = Icons.Rounded.History,
                        label = "Refresh History",
                        subtitle = "View background processing",
                        enabled = true,
                        onClick = onNavigateToRefreshHistory
                )

                DrawerMenuItem(
                        icon = Icons.Rounded.PowerSettingsNew,
                        label = "Battery Settings",
                        subtitle = "Configure background access",
                        enabled = true,
                        onClick = onNavigateToBatterySettings
                )

                DrawerMenuItem(
                        icon = Icons.Rounded.Info,
                        label = "App Onboarding",
                        subtitle = "Review app features",
                        enabled = true,
                        onClick = onNavigateToOnboarding
                )
        }
}

@Composable
private fun DrawerMenuItem(
        icon: ImageVector,
        label: String,
        subtitle: String,
        enabled: Boolean,
        onClick: () -> Unit
) {
        NavigationDrawerItem(
                label = {
                        Column {
                                Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                )
                                Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color =
                                                if (enabled)
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                else
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                                .copy(alpha = 0.5f)
                                )
                        }
                },
                icon = {
                        Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint =
                                        if (enabled) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                },
                selected = false,
                onClick = { if (enabled) onClick() },
                modifier = Modifier.padding(horizontal = 12.dp),
                colors =
                        NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor =
                                        if (enabled) MaterialTheme.colorScheme.surface
                                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
        )
}

@Composable
private fun MultiStageProgressRibbon(show: Boolean, uiState: HomeUiState) {
        val animatedOverallProgress by
                animateFloatAsState(
                        targetValue = uiState.overallProgress,
                        animationSpec = tween(durationMillis = 300),
                        label = "overallProgress"
                )

        AnimatedVisibility(
                visible = show,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300))
        ) {
                Card(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor =
                                                MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(20.dp)
                ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (uiState.isProcessing) {
                                                        CircularProgressIndicator(
                                                                modifier = Modifier.size(20.dp),
                                                                strokeWidth = 2.dp,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .primary
                                                        )
                                                } else {
                                                        Icon(
                                                                imageVector =
                                                                        Icons.Rounded.CameraAlt,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(20.dp),
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .primary
                                                        )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                        Text(
                                                                text = "Processing Photos",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .titleSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface
                                                        )
                                                        Text(
                                                                text = uiState.statusText,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }
                                        }

                                        Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(12.dp)
                                        ) {
                                                Text(
                                                        text = "${uiState.overallPercentage}%",
                                                        style =
                                                                MaterialTheme.typography
                                                                        .titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onPrimaryContainer,
                                                        modifier =
                                                                Modifier.padding(
                                                                        horizontal = 12.dp,
                                                                        vertical = 6.dp
                                                                )
                                                )
                                        }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                        StageIndicator(
                                                icon = Icons.AutoMirrored.Rounded.TextSnippet,
                                                stageProgress = uiState.ocrProgress,
                                                stageNumber = "1"
                                        )
                                        StageIndicator(
                                                icon = Icons.Rounded.QrCodeScanner,
                                                stageProgress = uiState.barcodeProgress,
                                                stageNumber = "2"
                                        )
                                        StageIndicator(
                                                icon = Icons.Rounded.Face,
                                                stageProgress = uiState.labelProgress,
                                                stageNumber = "3"
                                        )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Column {
                                        LinearProgressIndicator(
                                                progress = { animatedOverallProgress },
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .height(8.dp)
                                                                .clip(RoundedCornerShape(4.dp)),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor =
                                                        MaterialTheme.colorScheme.surfaceVariant,
                                                strokeCap = StrokeCap.Round
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                Text(
                                                        text = "${uiState.totalPending} remaining",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                                if (uiState.estimatedTimeRemainingSeconds > 0) {
                                                        Text(
                                                                text =
                                                                        uiState.formattedEstimatedTime +
                                                                                " remaining",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}

@Composable
private fun StageIndicator(icon: ImageVector, stageProgress: StageProgress, stageNumber: String) {
        val animatedProgress by
                animateFloatAsState(
                        targetValue = stageProgress.progress,
                        animationSpec = tween(durationMillis = 300),
                        label = "stageProgress"
                )

        val containerColor =
                when {
                        stageProgress.isComplete -> MaterialTheme.colorScheme.primaryContainer
                        stageProgress.isActive -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                }

        val contentColor =
                when {
                        stageProgress.isComplete -> MaterialTheme.colorScheme.onPrimaryContainer
                        stageProgress.isActive -> MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(100.dp)
        ) {
                Box(contentAlignment = Alignment.Center) {
                        Surface(
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(50),
                                color = containerColor
                        ) {
                                Box(contentAlignment = Alignment.Center) {
                                        if (stageProgress.isActive && !stageProgress.isComplete) {
                                                CircularProgressIndicator(
                                                        progress = { animatedProgress },
                                                        modifier = Modifier.size(48.dp),
                                                        strokeWidth = 3.dp,
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                        trackColor =
                                                                MaterialTheme.colorScheme
                                                                        .tertiaryContainer
                                                )
                                        }
                                        Icon(
                                                imageVector =
                                                        if (stageProgress.isComplete)
                                                                Icons.Rounded.Check
                                                        else icon,
                                                contentDescription = stageProgress.name,
                                                modifier = Modifier.size(24.dp),
                                                tint = contentColor
                                        )
                                }
                        }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                        text = stageProgress.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                        fontWeight =
                                if (stageProgress.isActive) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                )

                if (stageProgress.isComplete) {
                        Text(
                                text = "Done",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                        )
                } else if (stageProgress.isActive) {
                        Text(
                                text = "${stageProgress.percentage}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                        )
                } else {
                        Text(
                                text = "Waiting",
                                style = MaterialTheme.typography.bodySmall,
                                color =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.6f
                                        )
                        )
                }
        }
}

@Composable
private fun QuickActionCard(
        title: String,
        icon: ImageVector,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
        Card(
                onClick = onClick,
                modifier = modifier.height(110.dp),
                shape = RoundedCornerShape(20.dp),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
                Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                ) {
                        Surface(
                                color =
                                        MaterialTheme.colorScheme.primaryContainer.copy(
                                                alpha = 0.4f
                                        ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(40.dp)
                        ) {
                                Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                        )
                }
        }
}
