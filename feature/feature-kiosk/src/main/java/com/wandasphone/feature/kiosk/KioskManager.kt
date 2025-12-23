package com.wandasphone.feature.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages kiosk mode (lock task mode)
 * 
 * Features:
 * - Prevent exit from app
 * - Disable home button
 * - Disable recent apps
 * - Disable notifications
 * - Persist across reboots
 */
@Singleton
class KioskManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "KioskManager"
        const val REQUEST_CODE_ENABLE_ADMIN = 1001
    }
    
    private val devicePolicyManager = 
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    
    private val deviceAdminComponent = 
        ComponentName(context, WandasDeviceAdminReceiver::class.java)
    
    /**
     * Check if app is device owner
     */
    fun isDeviceOwner(): Boolean {
        return devicePolicyManager.isDeviceOwnerApp(context.packageName)
    }
    
    /**
     * Check if device admin is active
     */
    fun isDeviceAdmin(): Boolean {
        return devicePolicyManager.isAdminActive(deviceAdminComponent)
    }
    
    /**
     * Check if lock task mode is active
     */
    fun isInKioskMode(activity: Activity): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
        } else {
            false
        }
    }
    
    /**
     * Start lock task mode (kiosk)
     */
    fun startKioskMode(activity: Activity): Result<Unit> {
        return runCatching {
            if (!isDeviceOwner()) {
                throw SecurityException("App is not device owner. Run: adb shell dpm set-device-owner ${context.packageName}/${WandasDeviceAdminReceiver::class.java.name}")
            }
            
            // Set lock task packages
            devicePolicyManager.setLockTaskPackages(
                deviceAdminComponent,
                arrayOf(context.packageName)
            )
            
            // Start lock task mode
            activity.startLockTask()
            
            Log.d(TAG, "Started kiosk mode")
        }
    }
    
    /**
     * Stop lock task mode
     */
    fun stopKioskMode(activity: Activity): Result<Unit> {
        return runCatching {
            activity.stopLockTask()
            Log.d(TAG, "Stopped kiosk mode")
        }
    }
    
    /**
     * Request device admin activation
     */
    @Suppress("DEPRECATION")
    fun requestDeviceAdmin(activity: Activity) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Wanda's Phone needs device admin to enable kiosk mode and prevent accidental exits."
            )
        }
        activity.startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
    }
    
    /**
     * Clear device owner (for uninstall)
     */
    fun clearDeviceOwner(): Result<Unit> {
        return runCatching {
            if (isDeviceOwner()) {
                devicePolicyManager.clearDeviceOwnerApp(context.packageName)
                Log.d(TAG, "Cleared device owner")
            }
        }
    }
    
    /**
     * Configure kiosk mode settings
     */
    fun configureKioskSettings() {
        if (!isDeviceOwner()) return
        
        try {
            // Disable keyguard (lock screen)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                devicePolicyManager.setKeyguardDisabled(deviceAdminComponent, true)
            }
            
            // Disable status bar
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                devicePolicyManager.setStatusBarDisabled(deviceAdminComponent, true)
            }
            
            Log.d(TAG, "Configured kiosk settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure kiosk settings", e)
        }
    }
}
