package com.photo.searchai.core.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat

/**
 * Pure logic for checking permission states. No UI dependencies, can be used from Workers and
 * ViewModels.
 */
object PermissionChecker {

    /**
     * Check if a specific permission type is granted.
     *
     * @param context The context to use for permission checking
     * @param permissionType The type of permission to check
     * @return true if the permission is granted, false otherwise
     */
    fun hasPermission(context: Context, permissionType: PermissionType): Boolean {
        return when (permissionType) {
            PermissionType.ALL_FILES -> hasAllFilesAccess(context)
            PermissionType.NOTIFICATION -> hasNotificationPermission(context)
        }
    }

    /**
     * Check if all required permissions are granted.
     *
     * @param context The context to use for permission checking
     * @return true if all permissions are granted, false otherwise
     */
    fun hasAllPermissions(context: Context): Boolean {
        return PermissionType.entries.all { hasPermission(context, it) }
    }

    /**
     * Check if MANAGE_EXTERNAL_STORAGE permission is granted (Android 11+) or READ_EXTERNAL_STORAGE
     * for older versions.
     */
    private fun hasAllFilesAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ uses MANAGE_EXTERNAL_STORAGE
            Environment.isExternalStorageManager()
        } else {
            // Older versions use READ_EXTERNAL_STORAGE
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if POST_NOTIFICATIONS permission is granted (Android 13+). Returns true for older
     * versions where it's not required.
     */
    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            // Not required below Android 13
            true
        }
    }
}
