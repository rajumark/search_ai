package com.photo.searchai.feature.battery

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.getSystemService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for managing battery optimization permissions. Handles both standard Android and
 * OEM-specific battery optimizations.
 *
 * Supports:
 * - Standard Android Doze mode whitelist
 * - MIUI (Xiaomi) battery saver
 * - Samsung OneUI optimization
 * - Huawei/Honor battery management
 * - OPPO/OnePlus/Realme ColorOS
 * - Vivo FunTouch OS
 * - Meizu Flyme
 * - ASUS ZenUI
 * - Nokia (HMD)
 * - Lenovo/Motorola
 */
@Singleton
class BatteryOptimizationHelper @Inject constructor(private val context: Context) {
    private val powerManager: PowerManager? = context.getSystemService()
    private val packageManager: PackageManager = context.packageManager

    /** Current battery optimization state */
    enum class OptimizationState {
        /** App is whitelisted from battery optimizations */
        WHITELISTED,
        /** App is subject to battery optimizations */
        OPTIMIZED,
        /** Cannot determine optimization state */
        UNKNOWN
    }

    /** Detected device manufacturer type for OEM-specific handling */
    enum class DeviceManufacturer {
        XIAOMI,
        SAMSUNG,
        HUAWEI,
        OPPO,
        VIVO,
        ONEPLUS,
        REALME,
        MEIZU,
        ASUS,
        NOKIA,
        LENOVO,
        MOTOROLA,
        GOOGLE,
        OTHER
    }

    /** Check if app is whitelisted from battery optimizations */
    fun isIgnoringBatteryOptimizations(): Boolean {
        return try {
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } catch (e: Exception) {
            false
        }
    }

    /** Get current optimization state */
    fun getOptimizationState(): OptimizationState {
        return try {
            when {
                powerManager == null -> OptimizationState.UNKNOWN
                powerManager.isIgnoringBatteryOptimizations(context.packageName) ->
                        OptimizationState.WHITELISTED
                else -> OptimizationState.OPTIMIZED
            }
        } catch (e: Exception) {
            OptimizationState.UNKNOWN
        }
    }

    /** Detect the device manufacturer for OEM-specific handling */
    fun getDeviceManufacturer(): DeviceManufacturer {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()

        return when {
            manufacturer.contains("xiaomi") ||
                    brand.contains("xiaomi") ||
                    brand.contains("redmi") ||
                    brand.contains("poco") -> DeviceManufacturer.XIAOMI
            manufacturer.contains("samsung") -> DeviceManufacturer.SAMSUNG
            manufacturer.contains("huawei") || manufacturer.contains("honor") ->
                    DeviceManufacturer.HUAWEI
            manufacturer.contains("oppo") || brand.contains("oppo") -> DeviceManufacturer.OPPO
            manufacturer.contains("vivo") -> DeviceManufacturer.VIVO
            manufacturer.contains("oneplus") || brand.contains("oneplus") ->
                    DeviceManufacturer.ONEPLUS
            manufacturer.contains("realme") || brand.contains("realme") -> DeviceManufacturer.REALME
            manufacturer.contains("meizu") -> DeviceManufacturer.MEIZU
            manufacturer.contains("asus") -> DeviceManufacturer.ASUS
            manufacturer.contains("hmd") || brand.contains("nokia") -> DeviceManufacturer.NOKIA
            manufacturer.contains("lenovo") -> DeviceManufacturer.LENOVO
            manufacturer.contains("motorola") -> DeviceManufacturer.MOTOROLA
            manufacturer.contains("google") -> DeviceManufacturer.GOOGLE
            else -> DeviceManufacturer.OTHER
        }
    }

    /** Check if OEM has additional battery management that needs to be disabled */
    fun hasOemBatteryManagement(): Boolean {
        return when (getDeviceManufacturer()) {
            DeviceManufacturer.XIAOMI,
            DeviceManufacturer.SAMSUNG,
            DeviceManufacturer.HUAWEI,
            DeviceManufacturer.OPPO,
            DeviceManufacturer.VIVO,
            DeviceManufacturer.ONEPLUS,
            DeviceManufacturer.REALME,
            DeviceManufacturer.MEIZU,
            DeviceManufacturer.NOKIA -> true
            else -> false
        }
    }

