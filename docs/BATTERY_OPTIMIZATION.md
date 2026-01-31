# Battery Optimization & Background Execution Guide

This document provides technical guidance on the battery optimization and background execution implementation for Photo Search AI.

## Overview

The app is designed for side-loaded/enterprise deployment where Play Store policy compliance is not the primary concern. The implementation prioritizes **reliable background execution** while maintaining **minimal battery impact**.

## Architecture

### Components

1. **BatteryOptimizationHelper** - Manages battery optimization state and OEM-specific intents
2. **BatteryOptimizationPreferences** - DataStore for persisting user preferences
3. **BatteryOptimizationViewModel** - UI state management for battery settings
4. **BatteryOptimizationScreen** - User-facing configuration interface
5. **ImageProcessingService** - Foreground service for active processing
6. **PeriodicImageProcessingWorker** - WorkManager worker for scheduled processing

### Permission Flow

```
User opens app
    ↓
Check battery optimization state (PowerManager.isIgnoringBatteryOptimizations)
    ↓
If not whitelisted:
    ↓
Show rationale dialog explaining battery impact
    ↓
User confirms → Launch ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
    ↓
If OEM device detected (Xiaomi, Samsung, etc.):
    ↓
Show OEM-specific setup instructions
    ↓
Optionally launch OEM battery management activity
```

## Manifest Permissions

```xml
<!-- Required for foreground service -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

<!-- Battery optimization exemption request -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<!-- Wake lock (used judiciously) -->
<uses-permission android:name="android.permission.WAKE_LOCK" />

<!-- Notifications -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## Battery-Safe Design Principles

### 1. No Unnecessary Wake Locks

- WorkManager handles wake locks internally for expedited work
- Custom wake locks are avoided unless absolutely required
- If used, wake locks are PARTIAL_WAKE_LOCK with timeout

### 2. No Tight Loops or Busy Waiting

- Processing uses structured concurrency with coroutines
- Delay-based polling is avoided
- Flow-based reactive patterns for state updates

### 3. Adaptive Scheduling

```kotlin
// Check device state before heavy processing
fun isOptimalForProcessing(): Boolean {
    // Don't process if in power save mode
    if (powerManager.isPowerSaveMode) return false
    
    // Don't process in Doze mode
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (powerManager.isDeviceIdleMode) return false
    }
    
    return true
}
```

### 4. Batched Operations

- Database writes are batched
- Notification updates are throttled (1 per second max)
- Network requests (if any) are batched

### 5. Respect System Constraints

```kotlin
// WorkManager constraints for periodic work
val constraints = Constraints.Builder()
    .setRequiresBatteryNotLow(true)  // Don't drain low battery
    .build()
```

## OEM-Specific Handling

### Supported Manufacturers

| Manufacturer | OEM Layer | Auto-start Activity |
|-------------|-----------|---------------------|
| Xiaomi | MIUI | AutoStartManagementActivity |
| Samsung | OneUI | BatteryActivity |
| Huawei | EMUI | StartupNormalAppListActivity |
| OPPO | ColorOS | StartupAppListActivity |
| Vivo | FunTouch | BgStartUpManagerActivity |
| OnePlus | OxygenOS | ChainLaunchAppListActivity |
| Realme | Realme UI | StartupAppListActivity |
| Meizu | Flyme | PowerAppPermissionActivity |
| Nokia | Stock-like | PowerSaverExceptionActivity |

### Detection

```kotlin
fun getDeviceManufacturer(): DeviceManufacturer {
    val manufacturer = Build.MANUFACTURER.lowercase()
    val brand = Build.BRAND.lowercase()
    
    return when {
        manufacturer.contains("xiaomi") -> DeviceManufacturer.XIAOMI
        manufacturer.contains("samsung") -> DeviceManufacturer.SAMSUNG
        // ... etc
    }
}
```

## WorkManager Configuration

### Periodic Work (6 hours)

```kotlin
val periodicWorkRequest = PeriodicWorkRequestBuilder<PeriodicImageProcessingWorker>(
    repeatInterval = 6,
    repeatIntervalTimeUnit = TimeUnit.HOURS,
    flexTimeInterval = 30,  // Flexibility for battery optimization
    flexTimeIntervalUnit = TimeUnit.MINUTES
)
    .setConstraints(constraints)
    .setBackoffCriteria(
        BackoffPolicy.LINEAR,
        WorkRequest.MIN_BACKOFF_MILLIS,
        TimeUnit.MILLISECONDS
    )
    .build()

// Use KEEP policy to prevent duplicate scheduling
workManager.enqueueUniquePeriodicWork(
    uniqueWorkName,
    ExistingPeriodicWorkPolicy.KEEP,
    periodicWorkRequest
)
```

### Expedited Work (User-triggered)

```kotlin
val oneTimeRequest = OneTimeWorkRequestBuilder<ImageProcessingWorker>()
    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
    .build()
```

## Foreground Service

The foreground service is used for:
- User-initiated processing that needs progress updates
- Long-running operations that must complete

**NOT used for:**
- Scheduled background work (use WorkManager)
- Quick operations
- Anything that doesn't need user visibility

### Service Type

```xml
<service
    android:name=".service.ImageProcessingService"
    android:foregroundServiceType="dataSync" />
```

### Notification Updates

Notifications are batched to 1 update per second to minimize battery impact:

```kotlin
private const val NOTIFICATION_UPDATE_INTERVAL_MS = 1000L

private fun startNotificationUpdates() {
    notificationUpdateJob = serviceScope.launch {
        while (isActive) {
            updateNotification()
            delay(NOTIFICATION_UPDATE_INTERVAL_MS)
        }
    }
}
```

## Testing Recommendations

### 1. Doze Mode Testing

```bash
# Force device into Doze
adb shell dumpsys deviceidle force-idle

# Check if app is whitelisted
adb shell dumpsys deviceidle whitelist

# Exit Doze
adb shell dumpsys deviceidle unforce
```

### 2. App Standby Testing

```bash
# Set app to restricted bucket
adb shell am set-standby-bucket com.photo.searchai restricted

# Check bucket
adb shell am get-standby-bucket com.photo.searchai
```

### 3. Battery Stats

```bash
# Reset battery stats
adb shell dumpsys batterystats --reset

# Run app for a while, then:
adb shell dumpsys batterystats | grep com.photo.searchai
```

### 4. WorkManager Diagnostics

```bash
adb shell dumpsys jobscheduler | grep com.photo.searchai
```

## Success Criteria

1. ✅ App survives Doze mode when whitelisted
2. ✅ Periodic work runs reliably every 6 hours
3. ✅ Processing completes when app is in background
4. ✅ Battery impact < 1% per day average
5. ✅ Works on Android 10-14
6. ✅ Works across major OEM implementations

## Troubleshooting

### Work not running

1. Check if battery optimization is disabled
2. Check OEM-specific settings
3. Check WorkManager diagnostics
4. Verify constraints are satisfied

### High battery usage

1. Check for wake lock leaks: `adb shell dumpsys power | grep -i wake`
2. Review processing frequency
3. Check notification update frequency
4. Profile with Android Studio Profiler

### Killed by OEM battery manager

1. Enable auto-start permission
2. Lock app in recent apps (some OEMs)
3. Add to battery whitelist
4. Disable aggressive battery optimization in OEM settings
