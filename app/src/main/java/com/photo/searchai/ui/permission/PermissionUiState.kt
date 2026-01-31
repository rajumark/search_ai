package com.photo.searchai.ui.permission

/**
 * UI state for the permission screen.
 */
sealed class PermissionUiState {
    /**
     * Initial state before permission check.
     */
    data object Initial : PermissionUiState()
    
    /**
     * Permission has been granted.
     */
    data object Granted : PermissionUiState()
    
    /**
     * Permission was denied (can still request again).
     */
    data object Denied : PermissionUiState()
    
    /**
     * Permission was permanently denied (user selected "Don't ask again").
     */
    data object PermanentlyDenied : PermissionUiState()
}
