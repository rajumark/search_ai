package com.photo.searchai.core.permission

/**
 * Enum representing the different types of permissions the app requires. Maps to actual Android
 * permissions based on SDK version.
 */
enum class PermissionType {
    /**
     * All files access permission - MANAGE_EXTERNAL_STORAGE on Android 11+ READ_EXTERNAL_STORAGE on
     * older versions
     */
    ALL_FILES,

    /**
     * Notification permission - POST_NOTIFICATIONS on Android 13+ No permission required on older
     * versions
     */
    NOTIFICATION
}
