package com.photo.searchai.ui.permission

/**
 * Navigation events emitted by the PermissionViewModel.
 */
sealed class NavigationEvent {
    /**
     * No navigation event.
     */
    data object None : NavigationEvent()
    
    /**
     * Navigate to the home screen.
     */
    data object NavigateToHome : NavigationEvent()
}
