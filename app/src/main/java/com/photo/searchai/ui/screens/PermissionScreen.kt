package com.photo.searchai.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun PermissionScreen(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current

    // Check if we have full image access (not partial)
    fun hasFullImageAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= 34) {
            // On Android 14+, check if we have full access (not just partial/selected)
            val hasReadImages =
                    ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED

            // If READ_MEDIA_VISUAL_USER_SELECTED is granted but READ_MEDIA_IMAGES is not,
            // it means user selected partial access
            val hasPartialAccess =
                    ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    ) == PackageManager.PERMISSION_GRANTED

            hasReadImages && !hasPartialAccess
        } else if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required below Android 13
        }
    }

    var hasImages by remember { mutableStateOf(hasFullImageAccess()) }
    var hasNotifications by remember { mutableStateOf(hasNotificationPermission()) }
    var showPartialAccessWarning by remember { mutableStateOf(false) }

    // Check partial access state
    fun checkPartialAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= 34) {
            val hasPartial =
                    ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    ) == PackageManager.PERMISSION_GRANTED

            val hasFull =
                    ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED

            hasPartial && !hasFull
        } else {
            false
        }
    }

    // Permission launcher for images
    val imagePermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    isGranted ->
                hasImages = hasFullImageAccess()
                showPartialAccessWarning = checkPartialAccess()
            }

    // Permission launcher for notifications
    val notificationPermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    isGranted ->
                hasNotifications = isGranted
            }

    // Check if all permissions are granted
    LaunchedEffect(hasImages, hasNotifications) {
        if (hasImages && hasNotifications) {
            onPermissionsGranted()
        }
    }

    // Refresh permission state when returning from settings
    LaunchedEffect(Unit) {
        hasImages = hasFullImageAccess()
        hasNotifications = hasNotificationPermission()
        showPartialAccessWarning = checkPartialAccess()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            Text(
                    text = "Permissions Required",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                    text =
                            "This app needs access to all your photos to search and organize them. Please grant the following permissions:",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Image Permission Card
            Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                            CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                            text = "Photos Access",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (hasImages) {
                        Text(
                                text = "✓ Full access granted",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                        )
                    } else if (showPartialAccessWarning) {
                        Text(
                                text = "⚠ Partial access only. Please allow access to ALL photos.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                                text =
                                        "To grant full access:\n" +
                                                "1. Tap 'Open Settings' below\n" +
                                                "2. Go to 'Permissions' → 'Photos and videos'\n" +
                                                "3. Select 'Allow all'",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                                text = "Required to search all your photos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (Build.VERSION.SDK_INT >= 34) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                    text =
                                            "Important: When prompted, please select 'Allow all' to enable full functionality.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!hasImages) {
                        Button(
                                onClick = {
                                    val permission =
                                            if (Build.VERSION.SDK_INT >= 33) {
                                                Manifest.permission.READ_MEDIA_IMAGES
                                            } else {
                                                Manifest.permission.READ_EXTERNAL_STORAGE
                                            }
                                    imagePermissionLauncher.launch(permission)
                                },
                                modifier = Modifier.fillMaxWidth()
                        ) { Text("Grant Photo Access") }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notification Permission Card (Android 13+)
            if (Build.VERSION.SDK_INT >= 33) {
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                                text = "Notifications",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (hasNotifications) {
                            Text(
                                    text = "✓ Permission granted",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                    text = "Required for background processing updates",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                    onClick = {
                                        notificationPermissionLauncher.launch(
                                                Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                            ) { Text("Grant Notification Access") }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Open Settings Button
            OutlinedButton(
                    onClick = {
                        val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
            ) { Text("Open Settings") }

            if (showPartialAccessWarning) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                        text =
                                "After changing settings, return here and the app will update automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
