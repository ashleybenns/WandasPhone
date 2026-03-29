package com.tomsphone

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.tomsphone.core.ui.theme.UserScalingProvider
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tomsphone.core.config.SettingsRepository
import com.tomsphone.core.config.homeButtonRowCount
import com.tomsphone.core.telecom.CallDirection
import com.tomsphone.core.telecom.CallManager
import com.tomsphone.core.telecom.CallState
import com.tomsphone.core.telecom.DEFAULT_EMERGENCY_FALLBACK
import com.tomsphone.core.telecom.EmergencyNumberResolver
import com.tomsphone.core.telecom.MissedCallNagManager
import com.tomsphone.core.ui.theme.ThemeOption
import com.tomsphone.core.ui.theme.WandasPhoneTheme
import com.tomsphone.feature.home.AddBlockedCallerRoute
import com.tomsphone.feature.home.AddBlockedCallerScreen
import com.tomsphone.feature.home.HomeScreen
import com.tomsphone.feature.home.RecentCallsListViewModel
import com.tomsphone.feature.phone.EmergencyConfirmScreen
import com.tomsphone.feature.phone.EmergencyCallScreen
import com.tomsphone.feature.phone.EndIncomingCallScreen
import com.tomsphone.feature.phone.EndOutgoingCallScreen
import com.tomsphone.feature.phone.IncomingCallScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

// #region agent log helper
private fun debugLog(location: String, hypothesisId: String, message: String, data: Map<String, Any?> = emptyMap()) {
    Log.d("DEBUG_NAV", "[$hypothesisId] $location: $message | $data")
}
// #endregion

