package com.tomsphone.feature.carer

import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.config.CarerSettings
import com.tomsphone.core.config.FeatureLevel
import com.tomsphone.core.config.HomeSlotAssignments
import com.tomsphone.core.config.SettingsRepository
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.model.ContactType
import com.tomsphone.core.data.repository.ContactRepository
import com.tomsphone.core.config.ThemeOption
import com.tomsphone.core.telecom.BatteryAlertSmsSender
import com.tomsphone.core.analytics.AnalyticsManager
import com.tomsphone.core.analytics.AnalyticsEvent
import com.tomsphone.core.analytics.RemoteConfigManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

/**
 * When true, carer settings are shown without requiring PIN entry.
 * Set to false to re-enable PIN protection.
 */
private const val BYPASS_PIN = true

/**
 * ViewModel for carer configuration
 * 
 * Allows carers to:
 * - Manage contacts
 * - Change feature level
 * - Adjust settings
 * - Select theme
 * - Configure auto-answer
 * - Factory reset (wipe all data)
 */
@HiltViewModel
class CarerSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val contactRepository: ContactRepository,
    private val batteryAlertSmsSender: BatteryAlertSmsSender,
    private val analytics: AnalyticsManager,
    private val remoteConfig: RemoteConfigManager
) : ViewModel() {
    
    // Current settings
    val settings: StateFlow<CarerSettings> = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CarerSettings()
        )
    
    // All contacts
    val contacts: StateFlow<List<Contact>> = contactRepository.getContacts(100)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Home screen slot assignments (size 7). Empty list = use legacy toggles until migration. */
    val homeSlotAssignments: StateFlow<List<String>> = settings
        .map { it.homeSlotAssignments }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // PIN verification state
    private val _isPinVerified = MutableStateFlow(false)
    val isPinVerified: StateFlow<Boolean> = if (BYPASS_PIN) {
        MutableStateFlow(true).asStateFlow()
    } else {
        _isPinVerified.asStateFlow()
    }
    
    // UI state
    private val _showPinDialog = MutableStateFlow(true)
    val showPinDialog: StateFlow<Boolean> = if (BYPASS_PIN) {
        MutableStateFlow(false).asStateFlow()
    } else {
        _showPinDialog.asStateFlow()
    }
    
    /**
     * Get onboarding tip for a setting.
     * Returns null if no tip available.
     */
    fun getOnboardingTip(settingId: String): String? {
        return remoteConfig.getOnboardingTip(settingId)
    }
    
    /**
     * Get setting description from Remote Config.
     */
    fun getSettingDescription(settingId: String): String? {
        return remoteConfig.getSettingDescription(settingId)
    }
    
    /**
     * Track when a tip is viewed.
     */
    fun onTipViewed(tipId: String) {
        analytics.logEvent(AnalyticsEvent.OnboardingTipViewed(tipId = tipId))
    }
    
    /**
     * Verify PIN entry
     */
    fun verifyPin(pin: String) {
        viewModelScope.launch {
            val hashedPin = hashPin(pin)
            val currentSettings = settings.first()
            
            // If no PIN set yet, accept any 4-digit PIN and save it
            if (currentSettings.carerPin.isEmpty()) {
                if (pin.length == 4) {
                    settingsRepository.setPin(hashedPin)
                    _isPinVerified.value = true
                    _showPinDialog.value = false
                    // Track carer settings opened
                    analytics.logEvent(AnalyticsEvent.CarerSettingsOpened)
                }
            } else {
                // Verify against stored PIN
                if (settingsRepository.verifyPin(hashedPin)) {
                    _isPinVerified.value = true
                    _showPinDialog.value = false
                    // Track carer settings opened
                    analytics.logEvent(AnalyticsEvent.CarerSettingsOpened)
                }
            }
        }
    }
    
    /**
     * Update feature level
     */
    fun setFeatureLevel(level: FeatureLevel) {
        viewModelScope.launch {
            val currentLevel = settings.first().featureLevel
            settingsRepository.setFeatureLevel(level)
            // Track feature level change
            analytics.logEvent(AnalyticsEvent.FeatureLevelChanged(
                fromLevel = currentLevel.level,
                toLevel = level.level
            ))
        }
    }
    
    /**
     * Update theme
     */
    fun setTheme(theme: ThemeOption) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(
                current.copy(ui = current.ui.copy(theme = theme))
            )
        }
    }
    
    /**
     * Update user text size (for user-facing screens only)
     */
    fun setUserTextSize(textSize: com.tomsphone.core.config.UserTextSize) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(
                current.copy(ui = current.ui.copy(userTextSize = textSize))
            )
        }
    }
    
    fun setShowDisplayOffButton(enabled: Boolean) {
        viewModelScope.launch {
            // Read fresh from repository to avoid race conditions with other updates
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(showDisplayOffButton = enabled))
        }
    }
    
    fun setShowMissedCallsButton(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(homeShowMissedCallsButton = enabled))
        }
    }
    
    fun setShowMissedCallReturnButton(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(homeShowMissedCallReturnButton = enabled))
        }
    }
    
    fun setShowContactsListButton(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(homeShowContactsListButton = enabled))
        }
    }
    
    fun setContactsListShowGreyListOnly(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(homeContactsListShowGreyListOnly = enabled))
        }
    }
    
    fun setListTextAlignment(alignment: com.tomsphone.core.config.ListTextAlignment) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(listTextAlignment = alignment))
        }
    }
    
    fun setShowTimeInStatus(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(showTimeInStatus = enabled))
        }
    }
    
    fun setButtonActivation(preset: com.tomsphone.core.config.ButtonActivationPreset) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(buttonActivation = preset))
        }
    }
    
    fun setTouchDebounceMs(debounceMs: Int) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(touchDebounceMs = debounceMs))
        }
    }
    
    fun setAccumulatedTapThresholdMs(thresholdMs: Int) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(accumulatedTapThresholdMs = thresholdMs))
        }
    }
    
    fun setAccumulatedTapTimeoutMs(timeoutMs: Int) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(accumulatedTapTimeoutMs = timeoutMs))
        }
    }
    
    /**
     * Update user name
     */
    fun setUserName(name: String) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(userName = name))
        }
    }
    
    /**
     * Toggle auto-answer
     */
    fun setAutoAnswer(enabled: Boolean, delaySeconds: Int = 3) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(
                current.copy(
                    autoAnswerEnabled = enabled,
                    autoAnswerDelaySeconds = delaySeconds
                )
            )
        }
    }
    
    /**
     * Add or update contact
     * For new contacts, assigns next available buttonPosition within their type
     */
    fun saveContact(contact: Contact) {
        viewModelScope.launch {
            val contactType = if (contact.contactType == ContactType.CARER) "carer" else "grey_list"
            if (contact.id == 0L) {
                // For new contacts, assign next buttonPosition within their contact type
                val contactsOfSameType = contacts.first().filter { it.contactType == contact.contactType }
                val maxPosition = contactsOfSameType.maxOfOrNull { it.buttonPosition } ?: -1
                val contactToSave = contact.copy(buttonPosition = maxPosition + 1)
                contactRepository.addContact(contactToSave)
                // Track contact added
                analytics.logEvent(AnalyticsEvent.ContactAdded(contactType = contactType))
            } else {
                contactRepository.updateContact(contact)
                // Track contact edited
                analytics.logEvent(AnalyticsEvent.ContactEdited(contactType = contactType))
            }
        }
    }
    
    /**
     * Delete contact
     */
    fun deleteContact(id: Long) {
        viewModelScope.launch {
            // Get the contact to determine its type for analytics
            val contact = contacts.first().find { it.id == id }
            val contactType = if (contact?.contactType == ContactType.CARER) "carer" else "grey_list"
            
            contactRepository.removeContact(id)
            
            // Track contact deleted
            analytics.logEvent(AnalyticsEvent.ContactDeleted(contactType = contactType))
        }
    }
    
    /**
     * Move a contact up in the list (decrease buttonPosition)
     */
    fun moveContactUp(contact: Contact, allCarers: List<Contact>) {
        val sortedCarers = allCarers.sortedBy { it.buttonPosition }
        val currentIndex = sortedCarers.indexOfFirst { it.id == contact.id }
        if (currentIndex <= 0) return // Already at top
        
        viewModelScope.launch {
            // Swap positions using index values (guarantees unique positions)
            val aboveContact = sortedCarers[currentIndex - 1]
            contactRepository.updateButtonPositions(
                listOf(
                    contact.id to (currentIndex - 1),
                    aboveContact.id to currentIndex
                )
            )
        }
    }
    
    /**
     * Move a contact down in the list (increase buttonPosition)
     */
    fun moveContactDown(contact: Contact, allCarers: List<Contact>) {
        val sortedCarers = allCarers.sortedBy { it.buttonPosition }
        val currentIndex = sortedCarers.indexOfFirst { it.id == contact.id }
        if (currentIndex < 0 || currentIndex >= sortedCarers.size - 1) return // Already at bottom
        
        viewModelScope.launch {
            // Swap positions using index values (guarantees unique positions)
            val belowContact = sortedCarers[currentIndex + 1]
            contactRepository.updateButtonPositions(
                listOf(
                    contact.id to (currentIndex + 1),
                    belowContact.id to currentIndex
                )
            )
        }
    }

    // ========== HOME SCREEN LAYOUT (7 assignable slots + Emergency) ==========

    /**
     * Build legacy 7-slot list from current settings and carer contacts (for migration).
     * Order: carer contacts by buttonPosition, then mcr, mcl, oc, so; pad to 7.
     */
    private fun buildLegacySlotAssignments(contactsList: List<Contact>, current: CarerSettings): List<String> {
        val list = mutableListOf<String>()
        val carerCallable = contactsList
            .filter { it.contactType == ContactType.CARER && it.canCallOut }
            .sortedBy { it.buttonPosition }
            .take(HomeSlotAssignments.SLOT_COUNT)
        carerCallable.forEach { list.add(HomeSlotAssignments.contactSlot(it.id)) }
        if (current.homeShowMissedCallReturnButton) list.add(HomeSlotAssignments.MISSED_CALL_RETURN)
        if (current.homeShowMissedCallsButton) list.add(HomeSlotAssignments.MISSED_CALLS_LIST)
        if (current.homeShowContactsListButton) list.add(HomeSlotAssignments.OTHER_CONTACTS)
        if (current.showDisplayOffButton) list.add(HomeSlotAssignments.SCREEN_OFF)
        while (list.size < HomeSlotAssignments.SLOT_COUNT) list.add(HomeSlotAssignments.EMPTY)
        return list.take(HomeSlotAssignments.SLOT_COUNT)
    }

    /**
     * Run migration when opening Home Screen Layout: if homeSlotAssignments is empty or size != 7,
     * build from current contacts + toggles and save.
     */
    fun ensureMigrationOnLayoutOpen() {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            val list = current.homeSlotAssignments
            if (list.size != HomeSlotAssignments.SLOT_COUNT) {
                val contactsList = contacts.first()
                val migrated = buildLegacySlotAssignments(contactsList, current)
                settingsRepository.updateSettings(current.withSlotsSynced(migrated))
            }
        }
    }

    fun setHomeSlotAt(index: Int, value: String) {
        if (index !in 0 until HomeSlotAssignments.SLOT_COUNT) return
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            val list = current.homeSlotAssignments.toMutableList()
            // Ensure list is exactly SLOT_COUNT items
            while (list.size < HomeSlotAssignments.SLOT_COUNT) list.add(HomeSlotAssignments.EMPTY)
            // Trim to exactly SLOT_COUNT if it's larger
            val trimmedList = list.take(HomeSlotAssignments.SLOT_COUNT).toMutableList()
            trimmedList[index] = value
            settingsRepository.updateSettings(current.withSlotsSynced(trimmedList))
        }
    }

    fun moveHomeSlotUp(index: Int) {
        if (index <= 0 || index >= HomeSlotAssignments.SLOT_COUNT) return
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            val list = current.homeSlotAssignments.toMutableList()
            // Ensure list is exactly SLOT_COUNT items
            while (list.size < HomeSlotAssignments.SLOT_COUNT) list.add(HomeSlotAssignments.EMPTY)
            val trimmedList = list.take(HomeSlotAssignments.SLOT_COUNT).toMutableList()
            trimmedList[index] = trimmedList[index - 1].also { trimmedList[index - 1] = trimmedList[index] }
            settingsRepository.updateSettings(current.withSlotsSynced(trimmedList))
        }
    }

    fun moveHomeSlotDown(index: Int) {
        if (index < 0 || index >= HomeSlotAssignments.SLOT_COUNT - 1) return
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            val list = current.homeSlotAssignments.toMutableList()
            // Ensure list is exactly SLOT_COUNT items
            while (list.size < HomeSlotAssignments.SLOT_COUNT) list.add(HomeSlotAssignments.EMPTY)
            val trimmedList = list.take(HomeSlotAssignments.SLOT_COUNT).toMutableList()
            trimmedList[index] = trimmedList[index + 1].also { trimmedList[index + 1] = trimmedList[index] }
            settingsRepository.updateSettings(current.withSlotsSynced(trimmedList))
        }
    }
    
    // ========== ALWAYS ON MODE SETTINGS ==========
    
    /**
     * Toggle pinned mode (app stays in foreground)
     */
    fun setPinnedMode(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(pinnedModeEnabled = enabled))
        }
    }
    
    /**
     * Toggle screen always on
     */
    fun setScreenAlwaysOn(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(screenAlwaysOn = enabled))
        }
    }
    
    /**
     * Toggle volume button lock
     */
    fun setLockVolumeButtons(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(lockVolumeButtons = enabled))
        }
    }
    
    // ========== CALL HANDLING SETTINGS ==========
    
    /**
     * Toggle reject unknown calls
     */
    fun setRejectUnknownCalls(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(rejectUnknownCalls = enabled))
        }
    }

    /**
     * Toggle battery alert SMS (low battery and device connected after low battery).
     * Carers must have "Notify for battery alerts" enabled on their contact.
     */
    fun setBatteryAlertSmsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(batteryAlertSmsEnabled = enabled))
        }
    }

    /** On-screen status for battery alert card: (SMS permission granted, number of recipients with valid number). */
    private val _batteryAlertStatus = MutableStateFlow(Pair(false, 0))
    val batteryAlertStatus: StateFlow<Pair<Boolean, Int>> = _batteryAlertStatus.asStateFlow()

    /** Refresh permission and recipient count for the battery alert card. Call when the card is shown. */
    fun refreshBatteryAlertStatus() {
        viewModelScope.launch {
            val hasPermission = batteryAlertSmsSender.hasSmsPermission()
            val recipients = contactRepository.getCarerContactsWithBatteryAlerts()
            val count = recipients.count { it.phoneNumber.isNotBlank() }
            _batteryAlertStatus.value = hasPermission to count
        }
    }

    /**
     * Send a test battery alert SMS to all assistants with "Notify for battery alerts" enabled.
     * Returns a result message for the UI (success or failure reason).
     */
    fun sendTestBatteryAlertSms(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings().first()
            val recipients = contactRepository.getCarerContactsWithBatteryAlerts()
            val numbers = recipients.map { it.phoneNumber }.filter { it.isNotBlank() }
            val message = batteryAlertSmsSender.sendTestBatteryAlert(numbers, settings.userName)
            onResult(message)
            refreshBatteryAlertStatus()
        }
    }
    
    /**
     * Set default call volume (0-100 percent).
     * Restored when call ends; used as preset when call starts.
     * Also applies to device immediately so carers can set without leaving the app.
     */
    fun setSpeakerVolume(percent: Int) {
        viewModelScope.launch {
            val pct = percent.coerceIn(0, 100)
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(speakerVolume = pct))
            applyCallVolumeToDevice(pct)
        }
    }

    /**
     * Set ringtone volume (0-100 percent).
     * Applied to device immediately so carers can set without leaving the app.
     */
    fun setRingtoneVolume(percent: Int) {
        viewModelScope.launch {
            val pct = percent.coerceIn(0, 100)
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(ringtoneVolume = pct))
            applyRingtoneVolumeToDevice(pct)
        }
    }

    private fun applyRingtoneVolumeToDevice(volumePercent: Int) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            val targetVolume = (maxVolume * (volumePercent / 100f)).toInt().coerceIn(0, maxVolume)
            audioManager.setStreamVolume(AudioManager.STREAM_RING, targetVolume, 0)
        } catch (_: Exception) { /* ignore */ }
    }

    private fun applyCallVolumeToDevice(volumePercent: Int) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            val targetVolume = (maxVolume * (volumePercent / 100f)).toInt().coerceIn(0, maxVolume)
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, targetVolume, 0)
        } catch (_: Exception) { /* ignore */ }
    }

    /**
     * Apply saved ringtone and call volumes to the device.
     * Call when opening Volume/Call Handling so the device matches saved settings.
     */
    fun syncVolumesToDevice() {
        viewModelScope.launch {
            val s = settings.first()
            applyRingtoneVolumeToDevice(s.ringtoneVolume)
            applyCallVolumeToDevice(s.speakerVolume)
        }
    }

    /**
     * Toggle speakerphone always on
     */
    fun setSpeakerphoneAlwaysOn(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(speakerphoneAlwaysOn = enabled))
        }
    }
    
    fun setShowSpeakerButton(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(showSpeakerButton = enabled))
        }
    }
    
    fun setSpeakerDefaultOn(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(speakerDefaultOn = enabled))
        }
    }
    
    /**
     * Toggle TTS announcements (greeting, calling, speaker, mute, battery)
     * Separate from missed call nag and ringtone
     */
    fun setTtsAnnouncementsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(ttsAnnouncementsEnabled = enabled))
        }
    }
    
    /**
     * Toggle missed call nag enabled
     */
    fun setMissedCallNagEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(missedCallNagEnabled = enabled))
        }
    }
    
    /**
     * Set missed call nag interval
     */
    fun setMissedCallNagInterval(interval: com.tomsphone.core.config.MissedCallNagInterval) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(missedCallNagInterval = interval))
        }
    }
    
    // ========== USER PROFILE SETTINGS ==========
    
    /**
     * Set emergency number
     */
    fun setEmergencyNumber(number: String) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(emergencyNumber = number))
        }
    }
    
    /**
     * Set emergency test mode
     */
    fun setEmergencyTestMode(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(emergencyTestMode = enabled))
        }
    }
    
    /**
     * Set user address for emergency info
     */
    fun setUserAddress(address: String) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(userAddress = address))
        }
    }
    
    /**
     * Set user blood type
     */
    fun setUserBloodType(bloodType: String) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(userBloodType = bloodType))
        }
    }
    
    /**
     * Set user allergies
     */
    fun setUserAllergies(allergies: String) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(userAllergies = allergies))
        }
    }
    
    /**
     * Set user medications
     */
    fun setUserMedications(medications: String) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(userMedications = medications))
        }
    }
    
    /**
     * Set user medical conditions
     */
    fun setUserMedicalConditions(conditions: String) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(userMedicalConditions = conditions))
        }
    }
    
    /**
     * Set user emergency notes
     */
    fun setUserEmergencyNotes(notes: String) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(userEmergencyNotes = notes))
        }
    }
    
    /**
     * Set user surname
     */
    fun setUserSurname(surname: String) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(userSurname = surname))
        }
    }
    
    /**
     * Set emergency contact 1 name
     */
    fun setEmergencyContact1Name(name: String) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(emergencyContact1Name = name))
        }
    }
    
    /**
     * Set emergency contact 1 phone
     */
    fun setEmergencyContact1Phone(phone: String) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(emergencyContact1Phone = phone))
        }
    }
    
    /**
     * Set emergency contact 2 name
     */
    fun setEmergencyContact2Name(name: String) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(emergencyContact2Name = name))
        }
    }
    
    /**
     * Set emergency contact 2 phone
     */
    fun setEmergencyContact2Phone(phone: String) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(emergencyContact2Phone = phone))
        }
    }
    
    /**
     * Set user photo URI (for Emergency ID photo)
     */
    fun setUserPhotoUri(uri: String?) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(userPhotoUri = uri))
        }
    }
    
    /**
     * Hash PIN for secure storage
     */
    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    // ========== FACTORY RESET ==========
    
    /**
     * Factory reset - wipe ALL app data.
     * 
     * This deletes:
     * - All settings (DataStore)
     * - All contacts (Room database)
     * - All call logs (Room database)
     * 
     * After reset, the app will behave as if newly installed.
     * 
     * SECURITY: This is the only way to securely delete all user data
     * before giving the phone to a new user.
     * 
     * @param onComplete Called after local data is deleted. Caller should restart the app.
     */
    fun factoryReset(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                // 1. Clear DataStore (settings)
                settingsRepository.clearAllSettings()
                
                // 2. Delete Room database (contacts, call logs)
                // Database name must match DataModule.kt
                context.deleteDatabase("toms_phone_db_v5")
                
                // 3. TODO: Delete remote data when carer portal is implemented
                // When the cloud carer portal is added, call the server API here:
                // carerPortalApi.deleteUserData(userId)
                // This ensures no copies remain on remote servers.
                
                // 4. Signal completion - caller should restart app
                onComplete()
                
            } catch (e: Exception) {
                android.util.Log.e("CarerSettingsVM", "Factory reset failed: ${e.message}")
                // Still call onComplete to allow app restart attempt
                onComplete()
            }
        }
    }
}

/** Sync legacy toggles and homeContactCount from slot list when saving homeSlotAssignments. */
private fun CarerSettings.withSlotsSynced(slots: List<String>): CarerSettings = copy(
    homeSlotAssignments = slots,
    homeContactCount = slots.count { HomeSlotAssignments.isContact(it) },
    homeShowMissedCallReturnButton = slots.contains(HomeSlotAssignments.MISSED_CALL_RETURN),
    homeShowMissedCallsButton = slots.contains(HomeSlotAssignments.MISSED_CALLS_LIST),
    homeShowContactsListButton = slots.contains(HomeSlotAssignments.OTHER_CONTACTS),
    showDisplayOffButton = slots.contains(HomeSlotAssignments.SCREEN_OFF)
)
