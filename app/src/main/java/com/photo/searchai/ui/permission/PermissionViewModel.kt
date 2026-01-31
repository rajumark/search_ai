package com.photo.searchai.ui.permission

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for managing permission state and navigation.
 */
@HiltViewModel
class PermissionViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow<PermissionUiState>(PermissionUiState.Initial)
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()
    
    private val _navigationEvent = MutableStateFlow<NavigationEvent>(NavigationEvent.None)
    val navigationEvent: StateFlow<NavigationEvent> = _navigationEvent.asStateFlow()
    
    // Track if we've requested permission before in this session
    private var hasRequestedPermission = false
    
    /**
     * Called when the permission check result is received.
     * @param isGranted Whether the permission is granted.
     */
    fun onPermissionCheckResult(isGranted: Boolean) {
        if (isGranted) {
            _uiState.value = PermissionUiState.Granted
            _navigationEvent.value = NavigationEvent.NavigateToHome
        }
    }
    
    /**
     * Called when the permission request result is received.
     * @param isGranted Whether the permission was granted.
     * @param shouldShowRationale Whether we can show rationale (permission not permanently denied).
     */
    fun onPermissionResult(isGranted: Boolean, shouldShowRationale: Boolean) {
        when {
            isGranted -> {
                _uiState.value = PermissionUiState.Granted
                _navigationEvent.value = NavigationEvent.NavigateToHome
            }
            shouldShowRationale -> {
                _uiState.value = PermissionUiState.Denied
            }
            else -> {
                // User denied and selected "Don't ask again" or denied twice
                _uiState.value = if (hasRequestedPermission) {
                    PermissionUiState.PermanentlyDenied
                } else {
                    PermissionUiState.Denied
                }
            }
        }
        hasRequestedPermission = true
    }
    
    /**
     * Called when navigation has been handled.
     */
    fun onNavigationHandled() {
        _navigationEvent.value = NavigationEvent.None
    }
    
    /**
     * Called when the user denies permission permanently (after second denial).
     */
    fun onPermanentlyDenied() {
        _uiState.value = PermissionUiState.PermanentlyDenied
    }
}
