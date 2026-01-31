package com.photo.searchai.battery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for battery optimization settings screen. Manages the flow of requesting battery
 * optimization exemption.
 */
@HiltViewModel
class BatteryOptimizationViewModel
@Inject
constructor(
        private val batteryHelper: BatteryOptimizationHelper,
        private val batteryPreferences: BatteryOptimizationPreferences
) : ViewModel() {

    data class UiState(
            // Current optimization state
            val isWhitelisted: Boolean = false,
            val hasOemBatteryManagement: Boolean = false,
            val manufacturerName: String = "",
            val manufacturerInstructions: String = "",

            // Dialog states
            val showRationaleDialog: Boolean = false,
            val showOemDialog: Boolean = false,
            val showSuccessMessage: Boolean = false,
            val showDeniedMessage: Boolean = false,

            // User preferences
            val backgroundProcessingEnabled: Boolean = true,
            val hasCompletedOemSetup: Boolean = false,
            val shouldShowPrompt: Boolean = false,

            // Device info
            val deviceInfo: String = "",
            val isInPowerSaveMode: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Intents to launch
    private val _launchIntent = MutableStateFlow<Intent?>(null)
    val launchIntent: StateFlow<Intent?> = _launchIntent.asStateFlow()

    init {
        refreshState()
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            batteryPreferences.stateFlow.collect { prefs ->
                _uiState.update { state ->
                    state.copy(
                            backgroundProcessingEnabled = prefs.backgroundProcessingEnabled,
                            hasCompletedOemSetup = prefs.hasCompletedOemSetup,
                            shouldShowPrompt = prefs.shouldShowPrompt()
                    )
                }
            }
        }
    }

    /** Refresh the current battery optimization state */
    fun refreshState() {
        val manufacturer = batteryHelper.getDeviceManufacturer()

        _uiState.update { state ->
            state.copy(
                    isWhitelisted = batteryHelper.isIgnoringBatteryOptimizations(),
                    hasOemBatteryManagement = batteryHelper.hasOemBatteryManagement(),
                    manufacturerName =
                            manufacturer.name.lowercase().replaceFirstChar { it.uppercase() },
                    manufacturerInstructions = batteryHelper.getManufacturerInstructions(),
                    deviceInfo =
                            "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})",
                    isInPowerSaveMode = batteryHelper.isInPowerSaveMode()
            )
        }
    }

    /** Called when user action is needed to enable background processing */
    fun requestBatteryOptimization() {
        viewModelScope.launch {
            batteryPreferences.markRationaleSeen()
            batteryPreferences.recordPromptShown()
        }

        _uiState.update { it.copy(showRationaleDialog = true) }
    }

    /** User confirmed they want to proceed with battery optimization request */
    fun onRationaleAccepted() {
        _uiState.update { it.copy(showRationaleDialog = false) }

        // Launch the system battery optimization settings
        _launchIntent.value = batteryHelper.getIgnoreBatteryOptimizationsIntent()
    }

    /** User declined the rationale dialog */
    fun onRationaleDeclined() {
        _uiState.update { it.copy(showRationaleDialog = false) }

        viewModelScope.launch { batteryPreferences.recordUserDenied() }

        _uiState.update { it.copy(showDeniedMessage = true) }
    }

    /** User selected "Never ask again" */
    fun onNeverAskAgain() {
        _uiState.update { it.copy(showRationaleDialog = false) }

        viewModelScope.launch { batteryPreferences.setNeverAskAgain(true) }
    }

    /** Called after returning from system battery settings */
    fun onReturnFromBatterySettings() {
        _launchIntent.value = null
        refreshState()

        val isWhitelisted = batteryHelper.isIgnoringBatteryOptimizations()

        if (isWhitelisted) {
            viewModelScope.launch { batteryPreferences.recordUserGranted() }

            // Check if OEM setup is needed
            if (batteryHelper.hasOemBatteryManagement()) {
                _uiState.update { it.copy(showOemDialog = true) }
            } else {
                _uiState.update { it.copy(showSuccessMessage = true) }
            }
        } else {
            viewModelScope.launch { batteryPreferences.recordUserDenied() }
            _uiState.update { it.copy(showDeniedMessage = true) }
        }
    }

    /** User wants to configure OEM-specific settings */
    fun onOemSetupRequested() {
        _uiState.update { it.copy(showOemDialog = false) }

        val oemIntent = batteryHelper.getOemBatteryManagementIntent()
        if (oemIntent != null) {
            _launchIntent.value = oemIntent
        } else {
            // Fallback to app settings
            _launchIntent.value =
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${batteryHelper.getPackageName()}")
                    }
        }
    }

    /** User completed or skipped OEM setup */
    fun onOemSetupCompleted() {
        _uiState.update { it.copy(showOemDialog = false, showSuccessMessage = true) }

        viewModelScope.launch { batteryPreferences.markOemSetupCompleted() }
    }

    /** User skipped OEM setup */
    fun onOemSetupSkipped() {
        _uiState.update { it.copy(showOemDialog = false) }
    }

    /** Clear intent after it has been launched */
    fun onIntentLaunched() {
        _launchIntent.value = null
    }

    /** Toggle background processing enabled/disabled */
    fun setBackgroundProcessingEnabled(enabled: Boolean) {
        viewModelScope.launch { batteryPreferences.setBackgroundProcessingEnabled(enabled) }
    }

    /** Dismiss success message */
    fun dismissSuccessMessage() {
        _uiState.update { it.copy(showSuccessMessage = false) }
    }

    /** Dismiss denied message */
    fun dismissDeniedMessage() {
        _uiState.update { it.copy(showDeniedMessage = false) }
    }

    /** Open app details settings as fallback */
    fun openAppSettings(context: Context) {
        val intent =
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
        _launchIntent.value = intent
    }

    /** Get the full battery optimization flow intent sequence */
    fun getFullOptimizationFlow(): List<Intent> {
        val intents = mutableListOf<Intent>()

        // Standard battery optimization
        if (!batteryHelper.isIgnoringBatteryOptimizations()) {
            intents.add(batteryHelper.getIgnoreBatteryOptimizationsIntent())
        }

        // OEM-specific
        batteryHelper.getOemBatteryManagementIntent()?.let { intents.add(it) }

        return intents
    }

    /** Get package name for building intents */
    fun getPackageName(): String = batteryHelper.getPackageName()
}
