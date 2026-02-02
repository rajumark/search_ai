package com.photo.searchai.core.permission

import android.Manifest
import android.os.Build

/** SDK checks and version-specific permission mappings. */
object PermissionConstants {

    /**
     * Returns the actual Android permission string for a permission type. Returns null if no
     * runtime permission is needed (e.g., special intent required).
     */
    fun getPermissionString(permissionType: PermissionType): String? {
        return when (permissionType) {
            PermissionType.ALL_FILES -> {
                // MANAGE_EXTERNAL_STORAGE requires special intent, not runtime permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    null // Requires Settings intent, not a runtime permission
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
            }
            PermissionType.NOTIFICATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else {
                    null // Not required on older versions
                }
            }
        }
    }

    /** Check if a permission type requires a special settings intent instead of runtime request. */
    fun requiresSettingsIntent(permissionType: PermissionType): Boolean {
        return when (permissionType) {
            PermissionType.ALL_FILES -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            PermissionType.NOTIFICATION -> false
        }
    }

    /** Check if a permission type is relevant for the current SDK version. */
    fun isPermissionRequired(permissionType: PermissionType): Boolean {
        return when (permissionType) {
            PermissionType.ALL_FILES -> true // Always required
            PermissionType.NOTIFICATION -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        }
    }

    /** Minimum SDK version for each permission type. */
    fun getMinSdkVersion(permissionType: PermissionType): Int {
        return when (permissionType) {
            PermissionType.ALL_FILES -> 1 // Always relevant
            PermissionType.NOTIFICATION -> Build.VERSION_CODES.TIRAMISU
        }
    }
}
