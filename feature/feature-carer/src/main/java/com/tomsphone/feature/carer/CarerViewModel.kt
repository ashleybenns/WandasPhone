package com.tomsphone.feature.carer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.config.CarerSettings
import com.tomsphone.core.config.FeatureLevel
import com.tomsphone.core.config.SettingsRepository
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.repository.ContactRepository
import com.tomsphone.core.config.ThemeOption
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
 * - Factory reset (wipe all data)
 */
@HiltViewModel
class CarerSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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
            val current = settings.first()
            settingsRepository.updateSettings(
                current.copy(ui = current.ui.copy(userTextSize = textSize))
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
    
    // ========== CALL HANDLING SETTINGS ==========
    
    /**
     * Toggle reject unknown calls
     */
    fun setRejectUnknownCalls(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.first()
            settingsRepository.updateSettings(current.copy(rejectUnknownCalls = enabled))
        }
    }
    
    /**
     * Toggle speakerphone always on
     */
    fun setSpeakerphoneAlwaysOn(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.first()
            settingsRepository.updateSettings(current.copy(speakerphoneAlwaysOn = enabled))
        }
    }
    
    /**
     * Toggle missed call nag enabled
     */
    fun setMissedCallNagEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.first()
            settingsRepository.updateSettings(current.copy(missedCallNagEnabled = enabled))
        }
    }
    
    /**
     * Set missed call nag interval
     */
    fun setMissedCallNagInterval(interval: com.tomsphone.core.config.MissedCallNagInterval) {
        viewModelScope.launch {
            val current = settings.first()
            settingsRepository.updateSettings(current.copy(missedCallNagInterval = interval))
        }
    }
    
    // ========== USER PROFILE SETTINGS ==========
    
    /**
     * Set emergency number
     */
    fun setEmergencyNumber(number: String) {
        viewModelScope.launch {
            val current = settings.first()
            settingsRepository.updateSettings(current.copy(emergencyNumber = number))
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