/**
 * Main Activity - single-activity architecture
 * 
 * Screen Flow:
 * - home: Standby with contact buttons + calling animation
 * - incoming: Answer/Reject screen for incoming calls
 * - endIncoming: Green end call screen (after answering incoming)
 * - endOutgoing: Yellow end call screen (for outgoing calls)
 * 
 * Navigation is driven by call state:
 * - incomingRingingCall → incoming screen
 * - currentCall OUTGOING (any active state) → endOutgoing screen
 * - currentCall INCOMING ACTIVE → endIncoming screen
 * - No call → home screen
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    @Inject
    lateinit var callManager: CallManager

    @Inject
    lateinit var missedCallNagManager: MissedCallNagManager
    
    @Inject
    lateinit var batteryMonitor: com.tomsphone.core.telecom.BatteryMonitor
    
    private var lockVolumeButtons = true
    private var volumeKeysAllowedDuringCall = false  // Only true when call active and lockVolumeButtons=false
    private var pinnedModeEnabled = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Show over lock screen - allows app to appear without unlocking
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        
        hideSystemBars()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Start battery monitoring
        batteryMonitor.startMonitoring()
        
        lifecycleScope.launch {
            applySettings()
        }

        // Observe settings + call state for volume lock in real time
        // Volume keys: locked always when lockVolumeButtons=true; when false, only allow during active call (protect ringtone)
        lifecycleScope.launch {
            combine(
                settingsRepository.getSettings().map { it.lockVolumeButtons },
                callManager.currentCall.map { call ->
                    call != null && (call.state == CallState.ACTIVE || call.state == CallState.DIALING || call.state == CallState.RINGING)
                }
            ) { lockVol, hasActiveCall -> lockVol to hasActiveCall }
                .collect { (lockVol, hasActiveCall) ->
                    lockVolumeButtons = lockVol
                    volumeKeysAllowedDuringCall = hasActiveCall
                }
        }

        // Observe screen always on so toggle applies immediately (like volume lock)
        lifecycleScope.launch {
            settingsRepository.getSettings().map { it.screenAlwaysOn }.collect { screenAlwaysOn ->
                if (screenAlwaysOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                Log.d(TAG, "Screen always on updated: $screenAlwaysOn")
            }
        }

        setContent {
            WandasPhoneApp(
                callManager = callManager,
                missedCallNagManager = missedCallNagManager,
                settingsRepository = settingsRepository,
                batteryMonitor = batteryMonitor,
                onExitApp = { exitApp() },
                onTurnOffScreen = { turnOffScreenForLowBattery() },
                onRestoreScreen = { restoreScreen() }
            )
        }
    }
    
    override fun onResume() {
        super.onResume()
        hideSystemBars()
        
        // Re-check settings on each resume to respect carer changes
        lifecycleScope.launch {
            try {
                val settings = settingsRepository.getSettings().first()
                pinnedModeEnabled = settings.pinnedModeEnabled
                lockVolumeButtons = settings.lockVolumeButtons
                
                val isConfigured = settings.carerPin.isNotEmpty()
                if (pinnedModeEnabled && isConfigured) {
                    ensurePinnedMode()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check settings in onResume: ${e.message}")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        batteryMonitor.stopMonitoring()
    }
    
    private suspend fun applySettings() {
        try {
            val settings = settingsRepository.getSettings().first()
            
            lockVolumeButtons = settings.lockVolumeButtons
            pinnedModeEnabled = settings.pinnedModeEnabled
            
            if (settings.screenAlwaysOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            
            // Only pin if enabled AND carer has set up the app (PIN exists)
            // This prevents pinning on first launch before carer can configure
            val isConfigured = settings.carerPin.isNotEmpty()
            if (settings.pinnedModeEnabled && isConfigured) {
                // Use delay to ensure window is fully visible (Samsung needs this)
                startPinnedMode(delayMs = 500)
            } else if (settings.pinnedModeEnabled && !isConfigured) {
                Log.d(TAG, "Pinned mode enabled but app not configured yet - skipping pin")
            }
            
            Log.d(TAG, "Settings applied: pinned=$pinnedModeEnabled, configured=$isConfigured, volumeLock=$lockVolumeButtons")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying settings: ${e.message}")
        }
    }
    
    private fun startPinnedMode(delayMs: Long = 0) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Use a delay to ensure window is fully visible (helps on Samsung)
            val runPinning = Runnable {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                        if (am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE) {
                            Log.d(TAG, "Already in pinned mode")
                            return@Runnable
                        }
                    }
                    Log.d(TAG, "Requesting pinned mode (startLockTask)")
                    startLockTask()
                    Log.d(TAG, "Pinned mode request sent")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start pinned mode: ${e.message}")
                }
            }
            
            if (delayMs > 0) {
                Handler(Looper.getMainLooper()).postDelayed(runPinning, delayMs)
            } else {
                // Post to window to ensure UI is ready
                window.decorView.post(runPinning)
            }
        }
    }
    
    private fun ensurePinnedMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                // Shorter delay for resume - window should already be mostly ready
                startPinnedMode(delayMs = 200)
            }
        }
    }
    
    /**
     * Turn off screen for low battery power saving.
     * Removes keep screen on flag and sets brightness to minimum.
     */
    fun turnOffScreenForLowBattery() {
        Log.d(TAG, "Turning off screen for low battery")
        runOnUiThread {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 0.01f  // Minimum brightness
            window.attributes = params
        }
    }
    
    /**
     * Restore screen for normal operation.
     */
    fun restoreScreen() {
        Log.d(TAG, "Restoring screen")
        runOnUiThread {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = params
        }
    }
    
    /**
     * Exit the app - unpin, disable home launcher, and open settings
     * Used by carer as an escape hatch when pinning/home launcher causes issues
     */
    private fun exitApp() {
        Log.d(TAG, "Carer requested app exit")
        
        // Stop lock task (unpin)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                stopLockTask()
                Log.d(TAG, "Stopped lock task")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop lock task: ${e.message}")
            }
        }
        
        // Open Android Settings - this always works even if we're the home launcher
        // From settings, user can navigate anywhere or change default home app
        try {
            val settingsIntent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(settingsIntent)
            Log.d(TAG, "Opened Android Settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open settings: ${e.message}")
            // Fallback: try to open home app settings specifically
            try {
                val homeSettingsIntent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeSettingsIntent)
                Log.d(TAG, "Opened Home Settings")
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to open home settings: ${e2.message}")
            }
        }
        
        // Finish the activity
        finish()
    }
    
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE -> {
                // Lock ON: always consume (protect both call and ringtone volume)
                if (lockVolumeButtons) return true
                // Lock OFF: only allow during active call (protect ringtone when idle)
                if (!volumeKeysAllowedDuringCall) return true
                // Lock OFF + active call: pass through so system shows volume panel
                return super.dispatchKeyEvent(event)
            }
        }
        return super.dispatchKeyEvent(event)
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }
    
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        // Use BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE for most cases
        // Bars will hide again automatically after appearing
        controller.systemBarsBehavior = 
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        // Also set legacy flags for older devices and Samsung
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        Log.d(TAG, "Back press blocked")
    }
}

