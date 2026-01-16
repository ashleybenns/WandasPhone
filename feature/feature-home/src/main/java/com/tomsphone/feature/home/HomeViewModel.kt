package com.tomsphone.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.config.FeatureLevel
import com.tomsphone.core.config.SettingsRepository
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.repository.ContactRepository
import com.tomsphone.core.telecom.CallManager
import com.tomsphone.core.telecom.CallState
import com.tomsphone.core.telecom.MissedCallNagManager
import com.tomsphone.core.tts.TTSScripts
import com.tomsphone.core.tts.WandasTTS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for home screen
 * 
 * Responsibilities:
 * - Load contacts based on feature level
 * - Handle tap-to-call with 1-second animation
 * - Display status messages in the top text box
 * - Carer settings access (7-tap hidden button)
 * 
 * NOTE: End call UI is on separate screens (EndOutgoingCallScreen, EndIncomingCallScreen)
 * Navigation is handled by MainActivity observing call states.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val contactRepository: ContactRepository,
    private val callManager: CallManager,
    private val missedCallNagManager: MissedCallNagManager,
    private val tts: WandasTTS
) : ViewModel() {
    
    companion object {
        private const val TAG = "HomeViewModel"
    }
    
    // Current feature level
    val featureLevel: StateFlow<FeatureLevel> = settingsRepository.getFeatureLevel()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FeatureLevel.MINIMAL
        )
    
    // User name
    val userName: StateFlow<String> = settingsRepository.getUserName()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Tom"
        )
    
    // Maximum contacts based on level
    private val maxContacts: StateFlow<Int> = settingsRepository.getMaxContacts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 2
        )
    
    // Contacts to display
    val contacts: StateFlow<List<Contact>> = maxContacts
        .flatMapLatest { max ->
            contactRepository.getContacts(max)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Status message displayed in top text box
    private val _statusMessage = MutableStateFlow<String?>(null)
    private var statusMessageResetJob: Job? = null
    
    // Combine user name with status message for display
    val displayMessage: StateFlow<String> = combine(userName, _statusMessage) { name, status ->
        status ?: "$name's phone"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Tom's phone"
    )
    
    // Calling animation state - when active, shows the black button for 1 second
    private val _callingContact = MutableStateFlow<Contact?>(null)
    val callingContact: StateFlow<Contact?> = _callingContact.asStateFlow()
    
    // Expose currentCall for UI to prevent standby flash
    // HomeScreen uses this to detect active outgoing calls
    val currentCallForUI = callManager.currentCall
    
    // Carer settings tap counter
    private val _carerTapCount = MutableStateFlow(0)
    private val _showCarerAccess = MutableStateFlow(false)
    val showCarerAccess: StateFlow<Boolean> = _showCarerAccess
    
    init {
        // Announce greeting on app start
        viewModelScope.launch {
            userName.first().let { name ->
                tts.speak(TTSScripts.greeting(name))
            }
        }
        
        // Monitor missed calls and update status message
        viewModelScope.launch {
            missedCallNagManager.activeMissedCalls.collect { missedCalls ->
                if (missedCalls.isNotEmpty()) {
                    val caller = missedCalls.first().contactName ?: "someone"
                    val user = userName.value
                    setStatus("$user, you missed a call.\nPlease call $caller now.")
                } else {
                    if (_statusMessage.value?.contains("missed a call") == true) {
                        _statusMessage.value = null
                    }
                }
            }
        }
        
        // Listen for call state changes (for logging only)
        // NOTE: We don't clear _statusMessage or _callingContact here to avoid race conditions.
        // State is cleared via clearCallingStateIfNoCall() when HomeScreen is visible with no active call.
        viewModelScope.launch {
            callManager.currentCall.collect { callInfo ->
                val state = callInfo?.state ?: CallState.IDLE
                Log.d(TAG, "currentCall: state=$state, callingContact=${_callingContact.value?.name}, status=${_statusMessage.value}")
            }
        }
    }
    
    /**
     * Set a temporary status message
     */
    private fun setTemporaryStatus(message: String, durationMs: Long = 5000) {
        statusMessageResetJob?.cancel()
        _statusMessage.value = message
        
        statusMessageResetJob = viewModelScope.launch {
            delay(durationMs)
            _statusMessage.value = null
        }
    }
    
    /**
     * Set status message that stays until cleared
     */
    private fun setStatus(message: String?) {
        statusMessageResetJob?.cancel()
        _statusMessage.value = message
    }
    
    /**
     * Clear calling state - called by HomeScreen when screen is visible
     * and there's no active call
     */
    fun clearCallingStateIfNoCall() {
        val call = callManager.currentCall.value
        val hasActiveCall = call != null && 
            call.state != CallState.IDLE && 
            call.state != CallState.DISCONNECTED
        
        if (!hasActiveCall) {
            Log.d(TAG, "Clearing calling state (no active call)")
            _callingContact.value = null
            _statusMessage.value = null
        }
    }
    
    /**
     * User tapped a contact button - start calling animation then place call
     * 
     * UX Flow:
     * 1. Show "calling animation" (black button) for 1 second
     * 2. Place the call
     * 3. MainActivity navigates to EndOutgoingCallScreen when it sees outgoing call state
     */
    fun onContactTap(contact: Contact) {
        Log.d(TAG, "onContactTap: ${contact.name}")
        
        // Immediately enter calling mode - show the "calling animation"
        _callingContact.value = contact
        setStatus("Calling ${contact.name}")
        
        viewModelScope.launch {
            // Use speakNow to interrupt any ongoing TTS
            tts.speakNow(TTSScripts.calling(contact.name))
            
            // Show the calling animation for 1 second
            delay(1000)
            
            Log.d(TAG, "Animation complete, placing call")
            
            // Place the call - MainActivity will handle navigation
            val result = callManager.placeCall(contact.phoneNumber)
            
            if (result.isFailure) {
                Log.e(TAG, "Failed to place call: ${result.exceptionOrNull()}")
                _callingContact.value = null
                setTemporaryStatus("Couldn't place call")
                tts.speakNow("Sorry, I couldn't place that call.")
            }
            // On success, the call state collector clears _callingContact
            // and MainActivity navigates to EndOutgoingCallScreen
        }
    }
    
    /**
     * Hidden carer access button tapped
     */
    fun onCarerButtonTap() {
        _carerTapCount.value += 1
        
        viewModelScope.launch {
            val settings = settingsRepository.getSettings().first()
            if (_carerTapCount.value >= settings.settingsAccessTapCount) {
                _showCarerAccess.value = true
                _carerTapCount.value = 0
            }
        }
        
        viewModelScope.launch {
            delay(3000)
            _carerTapCount.value = 0
        }
    }
    
    /**
     * Reset carer access dialog
     */
    fun dismissCarerAccess() {
        _showCarerAccess.value = false
    }
    
    /**
     * Emergency button tapped - DEV MODE: goes to settings
     */
    fun onEmergencyButtonTap() {
        _showCarerAccess.value = true
    }
}
