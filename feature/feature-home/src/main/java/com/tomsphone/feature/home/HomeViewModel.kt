package com.tomsphone.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.config.CarerSettings
import com.tomsphone.core.config.FeatureLevel
import com.tomsphone.core.config.HomeButtonConfig
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
    
    // Settings for building home buttons
    private val settings: StateFlow<CarerSettings> = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CarerSettings()
        )
    
    // Warning: unknown calls are allowed (after emergency call)
    val unknownCallsAllowed: StateFlow<Boolean> = settings
        .map { !it.rejectUnknownCalls }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    /**
     * Home screen buttons - built from contacts + settings
     * 
     * Runtime model that combines:
     * - Contact data (stored in Room)
     * - CarerSettings (stored in DataStore)
     * 
     * Each underlying setting is discrete for remote sync and paywall gating.
     */
    val homeButtons: StateFlow<List<HomeButtonConfig>> = combine(
        contacts,
        settings
    ) { contactList, carerSettings ->
        buildHomeButtons(contactList, carerSettings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    /**
     * Build the list of buttons for the home screen.
     * 
     * Order:
     * 1. Contact buttons (sorted by buttonPosition)
     * 2. Menu buttons (if Level 2+)
     * 3. Emergency button (if enabled)
     */
    private fun buildHomeButtons(
        contacts: List<Contact>,
        settings: CarerSettings
    ): List<HomeButtonConfig> {
        val buttons = mutableListOf<HomeButtonConfig>()
        
        // 1. Contact buttons - only CARER contacts that can call out
        val callableContacts = contacts
            .filter { it.canCallOut }
            .sortedBy { it.buttonPosition }
            .take(settings.homeMaxButtons)
        
        callableContacts.forEach { contact ->
            buttons.add(
                HomeButtonConfig.ContactButton(
                    contactId = contact.id,
                    name = contact.name,
                    phoneNumber = contact.phoneNumber,
                    color = contact.buttonColor,
                    showAutoAnswerWarning = contact.autoAnswerEnabled,
                    isHalfWidth = contact.isHalfWidth
                )
            )
        }
        
        // 2. Menu buttons (Level 2+)
        if (settings.featureLevel.level >= 2) {
            if (settings.homeShowMissedCallsButton) {
                buttons.add(
                    HomeButtonConfig.MenuButton(
                        id = HomeButtonConfig.MenuButton.ID_MISSED_CALLS,
                        label = "Missed Calls",
                        color = settings.homeMissedCallsButtonColor,
                        isHalfWidth = true  // Menu buttons are typically half-width
                    )
                )
            }
            
            if (settings.homeShowContactsListButton) {
                buttons.add(
                    HomeButtonConfig.MenuButton(
                        id = HomeButtonConfig.MenuButton.ID_CONTACTS_LIST,
                        label = "Contacts",
                        color = settings.homeContactsListButtonColor,
                        isHalfWidth = true
                    )
                )
            }
        }
        
        // 3. Emergency button (always last, if enabled)
        if (settings.homeShowEmergencyButton) {
            buttons.add(HomeButtonConfig.EmergencyButton())
        }
        
        return buttons
    }
    
    // Status messages with priority levels to prevent race conditions
    // Priority: calling > missed_call > default
    private val _callingStatus = MutableStateFlow<String?>(null)      // Highest priority
    private val _missedCallStatus = MutableStateFlow<String?>(null)   // Medium priority
    private var statusMessageResetJob: Job? = null
    
    // Combine with priority: calling > missed_call > default
    val displayMessage: StateFlow<String> = combine(
        userName, 
        _callingStatus, 
        _missedCallStatus
    ) { name, callingMsg, missedMsg ->
        // Priority-based selection
        callingMsg ?: missedMsg ?: "$name's phone"
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
    
    // Emergency button tap counter
    private val _emergencyTapCount = MutableStateFlow(0)
    private val _showEmergencyConfirm = MutableStateFlow(false)
    val showEmergencyConfirm: StateFlow<Boolean> = _showEmergencyConfirm.asStateFlow()
    
    // Emergency settings for the confirm/call screens
    val emergencyNumber: StateFlow<String> = settingsRepository.getSettings()
        .map { it.emergencyNumber }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "999")
    
    val emergencyTestMode: StateFlow<Boolean> = settingsRepository.getSettings()
        .map { it.emergencyTestMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    
    init {
        // Announce greeting on app start
        viewModelScope.launch {
            userName.first().let { name ->
                tts.speak(TTSScripts.greeting(name))
            }
        }
        
        // Monitor missed calls and update status message (medium priority)
        viewModelScope.launch {
            missedCallNagManager.activeMissedCalls.collect { missedCalls ->
                if (missedCalls.isNotEmpty()) {
                    val caller = missedCalls.first().contactName ?: "someone"
                    val user = userName.value
                    // 3 lines with breaks at logical phrase boundaries:
                    // Line 1: "[User]."
                    // Line 2: "You missed a call."
                    // Line 3: "Call [carer] now."
                    _missedCallStatus.value = "$user.\nYou missed a call.\nCall $caller now."
                } else {
                    _missedCallStatus.value = null
                }
            }
        }
        
        // Listen for call state changes (for logging only)
        // NOTE: We don't clear _callingStatus or _callingContact here to avoid race conditions.
        // State is cleared via clearCallingStateIfNoCall() when HomeScreen is visible with no active call.
        viewModelScope.launch {
            callManager.currentCall.collect { callInfo ->
                val state = callInfo?.state ?: CallState.IDLE
                Log.d(TAG, "currentCall: state=$state, callingContact=${_callingContact.value?.name}, callingStatus=${_callingStatus.value}, missedStatus=${_missedCallStatus.value}")
            }
        }
    }
    
    /**
     * Set a temporary calling status message (highest priority)
     */
    private fun setTemporaryCallingStatus(message: String, durationMs: Long = 5000) {
        statusMessageResetJob?.cancel()
        _callingStatus.value = message
        
        statusMessageResetJob = viewModelScope.launch {
            delay(durationMs)
            _callingStatus.value = null
        }
    }
    
    /**
     * Set calling status message that stays until cleared (highest priority)
     */
    private fun setCallingStatus(message: String?) {
        statusMessageResetJob?.cancel()
        _callingStatus.value = message
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
            _callingStatus.value = null
            // Notify nag manager that call ended (if it was in progress)
            missedCallNagManager.onCallEnded()
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
        setCallingStatus("Calling ${contact.name}")
        
        // Notify nag manager that a call is starting - suppresses all nagging
        missedCallNagManager.onCallStarted()
        
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
                setTemporaryCallingStatus("Couldn't place call")
                missedCallNagManager.onCallEnded()  // Allow nag to resume
                tts.speakNow("Sorry, I couldn't place that call.")
            }
            // On success, the call state collector clears _callingContact
            // and MainActivity navigates to EndOutgoingCallScreen
        }
    }
    
    /**
     * User tapped a contact button (from HomeButtonConfig)
     * 
     * Used by the new data-driven HomeScreen.
     */
    fun onContactButtonTap(button: HomeButtonConfig.ContactButton) {
        Log.d(TAG, "onContactButtonTap: ${button.name}")
        
        // Create a minimal Contact for the calling animation
        // (we only need name and phoneNumber for the call)
        val contact = Contact(
            id = button.contactId,
            name = button.name,
            phoneNumber = button.phoneNumber,
            photoUri = null,
            priority = 0,
            isPrimary = false,
            contactType = com.tomsphone.core.data.model.ContactType.CARER,
            createdAt = 0,
            updatedAt = 0,
            buttonColor = button.color,
            autoAnswerEnabled = button.showAutoAnswerWarning,
            buttonPosition = 0,
            isHalfWidth = button.isHalfWidth
        )
        
        onContactTap(contact)
    }
    
    /**
     * User tapped a menu button (Level 2+)
     */
    fun onMenuButtonTap(button: HomeButtonConfig.MenuButton) {
        Log.d(TAG, "onMenuButtonTap: ${button.id}")
        
        // TODO: Navigate to appropriate list screen
        when (button.id) {
            HomeButtonConfig.MenuButton.ID_MISSED_CALLS -> {
                // Navigate to missed calls list
                Log.d(TAG, "TODO: Navigate to missed calls list")
            }
            HomeButtonConfig.MenuButton.ID_CONTACTS_LIST -> {
                // Navigate to contacts list
                Log.d(TAG, "TODO: Navigate to contacts list")
            }
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
     * Emergency button tapped - requires multiple taps to activate
     * After required taps, shows confirm screen
     */
    fun onEmergencyButtonTap() {
        _emergencyTapCount.value += 1
        
        viewModelScope.launch {
            val settings = settingsRepository.getSettings().first()
            val requiredTaps = settings.emergencyTapCount
            
            Log.d(TAG, "Emergency tap ${_emergencyTapCount.value}/$requiredTaps")
            
            if (_emergencyTapCount.value >= requiredTaps) {
                _showEmergencyConfirm.value = true
                _emergencyTapCount.value = 0
            }
        }
        
        // Reset tap count after 3 seconds of no taps
        viewModelScope.launch {
            delay(3000)
            _emergencyTapCount.value = 0
        }
    }
    
    /**
     * Dismiss emergency confirm screen
     */
    fun dismissEmergencyConfirm() {
        _showEmergencyConfirm.value = false
    }
    
    /**
     * Get remaining taps needed for emergency
     */
    fun getEmergencyTapsRemaining(): Int {
        val requiredTaps = 3  // Default, will be overridden by settings
        return (requiredTaps - _emergencyTapCount.value).coerceAtLeast(0)
    }
    
    /**
     * Long press on emergency button - goes to carer settings (temporary dev access)
     */
    fun onEmergencyButtonLongPress() {
        _showCarerAccess.value = true
    }
}
