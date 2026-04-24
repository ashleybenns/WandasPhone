package com.tomsphone.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.config.CarerSettings
import com.tomsphone.core.config.homeButtonRowCount
import com.tomsphone.core.config.HomeSlotAssignments
import com.tomsphone.core.config.ListTextAlignment
import com.tomsphone.core.config.SettingsRepository
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.model.sortedCarerCallableForHome
import com.tomsphone.core.data.model.sortedForContactList
import com.tomsphone.core.data.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * ViewModel for Contacts List Screen — everyone, paginated by how many home-height rows fit the list viewport.
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

    /**
     * Weighted row count on the home grid (from settings). Must match how [HomeScreen] splits space —
     * not MainActivity’s `coerceIn(2, 6)`, which only caps scale in [UserScalingProvider]; the real home
     * layout can have 7+ rows, so using 6 here made list rows taller than home.
     */
    val homeButtonRowCountForLayout: StateFlow<Int> = settings
        .map { s -> s.homeButtonRowCount.coerceIn(1, 12) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 4
        )
    
    // All contacts for home button count (slots may reference any contact)
    private val allContactsForCount: StateFlow<List<Contact>> = contactRepository.getContacts(200)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Calculate contacts per page: number of non-empty home buttons + 1 (emergency)
    private val defaultContactsPerPage: StateFlow<Int> = combine(
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

    private val _contactsPerPageOverride = MutableStateFlow<Int?>(null)

    private val contactsPerPage: StateFlow<Int> = combine(
        defaultContactsPerPage,
        _contactsPerPageOverride
    ) { fallback, override ->
        (override ?: fallback).coerceIn(1, 50)
    }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeSlotAssignments.TOTAL_SLOTS
        )
    
    private val allContacts: StateFlow<List<Contact>> = contactRepository.getContacts(200)
        .map { everyone -> everyone.sortedForContactList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // If page size or total contacts changes, clamp to the last valid page so we never "lose" rows.
        viewModelScope.launch {
            combine(
                allContacts.map { it.size }.distinctUntilChanged(),
                contactsPerPage
            ) { total, perPage -> total to perPage }
                .collect { (total, perPage) ->
                    val lastPage = if (total <= 0) 0 else (total - 1) / perPage.coerceAtLeast(1)
                    val current = _currentPage.value
                    if (current > lastPage) _currentPage.value = lastPage
                    if (_currentPage.value < 0) _currentPage.value = 0
                }
        }
    }
    
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
        // Single layout model: count the non-empty assigned home slots.
        // HomeViewModel migrates legacy installs to a valid slot list.
        return slots.count { it.isNotEmpty() }
    }
    
    val emptyMessage: StateFlow<String> = flowOf("No contacts")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "No contacts"
        )
    
    fun nextPage() {
        _currentPage.value++
    }

    /**
     * Set by the UI when it can measure how many rows fit in the list viewport.
     * This replaces the home-based fallback paging on the contacts list only.
     */
    fun setContactsPerPageFromLayout(perPage: Int) {
        val v = perPage.coerceIn(1, 50)
        if (_contactsPerPageOverride.value != v) {
            _contactsPerPageOverride.value = v
        }
    }
    
    /** @return true if back was consumed (moved to previous page); false if already on first page — caller should pop navigation. */
    fun goBackWithinList(): Boolean {
        val p = _currentPage.value
        if (p <= 0) return false
        _currentPage.value = p - 1
        return true
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

    val autoAnswerEnabled: StateFlow<Boolean> = settingsRepository.getSettings()
        .map { it.autoAnswerEnabled }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
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
