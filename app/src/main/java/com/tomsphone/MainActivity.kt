package com.tomsphone

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tomsphone.core.config.SettingsRepository
import com.tomsphone.core.telecom.CallDirection
import com.tomsphone.core.telecom.CallManager
import com.tomsphone.core.telecom.CallState
import com.tomsphone.core.ui.theme.ThemeOption
import com.tomsphone.core.ui.theme.WandasPhoneTheme
import com.tomsphone.feature.home.HomeScreen
import com.tomsphone.feature.phone.InCallScreen
import com.tomsphone.feature.phone.IncomingCallScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main Activity - the only activity users will ever see
 * 
 * Single-activity architecture: ALL screens are composables within this activity.
 * This allows pinned mode to work seamlessly with phone calls.
 * 
 * Always On features:
 * - Pinned mode (startLockTask) - prevents leaving app, works with calls
 * - Screen always on (FLAG_KEEP_SCREEN_ON)
 * - Volume button lock - prevents accidental volume changes
 * - Full screen immersive - hides status and navigation bars
 * - Back button blocked - prevents accidental exits
 * 
 * Phone calls work within pinned mode because they're system-level operations.
 * No need to exit/re-enter pinned mode for calls.
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
    
    private var lockVolumeButtons = true
    private var pinnedModeEnabled = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide status bar and navigation bar for clean full-screen UI
        hideSystemBars()
        
        // Keep screen on while app is active
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Load settings and apply
        lifecycleScope.launch {
            applySettings()
        }
        
        setContent {
            WandasPhoneApp(callManager)
        }
    }
    
    override fun onResume() {
        super.onResume()
        hideSystemBars()
        
        // Safety net: Re-enter pinned mode if we somehow exited
        // (e.g., system dialog, crash recovery)
        // Phone calls work WITHIN pinned mode - no need to exit for them
        if (pinnedModeEnabled) {
            ensurePinnedMode()
        }
    }
    
    /**
     * Apply settings from carer configuration
     */
    private suspend fun applySettings() {
        try {
            val settings = settingsRepository.getSettings().first()
            
            // Store for use in dispatchKeyEvent and onResume
            lockVolumeButtons = settings.lockVolumeButtons
            pinnedModeEnabled = settings.pinnedModeEnabled
            
            // Screen always on
            if (settings.screenAlwaysOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                Log.d(TAG, "Screen always on: enabled")
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            
            // Pinned mode - lock app to foreground
            // Works with phone calls since we use single-activity architecture
            if (settings.pinnedModeEnabled) {
                startPinnedMode()
            }
            
            Log.d(TAG, "Settings applied: pinned=$pinnedModeEnabled, volumeLock=$lockVolumeButtons")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying settings: ${e.message}")
        }
    }
    
    /**
     * Start pinned mode (screen pinning)
     * 
     * Phone calls work within pinned mode - they're system-level operations.
     * User can exit by holding Back + Overview (but unlikely to discover).
     */
    private fun startPinnedMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                // Check if already pinned
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    if (am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE) {
                        Log.d(TAG, "Already in pinned mode")
                        return
                    }
                }
                
                startLockTask()
                Log.d(TAG, "Pinned mode started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start pinned mode: ${e.message}")
            }
        }
    }
    
    /**
     * Safety net: ensure we're in pinned mode after returning from
     * unexpected interruptions (system dialogs, etc.)
     */
    private fun ensurePinnedMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                // We're out of pinned mode - re-enable it
                Log.d(TAG, "Re-entering pinned mode (safety net)")
                startPinnedMode()
            }
        }
    }
    
    /**
     * Block volume buttons when lockVolumeButtons is enabled
     * Volume is set by carer and should stay at that level
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (lockVolumeButtons) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_MUTE -> {
                    Log.d(TAG, "Volume button blocked")
                    return true  // Consume the event - do nothing
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }
    
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        // Hide BOTH status bar and navigation bar
        controller.hide(WindowInsetsCompat.Type.systemBars())
        // Only show bars on deliberate swipe, auto-hide after
        controller.systemBarsBehavior = 
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
    
    /**
     * Block back button/gesture to prevent accidental app exit
     * This is a backup - pinned mode already blocks most navigation
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do nothing - block back navigation entirely
        Log.d(TAG, "Back press blocked")
    }
}

@Composable
fun WandasPhoneApp(callManager: CallManager) {
    val navController = rememberNavController()
    
    // Observe call state for automatic navigation
    val currentCall by callManager.currentCall.collectAsState(initial = null)
    
    // Auto-navigate based on call state
    LaunchedEffect(currentCall) {
        val call = currentCall
        when {
            // Incoming call ringing -> show incoming call screen
            call?.state == CallState.RINGING && call.direction == CallDirection.INCOMING -> {
                if (navController.currentDestination?.route != "incoming") {
                    navController.navigate("incoming") {
                        launchSingleTop = true
                    }
                }
            }
            // No call or call ended -> return to home
            call == null || call.state == CallState.IDLE || call.state == CallState.DISCONNECTED -> {
                if (navController.currentDestination?.route == "incoming") {
                    navController.popBackStack("home", inclusive = false)
                }
            }
        }
    }
    
    WandasPhoneTheme(themeOption = ThemeOption.HIGH_CONTRAST_LIGHT) {
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") {
                HomeScreen(
                    onNavigateToCall = {
                        navController.navigate("call")
                    },
                    onNavigateToCarer = {
                        navController.navigate("carer")
                    }
                )
            }
            
            // Incoming call screen - shown when phone rings
            composable("incoming") {
                IncomingCallScreen(
                    onCallAnswered = {
                        // Stay on home - the end call UI is there
                        navController.popBackStack("home", inclusive = false)
                    },
                    onCallRejected = {
                        navController.popBackStack("home", inclusive = false)
                    }
                )
            }
            
            composable("call") {
                InCallScreen(
                    onNavigateBack = {
                        navController.popBackStack("home", inclusive = false)
                    }
                )
            }
            
            composable("carer") {
                com.tomsphone.feature.carer.CarerScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
