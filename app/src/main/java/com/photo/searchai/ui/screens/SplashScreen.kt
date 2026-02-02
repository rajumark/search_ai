package com.photo.searchai.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.photo.searchai.core.permission.PermissionChecker
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onPermissionsGranted: () -> Unit, onPermissionsMissing: () -> Unit) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        delay(1500) // Slightly longer for the clean M3 look to settle

        if (PermissionChecker.hasAllPermissions(context)) {
            onPermissionsGranted()
        } else {
            onPermissionsMissing()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                    text = "Photo Search AI",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
