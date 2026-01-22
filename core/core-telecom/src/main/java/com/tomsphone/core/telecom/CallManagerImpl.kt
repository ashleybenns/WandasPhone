package com.tomsphone.core.telecom

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of CallManager using Android Telecom API
 */
@Singleton
class CallManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val missedCallNagManager: dagger.Lazy<MissedCallNagManager>
) : CallManager {
    
    private companion object {
        const val TAG = "CallManager"
    }
    
    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // SEPARATE FLOWS for clean UI handling:
    // - _incomingRingingCall: For incoming RINGING calls (IncomingCallScreen observes)
    // - _currentCall: For outgoing calls and ACTIVE incoming calls (HomeScreen observes)
    private val _incomingRingingCall = MutableStateFlow<CallInfo?>(null)
    override val incomingRingingCall: StateFlow<CallInfo?> = _incomingRingingCall
    
    private val _currentCall = MutableStateFlow<CallInfo?>(null)
    override val currentCall: StateFlow<CallInfo?> = _currentCall
    
    // Emergency mode - when true, unknown calls bypass screening
    private val _isEmergencyMode = MutableStateFlow(false)
    override val isEmergencyMode: StateFlow<Boolean> = _isEmergencyMode
    
    override fun setEmergencyMode(enabled: Boolean) {
        Log.d(TAG, "setEmergencyMode: $enabled")
        _isEmergencyMode.value = enabled
    }
    
    override fun placeCall(phoneNumber: String): Result<Unit> {
        return runCatching {
            Log.d(TAG, "placeCall called for $phoneNumber")
            
            // Only need CALL_PHONE permission to place a call
            if (!hasCallPermission()) {
                Log.e(TAG, "Missing CALL_PHONE permission!")
                throw SecurityException("Missing CALL_PHONE permission")
            }
            
            // Stop nag audio (wrapped in try-catch to not block call)
            try {
                missedCallNagManager.get().stopAllAudio()
                Log.d(TAG, "Stopped nag audio")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop nag audio: ${e.message}")
                // Continue with call anyway
            }
            
            // Use TelecomManager.placeCall() - this works within pinned mode
            // because it doesn't launch a new activity, it just instructs the
            // telecom system to place the call. Our InCallService handles the rest.
            val uri = Uri.parse("tel:$phoneNumber")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                telecomManager.placeCall(uri, null)
                Log.d(TAG, "Placed call via TelecomManager to $phoneNumber")
            } else {
                // Fallback for older devices - this may trigger pinned mode warning
                val intent = Intent(Intent.ACTION_CALL, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Log.d(TAG, "Placed call via Intent (fallback) to $phoneNumber")
            }
            
            // Only dismiss nag if calling the person who called
            // If calling someone else, the nag will resume after this call
            scope.launch {
                try {
                    val dismissed = missedCallNagManager.get().dismissIfCallingMissedCaller(phoneNumber)
                    if (dismissed) {
                        Log.d(TAG, "Dismissed missed call nag (calling the missed caller)")
                    } else {
                        Log.d(TAG, "Nag not dismissed (calling different person)")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to check nag: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Check if we have permission to place calls
     */
    private fun hasCallPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    override fun answerCall(): Result<Unit> {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ANSWER_PHONE_CALLS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    telecomManager.acceptRingingCall()
                    Log.d(TAG, "Answered call")
                } else {
                    throw SecurityException("Missing ANSWER_PHONE_CALLS permission")
                }
            } else {
                throw UnsupportedOperationException("Answer call not supported on this Android version")
            }
        }
    }
    
    override fun rejectCall(): Result<Unit> {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ANSWER_PHONE_CALLS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    telecomManager.endCall()
                    Log.d(TAG, "Rejected call")
                } else {
                    throw SecurityException("Missing ANSWER_PHONE_CALLS permission")
                }
            }
        }
    }
    
    override fun endCall(): Result<Unit> {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ANSWER_PHONE_CALLS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    telecomManager.endCall()
                    Log.d(TAG, "Ended call")
                } else {
                    throw SecurityException("Missing ANSWER_PHONE_CALLS permission")
                }
            }
        }
    }
    
    override fun toggleSpeaker(): Result<Unit> {
        // Implemented in InCallService
        return Result.success(Unit)
    }
    
    override fun setSpeaker(enabled: Boolean): Result<Unit> {
        // Implemented in InCallService
        return Result.success(Unit)
    }
    
    override fun toggleMute(): Result<Unit> {
        // Implemented in InCallService
        return Result.success(Unit)
    }
    
    override fun setMute(muted: Boolean): Result<Unit> {
        // Implemented in InCallService
        return Result.success(Unit)
    }
    
    override fun setVolume(level: Int): Result<Unit> {
        // Implemented in InCallService
        return Result.success(Unit)
    }
    
    override fun hasPhonePermissions(): Boolean {
        val requiredPermissions = listOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.ANSWER_PHONE_CALLS
        )
        
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    override fun isDefaultDialer(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
        } else {
            telecomManager.defaultDialerPackage == context.packageName
        }
    }
    
    // Track whether we've notified nagManager of call start (to only notify once)
    private var hasNotifiedCallStart = false
    
    /**
     * Update current call state (called from InCallService)
     */
    /**
     * Update call state - routes to appropriate flow based on call direction/state
     * 
     * ROUTING LOGIC:
     * - Incoming RINGING → incomingRingingCall (IncomingCallScreen observes)
     * - Incoming ACTIVE → currentCall (call was answered, HomeScreen shows end call)
     * - Incoming DISCONNECTED/IDLE → clear both flows
     * - Outgoing (any state) → currentCall (HomeScreen handles)
     * - null → clear both flows
     * 
     * Also notifies MissedCallNagManager of call lifecycle to prevent race conditions.
     */
    fun updateCallState(callInfo: CallInfo?) {
        if (callInfo == null) {
            Log.d(TAG, "Call state cleared (null)")
            _incomingRingingCall.value = null
            _currentCall.value = null
            // Notify nag manager that call ended
            if (hasNotifiedCallStart) {
                try {
                    missedCallNagManager.get().onCallEnded()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to notify nag manager of call end: ${e.message}")
                }
                hasNotifiedCallStart = false
            }
            return
        }
        
        val isIncoming = callInfo.direction == CallDirection.INCOMING
        val state = callInfo.state
        
        Log.d(TAG, "updateCallState: state=$state, incoming=$isIncoming")
        
        when {
            // Incoming RINGING → only to incomingRingingCall
            isIncoming && state == CallState.RINGING -> {
                Log.d(TAG, ">>> Routing to incomingRingingCall ONLY (currentCall stays: ${_currentCall.value?.state})")
                _incomingRingingCall.value = callInfo
                // DON'T update currentCall - HomeScreen shouldn't see this
                // EXPLICIT: Make sure currentCall is not touched
            }
            
            // Incoming ACTIVE (answered) → move from incoming to current
            isIncoming && state == CallState.ACTIVE -> {
                Log.d(TAG, ">>> Incoming call answered - moving to currentCall")
                _incomingRingingCall.value = null  // Clear incoming
                _currentCall.value = callInfo      // Now HomeScreen can see it
                // Notify nag manager that an active call started
                if (!hasNotifiedCallStart) {
                    try {
                        missedCallNagManager.get().onCallStarted()
                        hasNotifiedCallStart = true
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to notify nag manager of call start: ${e.message}")
                    }
                }
                // Check if this is the missed caller - if so, dismiss the nag permanently
                callInfo.phoneNumber?.let { phone ->
                    scope.launch {
                        try {
                            missedCallNagManager.get().dismissIfTalkingToMissedCaller(phone)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to check if talking to missed caller: ${e.message}")
                        }
                    }
                }
            }
            
            // Incoming DISCONNECTED/IDLE → clear both
            isIncoming && (state == CallState.DISCONNECTED || state == CallState.IDLE) -> {
                Log.d(TAG, ">>> Incoming call ended - clearing both flows")
                _incomingRingingCall.value = null
                _currentCall.value = null
                // Notify nag manager that call ended
                if (hasNotifiedCallStart) {
                    try {
                        missedCallNagManager.get().onCallEnded()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to notify nag manager of call end: ${e.message}")
                    }
                    hasNotifiedCallStart = false
                }
            }
            
            // Outgoing calls → always to currentCall
            !isIncoming -> {
                Log.d(TAG, ">>> Outgoing call - routing to currentCall")
                _currentCall.value = callInfo
                // For outgoing calls, HomeViewModel already calls onCallStarted()
                // But track it here too in case it's missed
                if (state == CallState.ACTIVE && !hasNotifiedCallStart) {
                    try {
                        missedCallNagManager.get().onCallStarted()
                        hasNotifiedCallStart = true
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to notify nag manager of call start: ${e.message}")
                    }
                }
                // When outgoing call becomes ACTIVE, check if this is the missed caller
                if (state == CallState.ACTIVE) {
                    callInfo.phoneNumber?.let { phone ->
                        scope.launch {
                            try {
                                missedCallNagManager.get().dismissIfTalkingToMissedCaller(phone)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to check if talking to missed caller: ${e.message}")
                            }
                        }
                    }
                }
                // Handle outgoing call end
                if (state == CallState.DISCONNECTED || state == CallState.IDLE) {
                    if (hasNotifiedCallStart) {
                        try {
                            missedCallNagManager.get().onCallEnded()
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to notify nag manager of call end: ${e.message}")
                        }
                        hasNotifiedCallStart = false
                    }
                }
            }
            
            // Other incoming states (CONNECTING, etc.) → keep in incoming flow
            else -> {
                Log.d(TAG, ">>> Other incoming state - keeping in incomingRingingCall")
                _incomingRingingCall.value = callInfo
            }
        }
    }
    
    /**
     * Clear incoming ringing call (called when answered/rejected)
     */
    fun clearIncomingCall() {
        Log.d(TAG, "Clearing incoming ringing call")
        _incomingRingingCall.value = null
    }
}

