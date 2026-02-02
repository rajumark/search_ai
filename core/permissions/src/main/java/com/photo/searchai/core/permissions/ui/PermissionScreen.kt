package com.photo.searchai.core.permissions.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.photo.searchai.core.permissions.logic.PermissionManager

@SuppressLint("BatteryLife")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(onAllPermissionsGranted: () -> Unit) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val permissionManager = remember { PermissionManager() }

        // Observe lifecycle properly to refresh permission states when returning from Settings
        var refreshKey by remember { mutableStateOf(0) }

        DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                                refreshKey++
                        }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        val isStorageGranted = remember(refreshKey) { permissionManager.isStorageGranted() }

        val notificationPermissionState =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
                } else {
                        null
                }
        val isNotificationGranted = notificationPermissionState?.status?.isGranted ?: true

        val isBatteryOptimized =
                remember(refreshKey) { permissionManager.isBatteryOptimized(context) }

        // Check if critical permissions are granted to potentially auto-navigate or enable a
        // "Continue"
        // button
        // The user asked for specific permissions, battery is optional.
        // If Storage and Notification (if applicable) are granted, we can proceed.
        // However, usually, we want the user to explicitly click "Continue" or similar if we are in
        // an
        // onboarding flow.
        // Here we just provide the screen functionalities.

        val storageLauncher =
                rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                ) {
                        // Result handled by ON_RESUME refresh
                }

        val batteryLauncher =
                rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                ) {
                        // Result handled by ON_RESUME refresh
                }

        Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
                Text(
                        text = "Required Permissions",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                        text =
                                "To provide the best experience, this app needs access to the following:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 24.dp)
                )

                LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                ) {
                        item {
                                PermissionCard(
                                        title = "All Files Access",
                                        description =
                                                "Required to scan and organize your photos and media.",
                                        icon = Icons.Default.Folder,
                                        isGranted = isStorageGranted,
                                        onClick = {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                                                ) {
                                                        val intent =
                                                                Intent(
                                                                                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                                                                        )
                                                                        .apply {
                                                                                data =
                                                                                        Uri.fromParts(
                                                                                                "package",
                                                                                                context.packageName,
                                                                                                null
                                                                                        )
                                                                        }
                                                        storageLauncher.launch(intent)
                                                }
                                        }
                                )
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                item {
                                        PermissionCard(
                                                title = "Notifications",
                                                description =
                                                        "Get updates on scanning progress and new features.",
                                                icon = Icons.Default.Notifications,
                                                isGranted = isNotificationGranted,
                                                onClick = {
                                                        notificationPermissionState
                                                                ?.launchPermissionRequest()
                                                }
                                        )
                                }
                        }

                        item {
                                PermissionCard(
                                        title = "Battery Optimization",
                                        description =
                                                "Allow background processing for faster media analysis. (Optional)",
                                        icon = Icons.Default.BatteryAlert,
                                        isGranted = isBatteryOptimized,
                                        onClick = {
                                                val intent =
                                                        Intent(
                                                                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                                                )
                                                                .apply {
                                                                        data =
                                                                                Uri.fromParts(
                                                                                        "package",
                                                                                        context.packageName,
                                                                                        null
                                                                                )
                                                                }
                                                batteryLauncher.launch(intent)
                                        }
                                )
                        }
                }

                Button(
                        onClick = onAllPermissionsGranted,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        enabled = isStorageGranted // At least storage is critical
                ) { Text("Continue") }
        }
}

@Composable
fun PermissionCard(
        title: String,
        description: String,
        icon: ImageVector,
        isGranted: Boolean,
        onClick: () -> Unit
) {
        Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors =
                        CardDefaults.cardColors(
                                containerColor =
                                        if (isGranted) MaterialTheme.colorScheme.surfaceVariant
                                        else MaterialTheme.colorScheme.surface
                        )
        ) {
                Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Icon(
                                imageVector = if (isGranted) Icons.Default.CheckCircle else icon,
                                contentDescription = null,
                                tint =
                                        if (isGranted) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                )
                                Text(
                                        text = if (isGranted) "Permission Granted" else description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                        if (!isGranted) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = onClick) { Text("Allow") }
                        }
                }
        }
}
