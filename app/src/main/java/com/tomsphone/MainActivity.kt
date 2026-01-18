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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.tomsphone.core.ui.theme.UserScalingProvider
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
import com.tomsphone.feature.phone.EndIncomingCallScreen
import com.tomsphone.feature.phone.EndOutgoingCallScreen
import com.tomsphone.feature.phone.IncomingCallScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    
    private var lockVolumeButtons = true
    private var pinnedModeEnabled = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        hideSystemBars()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        lifecycleScope.launch {
            applySettings()
        }
        
        setContent {
            WandasPhoneApp(callManager, settingsRepository)
        }
    }
    
    override fun onResume() {
        super.onResume()
        hideSystemBars()
        
        if (pinnedModeEnabled) {
            ensurePinnedMode()
        }
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
            
            if (settings.pinnedModeEnabled) {
                startPinnedMode()
            }
            
            Log.d(TAG, "Settings applied: pinned=$pinnedModeEnabled, volumeLock=$lockVolumeButtons")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying settings: ${e.message}")
        }
    }
    
    private fun startPinnedMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    if (am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE) {
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
    
    private fun ensurePinnedMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                startPinnedMode()
            }
        }
    }
    
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (lockVolumeButtons) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_MUTE -> return true
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
        controller.systemBarsBehavior = 
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        Log.d(TAG, "Back press blocked")
    }
}

@Composable
fun WandasPhoneApp(callManager: CallManager, settingsRepository: SettingsRepository) {
    val navController = rememberNavController()
    
    // Observe both call flows
    val incomingCall by callManager.incomingRingingCall.collectAsState(initial = null)
    val currentCall by callManager.currentCall.collectAsState(initial = null)
    
    // Observe user text size setting (scale factor)
    val settings by settingsRepository.getSettings().collectAsState(initial = null)
    val userTextScale = settings?.ui?.userTextSize?.scale ?: 1.0f  // Default to NORMAL (100%)
    
    // Track the last outgoing contact name for navigation
    var lastOutgoingContactName by remember { mutableStateOf("Caller") }
    
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
    LaunchedEffect(currentCall) {
        val call = currentCall
        val currentRoute = navController.currentDestination?.route
        
        Log.d("WandasPhoneApp", "=== currentCall: state=${call?.state}, direction=${call?.direction}, route=$currentRoute ===")
        
        when {
            // No call or call ended → return to home (if on end call screen)
            call == null || call.state == CallState.IDLE || call.state == CallState.DISCONNECTED -> {
                if (currentRoute == "endIncoming" || currentRoute?.startsWith("endOutgoing") == true) {
                    Log.d("WandasPhoneApp", ">>> Call ended - returning to home")
                    navController.popBackStack("home", inclusive = false)
                }
            }
            
            // OUTGOING call active (DIALING, RINGING, ACTIVE) → yellow screen
            call.direction == CallDirection.OUTGOING && 
            (call.state == CallState.DIALING || call.state == CallState.RINGING || call.state == CallState.ACTIVE) -> {
                if (currentRoute != "endOutgoing" && !currentRoute.orEmpty().startsWith("endOutgoing")) {
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
    
    WandasPhoneTheme(themeOption = ThemeOption.HIGH_CONTRAST_LIGHT) {
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            // USER SCREENS - wrapped with UserScalingProvider
            // These scale text/buttons based on carer-configured userTextSize
            
            composable("home") {
                UserScalingProvider(scale = userTextScale) {
                    HomeScreen(
                        onNavigateToCarer = {
                            navController.navigate("carer")
                        }
                    )
                }
            }
            
            // Incoming call - Answer/Reject
            composable("incoming") {
                UserScalingProvider(scale = userTextScale) {
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
            
            // End call screen for INCOMING (GREEN)
            composable("endIncoming") {
                UserScalingProvider(scale = userTextScale) {
                    EndIncomingCallScreen(
                        onCallEnded = {
                            navController.popBackStack("home", inclusive = false)
                        }
                    )
                }
            }
            
            // End call screen for OUTGOING (YELLOW)
            composable("endOutgoing/{contactName}") { backStackEntry ->
                val contactName = backStackEntry.arguments?.getString("contactName") ?: "Caller"
                UserScalingProvider(scale = userTextScale) {
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
                    }
                )
            }
        }
    }
}
