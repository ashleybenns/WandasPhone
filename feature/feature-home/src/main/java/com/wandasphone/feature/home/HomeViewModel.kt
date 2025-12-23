package com.wandasphone.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wandasphone.core.config.FeatureLevel
import com.wandasphone.core.config.SettingsRepository
import com.wandasphone.core.data.model.Contact
import com.wandasphone.core.data.repository.ContactRepository
import com.wandasphone.core.telecom.CallManager
import com.wandasphone.core.tts.TTSScripts
import com.wandasphone.core.tts.WandasTTS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for home screen
 * 
 * Responsibilities:
 * - Load contacts based on feature level
 * - Handle tap-to-call
 * - Track inactivity
 * - Carer settings access (7-tap hidden button)
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val contactRepository: ContactRepository,
    private val callManager: CallManager,
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
            initialValue = "Wanda"
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
    }
    
    /**
     * User tapped a contact button - place call
     */
    fun onContactTap(contact: Contact) {
        viewModelScope.launch {
            tts.speak(TTSScripts.calling(contact.name), WandasTTS.Priority.IMMEDIATE)
            callManager.placeCall(contact.phoneNumber)
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
            if (_carerTapCount.value >= settings.settingsTapCount) {
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
     * Emergency button tapped (requires 3 taps)
     */
    private val _emergencyTapCount = MutableStateFlow(0)
    fun onEmergencyButtonTap() {
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
    }
}

