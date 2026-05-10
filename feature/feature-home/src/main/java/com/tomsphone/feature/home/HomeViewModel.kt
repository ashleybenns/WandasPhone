package com.tomsphone.feature.home

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.config.CarerSettings
import com.tomsphone.core.config.FeatureLevel
import com.tomsphone.core.config.HomeButtonConfig
import com.tomsphone.core.config.HomeSlotAssignments
import com.tomsphone.core.config.ListTextAlignment
import com.tomsphone.core.config.SettingsRepository
import com.tomsphone.core.config.withSlotsSynced
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.model.ContactType
import com.tomsphone.core.data.model.sortedCarerCallableForHome
import com.tomsphone.core.data.repository.CallLogRepository
import com.tomsphone.core.data.repository.ContactRepository
import android.content.Context
import com.tomsphone.core.telecom.BatteryAlertSmsSender
import com.tomsphone.core.telecom.CallManager
import com.tomsphone.core.telecom.CallState
import com.tomsphone.core.telecom.EmergencyNumberResolver
import com.tomsphone.core.telecom.MissedCallNagManager
import com.tomsphone.core.tts.TTSScripts
import com.tomsphone.core.tts.WandasTTS
import com.tomsphone.core.analytics.AnalyticsManager
import com.tomsphone.core.analytics.AnalyticsEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private fun CarerSettings.hasMissedCallReturnForStatus(): Boolean =
    if (HomeSlotAssignments.isEffectiveSlotMode(homeSlotAssignments)) {
        homeSlotAssignments.contains(HomeSlotAssignments.MISSED_CALL_RETURN)
    } else {
        homeShowMissedCallReturnButton
    }

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
    @ApplicationContext private val appContext: Context,
    private val settingsRepository: SettingsRepository,
    private val contactRepository: ContactRepository,
    private val callLogRepository: CallLogRepository,
    private val callManager: CallManager,
    private val missedCallNagManager: MissedCallNagManager,
    private val batteryAlertSmsSender: BatteryAlertSmsSender,
    private val tts: WandasTTS,
    private val analytics: AnalyticsManager
) : ViewModel() {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    
    companion object {
        private const val TAG = "HomeViewModel"
        private const val MISSED_CALLS_COUNT_QUERY_LIMIT = 500

        /** Home menu button label for [HomeButtonConfig.MenuButton.ID_MISSED_CALLS]. */
        fun missedCallsMenuButtonLabel(missedCallListCount: Int): String = when {
            missedCallListCount <= 0 -> "No Missed Calls"
            missedCallListCount == 1 -> "1 Missed Call"
            else -> "$missedCallListCount Missed Calls"
        }
    }

    /** Count of **unique callers** with outstanding missed/declined (drives menu button text). */
    private val missedCallsListCount: Flow<Int> = callLogRepository
        .getOutstandingMissedCallsPerCaller(MISSED_CALLS_COUNT_QUERY_LIMIT)
        .map { it.size }
    
    // User name
    val userName: StateFlow<String> = settingsRepository.getUserName()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Tom"
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
    
    /**
     * All contacts (up to 200) for resolving home slot assignments.
     * Slots may reference any contact; type alone does not define who has a home button.
     */
    val contacts: StateFlow<List<Contact>> = contactRepository.getContacts(200)
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
    
    // Current time (updates every 30 seconds for status display)
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val currentTime: StateFlow<String> = flow {
        while (true) {
            emit(timeFormatter.format(Date()))
            delay(30_000) // Update every 30 seconds
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = timeFormatter.format(Date())
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
     * One-tap return target: same source as the missed-calls menu count (unread missed + declined,
     * one row per caller, newest first). Includes assistants — aligns with nag and button label.
     */
    data class PrimaryMissedReturnCall(
        val callerName: String,
        val phoneNumber: String,
        val timestamp: Long
    )

    private val primaryMissedReturnCall: StateFlow<PrimaryMissedReturnCall?> =
        callLogRepository.getOutstandingMissedCallsPerCaller(MISSED_CALLS_COUNT_QUERY_LIMIT)
            .map { outstanding ->
                outstanding.firstOrNull()?.let { e ->
                    PrimaryMissedReturnCall(
                        callerName = e.contactName ?: e.phoneNumber,
                        phoneNumber = e.phoneNumber,
                        timestamp = e.timestamp
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
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
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val homeButtons: StateFlow<List<HomeButtonConfig>> =
        combine(
            contacts,
            settings,
            primaryMissedReturnCall,
            missedCallsListCount
        ) { contactList, carerSettings, primaryMissed, missedCount ->
            HomeButtonsInput(contactList, carerSettings, primaryMissed, missedCount)
        }
            .transformLatest { input ->
                val ensured = ensureHomeSlotsReady(input.contacts, input.settings)
                if (ensured != input.settings) {
                    // Persist migration so the rest of the app has a single layout model.
                    settingsRepository.updateSettings(ensured)
                }
                emit(
                    buildHomeButtons(
                        input.contacts,
                        ensured,
                        input.primaryMissed,
                        input.missedCount
                    )
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private data class HomeButtonsInput(
        val contacts: List<Contact>,
        val settings: CarerSettings,
        val primaryMissed: PrimaryMissedReturnCall?,
        val missedCount: Int
    )
    
    /**
     * Build the list of buttons for the home screen.
     * If [CarerSettings.homeSlotAssignments] is effective slot mode (7 entries, at least one filled),
     * order and content come from that list. Otherwise uses legacy toggles and contact order.
     */
    private fun buildHomeButtons(
        contacts: List<Contact>,
        settings: CarerSettings,
        primaryMissedReturn: PrimaryMissedReturnCall?,
        missedCallListCount: Int
    ): List<HomeButtonConfig> {
        val emergencyDigits =
            EmergencyNumberResolver.resolve(appContext, settings.emergencyNumber).dialDigits
        val slots = settings.homeSlotAssignments
        // Single layout model: always build from slots. If slots aren't a valid 7-entry list yet,
        // migrate them first (see ensureHomeSlotsReady()).
        return try {
            buildHomeButtonsFromSlots(
                contacts,
                settings,
                primaryMissedReturn,
                slots,
                missedCallListCount,
                emergencyDigits
            )
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "Error building buttons from slots", e)
            emptyList()
        }
    }

    /**
     * Ensure we always have a 7-entry slot list (slot mode), migrating from legacy toggles if needed.
     * Called from [homeButtons] pipeline so runtime layout never uses legacy calculations.
     */
    private suspend fun ensureHomeSlotsReady(contacts: List<Contact>, settings: CarerSettings): CarerSettings {
        val slots = settings.homeSlotAssignments
        return when {
            !HomeSlotAssignments.isValidSlotList(slots) -> settings.withSlotsSynced(buildLegacySlotAssignments(contacts, settings))
            // Treat "seven empty strings" as unmigrated legacy state; generate a sane default layout.
            slots.all { it.isEmpty() } -> settings.withSlotsSynced(buildLegacySlotAssignments(contacts, settings))
            else -> settings
        }
    }

    private fun buildHomeButtonsFromSlots(
        contacts: List<Contact>,
        settings: CarerSettings,
        primaryMissedReturn: PrimaryMissedReturnCall?,
        slotAssignments: List<String>,
        missedCallListCount: Int,
        emergencyDigits: String
    ): List<HomeButtonConfig> {
        val buttons = mutableListOf<HomeButtonConfig>()
        val contactById = contacts.associateBy { it.id }
        // Ensure we only process exactly SLOT_COUNT items
        val safeSlots = slotAssignments.take(HomeSlotAssignments.SLOT_COUNT)
        for (value in safeSlots) {
            when {
                value.isEmpty() -> { /* empty slot */ }
                HomeSlotAssignments.isContact(value) -> {
                    val id = HomeSlotAssignments.parseContactId(value) ?: continue
                    val contact = contactById[id] ?: continue
                    // Any contact may occupy a slot (elevated answer-only or assistant)
                    buttons.add(
                        HomeButtonConfig.ContactButton(
                            contactId = contact.id,
                            name = contact.name,
                            phoneNumber = contact.phoneNumber,
                            color = contact.buttonColor,
                            showAutoAnswerWarning = settings.autoAnswerEnabled && contact.autoAnswerEnabled,
                            isHalfWidth = contact.isHalfWidth
                        )
                    )
                }
                value == HomeSlotAssignments.MISSED_CALL_RETURN -> {
                    buttons.add(
                        HomeButtonConfig.MissedCallReturnButton(
                            callerName = primaryMissedReturn?.callerName,
                            phoneNumber = primaryMissedReturn?.phoneNumber
                        )
                    )
                }
                value == HomeSlotAssignments.MISSED_CALLS_LIST -> {
                    val label = missedCallsMenuButtonLabel(missedCallListCount)
                    buttons.add(
                        HomeButtonConfig.MenuButton(
                            id = HomeButtonConfig.MenuButton.ID_MISSED_CALLS,
                            label = label,
                            color = settings.homeMissedCallsButtonColor,
                            isHalfWidth = false
                        )
                    )
                }
                value == HomeSlotAssignments.OTHER_CONTACTS -> {
                    buttons.add(
                        HomeButtonConfig.MenuButton(
                            id = HomeButtonConfig.MenuButton.ID_CONTACTS_LIST,
                            label = "Contacts",
                            color = settings.homeContactsListButtonColor,
                            isHalfWidth = false
                        )
                    )
                }
                value == HomeSlotAssignments.DIALER -> {
                    buttons.add(
                        HomeButtonConfig.MenuButton(
                            id = HomeButtonConfig.MenuButton.ID_DIALER,
                            label = "Dial",
                            color = settings.homeDialerButtonColor,
                            isHalfWidth = false
                        )
                    )
                }
                value == HomeSlotAssignments.SCREEN_OFF -> {
                    buttons.add(HomeButtonConfig.DisplayOffButton())
                }
            }
        }
        if (settings.homeShowEmergencyButton) {
            buttons.add(
                HomeButtonConfig.EmergencyButton(dialDigitsDisplay = emergencyDigits)
            )
        }
        return buttons
    }

    private fun buildHomeButtonsLegacy(
        contacts: List<Contact>,
        settings: CarerSettings,
        primaryMissedReturn: PrimaryMissedReturnCall?,
        missedCallListCount: Int,
        emergencyDigits: String
    ): List<HomeButtonConfig> {
        val buttons = mutableListOf<HomeButtonConfig>()
        val missedCallReturnEnabled = settings.homeShowMissedCallReturnButton
        val slotsForOthers = (if (missedCallReturnEnabled) 1 else 0) +
            (if (settings.homeShowMissedCallsButton) 1 else 0) +
            (if (settings.homeShowContactsListButton) 1 else 0) +
            (if (settings.homeShowDialerButton) 1 else 0) +
            (if (settings.showDisplayOffButton) 1 else 0)
        val maxContactSlots = (6 - slotsForOthers).coerceAtLeast(0)
        val callableContacts = contacts.sortedCarerCallableForHome().take(maxContactSlots)

        callableContacts.forEach { contact ->
            buttons.add(
                HomeButtonConfig.ContactButton(
                    contactId = contact.id,
                    name = contact.name,
                    phoneNumber = contact.phoneNumber,
                    color = contact.buttonColor,
                    showAutoAnswerWarning = settings.autoAnswerEnabled && contact.autoAnswerEnabled,
                    isHalfWidth = contact.isHalfWidth
                )
            )
        }
        if (missedCallReturnEnabled) {
            buttons.add(
                HomeButtonConfig.MissedCallReturnButton(
                    callerName = primaryMissedReturn?.callerName,
                    phoneNumber = primaryMissedReturn?.phoneNumber
                )
            )
        }
        if (settings.homeShowMissedCallsButton) {
            val missedCallsLabel = missedCallsMenuButtonLabel(missedCallListCount)
            buttons.add(
                HomeButtonConfig.MenuButton(
                    id = HomeButtonConfig.MenuButton.ID_MISSED_CALLS,
                    label = missedCallsLabel,
                    color = settings.homeMissedCallsButtonColor,
                    isHalfWidth = false
                )
            )
        }
        if (settings.homeShowContactsListButton) {
            buttons.add(
                HomeButtonConfig.MenuButton(
                    id = HomeButtonConfig.MenuButton.ID_CONTACTS_LIST,
                    label = "Contacts",
                    color = settings.homeContactsListButtonColor,
                    isHalfWidth = false
                )
            )
        }
        if (settings.homeShowDialerButton) {
            buttons.add(
                HomeButtonConfig.MenuButton(
                    id = HomeButtonConfig.MenuButton.ID_DIALER,
                    label = "Dial",
                    color = settings.homeDialerButtonColor,
                    isHalfWidth = false
                )
            )
        }
        if (settings.showDisplayOffButton) {
            buttons.add(HomeButtonConfig.DisplayOffButton())
        }
        if (settings.homeShowEmergencyButton) {
            buttons.add(
                HomeButtonConfig.EmergencyButton(dialDigitsDisplay = emergencyDigits)
            )
        }
        return buttons
    }

    /**
     * Build legacy 7-slot list from current settings and contacts (for migration).
     * Order: contacts by buttonPosition, then mcr, mcl, oc, so; pad to 7 with "".
     */
    private fun buildLegacySlotAssignments(contacts: List<Contact>, settings: CarerSettings): List<String> {
        val list = mutableListOf<String>()
        val callable = contacts.sortedCarerCallableForHome().take(7)
        callable.forEach { list.add(HomeSlotAssignments.contactSlot(it.id)) }
        if (settings.homeShowMissedCallReturnButton) list.add(HomeSlotAssignments.MISSED_CALL_RETURN)
        if (settings.homeShowMissedCallsButton) list.add(HomeSlotAssignments.MISSED_CALLS_LIST)
        if (settings.homeShowContactsListButton) list.add(HomeSlotAssignments.OTHER_CONTACTS)
        if (settings.homeShowDialerButton) list.add(HomeSlotAssignments.DIALER)
        if (settings.showDisplayOffButton) list.add(HomeSlotAssignments.SCREEN_OFF)
        while (list.size < HomeSlotAssignments.SLOT_COUNT) list.add(HomeSlotAssignments.EMPTY)
        return list.take(HomeSlotAssignments.SLOT_COUNT)
    }
    
    // Status messages with priority levels to prevent race conditions
    // Priority: calling > carer_missed_call > grey_list_missed_call > default
    private val _callingStatus = MutableStateFlow<String?>(null)      // Highest priority
    private val _missedCallStatus = MutableStateFlow<String?>(null)   // Carer missed call nag
    private var statusMessageResetJob: Job? = null
    
    // Combine with priority: calling > carer_missed > grey_list_missed > default
    // Optional time prefix when showTimeInStatus is enabled
    // kotlinx.coroutines combine() only has tuple overloads up to 5 flows; 6+ uses vararg → Array<Any?> transform.
    val displayMessage: StateFlow<String> = combine(
        userName,
        _callingStatus,
        _missedCallStatus,
        primaryMissedReturnCall,
        settings,
        currentTime
    ) { emissions ->
        val name = emissions[0] as String
        val callingMsg = emissions[1] as String?
        val carerMissedMsg = emissions[2] as String?
        val primaryMissed = emissions[3] as PrimaryMissedReturnCall?
        val carerSettings = emissions[4] as CarerSettings
        val time = emissions[5] as String
        // Build status message with priority
        val baseMessage = when {
            callingMsg != null -> callingMsg
            carerMissedMsg != null -> carerMissedMsg
            primaryMissed != null && carerSettings.hasMissedCallReturnForStatus() ->
                "Missed call from ${primaryMissed.callerName}"
            else -> "$name's phone"
        }
        
        // Prepend time if enabled (only for default status, not during calls/nags)
        if (carerSettings.showTimeInStatus && callingMsg == null && carerMissedMsg == null) {
            "$time\n$baseMessage"
        } else {
            baseMessage
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Tom's phone"
    )
    
    // Display Off row: from slot when layout migrated, else legacy toggle
    val displayOffButtonEnabled: StateFlow<Boolean> = settings
        .map { s ->
            if (HomeSlotAssignments.isEffectiveSlotMode(s.homeSlotAssignments)) {
                s.homeSlotAssignments.contains(HomeSlotAssignments.SCREEN_OFF)
            } else {
                s.showDisplayOffButton
            }
        }
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

    private val _showDialer = MutableStateFlow(false)
    val showDialer: StateFlow<Boolean> = _showDialer.asStateFlow()
    
    // Emergency settings for the confirm/call screens
    val emergencyNumber: StateFlow<String> = settingsRepository.getSettings()
        .map { it.emergencyNumber }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "112")
    
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
            val contacts = contactRepository.getContacts(100).first()
            
            // Track app launch
            analytics.logEvent(AnalyticsEvent.AppLaunched(
                featureLevel = FeatureLevel.EFFECTIVE_PRODUCT_TIER.level,
                contactCount = contacts.size
            ))
            analytics.setCrashlyticsKey("feature_level", FeatureLevel.EFFECTIVE_PRODUCT_TIER.level)
            analytics.setCrashlyticsKey("contact_count", contacts.size)
            
            if (settings.ttsAnnouncementsEnabled) {
                val name = settingsRepository.getUserName().first()
                tts.speak(TTSScripts.greeting(name))
                analytics.logEvent(AnalyticsEvent.TtsAnnouncement(announcementType = "greeting"))
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
        
        // Track button tap
        analytics.logEvent(AnalyticsEvent.CallInitiated(contactType = "carer"))
        
        // Create a minimal Contact for the calling animation
        // (we only need name and phoneNumber for the call)
        val contact = Contact(
            id = button.contactId,
            name = button.name,
            phoneNumber = button.phoneNumber,
            photoUri = null,
            priority = 0,
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
            HomeButtonConfig.MenuButton.ID_DIALER -> {
                _showDialer.value = true
            }
        }
    }
    
    /**
     * User tapped the Missed Call Return button (Level 1).
     * Returns the top outstanding missed/declined caller (same ordering as the missed-calls count).
     */
    fun onMissedCallReturnButtonTap(button: HomeButtonConfig.MissedCallReturnButton) {
        val currentMissedCall = primaryMissedReturnCall.value
        
        val phoneNumber = currentMissedCall?.phoneNumber
        val callerName = currentMissedCall?.callerName
        
        Log.d(TAG, "onMissedCallReturnButtonTap: button.label=${button.label}, currentMissedCall=$currentMissedCall")
        
        if (phoneNumber == null || callerName == null) {
            // No missed call to return
            Log.d(TAG, "onMissedCallReturnButtonTap: No missed call to return")
            if (ttsAnnouncementsEnabled.value) {
                viewModelScope.launch {
                    tts.speak("No missed calls")
                }
            }
            return
        }
        
        Log.d(TAG, "onMissedCallReturnButtonTap: Calling back $callerName at $phoneNumber")
        
        // Enter calling mode
        _callingContact.value = Contact(
            id = 0,
            name = callerName,
            phoneNumber = phoneNumber,
            photoUri = null,
            priority = 0,
            contactType = com.tomsphone.core.data.model.ContactType.GREY_LIST,
            createdAt = 0,
            updatedAt = 0
        )
        setCallingStatus("Calling $callerName")
        
        // Notify nag manager that a call is starting
        missedCallNagManager.onCallStarted()
        
        viewModelScope.launch {
            // Announce and wait
            if (ttsAnnouncementsEnabled.value) {
                tts.speakAndWait(TTSScripts.calling(callerName))
            } else {
                delay(500)
            }
            
            // Clear unread rows for this number, stop nag if it was this caller (count + menu update via Flow)
            missedCallNagManager.markMissedCallsAsReadAndDismiss(phoneNumber)

            // Place the call
            val result = callManager.placeCall(phoneNumber)
            
            if (result.isFailure) {
                Log.e(TAG, "Failed to place call: ${result.exceptionOrNull()}")
                _callingContact.value = null
                setTemporaryCallingStatus("Couldn't place call")
                missedCallNagManager.onCallEnded()
                if (ttsAnnouncementsEnabled.value) {
                    tts.speakNow("Sorry, I couldn't place that call.")
                }
            }
        }
    }
    
    fun dismissMissedCallsList() {
        _showMissedCallsList.value = false
    }
    
    fun dismissContactsList() {
        _showContactsList.value = false
    }

    fun dismissDialer() {
        _showDialer.value = false
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
     * Emergency button tapped - navigates to emergency screen on single tap.
     * The 3-tap confirmation happens on the emergency screen itself.
     */
    fun onEmergencyButtonTap() {
        Log.d(TAG, "Emergency button tapped - navigating to emergency screen")
        viewModelScope.launch {
            try {
                val carerSettings = settingsRepository.getSettings().first()
                if (!carerSettings.sosSmsNotifyAssistantsEnabled) return@launch
                if (!batteryAlertSmsSender.hasSmsPermission()) {
                    Log.w(TAG, "SOS SMS skipped: SEND_SMS not granted — grant in User Profile → SOS or App info → Permissions")
                    Toast.makeText(
                        appContext,
                        "SOS can’t send texts: allow SMS for this app (carer settings → SOS, or system app permissions).",
                        Toast.LENGTH_LONG,
                    ).show()
                    return@launch
                }
                // CARER and GREY_LIST are the only types; new contacts are GREY_LIST until given a home slot.
                val assistants =
                    contactRepository.getContacts(100).first().filter {
                        it.phoneNumber.isNotBlank()
                    }
                val numbers = assistants.map { it.phoneNumber }
                if (numbers.isEmpty()) {
                    Log.w(TAG, "SOS SMS skipped: no contacts with phone numbers")
                    Toast.makeText(
                        appContext,
                        "SOS has no contacts to text — add contacts in carer settings.",
                        Toast.LENGTH_LONG,
                    ).show()
                    return@launch
                }
                val name = settingsRepository.getUserName().first()
                batteryAlertSmsSender.sendSosPressedAlertToAssistants(
                    recipientNumbers = numbers,
                    userName = name,
                    messageTemplate = carerSettings.sosSmsNotifyAssistantsMessage,
                )
            } catch (e: Exception) {
                Log.e(TAG, "SOS assistant SMS failed", e)
            }
        }
        _showEmergencyConfirm.value = true
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
