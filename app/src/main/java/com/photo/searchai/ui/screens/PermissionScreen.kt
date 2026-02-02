package com.photo.searchai.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.photo.searchai.core.permission.PermissionChecker
import com.photo.searchai.core.permission.PermissionType
import com.photo.searchai.core.permission.PermissionUiHelper
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current

    // Permission states
    var hasAllFilesAccess by remember {
        mutableStateOf(PermissionChecker.hasPermission(context, PermissionType.ALL_FILES))
    }
    var hasNotification by remember {
        mutableStateOf(PermissionChecker.hasPermission(context, PermissionType.NOTIFICATION))
    }

    // Launcher for notification permission
    val notificationPermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    isGranted ->
                hasNotification = isGranted
            }

    // Launcher for storage permission (Android 10 and below)
    val storagePermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    isGranted ->
                hasAllFilesAccess = isGranted
            }

    // Launcher for all files access settings (Android 11+)
    val allFilesAccessLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                val hasPermission =
                        PermissionChecker.hasPermission(context, PermissionType.ALL_FILES)
                hasAllFilesAccess = hasPermission
            }

    // Check if all permissions are granted
    LaunchedEffect(hasAllFilesAccess, hasNotification) {
        if (hasAllFilesAccess && hasNotification) {
            delay(500) // Brief delay for feedback
            onPermissionsGranted()
        }
    }

    // Refresh permission state periodically when returning from settings
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            val filesAccess = PermissionChecker.hasPermission(context, PermissionType.ALL_FILES)
            val notification = PermissionChecker.hasPermission(context, PermissionType.NOTIFICATION)

            if (filesAccess != hasAllFilesAccess) {
                hasAllFilesAccess = filesAccess
            }
            if (notification != hasNotification) {
                hasNotification = notification
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Permissions") }) }) { paddingValues ->
        Column(
                modifier =
                        Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                    text = "Get Started",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                    text =
                            "To provide the best experience, Photo Search AI needs access to your files and notifications.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    PermissionListItem(
                            icon = Icons.Default.Folder,
                            title = "Files and Media",
                            description = "Required to search and organize your photos.",
                            isGranted = hasAllFilesAccess,
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    val intent =
                                            PermissionUiHelper.createAllFilesAccessIntent(context)
                                    intent?.let { allFilesAccessLauncher.launch(it) }
                                } else {
                                    storagePermissionLauncher.launch(
                                            Manifest.permission.READ_EXTERNAL_STORAGE
                                    )
                                }
                            }
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    item {
                        PermissionListItem(
                                icon = Icons.Default.Notifications,
                                title = "Notifications",
                                description = "Required to show progress of photo indexing.",
                                isGranted = hasNotification,
                                onClick = {
                                    notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                    )
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (hasAllFilesAccess && hasNotification) {
                Button(
                        onClick = onPermissionsGranted,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                ) { Text("Continue") }
            } else {
                Text(
                        text = "Please grant all permissions to continue",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 48.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionListItem(
        icon: ImageVector,
        title: String,
        description: String,
        isGranted: Boolean,
        onClick: () -> Unit
) {
    Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
                headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text(description) },
                leadingContent = {
                    Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint =
                                    if (isGranted) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingContent = {
                    if (isGranted) {
                        Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Granted",
                                tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Button(
                                onClick = onClick,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                        ) { Text("Allow", style = MaterialTheme.typography.labelLarge) }
                    }
                },
                colors =
                        ListItemDefaults.colors(
                                containerColor = androidx.compose.ui.graphics.Color.Transparent
                        )
        )
    }
}
