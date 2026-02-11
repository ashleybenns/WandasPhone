package com.tomsphone.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.config.CarerSettings
import com.tomsphone.core.config.FeatureLevel
import com.tomsphone.core.config.HomeButtonConfig
import com.tomsphone.core.config.ListTextAlignment
import com.tomsphone.core.config.SettingsRepository
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.repository.CallLogRepository
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
    private val callLogRepository: CallLogRepository,
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
    
    // Text alignment for buttons (center vs left)
    val listTextAlignment: StateFlow<ListTextAlignment> = settingsRepository.getSettings()
        .map { it.listTextAlignment }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ListTextAlignment.CENTER
        )
    
    // Button activation mode (ON_RELEASE, ON_PRESS, DOUBLE_TAP)
    val buttonActivation: StateFlow<ButtonActivationPreset> = settingsRepository.getSettings()
        .map { it.buttonActivation }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ButtonActivationPreset.ON_RELEASE
        )
    
    // Debounce duration for accidental touch protection
    val touchDebounceMs: StateFlow<Int> = settingsRepository.getSettings()
        .map { it.touchDebounceMs }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 150
        )
    
    // Accumulated tap threshold (total touch time to activate)
    val accumulatedTapThresholdMs: StateFlow<Int> = settingsRepository.getSettings()
        .map { it.accumulatedTapThresholdMs }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 500
        )
    
    // Accumulated tap timeout (time before counter resets)
    val accumulatedTapTimeoutMs: StateFlow<Int> = settingsRepository.getSettings()
        .map { it.accumulatedTapTimeoutMs }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 3000
        )
    
    // TTS announcements enabled (separate from missed call nag)
    private val ttsAnnouncementsEnabled: StateFlow<Boolean> = settingsRepository.getSettings()
        .map { it.ttsAnnouncementsEnabled }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
    
    // Contacts to display (only CARER contacts that can be called)
    val contacts: StateFlow<List<Contact>> = maxContacts
        .flatMapLatest { max ->
            contactRepository.getCarerContacts(max)
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
    
    // Count of unread missed calls (for button label)
    private val missedCallsCount: StateFlow<Int> = callLogRepository.getMissedCalls(100)
        .map { calls -> 
            // Filter out empty phone numbers and count unique callers
            calls.filter { it.phoneNumber.isNotBlank() }
                .distinctBy { it.phoneNumber }
                .size
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    
    // Display off state - screen should be dimmed
    private val _isDisplayOff = MutableStateFlow(false)
    val isDisplayOff: StateFlow<Boolean> = _isDisplayOff.asStateFlow()
    
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
        settings,
        missedCallsCount
    ) { contactList, carerSettings, missedCount ->
        buildHomeButtons(contactList, carerSettings, missedCount)
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
        settings: CarerSettings,
        missedCallsCount: Int
    ): List<HomeButtonConfig> {
        val buttons = mutableListOf<HomeButtonConfig>()
        
        // 1. Contact buttons - only CARER contacts that can call out
        // Level determines max contacts (list buttons take remaining space)
        val maxByLevel = when (settings.featureLevel) {
            FeatureLevel.MINIMAL -> 4  // L1: 4 carers only
            FeatureLevel.BASIC -> 5    // L2: 5 carers + list buttons + Screen Off
            FeatureLevel.STANDARD -> 5 // L3: Same carers + menu buttons
            FeatureLevel.EXTENDED -> 6 // L4: Full 6 carers + all buttons
        }
        val callableContacts = contacts
            .filter { it.canCallOut }
            .sortedBy { it.buttonPosition }
            .take(maxByLevel)
        
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
        
        // 2. List buttons (Level 2+) - full-width, below carers
        if (settings.featureLevel.level >= 2) {
            if (settings.homeShowMissedCallsButton) {
                // Build label with count: "No Missed Calls", "1 Missed Call", "3 Missed Calls"
                val missedCallsLabel = when (missedCallsCount) {
                    0 -> "No Missed Calls"
                    1 -> "1 Missed Call"
                    else -> "$missedCallsCount Missed Calls"
                }
                buttons.add(
                    HomeButtonConfig.MenuButton(
                        id = HomeButtonConfig.MenuButton.ID_MISSED_CALLS,
                        label = missedCallsLabel,
                        color = settings.homeMissedCallsButtonColor,
                        isHalfWidth = false  // List buttons are full-width
                    )
                )
            }
            
            if (settings.homeShowContactsListButton) {
                buttons.add(
                    HomeButtonConfig.MenuButton(
                        id = HomeButtonConfig.MenuButton.ID_CONTACTS_LIST,
                        label = "Other Contacts",
                        color = settings.homeContactsListButtonColor,
                        isHalfWidth = false  // List buttons are full-width
                    )
                )
            }
            
            // Display Off button (Level 2+, if enabled)
            if (settings.showDisplayOffButton) {
                buttons.add(HomeButtonConfig.DisplayOffButton())
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
    
    // Display Off button enabled (Level 2+, setting on) - controls space reservation
    // Button always takes space when enabled, so other buttons don't move
    val displayOffButtonEnabled: StateFlow<Boolean> = settings
        .map { it.featureLevel.level >= 2 && it.showDisplayOffButton }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    // Display Off button active (not during missed call nag) - controls visibility/clickability
    // Note: Also hidden during calling animation via callingContact != null check in UI
    val displayOffButtonActive: StateFlow<Boolean> = _missedCallStatus
        .map { missedNag -> missedNag == null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
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
    val emergencyTapCount: StateFlow<Int> = _emergencyTapCount.asStateFlow()
    private val _showEmergencyConfirm = MutableStateFlow(false)
    val showEmergencyConfirm: StateFlow<Boolean> = _showEmergencyConfirm.asStateFlow()
    
    // List screen navigation (Level 2+)
    private val _showMissedCallsList = MutableStateFlow(false)
    val showMissedCallsList: StateFlow<Boolean> = _showMissedCallsList.asStateFlow()
    
    private val _showContactsList = MutableStateFlow(false)
    val showContactsList: StateFlow<Boolean> = _showContactsList.asStateFlow()
    
    // Emergency settings for the confirm/call screens
    val emergencyNumber: StateFlow<String> = settingsRepository.getSettings()
        .map { it.emergencyNumber }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "999")
    
    val emergencyRequiredTaps: StateFlow<Int> = settingsRepository.getSettings()
        .map { it.emergencyTapCount }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)
    
    val emergencyTestMode: StateFlow<Boolean> = settingsRepository.getSettings()
        .map { it.emergencyTestMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    
    init {
        // Announce greeting on app start (if TTS enabled)
        // Wait for actual settings to load, not just the initial StateFlow value
        viewModelScope.launch {
            // Get settings directly from repository to ensure we have the real values
            val settings = settingsRepository.getSettings().first()
            if (settings.ttsAnnouncementsEnabled) {
                val name = settingsRepository.getUserName().first()
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
            // Announce "Calling [name]" and wait for it to complete before placing call
            // This prevents the call audio from interrupting the announcement
            if (ttsAnnouncementsEnabled.value) {
                tts.speakAndWait(TTSScripts.calling(contact.name))
            } else {
                // If TTS disabled, still show animation briefly
                delay(500)
            }
            
            Log.d(TAG, "Announcement complete, placing call")
            
            // Place the call - MainActivity will handle navigation
            val result = callManager.placeCall(contact.phoneNumber)
            
            if (result.isFailure) {
                Log.e(TAG, "Failed to place call: ${result.exceptionOrNull()}")
                _callingContact.value = null
                setTemporaryCallingStatus("Couldn't place call")
                missedCallNagManager.onCallEnded()  // Allow nag to resume
                if (ttsAnnouncementsEnabled.value) {
                    tts.speakNow("Sorry, I couldn't place that call.")
                }
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
        
        when (button.id) {
            HomeButtonConfig.MenuButton.ID_MISSED_CALLS -> {
                _showMissedCallsList.value = true
            }
            HomeButtonConfig.MenuButton.ID_CONTACTS_LIST -> {
                _showContactsList.value = true
            }
        }
    }
    
    fun dismissMissedCallsList() {
        _showMissedCallsList.value = false
    }
    
    fun dismissContactsList() {
        _showContactsList.value = false
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
     * Display Off button tapped - dim the screen
     * Any touch will wake the screen again
     */
    fun onDisplayOffTap() {
        Log.d(TAG, "Display Off tapped")
        _isDisplayOff.value = true
    }
    
    /**
     * Wake up the display (called on any screen touch when display is off)
     */
    fun wakeDisplay() {
        if (_isDisplayOff.value) {
            Log.d(TAG, "Display waking up")
            _isDisplayOff.value = false
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
    
    /**
     * Settings access button activated (7-10 taps)
     * The tap counting is handled in the button component itself
     */
    fun onSettingsAccessTap() {
        Log.d(TAG, "Settings access button activated")
        _showCarerAccess.value = true
    }
}
