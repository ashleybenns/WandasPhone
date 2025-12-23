package com.wandasphone.core.telecom

import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import com.wandasphone.core.tts.TTSScripts
import com.wandasphone.core.tts.WandasTTS
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * InCallService for handling active phone calls
 * 
 * This is the heart of the phone functionality.
 * It receives all call state changes from Android system.
 */
@AndroidEntryPoint
class WandasInCallService : InCallService() {
    
    private companion object {
        const val TAG = "WandasInCallService"
    }
    
    @Inject
    lateinit var callManager: CallManagerImpl
    
    @Inject
    lateinit var tts: WandasTTS
    
    private var currentCall: Call? = null
    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            handleCallStateChange(call, state)
        }
        
        override fun onDetailsChanged(call: Call, details: Call.Details) {
            super.onDetailsChanged(call, details)
            Log.d(TAG, "Call details changed")
        }
    }
    
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "Call added: ${call.details.handle}")
        
        currentCall?.unregisterCallback(callCallback)
        currentCall = call
        call.registerCallback(callCallback)
        
        handleCallStateChange(call, call.state)
    }
    
    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "Call removed")
        
        call.unregisterCallback(callCallback)
        if (currentCall == call) {
            currentCall = null
        }
        
        callManager.updateCallState(null)
        tts.speak(TTSScripts.callEnded())
    }
    
    private fun handleCallStateChange(call: Call, state: Int) {
        val wandasState = when (state) {
            Call.STATE_DIALING -> CallState.DIALING
            Call.STATE_RINGING -> CallState.RINGING
            Call.STATE_CONNECTING -> CallState.CONNECTING
            Call.STATE_ACTIVE -> CallState.ACTIVE
            Call.STATE_HOLDING -> CallState.HOLDING
            Call.STATE_DISCONNECTING -> CallState.DISCONNECTING
            Call.STATE_DISCONNECTED -> CallState.DISCONNECTED
            else -> CallState.IDLE
        }
        
        val phoneNumber = call.details.handle?.schemeSpecificPart ?: "Unknown"
        val direction = if (state == Call.STATE_RINGING) {
            CallDirection.INCOMING
        } else {
            CallDirection.OUTGOING
        }
        
        val callInfo = CallInfo(
            callId = call.details.handle.toString(),
            phoneNumber = phoneNumber,
            contactName = null,  // Will be resolved by feature-phone
            contactId = null,
            state = wandasState,
            direction = direction,
            startTime = System.currentTimeMillis(),
            isSpeakerOn = false,
            isMuted = false
        )
        
        callManager.updateCallState(callInfo)
        
        // Announce state changes
        when (wandasState) {
            CallState.RINGING -> {
                tts.speak(TTSScripts.incomingCall(null))
            }
            CallState.ACTIVE -> {
                tts.speak(TTSScripts.callConnected(phoneNumber))
            }
            else -> {
                // Other states handled elsewhere
            }
        }
        
        Log.d(TAG, "Call state: $wandasState")
    }
}

