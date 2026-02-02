package com.photo.searchai.core.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * UI-specific helper for permission-related UI operations. Handles rationale dialogs and settings
 * intent creation.
 */
object PermissionUiHelper {

    /** Get a description of what a permission is used for. */
    fun getPermissionDescription(permissionType: PermissionType): String {
        return when (permissionType) {
            PermissionType.ALL_FILES ->
                    "Access all files on your device to search and organize photos"
            PermissionType.NOTIFICATION -> "Show progress updates during background processing"
        }
    }

    /** Get a title for a permission. */
    fun getPermissionTitle(permissionType: PermissionType): String {
        return when (permissionType) {
            PermissionType.ALL_FILES -> "All Files Access"
            PermissionType.NOTIFICATION -> "Notifications"
        }
    }

    /** Get the button text for granting a permission. */
    fun getGrantButtonText(permissionType: PermissionType): String {
        return when (permissionType) {
            PermissionType.ALL_FILES -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    "Grant All Files Access"
                } else {
                    "Grant Storage Access"
                }
            }
            PermissionType.NOTIFICATION -> "Grant Notification Access"
        }
    }

    /** Create an intent to open the app's settings page. */
    fun createAppSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }

    /** Create an intent to request MANAGE_EXTERNAL_STORAGE permission (Android 11+). */
    fun createAllFilesAccessIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        } else {
            null
        }
    }

    /** Get rationale text explaining why a permission is needed. */
    fun getRationaleText(permissionType: PermissionType): String {
        return when (permissionType) {
            PermissionType.ALL_FILES ->
                    "This app needs access to all files on your device to search and index your photos. " +
                            "Without this permission, the app won't be able to find and organize your photos."
            PermissionType.NOTIFICATION ->
                    "This app needs notification permission to show you updates when processing your photos in the background."
        }
    }
}
