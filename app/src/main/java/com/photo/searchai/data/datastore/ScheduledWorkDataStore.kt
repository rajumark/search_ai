package com.photo.searchai.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.scheduledWorkDataStore: DataStore<Preferences> by
        preferencesDataStore(name = "scheduled_work_preferences")

/**
 * DataStore for managing scheduled WorkManager configuration. Tracks when periodic work was last
 * scheduled/run and ensures reliable 6-hour scheduling.
 */
class ScheduledWorkDataStore @Inject constructor(private val context: Context) {

    private object Keys {
        val PERIODIC_WORK_ENABLED = booleanPreferencesKey("periodic_work_enabled")
        val LAST_SCHEDULED_TIME = longPreferencesKey("last_scheduled_time")
        val LAST_RUN_START_TIME = longPreferencesKey("last_run_start_time")
        val LAST_RUN_END_TIME = longPreferencesKey("last_run_end_time")
        val LAST_RUN_SUCCESS = booleanPreferencesKey("last_run_success")
        val CONSECUTIVE_FAILURES = intPreferencesKey("consecutive_failures")
        val TOTAL_RUNS = intPreferencesKey("total_runs")
        val TOTAL_SUCCESSFUL_RUNS = intPreferencesKey("total_successful_runs")
        val NEXT_SCHEDULED_TIME = longPreferencesKey("next_scheduled_time")
    }

    companion object {
        const val PERIODIC_INTERVAL_HOURS = 6L
        const val PERIODIC_INTERVAL_MS = PERIODIC_INTERVAL_HOURS * 60 * 60 * 1000
        const val MAX_CONSECUTIVE_FAILURES = 3
    }

    data class ScheduledWorkState(
            val isEnabled: Boolean = true,
            val lastScheduledTime: Long = 0L,
            val lastRunStartTime: Long = 0L,
            val lastRunEndTime: Long = 0L,
            val lastRunSuccess: Boolean = true,
            val consecutiveFailures: Int = 0,
            val totalRuns: Int = 0,
            val totalSuccessfulRuns: Int = 0,
            val nextScheduledTime: Long = 0L
    ) {
        val isOverdue: Boolean
            get() {
                if (nextScheduledTime == 0L) return false
                return System.currentTimeMillis() >
                        nextScheduledTime + (30 * 60 * 1000) // 30 min grace
            }

        val timeSinceLastRun: Long
            get() = if (lastRunEndTime > 0) System.currentTimeMillis() - lastRunEndTime else 0L

        val shouldRunNow: Boolean
            get() = timeSinceLastRun >= PERIODIC_INTERVAL_MS || lastRunEndTime == 0L
    }

    val stateFlow: Flow<ScheduledWorkState> =
            context.scheduledWorkDataStore.data.map { prefs ->
                ScheduledWorkState(
                        isEnabled = prefs[Keys.PERIODIC_WORK_ENABLED] ?: true,
                        lastScheduledTime = prefs[Keys.LAST_SCHEDULED_TIME] ?: 0L,
                        lastRunStartTime = prefs[Keys.LAST_RUN_START_TIME] ?: 0L,
                        lastRunEndTime = prefs[Keys.LAST_RUN_END_TIME] ?: 0L,
                        lastRunSuccess = prefs[Keys.LAST_RUN_SUCCESS] ?: true,
                        consecutiveFailures = prefs[Keys.CONSECUTIVE_FAILURES] ?: 0,
                        totalRuns = prefs[Keys.TOTAL_RUNS] ?: 0,
                        totalSuccessfulRuns = prefs[Keys.TOTAL_SUCCESSFUL_RUNS] ?: 0,
                        nextScheduledTime = prefs[Keys.NEXT_SCHEDULED_TIME] ?: 0L
                )
            }

    suspend fun getState(): ScheduledWorkState = stateFlow.first()

    suspend fun setPeriodicWorkEnabled(enabled: Boolean) {
        context.scheduledWorkDataStore.edit { prefs -> prefs[Keys.PERIODIC_WORK_ENABLED] = enabled }
    }

    suspend fun recordWorkScheduled() {
        val now = System.currentTimeMillis()
        context.scheduledWorkDataStore.edit { prefs ->
            prefs[Keys.LAST_SCHEDULED_TIME] = now
            prefs[Keys.NEXT_SCHEDULED_TIME] = now + PERIODIC_INTERVAL_MS
        }
    }

    suspend fun recordRunStarted() {
        context.scheduledWorkDataStore.edit { prefs ->
            prefs[Keys.LAST_RUN_START_TIME] = System.currentTimeMillis()
            prefs[Keys.TOTAL_RUNS] = (prefs[Keys.TOTAL_RUNS] ?: 0) + 1
        }
    }

    suspend fun recordRunCompleted(success: Boolean, errorMessage: String? = null) {
        val now = System.currentTimeMillis()
        context.scheduledWorkDataStore.edit { prefs ->
            prefs[Keys.LAST_RUN_END_TIME] = now
            prefs[Keys.LAST_RUN_SUCCESS] = success
            prefs[Keys.NEXT_SCHEDULED_TIME] = now + PERIODIC_INTERVAL_MS

            if (success) {
                prefs[Keys.CONSECUTIVE_FAILURES] = 0
                prefs[Keys.TOTAL_SUCCESSFUL_RUNS] = (prefs[Keys.TOTAL_SUCCESSFUL_RUNS] ?: 0) + 1
            } else {
                prefs[Keys.CONSECUTIVE_FAILURES] = (prefs[Keys.CONSECUTIVE_FAILURES] ?: 0) + 1
            }
        }
    }

    suspend fun resetFailureCount() {
        context.scheduledWorkDataStore.edit { prefs -> prefs[Keys.CONSECUTIVE_FAILURES] = 0 }
    }

    suspend fun clearAll() {
        context.scheduledWorkDataStore.edit { it.clear() }
    }
}
