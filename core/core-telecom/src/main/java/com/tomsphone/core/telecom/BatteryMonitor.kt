package com.tomsphone.core.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.tomsphone.core.tts.WandasTTS
import com.tomsphone.core.tts.TTSScripts
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BatteryMonitor"

/**
 * Monitors battery level and provides warnings.
 * 
 * Features:
 * - Visual warning on home screen when battery is low
 * - TTS announcement when battery drops below threshold
 * - Only announces once per threshold crossing (not repeatedly)
 */
@Singleton
class BatteryMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tts: dagger.Lazy<WandasTTS>
) {
    companion object {
        const val LOW_BATTERY_THRESHOLD = 20
        const val CRITICAL_BATTERY_THRESHOLD = 10
    }
    
    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()
    
    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()
    
    private val _isLowBattery = MutableStateFlow(false)
    val isLowBattery: StateFlow<Boolean> = _isLowBattery.asStateFlow()
    
    private var hasAnnouncedLowBattery = false
    private var hasAnnouncedCriticalBattery = false
    private var hasAnnouncedCharging = false
    
    private var isRegistered = false
    
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val batteryPct = if (scale > 0) (level * 100 / scale) else level
                    
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                   status == BatteryManager.BATTERY_STATUS_FULL
                    
                    updateBatteryState(batteryPct, charging)
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    Log.d(TAG, "Power connected")
                    _isCharging.value = true
                    announceCharging()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    Log.d(TAG, "Power disconnected")
                    _isCharging.value = false
                    hasAnnouncedCharging = false
                }
            }
        }
    }
    
    /**
     * Start monitoring battery. Call from MainActivity.
     */
    fun startMonitoring() {
        if (isRegistered) return
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        
        // Get initial battery state
        val batteryStatus = context.registerReceiver(batteryReceiver, filter)
        isRegistered = true
        
        // Process initial state
        batteryStatus?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = if (scale > 0) (level * 100 / scale) else level
            
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                           status == BatteryManager.BATTERY_STATUS_FULL
            
            _batteryLevel.value = batteryPct
            _isCharging.value = charging
            _isLowBattery.value = batteryPct <= LOW_BATTERY_THRESHOLD && !charging
            
            Log.d(TAG, "Initial battery: $batteryPct%, charging: $charging")
        }
    }
    
    /**
     * Stop monitoring. Call from MainActivity onDestroy.
     */
    fun stopMonitoring() {
        if (!isRegistered) return
        
        try {
            context.unregisterReceiver(batteryReceiver)
            isRegistered = false
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering receiver: ${e.message}")
        }
    }
    
    private fun updateBatteryState(level: Int, charging: Boolean) {
        val previousLevel = _batteryLevel.value
        val wasCharging = _isCharging.value
        
        _batteryLevel.value = level
        _isCharging.value = charging
        _isLowBattery.value = level <= LOW_BATTERY_THRESHOLD && !charging
        
        Log.d(TAG, "Battery: $level%, charging: $charging")
        
        // Reset announcement flags when battery rises above thresholds
        if (level > LOW_BATTERY_THRESHOLD) {
            hasAnnouncedLowBattery = false
        }
        if (level > CRITICAL_BATTERY_THRESHOLD) {
            hasAnnouncedCriticalBattery = false
        }
        
        // Announce low battery (once per threshold crossing)
        if (!charging) {
            when {
                level <= CRITICAL_BATTERY_THRESHOLD && !hasAnnouncedCriticalBattery -> {
                    announceLowBattery(level, critical = true)
                    hasAnnouncedCriticalBattery = true
                    hasAnnouncedLowBattery = true
                }
                level <= LOW_BATTERY_THRESHOLD && !hasAnnouncedLowBattery -> {
                    announceLowBattery(level, critical = false)
                    hasAnnouncedLowBattery = true
                }
            }
        }
        
        // Announce charging started
        if (charging && !wasCharging) {
            announceCharging()
        }
    }
    
    private fun announceLowBattery(level: Int, critical: Boolean) {
        Log.d(TAG, "Announcing ${if (critical) "critical" else "low"} battery: $level%")
        try {
            tts.get().speak(TTSScripts.batteryLow(level))
        } catch (e: Exception) {
            Log.e(TAG, "TTS error: ${e.message}")
        }
    }
    
    private fun announceCharging() {
        if (hasAnnouncedCharging) return
        hasAnnouncedCharging = true
        
        Log.d(TAG, "Announcing charging")
        try {
            tts.get().speak(TTSScripts.batteryCharging())
        } catch (e: Exception) {
            Log.e(TAG, "TTS error: ${e.message}")
        }
    }
}
