package com.tomsphone.feature.carer

import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.config.CarerSettings
import com.tomsphone.core.config.HomeSlotAssignments
import com.tomsphone.core.config.SettingsRepository
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.model.ContactType
import com.tomsphone.core.data.model.sortedCarerCallableForHome
import com.tomsphone.core.data.repository.CallLogRepository
import com.tomsphone.core.data.repository.ContactRepository
import com.tomsphone.core.config.ThemeOption
import com.tomsphone.core.telecom.DEFAULT_EMERGENCY_FALLBACK
import com.tomsphone.core.telecom.EmergencyNumberResolver
import com.tomsphone.core.telecom.contactIdsWithHomeCallButton
import com.tomsphone.core.analytics.AnalyticsManager
import com.tomsphone.core.analytics.AnalyticsEvent
import com.tomsphone.core.analytics.RemoteConfigManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.tomsphone.feature.carer.transfer.AppDataTransfer
import com.tomsphone.feature.carer.transfer.toNewContact
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import javax.inject.Inject

/**
 * ViewModel for carer configuration
 * 
 * Allows carers to:
 * - Manage contacts
 * - Change feature level
 * - Adjust settings
 * - Select theme
 * - Configure auto-answer
 * - App reset (wipe this app’s data only)
 */
@HiltViewModel
class CarerSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val contactRepository: ContactRepository,
    private val callLogRepository: CallLogRepository,
    private val json: Json,
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
    
    // PIN verification state (each visit to carer mode requires PIN unless already verified this session)
    private val _isPinVerified = MutableStateFlow(false)
    val isPinVerified: StateFlow<Boolean> = _isPinVerified.asStateFlow()

    private val _showPinDialog = MutableStateFlow(true)
    val showPinDialog: StateFlow<Boolean> = _showPinDialog.asStateFlow()

    val hasCarerPin: StateFlow<Boolean> = settingsRepository.hasCarerPin()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
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
            if (pin.length != 4) return@launch

            val pinExists = settingsRepository.hasCarerPin().first()
            if (!pinExists) {
                settingsRepository.setPin(hashedPin)
                _isPinVerified.value = true
                _showPinDialog.value = false
                analytics.logEvent(AnalyticsEvent.CarerSettingsOpened)
                return@launch
            }
            if (settingsRepository.verifyPin(hashedPin)) {
                _isPinVerified.value = true
                _showPinDialog.value = false
                analytics.logEvent(AnalyticsEvent.CarerSettingsOpened)
            }
        }
    }

    /**
     * Set or change the assistant PIN from settings. [currentPin] ignored when no PIN exists yet.
     */
    fun updateCarerPinFromSettings(
        currentPin: String,
        newPin: String,
        confirmPin: String,
        onResult: (ok: Boolean, message: String?) -> Unit,
    ) {
        viewModelScope.launch {
            if (newPin.length != 4 || confirmPin.length != 4) {
                onResult(false, "PIN must be exactly 4 digits")
                return@launch
            }
            if (newPin != confirmPin) {
                onResult(false, "New PIN and confirmation do not match")
                return@launch
            }
            val pinExists = settingsRepository.hasCarerPin().first()
            if (pinExists) {
                if (currentPin.length != 4) {
                    onResult(false, "Enter your current 4-digit PIN")
                    return@launch
                }
                if (!settingsRepository.verifyPin(hashPin(currentPin))) {
                    onResult(false, "Current PIN is incorrect")
                    return@launch
                }
            }
            settingsRepository.setPin(hashPin(newPin))
            val latest = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(latest.copy(assistantPinRequired = true))
            onResult(true, null)
        }
    }

    /**
     * Enter Assistant settings without a PIN when [CarerSettings.assistantPinRequired] is false.
     */
    fun enterCarerIfPinNotRequired() {
        viewModelScope.launch {
            _isPinVerified.value = true
            _showPinDialog.value = false
            analytics.logEvent(AnalyticsEvent.CarerSettingsOpened)
        }
    }

    /**
     * First-time (or any visit with no PIN): decline a PIN and rely on tap sequence only.
     */
    fun skipAssistantPinSetup() {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(assistantPinRequired = false))
            _isPinVerified.value = true
            _showPinDialog.value = false
        }
    }

    /**
     * Toggle whether a PIN is asked when opening Assistant settings.
     * Turning off clears any stored PIN.
     */
    fun setAssistantPinRequired(required: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            if (!required) {
                settingsRepository.updateSettings(
                    current.copy(assistantPinRequired = false, carerPin = "")
                )
            } else {
                settingsRepository.updateSettings(current.copy(assistantPinRequired = true))
            }
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
     * Add or update contact.
     * For new contacts, [onNewContactSaved] is invoked with the new row id (e.g. to assign a home slot).
     */
    fun saveContact(
        contact: Contact,
        onNewContactSaved: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val contactType = if (contact.contactType == ContactType.CARER) "carer" else "grey_list"
            if (contact.id == 0L) {
                val maxPosition = contacts.first()
                    .filter { it.contactType == ContactType.CARER }
                    .maxOfOrNull { it.buttonPosition } ?: -1
                val contactToSave = contact.copy(buttonPosition = maxPosition + 1)
                val result = contactRepository.addContact(contactToSave)
                if (result.isSuccess) {
                    val newId = result.getOrNull() ?: return@launch
                    analytics.logEvent(AnalyticsEvent.ContactAdded(contactType = contactType))
                    callLogRepository.syncCallLogsWithContact(contactToSave.copy(id = newId))
                    onNewContactSaved(newId)
                }
            } else {
                contactRepository.updateContact(contact)
                analytics.logEvent(AnalyticsEvent.ContactEdited(contactType = contactType))
                callLogRepository.syncCallLogsWithContact(contact)
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
    fun moveContactUp(contact: Contact, orderedContacts: List<Contact>) {
        val sorted = orderedContacts.sortedBy { it.buttonPosition }
        val currentIndex = sorted.indexOfFirst { it.id == contact.id }
        if (currentIndex <= 0) return // Already at top

        viewModelScope.launch {
            val aboveContact = sorted[currentIndex - 1]
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
    fun moveContactDown(contact: Contact, orderedContacts: List<Contact>) {
        val sorted = orderedContacts.sortedBy { it.buttonPosition }
        val currentIndex = sorted.indexOfFirst { it.id == contact.id }
        if (currentIndex < 0 || currentIndex >= sorted.size - 1) return // Already at bottom

        viewModelScope.launch {
            val belowContact = sorted[currentIndex + 1]
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
        val carerCallable = contactsList.sortedCarerCallableForHome().take(HomeSlotAssignments.SLOT_COUNT)
        carerCallable.forEach { list.add(HomeSlotAssignments.contactSlot(it.id)) }
        if (current.homeShowMissedCallReturnButton) list.add(HomeSlotAssignments.MISSED_CALL_RETURN)
        if (current.homeShowMissedCallsButton) list.add(HomeSlotAssignments.MISSED_CALLS_LIST)
        if (current.homeShowContactsListButton) list.add(HomeSlotAssignments.OTHER_CONTACTS)
        if (current.homeShowDialerButton) list.add(HomeSlotAssignments.DIALER)
        if (current.showDisplayOffButton) list.add(HomeSlotAssignments.SCREEN_OFF)
        while (list.size < HomeSlotAssignments.SLOT_COUNT) list.add(HomeSlotAssignments.EMPTY)
        return list.take(HomeSlotAssignments.SLOT_COUNT)
    }

    /**
     * Run migration when opening Home Screen Layout: if size != 7, empty list, or seven empty strings,
     * build from current contacts + toggles and save.
     */
    fun ensureMigrationOnLayoutOpen() {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            val list = current.homeSlotAssignments
            when {
                list.size != HomeSlotAssignments.SLOT_COUNT -> {
                    val contactsList = contacts.first()
                    val migrated = buildLegacySlotAssignments(contactsList, current)
                    settingsRepository.updateSettings(current.withSlotsSynced(migrated))
                }
                list.all { it.isEmpty() } -> {
                    val contactsList = contacts.first()
                    val migrated = buildLegacySlotAssignments(contactsList, current)
                    settingsRepository.updateSettings(current.withSlotsSynced(migrated))
                }
                else -> {
                    val trimmed = list.take(HomeSlotAssignments.SLOT_COUNT)
                    val synced = current.withSlotsSynced(trimmed)
                    if (synced != current) {
                        settingsRepository.updateSettings(synced)
                    }
                }
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
            val digits = number.filter { it.isDigit() }
            val toStore =
                if (digits.isEmpty()) DEFAULT_EMERGENCY_FALLBACK else number.trim()
            settingsRepository.updateSettings(current.copy(emergencyNumber = toStore))
        }
    }

    /**
     * If the stored value is still the generic default, replace it with a device-appropriate code when known.
     */
    fun syncEmergencyNumberWithRegionIfNeeded() {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            val suggested =
                EmergencyNumberResolver.resolve(context, current.emergencyNumber)
                    .suggestedPersistDigits
                    ?: return@launch
            settingsRepository.updateSettings(current.copy(emergencyNumber = suggested))
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

    /** SMS all assistants when SOS is pressed (optional; requires SEND_SMS). */
    fun setSosSmsNotifyAssistantsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(sosSmsNotifyAssistantsEnabled = enabled))
        }
    }

    /** Custom SOS SMS body; empty uses default. Placeholders: {userName}, {name}. */
    fun setSosSmsNotifyAssistantsMessage(message: String) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(current.copy(sosSmsNotifyAssistantsMessage = message))
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
    
    // ========== TRANSFER (EXPORT / IMPORT) ==========

    /**
     * JSON snapshot for sharing to another device or keeping before [factoryReset].
     * Contains contacts and carer settings; photos use device URIs and may not work on a new phone.
     */
    suspend fun buildTransferExportJson(): String {
        val contactList = contactRepository.getContacts(500).first()
        val currentSettings = settingsRepository.getSettings().first()
        return AppDataTransfer.exportJson(contactList, currentSettings, json)
    }

    /**
     * Replace all contacts and carer settings from a transfer file. Clears in-app call history.
     * Does not change data outside this app.
     */
    suspend fun importTransferFromJson(jsonString: String): Result<Unit> {
        val payload = AppDataTransfer.parseTransfer(jsonString, json).getOrElse { return Result.failure(it) }
        contactRepository.deleteAllContacts().getOrElse { return Result.failure(it) }
        callLogRepository.deleteAllCallLogs().getOrElse { return Result.failure(it) }
        val sorted = payload.contacts.sortedBy { it.exportId }
        val exportToLocal = LinkedHashMap<Long, Long>()
        for (ec in sorted) {
            val newId = contactRepository.addContact(ec.toNewContact()).getOrElse { return Result.failure(it) }
            exportToLocal[ec.exportId] = newId
        }
        val finalSettings = AppDataTransfer.remapImportedSettings(payload.settings, exportToLocal)
        settingsRepository.updateSettings(finalSettings).getOrElse { return Result.failure(it) }
        return Result.success(Unit)
    }

    // ========== APP RESET ==========
    
    /**
     * App reset — wipe all data stored by this app only (not the device, not other apps’ contacts).
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
                android.util.Log.e("CarerSettingsVM", "App reset failed: ${e.message}")
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
    homeShowDialerButton = slots.contains(HomeSlotAssignments.DIALER),
    showDisplayOffButton = slots.contains(HomeSlotAssignments.SCREEN_OFF)
)
