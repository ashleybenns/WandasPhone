package com.tomsphone.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.config.ListTextAlignment
import com.tomsphone.core.config.SettingsRepository
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * ViewModel for Contacts List Screen
 * 
 * Shows contacts based on carer settings:
 * - If homeContactsListShowGreyListOnly: Only grey list contacts (answer-only)
 * - Otherwise: All contacts (carers + grey list)
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContactsListViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    // Whether to show only grey list contacts
    private val showGreyListOnly: StateFlow<Boolean> = settingsRepository.getSettings()
        .map { settings -> settings.homeContactsListShowGreyListOnly }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    // Contacts to display based on setting
    val contacts: StateFlow<List<Contact>> = showGreyListOnly
        .flatMapLatest { greyListOnly: Boolean ->
            if (greyListOnly) {
                contactRepository.getGreyListContacts(100)
            } else {
                // Show all contacts (carers + grey list combined)
                combine(
                    contactRepository.getCarerContacts(100),
                    contactRepository.getGreyListContacts(100)
                ) { carers: List<Contact>, greyList: List<Contact> ->
                    carers + greyList
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Screen title based on mode
    val screenTitle: StateFlow<String> = showGreyListOnly
        .map { greyListOnly: Boolean ->
            if (greyListOnly) "Other Contacts" else "All Contacts"
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Contacts"
        )
    
    // Empty message based on mode
    val emptyMessage: StateFlow<String> = showGreyListOnly
        .map { greyListOnly: Boolean ->
            if (greyListOnly) "No Other Contacts" else "No Contacts"
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "No Contacts"
        )
    
    // Text alignment for list items
    val listTextAlignment: StateFlow<ListTextAlignment> = settingsRepository.getSettings()
        .map { settings -> settings.listTextAlignment }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ListTextAlignment.CENTER
        )
    
    // Button activation mode
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
}
