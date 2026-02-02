package com.photo.searchai.app.ui.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun PermissionScreen(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current

    val permissionsToRequest =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.POST_NOTIFICATIONS
                )
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

    var arePermissionsGranted by remember {
        mutableStateOf(
                permissionsToRequest.all {
                    ContextCompat.checkSelfPermission(context, it) ==
                            PackageManager.PERMISSION_GRANTED
                }
        )
    }

    val launcher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { result ->
                arePermissionsGranted =
                        permissionsToRequest.all {
                            result[it] == true ||
                                    ContextCompat.checkSelfPermission(context, it) ==
                                            PackageManager.PERMISSION_GRANTED
                        }
                if (arePermissionsGranted) {
                    onPermissionsGranted()
                }
            }

    LaunchedEffect(Unit) {
        if (arePermissionsGranted) {
            onPermissionsGranted()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
                modifier = Modifier.padding(innerPadding).fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                    text = "Permissions Required",
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                    text =
                            "To function offline and notify you of progress, we need the following permissions:",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            StatusText(
                    text = "File Access: ${if (arePermissionsGranted) "Granted" else "Not Granted"}"
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                    onClick = { launcher.launch(permissionsToRequest) },
                    enabled = !arePermissionsGranted,
                    modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                        text =
                                if (arePermissionsGranted) "Permissions Granted"
                                else "Grant Permissions"
                )
            }
        }
    }
}

@Composable
private fun StatusText(text: String) {
    Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
    )
}
