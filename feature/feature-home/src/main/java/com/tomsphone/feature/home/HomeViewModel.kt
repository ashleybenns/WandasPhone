package com.tomsphone.feature.home

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
 * - Handle tap-to-call
 * - Display status messages in the top text box
 * - Track inactivity
 * - Carer settings access (7-tap hidden button)
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val contactRepository: ContactRepository,
    private val callManager: CallManager,
    private val missedCallNagManager: MissedCallNagManager,
    private val tts: WandasTTS
) : ViewModel() {
    
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
    // Default: "[User]'s phone", changes to "Calling [name]", "Missed call...", etc.
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
    
    // Calling state - when active, shows only the calling contact button
    // Other buttons vanish, tapped button fades to black
    private val _callingContact = MutableStateFlow<Contact?>(null)
    val callingContact: StateFlow<Contact?> = _callingContact.asStateFlow()
    
    // Show end call button after brief "calling" animation
    private val _showEndCallButton = MutableStateFlow(false)
    val showEndCallButton: StateFlow<Boolean> = _showEndCallButton.asStateFlow()
    
    // Current call state from CallManager
    val currentCallState: StateFlow<CallState> = callManager.currentCall
        .map { it?.state ?: CallState.IDLE }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CallState.IDLE
        )
    
    // Is call currently active (connected)?
    val isCallActive: StateFlow<Boolean> = currentCallState
        .map { it == CallState.ACTIVE }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    // End call button state - requires double tap
    private val _endCallTapCount = MutableStateFlow(0)
    private var endCallResetJob: Job? = null
    
    // Track if we've seen an active call (to avoid false "call ended" on initial IDLE)
    private var hasSeenActiveCall = false
    
    val endCallConfirmPending: StateFlow<Boolean> = _endCallTapCount
        .map { it == 1 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
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
        // Message STAYS until user calls back (nag is dismissed)
        viewModelScope.launch {
            missedCallNagManager.activeMissedCalls.collect { missedCalls ->
                if (missedCalls.isNotEmpty()) {
                    val caller = missedCalls.first().contactName ?: "someone"
                    val user = userName.value
                    // Two-line message: "[User] you missed a call.\nPlease call [carer] now."
                    setStatus("$user, you missed a call.\nPlease call $caller now.")
                } else {
                    // No active missed calls - clear if we were showing missed call message
                    if (_statusMessage.value?.contains("missed a call") == true) {
                        _statusMessage.value = null
                    }
                }
            }
        }
        
        // Monitor call state changes
        viewModelScope.launch {
            callManager.currentCall.collect { callInfo ->
                val state = callInfo?.state ?: CallState.IDLE
                
                when (state) {
                    CallState.DIALING, CallState.CONNECTING, CallState.RINGING -> {
                        // Call in progress but not yet connected
                        // Keep the calling UI visible
                    }
                    CallState.ACTIVE -> {
                        // Call connected - mark that we've seen an active call
                        hasSeenActiveCall = true
                        
                        // Show end call button for ANY active call (outgoing OR incoming)
                        _showEndCallButton.value = true
                        
                        // Update status with contact name
                        if (_callingContact.value != null) {
                            // Outgoing call - we already have the contact
                            setStatus("On call with ${_callingContact.value?.name}")
                        } else {
                            // Incoming call or outgoing without contact - look up the name
                            val phoneNumber = callInfo?.phoneNumber
                            if (phoneNumber != null) {
                                // Look up contact name from phone number
                                val contact = contactRepository.getContactByPhone(phoneNumber).first()
                                val displayName = contact?.name ?: phoneNumber
                                setStatus("On call with $displayName")
                            } else {
                                setStatus("On call")
                            }
                        }
                    }
                    CallState.DISCONNECTED, CallState.IDLE -> {
                        // Call ended - reset UI if we were showing end call button
                        if (_showEndCallButton.value || _callingContact.value != null) {
                            _callingContact.value = null
                            _showEndCallButton.value = false
                            _endCallTapCount.value = 0
                            if (hasSeenActiveCall) {
                                setTemporaryStatus("Call ended", 3000)
                            }
                            hasSeenActiveCall = false
                            // Don't announce "call ended" - it talks over voicemail etc.
                        }
                    }
                    else -> {
                        // Other states (HOLDING, DISCONNECTING, etc.)
                    }
                }
            }
        }
    }
    
    /**
     * Set a temporary status message that reverts to default after delay
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
     * Clear any temporary status and exit calling mode
     * Reverts to default "[User]'s phone" display
     * 
     * NOTE: Only clears if there's no active call in progress
     */
    fun clearStatus() {
        // Don't clear if we're in an active call or dialing
        val currentState = currentCallState.value
        if (currentState != CallState.IDLE && currentState != CallState.DISCONNECTED) {
            return
        }
        
        statusMessageResetJob?.cancel()
        _statusMessage.value = null
        _callingContact.value = null
    }
    
    /**
     * User tapped a contact button - place call
     * 
     * UX Flow:
     * 1. Button fades to black immediately (visual feedback)
     * 2. After brief animation (500ms), End Call button appears
     * 3. User can end call at any point (dialing, ringing, connected)
     */
    fun onContactTap(contact: Contact) {
        // Immediately enter calling mode - visual feedback FIRST
        // This is critical: user sees instant response to their tap
        _callingContact.value = contact
        _showEndCallButton.value = false  // Start with calling animation
        setStatus("Calling ${contact.name}")
        
        viewModelScope.launch {
            // Use speakNow to interrupt any ongoing TTS (like nag messages)
            tts.speakNow(TTSScripts.calling(contact.name))
            
            // Place the call (this will also stop nag audio)
            val result = callManager.placeCall(contact.phoneNumber)
            
            if (result.isFailure) {
                // Exit calling mode on failure
                _callingContact.value = null
                _showEndCallButton.value = false
                setTemporaryStatus("Couldn't place call")
                tts.speakNow("Sorry, I couldn't place that call.")
            } else {
                // Brief delay to show button fade to black, then show end call button
                // 1 second gives visual feedback without needing to read button text
                // (status box at top already says "Calling [name]")
                delay(1000)
                _showEndCallButton.value = true
            }
        }
    }
    
    /**
     * End call button tapped - requires double tap for protection
     * 
     * First tap: Shows "Tap again to end call"
     * Second tap: Actually ends the call
     * Resets after 3 seconds if not completed
     */
    fun onEndCallTap() {
        endCallResetJob?.cancel()
        _endCallTapCount.value += 1
        
        if (_endCallTapCount.value >= 2) {
            // Second tap - end the call
            _endCallTapCount.value = 0
            callManager.endCall()
            tts.speakNow("Ending call")
        } else {
            // First tap - show confirmation message
            tts.speak("Tap again to end call")
            
            // Reset after 3 seconds if they don't tap again
            endCallResetJob = viewModelScope.launch {
                delay(3000)
                _endCallTapCount.value = 0
            }
        }
    }
    
    /**
     * Hidden carer access button tapped
     */
    fun onCarerButtonTap() {
        _carerTapCount.value += 1
        
        // Check if we've reached the threshold
        viewModelScope.launch {
            val settings = settingsRepository.getSettings().first()
            if (_carerTapCount.value >= settings.settingsAccessTapCount) {
                _showCarerAccess.value = true
                _carerTapCount.value = 0
            }
        }
        
        // Reset counter after 3 seconds of no taps
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
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
     * Emergency button tapped
     * 
     * TODO: Restore emergency call functionality before production!
     * For development: goes directly to carer settings
     */
    fun onEmergencyButtonTap() {
        // DEV MODE: Go straight to settings
        _showCarerAccess.value = true
        
        /* PRODUCTION: Uncomment this block and remove above
        _emergencyTapCount.value += 1
        
        viewModelScope.launch {
            val settings = settingsRepository.getSettings().first()
            if (_emergencyTapCount.value >= settings.emergencyTapCount) {
                // Place emergency call
                tts.speakNow(TTSScripts.emergencyCallStarting())
                callManager.placeCall(settings.emergencyNumber)
                _emergencyTapCount.value = 0
            }
        }
        
        // Reset after 2 seconds
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _emergencyTapCount.value = 0
        }
        */
    }
    
    // Keep for production use
    private val _emergencyTapCount = MutableStateFlow(0)
}