@Composable
fun WandasPhoneApp(
    callManager: CallManager,
    missedCallNagManager: MissedCallNagManager,
    settingsRepository: SettingsRepository,
    batteryMonitor: com.tomsphone.core.telecom.BatteryMonitor,
    onExitApp: () -> Unit,
    onTurnOffScreen: () -> Unit = {},
    onRestoreScreen: () -> Unit = {}
) {
    val navController = rememberNavController()
    
    // Observe both call flows
    val incomingCall by callManager.incomingRingingCall.collectAsState(initial = null)
    val currentCall by callManager.currentCall.collectAsState(initial = null)
    
    // Observe user text size setting (scale factor)
    val settings by settingsRepository.getSettings().collectAsState(initial = null)
    val userTextScale = settings?.ui?.userTextSize?.scale ?: 1.0f  // Default to NORMAL (100%)
    
    // Track the last outgoing contact name for navigation
    var lastOutgoingContactName by remember { mutableStateOf("Caller") }
    
    // Emergency mode from CallManager - shared with CallScreeningService
    val isEmergencyMode by callManager.isEmergencyMode.collectAsState()
    
    // Battery state for low battery warning
    val batteryLevel by batteryMonitor.batteryLevel.collectAsState()
    val isLowBattery by batteryMonitor.isLowBattery.collectAsState()
    val isCharging by batteryMonitor.isCharging.collectAsState()
    val shouldTurnOffScreen by batteryMonitor.shouldTurnOffScreen.collectAsState()
    
    // Handle low battery screen off
    LaunchedEffect(shouldTurnOffScreen) {
        if (shouldTurnOffScreen) {
            onTurnOffScreen()
        } else {
            onRestoreScreen()
        }
    }
    
    // Restore screen when charging starts
    LaunchedEffect(isCharging) {
        if (isCharging) {
            onRestoreScreen()
        }
    }
    
    // Update contact name when we have call info
    LaunchedEffect(currentCall) {
        currentCall?.let { call ->
            if (call.direction == CallDirection.OUTGOING) {
                call.contactName?.let { name ->
                    lastOutgoingContactName = name
                }
            }
        }
    }
    
    // INCOMING RINGING → incoming screen
    // Also handles call ending (caller hangs up, voicemail, etc.)
    LaunchedEffect(incomingCall) {
        val call = incomingCall
        val currentRoute = navController.currentDestination?.route
        
        Log.d("WandasPhoneApp", "=== incomingRingingCall: ${call?.state}, route=$currentRoute ===")
        
        when {
            // Call is ringing → show incoming screen
            call != null && call.state == CallState.RINGING -> {
                if (currentRoute != "incoming") {
                    Log.d("WandasPhoneApp", ">>> Navigating to incoming screen")
                    navController.navigate("incoming") {
                        launchSingleTop = true
                    }
                }
            }
            
            // Call ended or cleared → return to home (if on incoming screen)
            // This handles: caller hung up, went to voicemail, timed out
            (call == null || call.state == CallState.DISCONNECTED || call.state == CallState.IDLE) -> {
                if (currentRoute == "incoming") {
                    Log.d("WandasPhoneApp", ">>> Incoming call ended - returning to home")
                    navController.popBackStack("home", inclusive = false)
                }
            }
        }
    }
    
    // CURRENT CALL STATE → appropriate end call screen
    // Include isEmergencyMode as key so we re-evaluate when it changes
    LaunchedEffect(currentCall, isEmergencyMode) {
        val call = currentCall
        val currentRoute = navController.currentDestination?.route
        
        Log.d("WandasPhoneApp", "=== currentCall: state=${call?.state}, direction=${call?.direction}, route=$currentRoute, emergencyMode=$isEmergencyMode ===")
        
        when {
            // No call or call ended → return to home (if on end call screen or emergency screen)
            // BUT for emergency mode, only exit on explicit DISCONNECTED (not null - call may still be connecting)
            call == null || call.state == CallState.IDLE || call.state == CallState.DISCONNECTED -> {
                // For emergency screen: only exit on DISCONNECTED, not on null (call still connecting)
                if (currentRoute == "emergencyCall") {
                    // DON'T auto-navigate away from emergency screen
                    // Keep medical info visible for EMTs even after call ends
                    // User can manually exit with "Back to Home"
                    Log.d("WandasPhoneApp", ">>> Emergency screen: call ended but staying on info screen")
                } else if (currentRoute == "endIncoming" || currentRoute?.startsWith("endOutgoing") == true) {
                    Log.d("WandasPhoneApp", ">>> Call ended - returning to home from $currentRoute")
                    navController.popBackStack("home", inclusive = false)
                }
            }
            
            // OUTGOING call active (DIALING, RINGING, ACTIVE) → yellow screen
            // BUT skip if in emergency mode (emergency calls stay on emergency info page)
            call.direction == CallDirection.OUTGOING && 
            (call.state == CallState.DIALING || call.state == CallState.RINGING || call.state == CallState.ACTIVE) -> {
                if (isEmergencyMode) {
                    Log.d("WandasPhoneApp", ">>> Emergency mode active - staying on emergency screen")
                } else if (currentRoute != "endOutgoing" && !currentRoute.orEmpty().startsWith("endOutgoing")) {
                    val contactName = call.contactName ?: lastOutgoingContactName
                    Log.d("WandasPhoneApp", ">>> Outgoing call - navigating to endOutgoing ($contactName)")
                    navController.navigate("endOutgoing/$contactName") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
            
            // INCOMING call answered (ACTIVE) → green screen
            call.direction == CallDirection.INCOMING && call.state == CallState.ACTIVE -> {
                if (currentRoute != "endIncoming") {
                    Log.d("WandasPhoneApp", ">>> Incoming call answered - navigating to endIncoming")
                    navController.navigate("endIncoming") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
        }
    }
    
    // Button row count from home layout (slots when migrated, else legacy toggles)
    val homeButtonRowCount = settings?.homeButtonRowCount?.coerceIn(2, 6) ?: 4
    
    WandasPhoneTheme(themeOption = ThemeOption.HIGH_CONTRAST_LIGHT) {
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            // USER SCREENS - all use the same scale calculated from home screen
            // This ensures consistent text sizing across all screens
            
            composable("home") {
                // #region agent log
                LaunchedEffect(Unit) { debugLog("MainActivity.kt:378", "H2", "home composable entered", mapOf("settingsNull" to (settings == null))) }
                // #endregion
                UserScalingProvider(
                    buttonRowCount = homeButtonRowCount,
                    userScaleReduction = userTextScale.coerceIn(0.7f, 1.0f)
                ) {
                    HomeScreen(
                        onNavigateToCarer = {
                            navController.navigate("carer")
                        },
                        onNavigateToEmergencyConfirm = {
                            navController.navigate("emergencyConfirm")
                        },
                        onNavigateToMissedCalls = {
                            navController.navigate("missedCalls")
                        },
                        onNavigateToContactsList = {
                            navController.navigate("contactsList")
                        },
                        onNavigateToDialer = {
                            navController.navigate("dialer")
                        },
                        batteryLevel = batteryLevel,
                        isLowBattery = isLowBattery,
                        isCharging = isCharging
                    )
                }
            }
            
            // Missed Calls List (Level 2+)
            composable("missedCalls") {
                // #region agent log
                LaunchedEffect(Unit) { debugLog("MainActivity.kt:410", "H1", "missedCalls composable entered", mapOf("backStackSize" to navController.currentBackStack.value.size)) }
                // #endregion
                val scope = rememberCoroutineScope()
                UserScalingProvider(
                    buttonRowCount = homeButtonRowCount,
                    userScaleReduction = userTextScale.coerceIn(0.7f, 1.0f)
                ) {
                    val addContactContext = LocalContext.current
                    com.tomsphone.feature.home.MissedCallsListScreen(
                        onBack = {
                            // #region agent log
                            debugLog("MainActivity.kt:414", "H1", "missedCalls onBack called", mapOf("canPopBack" to navController.previousBackStackEntry?.destination?.route))
                            // #endregion
                            navController.popBackStack()
                        },
                        onCallContact = { _, phoneNumber ->
                            navController.popBackStack()
                            scope.launch {
                                missedCallNagManager.markMissedCallsAsReadAndDismiss(phoneNumber)
                                callManager.placeCall(phoneNumber)
                            }
                        },
                        onAddToContacts = { phoneNumber ->
                            val intent = Intent(Intent.ACTION_INSERT).apply {
                                type = ContactsContract.Contacts.CONTENT_TYPE
                                putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber)
                            }
                            try {
                                addContactContext.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Add to contacts failed", e)
                                Toast.makeText(
                                    addContactContext,
                                    "Could not open contacts app",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onAddBlockedToApp = { phoneNumber, suggestedName ->
                            val token = AddBlockedCallerRoute.encode(phoneNumber, suggestedName)
                            navController.navigate("addAppContact/$token")
                        }
                    )
                }
            }

            composable(
                route = "addAppContact/{token}",
                arguments = listOf(
                    navArgument("token") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token").orEmpty()
                val decoded = remember(token) {
                    try {
                        AddBlockedCallerRoute.decode(token)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Invalid addAppContact token", e)
                        null
                    }
                }
                val addCtx = LocalContext.current
                if (decoded == null) {
                    LaunchedEffect(Unit) {
                        Toast.makeText(addCtx, "Could not open add contact", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                    return@composable
                }
                val (blockedPhone, suggestedName) = decoded
                val recentVm: RecentCallsListViewModel = hiltViewModel()
                val buttonActivation by recentVm.buttonActivation.collectAsState()
                val touchDebounceMs by recentVm.touchDebounceMs.collectAsState()
                val accumulatedThresholdMs by recentVm.accumulatedTapThresholdMs.collectAsState()
                val accumulatedTimeoutMs by recentVm.accumulatedTapTimeoutMs.collectAsState()
                UserScalingProvider(
                    buttonRowCount = homeButtonRowCount,
                    userScaleReduction = userTextScale.coerceIn(0.7f, 1.0f)
                ) {
                    AddBlockedCallerScreen(
                        phoneNumber = blockedPhone,
                        suggestedDisplayName = suggestedName,
                        onBack = { navController.popBackStack() },
                        onAdded = { navController.popBackStack() },
                        buttonActivation = buttonActivation,
                        touchDebounceMs = touchDebounceMs,
                        accumulatedThresholdMs = accumulatedThresholdMs,
                        accumulatedTimeoutMs = accumulatedTimeoutMs
                    )
                }
            }
            
            composable("dialer") {
                val scope = rememberCoroutineScope()
                val dialerVm: com.tomsphone.feature.home.DialerScreenViewModel = hiltViewModel()
                val buttonActivation by dialerVm.buttonActivation.collectAsState()
                val touchDebounceMs by dialerVm.touchDebounceMs.collectAsState()
                val accumulatedThresholdMs by dialerVm.accumulatedTapThresholdMs.collectAsState()
                val accumulatedTimeoutMs by dialerVm.accumulatedTapTimeoutMs.collectAsState()
                // Dialer uses screen-based typography/layout only (not UserScalingProvider / home row count).
                com.tomsphone.feature.home.DialerScreen(
                    onBack = { navController.popBackStack() },
                    onPlaceCall = { e164 ->
                        navController.popBackStack()
                        scope.launch {
                            callManager.placeCall(e164)
                        }
                    },
                    buttonActivation = buttonActivation,
                    touchDebounceMs = touchDebounceMs,
                    accumulatedThresholdMs = accumulatedThresholdMs,
                    accumulatedTimeoutMs = accumulatedTimeoutMs
                )
            }

            // Contacts List (Level 2+)
            composable("contactsList") {
                val scope = rememberCoroutineScope()
                UserScalingProvider(
                    buttonRowCount = homeButtonRowCount,
                    userScaleReduction = userTextScale.coerceIn(0.7f, 1.0f)
                ) {
                    com.tomsphone.feature.home.ContactsListScreen(
                        onBack = { navController.popBackStack() },
                        onCallContact = { name, phoneNumber ->
                            // Navigate back to home and place call
                            navController.popBackStack()
                            scope.launch {
                                callManager.placeCall(phoneNumber)
                            }
                        }
                    )
                }
            }
            
            // Emergency confirm screen (after 3 taps)
            composable("emergencyConfirm") {
                val appCtx = LocalContext.current.applicationContext
                val storedEmergency = settings?.emergencyNumber.orEmpty()
                val emergencyResolution = remember(storedEmergency) {
                    EmergencyNumberResolver.resolve(appCtx, storedEmergency)
                }
                val emergencyNumber = emergencyResolution.dialDigits.ifEmpty {
                    DEFAULT_EMERGENCY_FALLBACK
                }
                val isTestMode = settings?.emergencyTestMode ?: true
                
                UserScalingProvider(
                    buttonRowCount = homeButtonRowCount,
                    userScaleReduction = userTextScale.coerceIn(0.7f, 1.0f)
                ) {
                    EmergencyConfirmScreen(
                        emergencyNumber = emergencyNumber,
                        isTestMode = isTestMode,
                        onConfirm = {
                            // Set emergency mode for navigation tracking
                            callManager.setEmergencyMode(true)
                            Log.d("WandasPhoneApp", "Emergency mode ENABLED")
                            
                            // DISABLE reject unknown calls - allows EMT/services to call back
                            // This persists until carer re-enables it
                            kotlinx.coroutines.MainScope().launch {
                                val currentSettings = settingsRepository.getSettings().first()
                                if (currentSettings.rejectUnknownCalls) {
                                    settingsRepository.updateSettings(
                                        currentSettings.copy(rejectUnknownCalls = false)
                                    )
                                    Log.d("WandasPhoneApp", "Disabled reject unknown calls for emergency")
                                }
                            }
                            
                            // Navigate to emergency info screen BEFORE placing call
                            navController.navigate("emergencyCall") {
                                popUpTo("home")
                            }
                            
                            // Place call AFTER navigation (gives UI time to update)
                            if (isTestMode) {
                                Log.d("WandasPhoneApp", "Emergency TEST mode - not placing real call")
                            } else {
                                Log.d("WandasPhoneApp", "Emergency REAL mode - placing call to $emergencyNumber")
                                val result = callManager.placeCall(emergencyNumber)
                                if (result.isSuccess) {
                                    Log.d("WandasPhoneApp", "Emergency call placed successfully")
                                } else {
                                    Log.e("WandasPhoneApp", "Emergency call failed: ${result.exceptionOrNull()?.message}")
                                }
                            }
                        },
                        onCancel = {
                            navController.popBackStack("home", inclusive = false)
                        }
                    )
                }
            }
            
            // Emergency call screen (shows user info during call)
            composable("emergencyCall") {
                val appCtx = LocalContext.current.applicationContext
                val storedEmergency = settings?.emergencyNumber.orEmpty()
                val emergencyResolution = remember(storedEmergency) {
                    EmergencyNumberResolver.resolve(appCtx, storedEmergency)
                }
                val emergencyDialDigits = emergencyResolution.dialDigits.ifEmpty {
                    DEFAULT_EMERGENCY_FALLBACK
                }
                val userName = settings?.userName ?: "User"
                val isTestMode = settings?.emergencyTestMode ?: true
                
                // Check if call is still active
                val isCallActive = currentCall?.let { call ->
                    call.state == CallState.DIALING || 
                    call.state == CallState.RINGING || 
                    call.state == CallState.ACTIVE ||
                    call.state == CallState.CONNECTING
                } ?: false
                
                UserScalingProvider(
                    buttonRowCount = homeButtonRowCount,
                    userScaleReduction = userTextScale.coerceIn(0.7f, 1.0f)
                ) {
                    EmergencyCallScreen(
                        emergencyDialDigits = emergencyDialDigits,
                        userName = userName,
                        userSurname = settings?.userSurname ?: "",
                        userPhotoUri = settings?.userPhotoUri,
                        userAddress = settings?.userAddress ?: "",
                        userBloodType = settings?.userBloodType ?: "",
                        userAllergies = settings?.userAllergies ?: "",
                        userMedications = settings?.userMedications ?: "",
                        userMedicalConditions = settings?.userMedicalConditions ?: "",
                        userEmergencyNotes = settings?.userEmergencyNotes ?: "",
                        emergencyContact1Name = settings?.emergencyContact1Name ?: "",
                        emergencyContact1Phone = settings?.emergencyContact1Phone ?: "",
                        emergencyContact2Name = settings?.emergencyContact2Name ?: "",
                        emergencyContact2Phone = settings?.emergencyContact2Phone ?: "",
                        isTestMode = isTestMode,
                        isCallActive = isCallActive,
                        onEndCall = {
                            Log.d("WandasPhoneApp", "Emergency screen exit requested")
                            callManager.setEmergencyMode(false)
                            navController.popBackStack("home", inclusive = false)
                        }
                    )
                }
            }
            
            // Incoming call - Answer/Reject
            composable("incoming") {
                UserScalingProvider(
                    buttonRowCount = homeButtonRowCount,
                    userScaleReduction = userTextScale.coerceIn(0.7f, 1.0f)
                ) {
                    IncomingCallScreen(
                        onCallAnswered = {
                            // Will navigate to endIncoming when call becomes ACTIVE
                            Log.d("WandasPhoneApp", "Call answered - waiting for ACTIVE state")
                        },
                        onCallRejected = {
                            navController.popBackStack("home", inclusive = false)
                        }
                    )
                }
            }
            
            // End call screen for INCOMING
            composable("endIncoming") {
                UserScalingProvider(
                    buttonRowCount = homeButtonRowCount,
                    userScaleReduction = userTextScale.coerceIn(0.7f, 1.0f)
                ) {
                    EndIncomingCallScreen(
                        onCallEnded = {
                            navController.popBackStack("home", inclusive = false)
                        }
                    )
                }
            }
            
            // End call screen for OUTGOING
            composable("endOutgoing/{contactName}") { backStackEntry ->
                val contactName = backStackEntry.arguments?.getString("contactName") ?: "Caller"
                UserScalingProvider(
                    buttonRowCount = homeButtonRowCount,
                    userScaleReduction = userTextScale.coerceIn(0.7f, 1.0f)
                ) {
                    EndOutgoingCallScreen(
                        contactName = contactName,
                        onCallEnded = {
                            navController.popBackStack("home", inclusive = false)
                        }
                    )
                }
            }
            
            // CARER SCREEN - NO scaling, uses normal text size for readability
            composable("carer") {
                com.tomsphone.feature.carer.CarerScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onExitApp = onExitApp
                )
            }
        }
    }
}
