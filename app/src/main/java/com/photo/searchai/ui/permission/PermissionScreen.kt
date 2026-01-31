package com.photo.searchai.ui.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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

@Composable
fun PermissionScreen(
    viewModel: PermissionViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    val permission = getPhotoPermission()
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val shouldShowRationale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (context as? android.app.Activity)?.shouldShowRequestPermissionRationale(permission) ?: false
        } else {
            false
        }
        viewModel.onPermissionResult(isGranted, shouldShowRationale)
    }
    
    // Check initial permission state on launch
    LaunchedEffect(Unit) {
        val isGranted = context.checkSelfPermission(permission) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionCheckResult(isGranted)
    }
    
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Icon(
                imageVector = Icons.Rounded.PhotoLibrary,
                contentDescription = "Photos",
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title
            Text(
                text = "Access your photos",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description
            Text(
                text = when (uiState) {
                    is PermissionUiState.Initial, is PermissionUiState.Granted -> {
                        "To help you search and organize your photos, we need access to your photo library."
                    }
                    is PermissionUiState.Denied -> {
                        "Photo access was denied. Please grant permission to continue using the app."
                    }
                    is PermissionUiState.PermanentlyDenied -> {
                        "Photo access was permanently denied. Please enable it in Settings to continue."
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Primary action button
            when (uiState) {
                is PermissionUiState.Initial, is PermissionUiState.Denied -> {
                    Button(
                        onClick = { permissionLauncher.launch(permission) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(text = "Allow Access")
                    }
                }
                is PermissionUiState.PermanentlyDenied -> {
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(text = "Open Settings")
                    }
                }
                is PermissionUiState.Granted -> {
                    // No button needed, navigation will happen
                }
            }
            
            // Secondary action for denied state
            if (uiState is PermissionUiState.Denied) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.onPermanentlyDenied()
                    }
                ) {
                    Text(text = "Open Settings Instead")
                }
            }
        }
    }
}
