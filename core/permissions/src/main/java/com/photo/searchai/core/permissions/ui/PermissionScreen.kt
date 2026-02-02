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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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

        // Check if critical permissions are granted to potentially enable the "Continue" button
        // The auto-skip is handled at the navigation level in AppNavHost.
        // We stay on this screen to allow the user to see and grant optional permissions like
        // Battery.

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

        Scaffold(
                bottomBar = {
                        Button(
                                onClick = onAllPermissionsGranted,
                                modifier = Modifier.fillMaxWidth().padding(24.dp).height(56.dp),
                                enabled = isStorageGranted && isNotificationGranted,
                                shape = MaterialTheme.shapes.extraLarge
                        ) { Text("Continue", style = MaterialTheme.typography.titleMedium) }
                }
        ) { innerPadding ->
                Column(
                        modifier =
                                Modifier.fillMaxSize()
                                        .padding(innerPadding)
                                        .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.Start
                ) {
                        Spacer(modifier = Modifier.size(48.dp))

                        Text(
                                text = "App permissions",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                                text = "Allow Search AI to access",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                        )

                        LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                item {
                                        PermissionListItem(
                                                title = "Files and media",
                                                description =
                                                        "Allow access to all files to scan and organize photos",
                                                icon = Icons.Default.Folder,
                                                isGranted = isStorageGranted,
                                                onClick = {
                                                        if (Build.VERSION.SDK_INT >=
                                                                        Build.VERSION_CODES.R
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
                                        HorizontalDivider(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                thickness = 0.5.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                }

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        item {
                                                PermissionListItem(
                                                        title = "Notifications",
                                                        description =
                                                                "Get updates on scanning progress",
                                                        icon = Icons.Default.Notifications,
                                                        isGranted = isNotificationGranted,
                                                        onClick = {
                                                                notificationPermissionState
                                                                        ?.launchPermissionRequest()
                                                        }
                                                )
                                                HorizontalDivider(
                                                        modifier =
                                                                Modifier.padding(vertical = 8.dp),
                                                        thickness = 0.5.dp,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .outlineVariant
                                                )
                                        }
                                }

                                item {
                                        PermissionListItem(
                                                title = "Battery optimization",
                                                description =
                                                        "Allow background processing (Optional)",
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
                }
        }
}

@Composable
fun PermissionListItem(
        title: String,
        description: String,
        icon: ImageVector,
        isGranted: Boolean,
        onClick: () -> Unit
) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Switch(
                        checked = isGranted,
                        onCheckedChange = { onClick() },
                        colors =
                                SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                        uncheckedTrackColor =
                                                MaterialTheme.colorScheme.surfaceVariant,
                                )
                )
        }
}
