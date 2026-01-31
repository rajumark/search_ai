package com.photo.searchai.ui.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

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
        null // No runtime permission needed below Android 13
    }
}

@Composable
fun PermissionScreen(
    viewModel: PermissionViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    val photoPermission = getPhotoPermission()
    val notificationPermission = getNotificationPermission()
    
    // Track permission states
    var isPhotoPermissionGranted by remember { mutableStateOf(false) }
    var isNotificationPermissionGranted by remember { mutableStateOf(false) }
    
    // Photo permission launcher
    val photoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isPhotoPermissionGranted = isGranted
        val shouldShowRationale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (context as? android.app.Activity)?.shouldShowRequestPermissionRationale(photoPermission) ?: false
        } else {
            false
        }
        viewModel.onPermissionResult(isGranted, shouldShowRationale)
    }
    
    // Notification permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isNotificationPermissionGranted = isGranted
    }
    
    // Check initial permission states on launch
    LaunchedEffect(Unit) {
        isPhotoPermissionGranted = ContextCompat.checkSelfPermission(
            context, photoPermission
        ) == PackageManager.PERMISSION_GRANTED
        
        isNotificationPermissionGranted = notificationPermission?.let {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        } ?: true // If no runtime permission needed, consider it granted
        
        viewModel.onPermissionCheckResult(isPhotoPermissionGranted)
    }
    
    // Refresh permission states when returning from settings
    LaunchedEffect(uiState) {
        isPhotoPermissionGranted = ContextCompat.checkSelfPermission(
            context, photoPermission
        ) == PackageManager.PERMISSION_GRANTED
        
        isNotificationPermissionGranted = notificationPermission?.let {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        } ?: true
    }
    
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Welcome to\nPhoto Search AI",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Grant permissions to unlock the full potential of intelligent photo search",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Photo Permission Card
                PermissionCard(
                    icon = Icons.Rounded.PhotoLibrary,
                    title = "Photo Library Access",
                    description = "Allow access to your photos so we can search and organize them using AI-powered text recognition.",
                    isGranted = isPhotoPermissionGranted,
                    isPermanentlyDenied = uiState is PermissionUiState.PermanentlyDenied,
                    onGrantClick = {
                        if (uiState is PermissionUiState.PermanentlyDenied) {
                            // Open settings
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } else {
                            photoPermissionLauncher.launch(photoPermission)
                        }
                    },
                    accentColor = Color(0xFF6366F1) // Indigo
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Notification Permission Card (only show on Android 13+)
                if (notificationPermission != null) {
                    PermissionCard(
                        icon = Icons.Rounded.Notifications,
                        title = "Notification Access",
                        description = "Get notified about the indexing progress and when your photos are ready to search.",
                        isGranted = isNotificationPermissionGranted,
                        isPermanentlyDenied = false,
                        onGrantClick = {
                            notificationPermissionLauncher.launch(notificationPermission)
                        },
                        accentColor = Color(0xFF10B981) // Emerald
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Continue button (only enabled when photo permission is granted)
                Button(
                    onClick = {
                        if (isPhotoPermissionGranted) {
                            viewModel.onPermissionCheckResult(true)
                        }
                    },
                    enabled = isPhotoPermissionGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (isPhotoPermissionGranted) "Continue" else "Grant Photo Access to Continue",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    isPermanentlyDenied: Boolean,
    onGrantClick: () -> Unit,
    accentColor: Color
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isGranted) 0.7f else 1f,
        label = "cardAlpha"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isGranted) {
            Color(0xFF10B981) // Green for granted
        } else {
            accentColor.copy(alpha = 0.3f)
        },
        label = "borderColor"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isGranted) {
            Color(0xFF10B981).copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "backgroundColor"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(animatedAlpha)
            .border(
                width = 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isGranted) 0.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with gradient background
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (isGranted) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF10B981),
                                        Color(0xFF059669)
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        accentColor,
                                        accentColor.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isGranted) Icons.Rounded.Check else icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (isGranted) {
                        Text(
                            text = "Permission Granted",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Grant button
            Button(
                onClick = onGrantClick,
                enabled = !isGranted,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) {
                        Color(0xFF10B981)
                    } else {
                        accentColor
                    },
                    disabledContainerColor = Color(0xFF10B981).copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.8f)
                )
            ) {
                Text(
                    text = when {
                        isGranted -> "Already Granted"
                        isPermanentlyDenied -> "Open Settings"
                        else -> "Grant Permission"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}
