package com.photo.searchai.ui.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Permission types that can be requested.
 */
enum class PermissionType {
    PHOTO_FULL_ACCESS,
    NOTIFICATIONS
}

/**
 * Permission status for each permission type.
 */
data class PermissionStatus(
    val isGranted: Boolean,
    val isPartialAccess: Boolean = false,
    val needsSettings: Boolean = false
)

/**
 * Gets the appropriate photo permission based on API level.
 */
private fun getPhotoPermission(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

/**
 * Gets the notification permission (only required on Android 13+).
 */
private fun getNotificationPermission(): String? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        null
    }
}

@Composable
fun PermissionScreen(
    viewModel: PermissionViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    
    val photoPermission = getPhotoPermission()
    val notificationPermission = getNotificationPermission()
    
    // Track permission states
    var photoStatus by remember { mutableStateOf(PermissionStatus(isGranted = false)) }
    var notificationStatus by remember { mutableStateOf(PermissionStatus(isGranted = false)) }
    
    // Function to refresh all permission states and auto-navigate if all granted
    fun refreshPermissions() {
        // Photo permission check
        val isPhotoGranted = ContextCompat.checkSelfPermission(
            context, photoPermission
        ) == PackageManager.PERMISSION_GRANTED
        
        // On Android 14+, check for partial access
        val isPartialAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val partialPermission = Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            val hasPartial = ContextCompat.checkSelfPermission(
                context, partialPermission
            ) == PackageManager.PERMISSION_GRANTED
            hasPartial && !isPhotoGranted
        } else {
            false
        }
        
        photoStatus = PermissionStatus(
            isGranted = isPhotoGranted,
            isPartialAccess = isPartialAccess,
            needsSettings = isPartialAccess
        )
        
        // Notification permission check
        notificationStatus = notificationPermission?.let {
            PermissionStatus(
                isGranted = ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            )
        } ?: PermissionStatus(isGranted = true)
        
        // Auto-navigate when ALL permissions are granted
        val allPermissionsGranted = isPhotoGranted && !isPartialAccess && 
            (notificationPermission == null || notificationStatus.isGranted)
        
        if (allPermissionsGranted) {
            viewModel.onPermissionCheckResult(true)
        }
    }
    
    // Photo permission launcher
    val photoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        refreshPermissions()
        if (!isGranted) {
            val shouldShowRationale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                (context as? android.app.Activity)?.shouldShowRequestPermissionRationale(photoPermission) ?: false
            } else {
                false
            }
            viewModel.onPermissionResult(isGranted, shouldShowRationale)
        }
    }
    
    // Notification permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        refreshPermissions()
    }
    
    // Check initial permission states on launch
    LaunchedEffect(Unit) {
        refreshPermissions()
    }
    
    // Refresh permissions when returning from settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Helper to open app settings
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }
    
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp)
            )
            
            Text(
                text = "Photo Search AI needs these permissions to work properly",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Photo Permission Row
            PermissionRow(
                icon = Icons.Outlined.PhotoLibrary,
                title = "Photos and videos",
                subtitle = when {
                    photoStatus.isGranted -> "Allowed"
                    photoStatus.isPartialAccess -> "Limited access · Tap to allow all"
                    else -> "Not allowed"
                },
                isChecked = photoStatus.isGranted || photoStatus.isPartialAccess,
                onClick = {
                    if (photoStatus.isPartialAccess || uiState is PermissionUiState.PermanentlyDenied) {
                        openAppSettings()
                    } else if (!photoStatus.isGranted) {
                        photoPermissionLauncher.launch(photoPermission)
                    }
                },
                onCheckedChange = { checked ->
                    if (checked) {
                        if (photoStatus.isPartialAccess) {
                            openAppSettings()
                        } else {
                            photoPermissionLauncher.launch(photoPermission)
                        }
                    } else {
                        openAppSettings()
                    }
                }
            )
            
            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            // Notification Permission Row (only on Android 13+)
            if (notificationPermission != null) {
                PermissionRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Notifications",
                    subtitle = if (notificationStatus.isGranted) "Allowed" else "Not allowed",
                    isChecked = notificationStatus.isGranted,
                    onClick = {
                        if (!notificationStatus.isGranted) {
                            notificationPermissionLauncher.launch(notificationPermission)
                        } else {
                            openAppSettings()
                        }
                    },
                    onCheckedChange = { checked ->
                        if (checked) {
                            notificationPermissionLauncher.launch(notificationPermission)
                        } else {
                            openAppSettings()
                        }
                    }
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Info text
            Text(
                text = "Your data stays on your device. We never upload your photos to any server.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            // Partial access warning
            if (photoStatus.isPartialAccess) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "You've selected \"Allow limited access\". To search all your photos, tap Photos and videos above and select \"Allow all\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}
