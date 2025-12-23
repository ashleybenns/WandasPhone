package com.wandasphone.feature.carer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wandasphone.core.config.CarerSettings
import com.wandasphone.core.config.FeatureLevel
import com.wandasphone.core.config.SettingsRepository
import com.wandasphone.core.data.model.Contact
import com.wandasphone.core.data.repository.ContactRepository
import com.wandasphone.core.ui.theme.ThemeOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

// DEV MODE: Set to false for production
private const val DEV_MODE = true

/**
 * ViewModel for carer configuration
 * 
 * Allows carers to:
 * - Manage contacts
 * - Change feature level
 * - Adjust settings
 * - Select theme
 * - Configure auto-answer
 */
@HiltViewModel
class CarerViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val contactRepository: ContactRepository
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
    
    // PIN verification state - bypass in DEV_MODE
    private val _isPinVerified = MutableStateFlow(DEV_MODE)
    val isPinVerified: StateFlow<Boolean> = _isPinVerified.asStateFlow()
    
    // UI state - skip PIN dialog in DEV_MODE
    private val _showPinDialog = MutableStateFlow(!DEV_MODE)
    val showPinDialog: StateFlow<Boolean> = _showPinDialog.asStateFlow()
    
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
                }
            } else {
                // Verify against stored PIN
                if (settingsRepository.verifyPin(hashedPin)) {
                    _isPinVerified.value = true
                    _showPinDialog.value = false
                }
            }
        }
    }
    
    /**
     * Update feature level
     */
    fun setFeatureLevel(level: FeatureLevel) {
        viewModelScope.launch {
            settingsRepository.setFeatureLevel(level)
        }
    }
    
    /**
     * Update theme
     */
    fun setTheme(themeOption: ThemeOption) {
        viewModelScope.launch {
            val current = settings.first()
            settingsRepository.updateSettings(
                current.copy(themeOption = themeOption.ordinal)
            )
        }
    }
    
    /**
     * Update user name
     */
    fun setUserName(name: String) {
        viewModelScope.launch {
            val current = settings.first()
            settingsRepository.updateSettings(current.copy(userName = name))
        }
    }
    
    /**
     * Toggle auto-answer
     */
    fun setAutoAnswer(enabled: Boolean, rings: Int = 3) {
        viewModelScope.launch {
            val current = settings.first()
            settingsRepository.updateSettings(
                current.copy(
                    autoAnswerEnabled = enabled,
                    autoAnswerRings = rings
                )
            )
        }
    }
    
    /**
     * Add or update contact
     */
    fun saveContact(contact: Contact) {
        viewModelScope.launch {
            if (contact.id == 0L) {
                contactRepository.addContact(contact)
            } else {
                contactRepository.updateContact(contact)
            }
        }
    }
    
    /**
     * Delete contact
     */
    fun deleteContact(id: Long) {
        viewModelScope.launch {
            contactRepository.removeContact(id)
        }
    }
    
    /**
     * Set primary contact
     */
    fun setPrimaryContact(id: Long) {
        viewModelScope.launch {
            contactRepository.setPrimaryContact(id)
        }
    }
    
    /**
     * Update speaker volume
     */
    fun setSpeakerVolume(volume: Int) {
        viewModelScope.launch {
            val current = settings.first()
            settingsRepository.updateSettings(current.copy(speakerVolume = volume.coerceIn(1, 10)))
        }
    }
    
    /**
     * Update emergency number
     */
    fun setEmergencyNumber(number: String) {
        viewModelScope.launch {
            val current = settings.first()
            settingsRepository.updateSettings(current.copy(emergencyNumber = number))
        }
    }
    
    /**
     * Update missed call nag interval
     */
    fun setMissedCallNagInterval(minutes: Int) {
        viewModelScope.launch {
            val current = settings.first()
            settingsRepository.updateSettings(current.copy(missedCallNagIntervalMinutes = minutes.coerceIn(1, 15)))
        }
    }
    
    /**
     * Hash PIN for secure storage
     */
    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

