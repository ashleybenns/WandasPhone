package com.wandasphone.feature.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wandasphone.core.config.FeatureLevel
import com.wandasphone.core.config.SettingsRepository
import com.wandasphone.core.data.model.CallLogEntry
import com.wandasphone.core.data.model.CallType
import com.wandasphone.core.data.model.Contact
import com.wandasphone.core.data.repository.CallLogRepository
import com.wandasphone.core.data.repository.ContactRepository
import com.wandasphone.core.telecom.CallInfo
import com.wandasphone.core.telecom.CallManager
import com.wandasphone.core.telecom.CallState
import com.wandasphone.core.tts.TTSScripts
import com.wandasphone.core.tts.WandasTTS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for in-call screen
 * 
 * Handles:
 * - Call state management
 * - Contact name resolution
 * - Call controls (Level 1: end only, Level 2+: speaker/mute/volume)
 * - Call logging
 */
@HiltViewModel
class InCallViewModel @Inject constructor(
    private val callManager: CallManager,
    private val contactRepository: ContactRepository,
    private val callLogRepository: CallLogRepository,
    private val settingsRepository: SettingsRepository,
    private val tts: WandasTTS
) : ViewModel() {
    
    // Current call with contact name resolved
    val currentCall: StateFlow<CallInfo?> = callManager.currentCall
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    // Feature level (controls what buttons are shown)
    val featureLevel: StateFlow<FeatureLevel> = settingsRepository.getFeatureLevel()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FeatureLevel.MINIMAL
        )
    
    // Contact name for current call
    val contactName: StateFlow<String?> = currentCall
        .flatMapLatest { call ->
            if (call != null) {
                contactRepository.getContactByPhone(call.phoneNumber)
                    .map { it?.name }
            } else {
                flowOf(null)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    init {
        // Monitor call state for automatic actions
        viewModelScope.launch {
            currentCall.collect { call ->
                when (call?.state) {
                    CallState.DISCONNECTED -> {
                        // Log the call
                        logCall(call)
                    }
                    CallState.ACTIVE -> {
                        // Auto-enable speaker if configured
                        val settings = settingsRepository.getSettings().first()
                        if (settings.speakerVolume > 0) {
                            callManager.setSpeaker(true)
                            tts.speak(TTSScripts.speakerOn())
                        }
                    }
                    else -> {
                        // Other states handled by InCallService
                    }
                }
            }
        }
    }
    
    /**
     * End call button pressed
     */
    fun onEndCall() {
        viewModelScope.launch {
            tts.speak(TTSScripts.callEnded())
            callManager.endCall()
        }
    }
    
    /**
     * Toggle speaker (Level 2+)
     */
    fun onToggleSpeaker() {
        viewModelScope.launch {
            val result = callManager.toggleSpeaker()
            if (result.isSuccess) {
                val call = currentCall.value
                val enabled = call?.isSpeakerOn == false
                tts.speak(if (enabled) TTSScripts.speakerOn() else TTSScripts.speakerOff())
            }
        }
    }
    
    /**
     * Toggle mute (Level 2+)
     */
    fun onToggleMute() {
        viewModelScope.launch {
            val result = callManager.toggleMute()
            if (result.isSuccess) {
                val call = currentCall.value
                val muted = call?.isMuted == false
                tts.speak(if (muted) TTSScripts.muted() else TTSScripts.unmuted())
            }
        }
    }
    
    /**
     * Adjust volume (Level 2+)
     */
    fun onVolumeUp() {
        // Will be implemented with actual audio control
    }
    
    fun onVolumeDown() {
        // Will be implemented with actual audio control
    }
    
    /**
     * Log completed call to database
     */
    private fun logCall(call: CallInfo) {
        viewModelScope.launch {
            val contact = contactRepository.getContactByPhone(call.phoneNumber).first()
            val duration = System.currentTimeMillis() - call.startTime
            
            val callType = when {
                call.direction == com.wandasphone.core.telecom.CallDirection.INCOMING -> CallType.INCOMING
                call.direction == com.wandasphone.core.telecom.CallDirection.OUTGOING -> CallType.OUTGOING
                else -> CallType.MISSED
            }
            
            val entry = CallLogEntry(
                id = 0,
                contactId = contact?.id,
                phoneNumber = call.phoneNumber,
                contactName = contact?.name,
                type = callType,
                timestamp = call.startTime,
                duration = duration,
                isRead = true
            )
            
            callLogRepository.logCall(entry)
        }
    }
}

