package com.tomsphone.core.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.tomsphone.core.config.HomeSlotAssignments
import com.tomsphone.core.config.SettingsRepository
import com.tomsphone.core.data.repository.ContactRepository
import com.tomsphone.core.tts.WandasTTS
import com.tomsphone.core.tts.TTSScripts
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BatteryMonitor"

/**
 * Monitors battery level and provides warnings.
 * 
 * Features:
 * - Visual warning on home screen when battery is low
 * - TTS announcement when battery drops below threshold
 * - REPEATING TTS announcements while in low battery (not charging)
 * - TTS announcement when charging resumes AFTER low battery
 * - Screen should turn off in low battery mode to conserve power
 */
@Singleton
class BatteryMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tts: dagger.Lazy<WandasTTS>,
    private val settingsRepository: SettingsRepository,
    private val ringtonePlayer: RingtonePlayer,
    private val contactRepository: ContactRepository,
    private val batteryAlertSmsSender: BatteryAlertSmsSender
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    companion object {
        const val LOW_BATTERY_THRESHOLD = 20
        const val CRITICAL_BATTERY_THRESHOLD = 10
        const val LOW_BATTERY_REPEAT_INTERVAL_MS = 120_000L  // 2 minutes
        const val LOW_BATTERY_SCREEN_OFF_DELAY_MS = 10_000L  // 10 seconds before screen off
    }
    
    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()
    
    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()
    
    private val _isLowBattery = MutableStateFlow(false)
    val isLowBattery: StateFlow<Boolean> = _isLowBattery.asStateFlow()
    
    // Request screen off when in low battery mode (not charging)
    // MainActivity should observe this and turn off screen
    private val _shouldTurnOffScreen = MutableStateFlow(false)
    val shouldTurnOffScreen: StateFlow<Boolean> = _shouldTurnOffScreen.asStateFlow()
    
    // Track if we were in low battery state (for charging announcement)
    private var wasInLowBatteryState = false
    
    // Job for repeating low battery announcements
    private var lowBatteryRepeatJob: Job? = null
    
    // Job for screen off delay
    private var screenOffJob: Job? = null
    
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
                    handlePowerConnected()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    Log.d(TAG, "Power disconnected")
                    _isCharging.value = false
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
            
            val isLow = batteryPct <= LOW_BATTERY_THRESHOLD && !charging
            _isLowBattery.value = isLow
            
            // If starting in low battery state, begin repeating announcements
            if (isLow) {
                wasInLowBatteryState = true
                startLowBatteryMode()
            }
            
            Log.d(TAG, "Initial battery: $batteryPct%, charging: $charging, lowBattery: $isLow")
        }
    }
    
    /**
     * Stop monitoring. Call from MainActivity onDestroy.
     */
    fun stopMonitoring() {
        if (!isRegistered) return
        
        lowBatteryRepeatJob?.cancel()
        screenOffJob?.cancel()
        
        try {
            context.unregisterReceiver(batteryReceiver)
            isRegistered = false
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering receiver: ${e.message}")
        }
    }
    
    /**
     * Called when user interacts with screen during low battery mode.
     * Resets the screen off timer.
     */
    fun onUserInteraction() {
        if (_isLowBattery.value && !_isCharging.value) {
            // Reset screen off timer
            _shouldTurnOffScreen.value = false
            startScreenOffTimer()
        }
    }
    
    private fun handlePowerConnected() {
        _isCharging.value = true
        
        // Stop low battery mode
        stopLowBatteryMode()
        
        // Only announce charging if we were in low battery state
        if (wasInLowBatteryState) {
            Log.d(TAG, "Charging after low battery - announcing")
            announceCharging()
            sendDeviceConnectedSms()
            wasInLowBatteryState = false
        } else {
            Log.d(TAG, "Charging connected (not from low battery) - no announcement")
        }
    }
    
    private fun updateBatteryState(level: Int, charging: Boolean) {
        val wasLow = _isLowBattery.value
        
        _batteryLevel.value = level
        _isCharging.value = charging
        
        val isLow = level <= LOW_BATTERY_THRESHOLD && !charging
        _isLowBattery.value = isLow
        
        Log.d(TAG, "Battery: $level%, charging: $charging, lowBattery: $isLow")
        
        // Entered low battery mode
        if (isLow && !wasLow) {
            Log.d(TAG, "Entering low battery mode")
            wasInLowBatteryState = true
            startLowBatteryMode()
        }
        
        // Exited low battery mode (either by charging or battery rising)
        if (!isLow && wasLow) {
            Log.d(TAG, "Exiting low battery mode")
            stopLowBatteryMode()
            
            // If exited because battery rose above threshold (not charging), clear flag
            if (!charging && level > LOW_BATTERY_THRESHOLD) {
                wasInLowBatteryState = false
            }
        }
    }
    
    private fun startLowBatteryMode() {
        // Send low-battery SMS once (if enabled and we have permission)
        scope.launch {
            try {
                val settings = settingsRepository.getSettings().first()
                if (!settings.batteryAlertSmsEnabled) {
                    Log.d(TAG, "Low battery: SMS alerts disabled in settings")
                    return@launch
                }
                if (!batteryAlertSmsSender.hasSmsPermission()) {
                    Log.w(TAG, "Low battery: SMS permission not granted - alerts not sent")
                    return@launch
                }
                val onHome = HomeSlotAssignments.contactIdsOnHome(settings.homeSlotAssignments)
                val recipients = contactRepository.getContactsWithBatteryAlertsEnabled()
                    .filter { it.id in onHome }
                val numbers = recipients.map { it.phoneNumber }.filter { it.isNotBlank() }
                if (numbers.isEmpty()) {
                    Log.w(TAG, "Low battery: no home-slot contacts with battery alerts / valid numbers - SMS not sent")
                    return@launch
                }
                Log.d(TAG, "Sending low battery alert to ${numbers.size} assistant(s) using stored numbers")
                batteryAlertSmsSender.sendLowBatteryAlert(numbers, settings.userName)
            } catch (e: Exception) {
                Log.e(TAG, "Battery alert SMS error: ${e.message}", e)
            }
        }
        // Start repeating TTS announcements
        lowBatteryRepeatJob?.cancel()
        lowBatteryRepeatJob = scope.launch {
            while (true) {
                announceLowBattery()
                delay(LOW_BATTERY_REPEAT_INTERVAL_MS)
                
                // Check if we should stop
                if (!_isLowBattery.value || _isCharging.value) {
                    break
                }
            }
        }
        
        // Start screen off timer
        startScreenOffTimer()
    }
    
    private fun stopLowBatteryMode() {
        lowBatteryRepeatJob?.cancel()
        lowBatteryRepeatJob = null
        
        screenOffJob?.cancel()
        screenOffJob = null
        
        _shouldTurnOffScreen.value = false
    }
    
    private fun startScreenOffTimer() {
        screenOffJob?.cancel()
        screenOffJob = scope.launch {
            delay(LOW_BATTERY_SCREEN_OFF_DELAY_MS)
            if (_isLowBattery.value && !_isCharging.value) {
                Log.d(TAG, "Low battery: requesting screen off")
                _shouldTurnOffScreen.value = true
            }
        }
    }
    
    private fun announceLowBattery() {
        val level = _batteryLevel.value
        Log.d(TAG, "Announcing low battery: $level%")
        scope.launch {
            try {
                val settings = settingsRepository.getSettings().first()
                if (settings.ttsAnnouncementsEnabled) {
                    // Play Tannoy bing-bong attention sound first
                    ringtonePlayer.playAndWait(RingtonePlayer.Ringtone.TANNOY_SHORT)
                    // Then announce with user's name
                    tts.get().speak(TTSScripts.batteryLow(settings.userName))
                }
            } catch (e: Exception) {
                Log.e(TAG, "TTS error: ${e.message}")
            }
        }
    }
    
    private fun announceCharging() {
        Log.d(TAG, "Announcing charging (after low battery)")
        scope.launch {
            try {
                val settings = settingsRepository.getSettings().first()
                if (settings.ttsAnnouncementsEnabled) {
                    tts.get().speak(TTSScripts.batteryCharging())
                }
            } catch (e: Exception) {
                Log.e(TAG, "TTS error: ${e.message}")
            }
        }
    }

    private fun sendDeviceConnectedSms() {
        scope.launch {
            try {
                val settings = settingsRepository.getSettings().first()
                if (settings.batteryAlertSmsEnabled) {
                    val onHome = HomeSlotAssignments.contactIdsOnHome(settings.homeSlotAssignments)
                    val recipients = contactRepository.getContactsWithBatteryAlertsEnabled()
                        .filter { it.id in onHome }
                    val numbers = recipients.map { it.phoneNumber }.filter { it.isNotBlank() }
                    if (numbers.isNotEmpty()) {
                        Log.d(TAG, "Sending device connected alert to ${numbers.size} assistant(s) using stored numbers")
                        batteryAlertSmsSender.sendDeviceConnectedAlert(numbers, settings.userName)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Device connected SMS error: ${e.message}")
            }
        }
    }
}
