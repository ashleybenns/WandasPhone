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
    
    private val _currentCall = MutableStateFlow<CallInfo?>(null)
    override val currentCall: StateFlow<CallInfo?> = _currentCall
    
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
            
            // Cancel missed call nag in background
            scope.launch {
                try {
                    missedCallNagManager.get().dismissAll()
                    Log.d(TAG, "Dismissed missed call nag")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to dismiss nag: ${e.message}")
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
    
    /**
     * Update current call state (called from InCallService)
     */
    fun updateCallState(callInfo: CallInfo?) {
        _currentCall.value = callInfo
        Log.d(TAG, "Call state updated: ${callInfo?.state}")
    }
}

