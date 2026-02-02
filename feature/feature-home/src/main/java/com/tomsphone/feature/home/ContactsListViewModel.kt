package com.tomsphone.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * ViewModel for Contacts List Screen
 * 
 * Shows all CARER contacts ordered by buttonPosition.
 * No limit - shows all carers including those that don't fit on home screen.
 */
@HiltViewModel
class ContactsListViewModel @Inject constructor(
    private val contactRepository: ContactRepository
) : ViewModel() {
    
    // All carer contacts, ordered by buttonPosition
    val contacts: StateFlow<List<Contact>> = contactRepository.getCarerContacts(100) // Large limit to get all
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
