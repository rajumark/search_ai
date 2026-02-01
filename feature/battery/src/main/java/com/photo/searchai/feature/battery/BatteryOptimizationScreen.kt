package com.photo.searchai.feature.battery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryOptimizationScreen(
        viewModel: BatteryOptimizationViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val launchIntent by viewModel.launchIntent.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Activity result launcher for battery settings
    val batterySettingsLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
            ) { viewModel.onReturnFromBatterySettings() }

    // Launch intents when available
    LaunchedEffect(launchIntent) {
        launchIntent?.let { intent ->
            try {
                batterySettingsLauncher.launch(intent)
                viewModel.onIntentLaunched()
            } catch (e: Exception) {
                // Fallback if intent fails
                viewModel.openAppSettings(context)
            }
        }
    }

    // Refresh state when screen becomes visible
    LaunchedEffect(Unit) { viewModel.refreshState() }

    // Show success/denied messages
    LaunchedEffect(uiState.showSuccessMessage) {
        if (uiState.showSuccessMessage) {
            snackbarHostState.showSnackbar("Background processing enabled successfully!")
            viewModel.dismissSuccessMessage()
        }
    }

    LaunchedEffect(uiState.showDeniedMessage) {
        if (uiState.showDeniedMessage) {
            snackbarHostState.showSnackbar("Background processing may be limited")
            viewModel.dismissDeniedMessage()
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Battery Settings") },
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
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            item {
                BatteryStatusCard(
                        isWhitelisted = uiState.isWhitelisted,
                        isInPowerSaveMode = uiState.isInPowerSaveMode,
                        deviceInfo = uiState.deviceInfo
                )
            }

            // Background Processing Toggle
            item {
                BackgroundProcessingCard(
                        enabled = uiState.backgroundProcessingEnabled,
                        onToggle = viewModel::setBackgroundProcessingEnabled
                )
            }

            // Battery Optimization Action
            item {
                BatteryOptimizationActionCard(
                        isWhitelisted = uiState.isWhitelisted,
                        onRequestOptimization = viewModel::requestBatteryOptimization
                )
            }

            // OEM-specific section
            if (uiState.hasOemBatteryManagement) {
                item {
                    OemBatteryCard(
                            manufacturerName = uiState.manufacturerName,
                            instructions = uiState.manufacturerInstructions,
                            hasCompleted = uiState.hasCompletedOemSetup,
                            onSetup = viewModel::onOemSetupRequested
                    )
                }
            }

            // Info section
            item { BatteryInfoCard() }
        }
    }

    // Rationale Dialog
    if (uiState.showRationaleDialog) {
        BatteryRationaleDialog(
                onConfirm = viewModel::onRationaleAccepted,
                onDismiss = viewModel::onRationaleDeclined,
                onNeverAskAgain = viewModel::onNeverAskAgain
        )
    }

    // OEM Setup Dialog
    if (uiState.showOemDialog) {
        OemSetupDialog(
                manufacturerName = uiState.manufacturerName,
                onSetup = viewModel::onOemSetupRequested,
                onSkip = viewModel::onOemSetupSkipped,
                onDone = viewModel::onOemSetupCompleted
        )
    }
}

@Composable
private fun BatteryStatusCard(
        isWhitelisted: Boolean,
        isInPowerSaveMode: Boolean,
        deviceInfo: String
) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    if (isWhitelisted)
                                            MaterialTheme.colorScheme.primaryContainer.copy(
                                                    alpha = 0.3f
                                            )
                                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
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
                                if (isWhitelisted) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                                imageVector =
                                        if (isWhitelisted) Icons.Rounded.CheckCircle
                                        else Icons.Rounded.BatteryAlert,
                                contentDescription = null,
                                tint =
                                        if (isWhitelisted) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            text =
                                    if (isWhitelisted) "Optimized for Background"
                                    else "Battery Optimization Active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                            text =
                                    if (isWhitelisted) "Background processing will run reliably"
                                    else "Background tasks may be limited",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isInPowerSaveMode) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                                imageVector = Icons.Rounded.BatterySaver,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                                text = "Power Save Mode is enabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        imageVector = Icons.Rounded.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                        text = deviceInfo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BackgroundProcessingCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
            shape = RoundedCornerShape(16.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = "Background Processing",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                )
                Text(
                        text = "Process photos automatically every 6 hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun BatteryOptimizationActionCard(
        isWhitelisted: Boolean,
        onRequestOptimization: () -> Unit
) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
            shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                    text = "Battery Optimization",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                    text =
                            if (isWhitelisted)
                                    "App is exempted from battery optimization. Background processing will work reliably."
                            else
                                    "Enable unrestricted battery access to ensure photos are processed even when the app is closed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isWhitelisted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                            text = "Battery optimization disabled",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Button(onClick = onRequestOptimization, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disable Battery Optimization")
                }
            }
        }
    }
}

