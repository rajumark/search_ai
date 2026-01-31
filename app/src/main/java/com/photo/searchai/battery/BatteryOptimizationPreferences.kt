package com.photo.searchai.battery

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.batteryPrefsDataStore: DataStore<Preferences> by
        preferencesDataStore(name = "battery_optimization_preferences")

/** DataStore for persisting battery optimization preferences and user decisions. */
@Singleton
class BatteryOptimizationPreferences @Inject constructor(private val context: Context) {
    private object Keys {
        val HAS_SEEN_RATIONALE = booleanPreferencesKey("has_seen_rationale")
        val USER_GRANTED_PERMISSION = booleanPreferencesKey("user_granted_permission")
        val USER_DENIED_COUNT = intPreferencesKey("user_denied_count")
        val LAST_PROMPT_TIME = longPreferencesKey("last_prompt_time")
        val USER_PERMANENTLY_DECLINED = booleanPreferencesKey("user_permanently_declined")
        val BACKGROUND_PROCESSING_ENABLED = booleanPreferencesKey("background_processing_enabled")
        val HAS_COMPLETED_OEM_SETUP = booleanPreferencesKey("has_completed_oem_setup")
        val NEVER_ASK_AGAIN = booleanPreferencesKey("never_ask_again")
    }

    companion object {
        // Minimum interval between prompts (7 days)
        const val MIN_PROMPT_INTERVAL_MS = 7L * 24 * 60 * 60 * 1000
        // Max denials before stopping prompts
        const val MAX_DENIAL_COUNT = 3
    }

    data class BatteryPreferencesState(
            val hasSeenRationale: Boolean = false,
            val userGrantedPermission: Boolean = false,
            val userDeniedCount: Int = 0,
            val lastPromptTime: Long = 0L,
            val userPermanentlyDeclined: Boolean = false,
            val backgroundProcessingEnabled: Boolean = true,
            val hasCompletedOemSetup: Boolean = false,
            val neverAskAgain: Boolean = false
    ) {
        /** Should we show the battery optimization prompt? */
        fun shouldShowPrompt(currentTime: Long = System.currentTimeMillis()): Boolean {
            // User has permanently declined or said never ask again
            if (userPermanentlyDeclined || neverAskAgain) return false

            // User has already granted permission
            if (userGrantedPermission) return false

            // Too many denials
            if (userDeniedCount >= MAX_DENIAL_COUNT) return false

            // Not enough time since last prompt
            if (lastPromptTime > 0 && (currentTime - lastPromptTime) < MIN_PROMPT_INTERVAL_MS) {
                return false
            }

            return true
        }
    }

    val stateFlow: Flow<BatteryPreferencesState> =
            context.batteryPrefsDataStore.data.map { prefs ->
                BatteryPreferencesState(
                        hasSeenRationale = prefs[Keys.HAS_SEEN_RATIONALE] ?: false,
                        userGrantedPermission = prefs[Keys.USER_GRANTED_PERMISSION] ?: false,
                        userDeniedCount = prefs[Keys.USER_DENIED_COUNT] ?: 0,
                        lastPromptTime = prefs[Keys.LAST_PROMPT_TIME] ?: 0L,
                        userPermanentlyDeclined = prefs[Keys.USER_PERMANENTLY_DECLINED] ?: false,
                        backgroundProcessingEnabled = prefs[Keys.BACKGROUND_PROCESSING_ENABLED]
                                        ?: true,
                        hasCompletedOemSetup = prefs[Keys.HAS_COMPLETED_OEM_SETUP] ?: false,
                        neverAskAgain = prefs[Keys.NEVER_ASK_AGAIN] ?: false
                )
            }

    suspend fun getState(): BatteryPreferencesState = stateFlow.first()

    suspend fun markRationaleSeen() {
        context.batteryPrefsDataStore.edit { prefs -> prefs[Keys.HAS_SEEN_RATIONALE] = true }
    }

    suspend fun recordUserGranted() {
        context.batteryPrefsDataStore.edit { prefs ->
            prefs[Keys.USER_GRANTED_PERMISSION] = true
            prefs[Keys.USER_DENIED_COUNT] = 0
        }
    }

    suspend fun recordUserDenied() {
        context.batteryPrefsDataStore.edit { prefs ->
            val currentCount = prefs[Keys.USER_DENIED_COUNT] ?: 0
            prefs[Keys.USER_DENIED_COUNT] = currentCount + 1
            prefs[Keys.LAST_PROMPT_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun recordUserPermanentlyDeclined() {
        context.batteryPrefsDataStore.edit { prefs -> prefs[Keys.USER_PERMANENTLY_DECLINED] = true }
    }

    suspend fun setNeverAskAgain(value: Boolean) {
        context.batteryPrefsDataStore.edit { prefs -> prefs[Keys.NEVER_ASK_AGAIN] = value }
    }

    suspend fun setBackgroundProcessingEnabled(enabled: Boolean) {
        context.batteryPrefsDataStore.edit { prefs ->
            prefs[Keys.BACKGROUND_PROCESSING_ENABLED] = enabled
        }
    }

    suspend fun markOemSetupCompleted() {
        context.batteryPrefsDataStore.edit { prefs -> prefs[Keys.HAS_COMPLETED_OEM_SETUP] = true }
    }

    suspend fun recordPromptShown() {
        context.batteryPrefsDataStore.edit { prefs ->
            prefs[Keys.LAST_PROMPT_TIME] = System.currentTimeMillis()
        }
    }

    /** Reset all preferences (for testing or user request) */
    suspend fun resetAll() {
        context.batteryPrefsDataStore.edit { it.clear() }
    }
}
