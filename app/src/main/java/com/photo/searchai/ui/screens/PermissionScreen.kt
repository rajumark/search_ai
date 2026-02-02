package com.photo.searchai.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photo.searchai.core.permission.PermissionChecker
import com.photo.searchai.core.permission.PermissionType
import com.photo.searchai.core.permission.PermissionUiHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Premium color palette
private val GradientStart = Color(0xFF6366F1) // Indigo
private val GradientEnd = Color(0xFF8B5CF6) // Purple
private val SuccessGreen = Color(0xFF10B981) // Emerald
private val SuccessGreenLight = Color(0xFF34D399)
private val CardBackground = Color(0xFF1E1B4B) // Deep indigo
private val SurfaceColor = Color(0xFF0F0D2E) // Very dark purple
private val TextPrimary = Color(0xFFF8FAFC)
private val TextSecondary = Color(0xFF94A3B8)

@Composable
fun PermissionScreen(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Permission states
    var hasAllFilesAccess by remember {
        mutableStateOf(PermissionChecker.hasPermission(context, PermissionType.ALL_FILES))
    }
    var hasNotification by remember {
        mutableStateOf(PermissionChecker.hasPermission(context, PermissionType.NOTIFICATION))
    }

    // Animation states for success
    var showFilesSuccess by remember { mutableStateOf(hasAllFilesAccess) }
    var showNotificationSuccess by remember { mutableStateOf(hasNotification) }

    // Launcher for notification permission
    val notificationPermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    isGranted ->
                hasNotification = isGranted
                showNotificationSuccess = isGranted
            }

    // Launcher for storage permission (Android 10 and below)
    val storagePermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    isGranted ->
                hasAllFilesAccess = isGranted
                showFilesSuccess = isGranted
            }

    // Launcher for all files access settings (Android 11+)
    val allFilesAccessLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                // Check permission status when returning from settings
                val hasPermission =
                        PermissionChecker.hasPermission(context, PermissionType.ALL_FILES)
                hasAllFilesAccess = hasPermission
                showFilesSuccess = hasPermission
            }

    // Check if all permissions are granted
    LaunchedEffect(hasAllFilesAccess, hasNotification) {
        if (hasAllFilesAccess && hasNotification) {
            delay(800) // Let the user see the success animation
            onPermissionsGranted()
        }
    }

    // Refresh permission state periodically when returning from settings
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            val filesAccess = PermissionChecker.hasPermission(context, PermissionType.ALL_FILES)
            val notification = PermissionChecker.hasPermission(context, PermissionType.NOTIFICATION)

            if (filesAccess != hasAllFilesAccess) {
                hasAllFilesAccess = filesAccess
                showFilesSuccess = filesAccess
            }
            if (notification != hasNotification) {
                hasNotification = notification
                showNotificationSuccess = notification
            }
        }
    }

    Box(
            modifier =
                    Modifier.fillMaxSize()
                            .background(
                                    Brush.verticalGradient(
                                            colors =
                                                    listOf(
                                                            SurfaceColor,
                                                            CardBackground.copy(alpha = 0.3f),
                                                            SurfaceColor
                                                    )
                                    )
                            )
    ) {
        // Decorative circles in background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                    color = GradientStart.copy(alpha = 0.1f),
                    radius = 400f,
                    center = Offset(size.width * 0.8f, size.height * 0.2f)
            )
            drawCircle(
                    color = GradientEnd.copy(alpha = 0.1f),
                    radius = 300f,
                    center = Offset(size.width * 0.2f, size.height * 0.7f)
            )
        }

        Column(
                modifier = Modifier.fillMaxSize().padding(24.dp).statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Header with gradient text effect
            Text(
                    text = "Welcome",
                    fontSize = 16.sp,
                    color = TextSecondary,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                    text = "Enable Permissions",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                    text =
                            "Grant the permissions below to unlock\nthe full power of Photo Search AI",
                    fontSize = 16.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // All Files Access Card
            PermissionCard(
                    icon = Icons.Default.Folder,
                    title = "All Files Access",
                    description = "Search and organize all your photos",
                    isGranted = hasAllFilesAccess,
                    showSuccessAnimation = showFilesSuccess,
                    onRequestPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val intent = PermissionUiHelper.createAllFilesAccessIntent(context)
                            intent?.let { allFilesAccessLauncher.launch(it) }
                        } else {
                            storagePermissionLauncher.launch(
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                            )
                        }
                    }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Notification Permission Card (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionCard(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        description = "Get updates on background processing",
                        isGranted = hasNotification,
                        showSuccessAnimation = showNotificationSuccess,
                        onRequestPermission = {
                            notificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                            )
                        }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Progress indicator
            ProgressIndicator(
                    totalPermissions =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) 2 else 1,
                    grantedPermissions = listOf(hasAllFilesAccess, hasNotification).count { it }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PermissionCard(
        icon: ImageVector,
        title: String,
        description: String,
        isGranted: Boolean,
        showSuccessAnimation: Boolean,
        onRequestPermission: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    val successScale =
            animateFloatAsState(
                    targetValue = if (showSuccessAnimation) 1f else 0f,
                    animationSpec =
                            spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                            ),
                    label = "successScale"
            )

    LaunchedEffect(showSuccessAnimation) {
        if (showSuccessAnimation) {
            scale.animateTo(1.05f, spring(stiffness = Spring.StiffnessHigh))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    Card(
            modifier = Modifier.fillMaxWidth().scale(scale.value),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.8f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container with gradient background
            Box(
                    modifier =
                            Modifier.size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                            if (isGranted) {
                                                Brush.linearGradient(
                                                        listOf(SuccessGreen, SuccessGreenLight)
                                                )
                                            } else {
                                                Brush.linearGradient(
                                                        listOf(GradientStart, GradientEnd)
                                                )
                                            }
                                    ),
                    contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                        visible = isGranted,
                        enter =
                                scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
                                        fadeIn(),
                        exit = scaleOut() + fadeOut()
                ) {
                    Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Granted",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                        visible = !isGranted,
                        enter = fadeIn(),
                        exit = fadeOut()
                ) {
                    Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, fontSize = 14.sp, color = TextSecondary)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Status/Action
            androidx.compose.animation.AnimatedVisibility(
                    visible = isGranted,
                    enter =
                            scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
                                    fadeIn(),
                    exit = scaleOut() + fadeOut()
            ) {
                Box(
                        modifier =
                                Modifier.size(48.dp)
                                        .clip(CircleShape)
                                        .background(SuccessGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                ) {
                    AnimatedCheckmark(
                            isVisible = showSuccessAnimation,
                            modifier = Modifier.size(28.dp)
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                    visible = !isGranted,
                    enter = fadeIn(),
                    exit = fadeOut()
            ) {
                Button(
                        onClick = onRequestPermission,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GradientStart),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) { Text(text = "Grant", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
            }
        }
    }
}

@Composable
private fun AnimatedCheckmark(isVisible: Boolean, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(100) // Small delay for better effect
            progress.animateTo(1f, tween(400))
        } else {
            progress.snapTo(0f)
        }
    }

    Canvas(modifier = modifier) {
        val strokeWidth = 3.dp.toPx()
        val checkProgress = progress.value

        // Draw checkmark path
        val startX = size.width * 0.2f
        val startY = size.height * 0.5f
        val midX = size.width * 0.4f
        val midY = size.height * 0.75f
        val endX = size.width * 0.8f
        val endY = size.height * 0.25f

        if (checkProgress > 0) {
            // First stroke (down)
            val firstStrokeEnd =
                    if (checkProgress < 0.5f) {
                        Offset(
                                startX + (midX - startX) * (checkProgress * 2),
                                startY + (midY - startY) * (checkProgress * 2)
                        )
                    } else {
                        Offset(midX, midY)
                    }

            drawLine(
                    color = SuccessGreen,
                    start = Offset(startX, startY),
                    end = firstStrokeEnd,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
            )

            // Second stroke (up)
            if (checkProgress > 0.5f) {
                val secondProgress = (checkProgress - 0.5f) * 2
                drawLine(
                        color = SuccessGreen,
                        start = Offset(midX, midY),
                        end =
                                Offset(
                                        midX + (endX - midX) * secondProgress,
                                        midY + (endY - midY) * secondProgress
                                ),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun ProgressIndicator(totalPermissions: Int, grantedPermissions: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(totalPermissions) { index ->
                Box(
                        modifier =
                                Modifier.size(width = 40.dp, height = 4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                                if (index < grantedPermissions) {
                                                    Brush.linearGradient(
                                                            listOf(SuccessGreen, SuccessGreenLight)
                                                    )
                                                } else {
                                                    Brush.linearGradient(
                                                            listOf(
                                                                    TextSecondary.copy(
                                                                            alpha = 0.3f
                                                                    ),
                                                                    TextSecondary.copy(alpha = 0.3f)
                                                            )
                                                    )
                                                }
                                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
                text =
                        if (grantedPermissions == totalPermissions) {
                            "All permissions granted! ✨"
                        } else {
                            "$grantedPermissions of $totalPermissions permissions granted"
                        },
                fontSize = 14.sp,
                color = if (grantedPermissions == totalPermissions) SuccessGreen else TextSecondary
        )
    }
}
