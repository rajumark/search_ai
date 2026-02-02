package com.photo.searchai.core.permission

/** Represents the state of a permission request result. */
sealed class PermissionState {
    /** Permission has been granted by the user. */
    data object Granted : PermissionState()

    /** Permission was denied by the user. */
    data object Denied : PermissionState()

    /**
     * Permission was permanently denied (user selected "Don't ask again"). User must go to app
     * settings to grant the permission.
     */
    data object PermanentlyDenied : PermissionState()

    /** Permission has not been requested yet. */
    data object NotRequested : PermissionState()

    val isGranted: Boolean
        get() = this is Granted
}
