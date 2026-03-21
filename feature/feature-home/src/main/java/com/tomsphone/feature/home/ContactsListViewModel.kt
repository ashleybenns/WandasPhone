package com.tomsphone.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.config.CarerSettings
import com.tomsphone.core.config.HomeSlotAssignments
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
 * 
 * Paginates with up to 8 contacts per page (matching home screen button count).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContactsListViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    // Current page (0-indexed)
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()
    
    // Settings to calculate button count
    private val settings: StateFlow<CarerSettings> = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CarerSettings()
        )
    
    // Get all contacts for button count calculation
    private val allContactsForCount: StateFlow<List<Contact>> = contactRepository.getCarerContacts(100)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Calculate contacts per page: number of non-empty home buttons + 1 (emergency)
    private val contactsPerPage: StateFlow<Int> = combine(
        settings,
        allContactsForCount
    ) { s, contacts ->
        countNonEmptyHomeButtons(s, contacts) + 1 // +1 for emergency button
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeSlotAssignments.TOTAL_SLOTS // fallback to 8
        )
    
    // Whether to show only grey list contacts
    private val showGreyListOnly: StateFlow<Boolean> = settings
        .map { it.homeContactsListShowGreyListOnly }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    // All contacts to display based on setting
    private val allContacts: StateFlow<List<Contact>> = showGreyListOnly
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
    
    // Paginated contacts for current page
    val contacts: StateFlow<List<Contact>> = combine(
        allContacts,
        currentPage,
        contactsPerPage
    ) { all, page, perPage ->
        val start = page * perPage
        val end = (start + perPage).coerceAtMost(all.size)
        all.subList(start, end)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Whether there are more pages
    val hasNextPage: StateFlow<Boolean> = combine(
        allContacts,
        currentPage,
        contactsPerPage
    ) { all, page, perPage ->
        val start = (page + 1) * perPage
        start < all.size
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    /**
     * Count non-empty buttons on home screen (excluding emergency which is always +1).
     * Matches the logic in HomeViewModel.buildHomeButtons.
     */
    private fun countNonEmptyHomeButtons(settings: CarerSettings, contacts: List<Contact>): Int {
        val slots = settings.homeSlotAssignments
        return if (slots.size == HomeSlotAssignments.SLOT_COUNT) {
            // Count non-empty slots
            slots.count { it.isNotEmpty() }
        } else {
            // Legacy mode: count enabled toggles + contacts
            var count = 0
            if (settings.homeShowMissedCallReturnButton) count++
            if (settings.homeShowMissedCallsButton) count++
            if (settings.homeShowContactsListButton) count++
            if (settings.showDisplayOffButton) count++
            val slotsForOthers = count
            val maxContactSlots = (6 - slotsForOthers).coerceAtLeast(0)
            val callableContacts = contacts
                .filter { it.canCallOut }
                .sortedBy { it.buttonPosition }
                .take(maxContactSlots)
            callableContacts.size + count
        }
    }
    
    // Screen title - empty to hide header
    val screenTitle: StateFlow<String> = flowOf("")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
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
    
    fun nextPage() {
        _currentPage.value++
    }
    
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
