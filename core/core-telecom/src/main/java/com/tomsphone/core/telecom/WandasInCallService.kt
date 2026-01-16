package com.tomsphone.core.telecom

import android.media.AudioManager
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import com.tomsphone.core.config.FeatureLevel
import com.tomsphone.core.config.SettingsRepository
import com.tomsphone.core.data.repository.ContactRepository
import com.tomsphone.core.tts.TTSScripts
import com.tomsphone.core.tts.WandasTTS
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * InCallService for handling active phone calls
 * 
 * This is the heart of the phone functionality.
 * It receives all call state changes from Android system.
 * 
 * Speakerphone behavior:
 * - Level 1 (MINIMAL): Always on speaker, no toggle
 * - Level 2+ (BASIC+): Toggle available, default from settings
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
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    @Inject
    lateinit var contactRepository: ContactRepository
    
    @Inject
    lateinit var missedCallNagManager: dagger.Lazy<MissedCallNagManager>
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var currentCall: Call? = null
    private var isSpeakerOn = false
    private var isMuted = false
    private var wasCallActive = false  // Track if call was ever connected
    private var wasIncomingCall = false  // Track if this was an incoming call
    private var lastIncomingPhoneNumber: String? = null  // For missed call nag
    private var lastIncomingContactName: String? = null
    
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
        val isIncoming = call.details.callDirection == Call.Details.DIRECTION_INCOMING
        val phoneNumber = call.details.handle?.schemeSpecificPart ?: "Unknown"
        
        Log.d(TAG, "========================================")
        Log.d(TAG, "onCallAdded: $phoneNumber, incoming=$isIncoming, state=${call.state}")
        
        // BACKUP: If CallScreeningService didn't reject, check here
        if (isIncoming) {
            // Synchronously check if caller is allowed
            val isAllowed = runBlocking {
                try {
                    val settings = settingsRepository.getSettings().first()
                    if (!settings.rejectUnknownCalls) {
                        Log.d(TAG, "rejectUnknownCalls is disabled, allowing")
                        true
                    } else {
                        val contact = findContactByPhone(phoneNumber)
                        Log.d(TAG, "Contact lookup: ${contact ?: "NOT FOUND"}")
                        contact != null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking caller: ${e.message}")
                    true // Allow on error
                }
            }
            
            if (!isAllowed) {
                Log.d(TAG, ">>> REJECTING unknown caller in InCallService")
                call.reject(false, null)
                Log.d(TAG, "========================================")
                return // Don't process further
            }
            Log.d(TAG, ">>> Caller is allowed")
        }
        
        Log.d(TAG, "========================================")
        
        currentCall?.unregisterCallback(callCallback)
        currentCall = call
        call.registerCallback(callCallback)
        wasCallActive = false  // Reset for new call
        wasIncomingCall = isIncoming
        lastIncomingPhoneNumber = if (isIncoming) phoneNumber else null
        lastIncomingContactName = null  // Will be set during contact lookup
        
        // Enable speakerphone when call is added
        serviceScope.launch {
            enableSpeakerBasedOnSettings()
            
            // Look up contact name for missed call tracking
            if (isIncoming) {
                lastIncomingContactName = findContactByPhone(phoneNumber)
            }
        }
        
        handleCallStateChange(call, call.state)
    }
    
    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "Call removed, wasActive: $wasCallActive, wasIncoming: $wasIncomingCall")
        
        call.unregisterCallback(callCallback)
        if (currentCall == call) {
            currentCall = null
        }
        
        callManager.updateCallState(null)
        
        // Handle call end based on type
        if (wasCallActive) {
            // Call was connected - announce "call ended"
            tts.speak(TTSScripts.callEnded())
        } else if (wasIncomingCall && lastIncomingPhoneNumber != null) {
            // Incoming call was NOT answered → trigger missed call nag
            Log.d(TAG, "Unanswered incoming call from ${lastIncomingContactName ?: lastIncomingPhoneNumber} - triggering nag")
            missedCallNagManager.get().onMissedCall(
                lastIncomingPhoneNumber!!,
                lastIncomingContactName
            )
        }
        
        // Reset state
        wasCallActive = false
        wasIncomingCall = false
        lastIncomingPhoneNumber = null
        lastIncomingContactName = null
    }
    
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        audioState?.let {
            isSpeakerOn = it.route == CallAudioState.ROUTE_SPEAKER
            Log.d(TAG, "Audio state changed - Speaker: $isSpeakerOn")
            updateCallInfo()
        }
    }
    
    /**
     * Enable speakerphone based on feature level and settings
     * 
     * Level 1: Always speaker (no choice)
     * Level 2+: Use carer's default setting
     */
    private suspend fun enableSpeakerBasedOnSettings() {
        val settings = settingsRepository.getSettings().first()
        
        val shouldEnableSpeaker = when (settings.featureLevel) {
            FeatureLevel.MINIMAL -> {
                // Level 1: ALWAYS on speaker, no exceptions
                Log.d(TAG, "Level 1: Forcing speakerphone ON")
                true
            }
            else -> {
                // Level 2+: Use carer's default setting
                // TODO Level 2: Add carer setting "Default speaker on/off" to CarerScreen
                // TODO Level 2: User can toggle during call via InCallScreen button
                Log.d(TAG, "Level 2+: Using default speaker setting: ${settings.speakerphoneAlwaysOn}")
                settings.speakerphoneAlwaysOn
            }
        }
        
        if (shouldEnableSpeaker) {
            setSpeaker(true)
        }
        
        // Set volume based on settings
        setCallVolume(settings.speakerVolume)
    }
    
    /**
     * Set speakerphone state
     */
    fun setSpeaker(enabled: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val route = if (enabled) {
                    CallAudioState.ROUTE_SPEAKER
                } else {
                    CallAudioState.ROUTE_EARPIECE
                }
                setAudioRoute(route)
                isSpeakerOn = enabled
                Log.d(TAG, "Speakerphone set to: $enabled")
                updateCallInfo()
                
                // Announce change (only if user toggled it at Level 2+)
                serviceScope.launch {
                    val settings = settingsRepository.getSettings().first()
                    if (settings.featureLevel != FeatureLevel.MINIMAL) {
                        if (enabled) {
                            tts.speak(TTSScripts.speakerOn())
                        } else {
                            tts.speak(TTSScripts.speakerOff())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting speakerphone: ${e.message}")
        }
    }
    
    /**
     * Toggle speakerphone (for Level 2+ UI)
     * 
     * TODO Level 2: Add speaker toggle button to InCallScreen UI
     * TODO Level 2: Only show toggle when featureLevel >= BASIC
     */
    fun toggleSpeaker() {
        setSpeaker(!isSpeakerOn)
    }
    
    /**
     * Set mute state
     */
    fun setMute(muted: Boolean) {
        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            audioManager.isMicrophoneMute = muted
            isMuted = muted
            Log.d(TAG, "Mute set to: $muted")
            updateCallInfo()
            
            if (muted) {
                tts.speak(TTSScripts.muted())
            } else {
                tts.speak(TTSScripts.unmuted())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting mute: ${e.message}")
        }
    }
    
    /**
     * Toggle mute
     */
    fun toggleMute() {
        setMute(!isMuted)
    }
    
    /**
     * Set call volume (0-100 percent)
     */
    private fun setCallVolume(volumePercent: Int) {
        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            val targetVolume = (maxVolume * (volumePercent / 100f)).toInt().coerceIn(0, maxVolume)
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, targetVolume, 0)
            Log.d(TAG, "Volume set to $volumePercent% ($targetVolume/$maxVolume)")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume: ${e.message}")
        }
    }
    
    /**
     * Answer the current call
     */
    fun answerCall() {
        currentCall?.let { call ->
            if (call.state == Call.STATE_RINGING) {
                call.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
                Log.d(TAG, "Answered call")
            }
        }
    }
    
    /**
     * End the current call
     */
    fun endCall() {
        currentCall?.let { call ->
            call.disconnect()
            Log.d(TAG, "Ended call")
        }
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
        val direction = if (call.details.callDirection == Call.Details.DIRECTION_INCOMING) {
            CallDirection.INCOMING
        } else {
            CallDirection.OUTGOING
        }
        
        // Emit state IMMEDIATELY so UI can react (especially for incoming calls)
        // Contact name will be updated in a follow-up emission
        val immediateCallInfo = CallInfo(
            callId = call.details.handle.toString(),
            phoneNumber = phoneNumber,
            contactName = null,  // Will be updated shortly
            contactId = null,
            state = wandasState,
            direction = direction,
            startTime = System.currentTimeMillis(),
            isSpeakerOn = isSpeakerOn,
            isMuted = isMuted
        )
        callManager.updateCallState(immediateCallInfo)
        Log.d(TAG, "Call state (immediate): $wandasState, direction: $direction")
        
        // Then look up contact name and update again
        serviceScope.launch {
            val contactName = try {
                findContactByPhone(phoneNumber)
            } catch (e: Exception) {
                Log.e(TAG, "Error looking up contact: ${e.message}")
                null
            }
            
            if (contactName != null) {
                val updatedCallInfo = immediateCallInfo.copy(contactName = contactName)
                callManager.updateCallState(updatedCallInfo)
                Log.d(TAG, "Call state (with contact): $wandasState, contact: $contactName")
            }
        }
        
        // When call becomes active, ensure speaker is set correctly
        if (wandasState == CallState.ACTIVE) {
            wasCallActive = true
            serviceScope.launch {
                enableSpeakerBasedOnSettings()
                val contactName = findContactByPhone(phoneNumber)
                tts.speak(TTSScripts.callConnected(contactName ?: phoneNumber))
            }
        }
    }
    
    /**
     * Find contact by phone number (with flexible matching)
     */
    private suspend fun findContactByPhone(phoneNumber: String): String? {
        // Try exact match first
        var contact = contactRepository.getContactByPhone(phoneNumber).first()
        if (contact != null) return contact.name
        
        // Try normalized match
        val normalizedNumber = normalizePhoneNumber(phoneNumber)
        if (normalizedNumber != phoneNumber) {
            contact = contactRepository.getContactByPhone(normalizedNumber).first()
            if (contact != null) return contact.name
        }
        
        return null
    }
    
    /**
     * Normalize phone number for matching (UK format)
     */
    private fun normalizePhoneNumber(phone: String): String {
        var digits = phone.replace(Regex("[^0-9]"), "")
        // Handle UK +44 prefix -> 0
        if (digits.startsWith("44") && digits.length > 10) {
            digits = "0" + digits.substring(2)
        }
        return digits
    }
    
    private fun updateCallInfo() {
        currentCall?.let { call ->
            val phoneNumber = call.details.handle?.schemeSpecificPart ?: "Unknown"
            val state = when (call.state) {
                Call.STATE_ACTIVE -> CallState.ACTIVE
                Call.STATE_RINGING -> CallState.RINGING
                Call.STATE_DIALING -> CallState.DIALING
                else -> CallState.IDLE
            }
            
            // FIX: Use actual direction from call details, not hardcoded OUTGOING
            val direction = if (call.details.callDirection == Call.Details.DIRECTION_INCOMING) {
                CallDirection.INCOMING
            } else {
                CallDirection.OUTGOING
            }
            
            val callInfo = CallInfo(
                callId = call.details.handle.toString(),
                phoneNumber = phoneNumber,
                contactName = null,
                contactId = null,
                state = state,
                direction = direction,
                startTime = System.currentTimeMillis(),
                isSpeakerOn = isSpeakerOn,
                isMuted = isMuted
            )
            callManager.updateCallState(callInfo)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }
}