    /** Get intent to request standard battery optimization exemption */
    @SuppressLint("BatteryLife")
    fun getIgnoreBatteryOptimizationsIntent(): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    /** Get intent to open battery optimization settings (fallback) */
    fun getBatteryOptimizationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }

    /**
     * Get OEM-specific battery management intent Returns null if no specific intent is available
     * for this manufacturer
     */
    fun getOemBatteryManagementIntent(): Intent? {
        val intents = getOemIntentsList()

        for (intent in intents) {
            if (isIntentResolvable(intent)) {
                return intent
            }
        }

        return null
    }

    /** Get all available OEM intents for the current manufacturer */
    private fun getOemIntentsList(): List<Intent> {
        return when (getDeviceManufacturer()) {
            DeviceManufacturer.XIAOMI ->
                    listOf(
                            Intent().apply {
                                component =
                                        ComponentName(
                                                "com.miui.securitycenter",
                                                "com.miui.permcenter.autostart.AutoStartManagementActivity"
                                        )
                            },
                            Intent().apply {
                                component =
                                        ComponentName(
                                                "com.miui.securitycenter",
                                                "com.miui.powercenter.PowerSettings"
                                        )
                            },
                            Intent("miui.intent.action.POWER_HIDE_MODE_APP_LIST").apply {
                                putExtra("package_name", context.packageName)
                                putExtra("package_label", getAppLabel())
                            }
                    )
            DeviceManufacturer.SAMSUNG ->
                    listOf(
                            Intent().apply {
                                component =
                                        ComponentName(
                                                "com.samsung.android.lool",
                                                "com.samsung.android.sm.battery.ui.BatteryActivity"
                                        )
                            },
                            Intent().apply {
                                component =
                                        ComponentName(
                                                "com.samsung.android.sm",
                                                "com.samsung.android.sm.battery.ui.BatteryActivity"
                                        )
                            },
                            Intent().apply {
                                component =
                                        ComponentName(
                                                "com.samsung.android.sm_cn",
                                                "com.samsung.android.sm.battery.ui.BatteryActivity"
                                        )
                            }
                    )
            DeviceManufacturer.HUAWEI ->
                    listOf(
                            Intent().apply {
                                component =
                                        ComponentName(
                                                "com.huawei.systemmanager",
                                                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                                        )
                            },
                            Intent().apply {
                                component =
                                        ComponentName(
                                                "com.huawei.systemmanager",
                                                "com.huawei.systemmanager.optimize.process.ProtectActivity"
                                        )
                            },
                            Intent().apply {
                                component =
                                        ComponentName(
                                                "com.huawei.systemmanager",
                                                "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
                                        )
                            }
                    )
            DeviceManufacturer.OPPO, DeviceManufacturer.REALME ->
                    listOf(
                            Intent().apply {
                                component =
                                        ComponentName(
                                                "com.coloros.safecenter",
                                                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                                        )
                            },
                            Intent().apply {
                                component =
                                        ComponentName(
                                                "com.oppo.safe",
                                                "com.oppo.safe.permission.startup.StartupAppListActivity"
                                        )
                            },
                            Intent().apply {
                                component =
                                        ComponentName(
                                                "com.coloros.oppoguardelf",
                                                "com.coloros.powermanager.fuelga498.PowerUsageModelActivity"
                                        )
                            }
                    )
            DeviceManufacturer.VIVO ->
                    listOf(
                            Intent().apply {
                                component =
                                        ComponentName(
                                                "com.vivo.permissionmanager",
                                                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                                        )
                            },
                            Intent().apply {
                                component =
                                        ComponentName(
                                                "com.iqoo.secure",
                                                "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                                        )
                            }
                    )
            DeviceManufacturer.ONEPLUS ->
                    listOf(
                            Intent().apply {
                                component =
                                        ComponentName(
                                                "com.oneplus.security",
                                                "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                                        )
                            }
                    )
            DeviceManufacturer.MEIZU ->
                    listOf(
                            Intent().apply {
                                component =
                                        ComponentName(
                                                "com.meizu.safe",
                                                "com.meizu.safe.powerui.PowerAppPermissionActivity"
                                        )
                            }
                    )
            DeviceManufacturer.ASUS ->
                    listOf(
                            Intent().apply {
                                component =
                                        ComponentName(
                                                "com.asus.mobilemanager",
                                                "com.asus.mobilemanager.autostart.AutoStartActivity"
                                        )
                            }
                    )
            DeviceManufacturer.NOKIA ->
                    listOf(
                            Intent().apply {
                                component =
                                        ComponentName(
                                                "com.evenwell.powersaving.g3",
                                                "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity"
                                        )
                            }
                    )
            else -> emptyList()
        }
    }

    /** Check if an intent can be resolved */
    private fun isIntentResolvable(intent: Intent): Boolean {
        return try {
            val resolveInfo =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        packageManager.resolveActivity(
                                intent,
                                PackageManager.ResolveInfoFlags.of(
                                        PackageManager.MATCH_DEFAULT_ONLY.toLong()
                                )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    }
            resolveInfo != null
        } catch (e: Exception) {
            false
        }
    }

    /** Get app label for display */
    private fun getAppLabel(): String {
        return try {
            context.applicationInfo.loadLabel(packageManager).toString()
        } catch (e: Exception) {
            context.packageName
        }
    }

    /** Get manufacturer-specific instructions for the user */
    fun getManufacturerInstructions(): String {
        return when (getDeviceManufacturer()) {
            DeviceManufacturer.XIAOMI ->
                    """
                To ensure background processing works reliably on your Xiaomi device:
                
                1. Go to Settings → Apps → Manage apps → Photo Search AI
                2. Enable "Autostart"
                3. Set Battery saver to "No restrictions"
                4. Under "Other permissions", enable all background permissions
            """.trimIndent()
            DeviceManufacturer.SAMSUNG ->
                    """
                To ensure background processing works reliably on your Samsung device:
                
                1. Go to Settings → Apps → Photo Search AI → Battery
                2. Select "Unrestricted"
                3. In Device care → Battery → Background usage limits
                4. Add Photo Search AI to "Never sleeping apps"
            """.trimIndent()
            DeviceManufacturer.HUAWEI ->
                    """
                To ensure background processing works reliably on your Huawei device:
                
                1. Go to Settings → Apps → Apps → Photo Search AI
                2. Tap "Battery" and select "Unrestricted"
                3. Go to Settings → Battery → App launch
                4. Find Photo Search AI and set to "Manage manually"
                5. Enable all toggles (Auto-launch, Secondary launch, Run in background)
            """.trimIndent()
            DeviceManufacturer.OPPO, DeviceManufacturer.REALME ->
                    """
                To ensure background processing works reliably on your device:
                
                1. Go to Settings → Battery → More battery settings
                2. Disable "Optimize battery use" for Photo Search AI
                3. Go to Settings → App Management → Photo Search AI
                4. Enable "Allow auto startup"
                5. Enable "Allow background activity"
            """.trimIndent()
            DeviceManufacturer.VIVO ->
                    """
                To ensure background processing works reliably on your Vivo device:
                
                1. Go to iManager → App manager → Autostart manager
                2. Enable autostart for Photo Search AI
                3. In App manager → Permissions → Background pop-up
                4. Enable for Photo Search AI
            """.trimIndent()
            DeviceManufacturer.ONEPLUS ->
                    """
                To ensure background processing works reliably on your OnePlus device:
                
                1. Go to Settings → Apps → Photo Search AI → Battery
                2. Set to "Don't optimize"
                3. Go to Settings → Battery → Battery optimization
                4. Find Photo Search AI and select "Don't optimize"
            """.trimIndent()
            else ->
                    """
                To ensure background processing works reliably:
                
                1. Go to Settings → Apps → Photo Search AI → Battery
                2. Select "Unrestricted" or "Don't optimize"
                3. If available, enable "Autostart" permission
            """.trimIndent()
        }
    }

    /** Check if the device is currently in power save mode */
    fun isInPowerSaveMode(): Boolean {
        return powerManager?.isPowerSaveMode ?: false
    }

    /** Check if the device is currently idle (Doze mode) */
    fun isDeviceIdle(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isDeviceIdleMode ?: false
        } else {
            false
        }
    }

    /** Check if the device is currently interactive (screen on) */
    fun isInteractive(): Boolean {
        return powerManager?.isInteractive ?: true
    }

    /** Get the package name */
    fun getPackageName(): String = context.packageName
}
