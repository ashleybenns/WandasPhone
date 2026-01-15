package com.tomsphone.feature.carer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.config.CarerSettings
import com.tomsphone.core.config.FeatureLevel
import com.tomsphone.core.config.SettingsRepository
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.repository.ContactRepository
import com.tomsphone.core.ui.theme.ThemeOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    
    // PIN verification state
    private val _isPinVerified = MutableStateFlow(false)
    val isPinVerified: StateFlow<Boolean> = _isPinVerified.asStateFlow()
    
    // UI state
    private val _showPinDialog = MutableStateFlow(true)
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
    fun setTheme(theme: ThemeOption) {
        viewModelScope.launch {
            val current = settings.first()
            val newTheme = com.tomsphone.core.config.ThemeOption.valueOf(theme.name)
            settingsRepository.updateSettings(
                current.copy(ui = current.ui.copy(theme = newTheme))
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
    fun setAutoAnswer(enabled: Boolean, delaySeconds: Int = 3) {
        viewModelScope.launch {
            val current = settings.first()
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
    
    // ========== ALWAYS ON MODE SETTINGS ==========
    
    /**
     * Toggle pinned mode (app stays in foreground)
     */
    fun setPinnedMode(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.first()
            settingsRepository.updateSettings(current.copy(pinnedModeEnabled = enabled))
        }
    }
    
    /**
     * Toggle screen always on
     */
    fun setScreenAlwaysOn(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.first()
            settingsRepository.updateSettings(current.copy(screenAlwaysOn = enabled))
        }
    }
    
    /**
     * Toggle volume button lock
     */
    fun setLockVolumeButtons(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.first()
            settingsRepository.updateSettings(current.copy(lockVolumeButtons = enabled))
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

