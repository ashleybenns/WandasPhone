package com.tomsphone.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.config.CarerSettings
import com.tomsphone.core.config.HomeSlotAssignments
import com.tomsphone.core.config.ListTextAlignment
import com.tomsphone.core.config.SettingsRepository
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.model.ContactType
import com.tomsphone.core.data.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * ViewModel for Contacts List Screen
 *
 * - Default: everyone (single list).
 * - Optional: only contacts **without** a home call button (slot), when
 *   [CarerSettings.homeContactsListShowGreyListOnly] is on (legacy setting key).
 *
 * Paginates with up to 8 contacts per page (matching home screen button count).
 */
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
    
    // All contacts for home button count (slots may reference any contact)
    private val allContactsForCount: StateFlow<List<Contact>> = contactRepository.getContacts(200)
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
    
    private val allContacts: StateFlow<List<Contact>> = combine(
        settings,
        contactRepository.getContacts(200)
    ) { s, everyone ->
        val onHome = HomeSlotAssignments.contactIdsOnHome(s.homeSlotAssignments)
        if (s.homeContactsListShowGreyListOnly) {
            everyone.filter { it.id !in onHome }.sortedBy { it.name.lowercase() }
        } else {
            everyone.sortedWith(compareBy<Contact> { it.buttonPosition }.thenBy { it.name.lowercase() })
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
                .filter { it.contactType == ContactType.CARER }
                .sortedBy { it.buttonPosition }
                .take(maxContactSlots)
            callableContacts.size + count
        }
    }
    
    /** Shown when the list has at least one row */
    val screenTitle: StateFlow<String> = flowOf("Contacts")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Contacts"
        )
    
    val emptyMessage: StateFlow<String> = settings
        .map { s ->
            if (s.homeContactsListShowGreyListOnly) "No contacts without a home button"
            else "No contacts"
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "No contacts"
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