@Composable
private fun OemBatteryCard(
        manufacturerName: String,
        instructions: String,
        hasCompleted: Boolean,
        onSetup: () -> Unit
) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    ),
            shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        text = "$manufacturerName Device Detected",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                    text =
                            "$manufacturerName devices have additional battery management that may prevent background processing. Follow these steps:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                        text = instructions,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (hasCompleted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                            text = "Setup completed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                    )
                }
            } else {
                FilledTonalButton(onClick = onSetup, modifier = Modifier.fillMaxWidth()) {
                    Text("Open $manufacturerName Settings")
                }
            }
        }
    }
}

@Composable
private fun BatteryInfoCard() {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
            shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        text = "About Battery Usage",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                    text =
                            """
                    Photo Search AI processes your photos in the background to enable quick searching. This is designed to be battery-efficient:
                    
                    • Processing runs every 6 hours with smart scheduling
                    • Uses adaptive scheduling based on device idle state
                    • No wake locks or tight loops
                    • Work is deferred when battery is low
                    • No internet connection required
                    
                    Battery impact is minimal and comparable to other background apps.
                """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BatteryRationaleDialog(
        onConfirm: () -> Unit,
        onDismiss: () -> Unit,
        onNeverAskAgain: () -> Unit
) {
    AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                        imageVector = Icons.Rounded.BatteryAlert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                )
            },
            title = { Text(text = "Enable Background Processing", textAlign = TextAlign.Center) },
            text = {
                Column {
                    Text(
                            text =
                                    "To process your photos in the background, Photo Search AI needs to be exempted from battery optimization.",
                            style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                            text = "This allows the app to:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    BulletPoint("Process photos every 6 hours")
                    BulletPoint("Complete processing when app is closed")
                    BulletPoint("Provide faster search results")

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                    text =
                                            "Battery impact is minimal. The app uses efficient scheduling and does not run continuously.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = onConfirm) { Text("Continue") } },
            dismissButton = {
                Column {
                    TextButton(onClick = onDismiss) { Text("Not Now") }
                    TextButton(onClick = onNeverAskAgain) {
                        Text(
                                text = "Never Ask Again",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
    )
}

@Composable
fun OemSetupDialog(
        manufacturerName: String,
        onSetup: () -> Unit,
        onSkip: () -> Unit,
        onDone: () -> Unit
) {
    AlertDialog(
            onDismissRequest = onSkip,
            icon = {
                Icon(
                        imageVector = Icons.Rounded.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(48.dp)
                )
            },
            title = { Text(text = "Additional Setup Required", textAlign = TextAlign.Center) },
            text = {
                Text(
                        text =
                                "$manufacturerName devices may have additional battery management that can limit background processing. For best results, we recommend configuring the manufacturer-specific settings.\n\nYou can skip this step and configure it later in Settings.",
                        style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = { Button(onClick = onSetup) { Text("Open Settings") } },
            dismissButton = {
                Column {
                    FilledTonalButton(onClick = onDone) { Text("I've Done This") }
                    TextButton(onClick = onSkip) { Text("Skip for Now") }
                }
            }
    )
}

@Composable
private fun BulletPoint(text: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Text(
                text = "•",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp)
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
