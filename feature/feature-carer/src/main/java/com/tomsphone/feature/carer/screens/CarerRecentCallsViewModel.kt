package com.tomsphone.feature.carer.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.data.model.CallLogEntry
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.repository.CallLogRepository
import com.tomsphone.core.data.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val RECENT_CALLS_LIMIT = 500

/**
 * Read-only recent call history for carers (same Room `call_logs` as the user list).
 * [contacts] is used to resolve names and button colours when displaying grouped history.
 */
@HiltViewModel
class CarerRecentCallsViewModel @Inject constructor(
    callLogRepository: CallLogRepository,
    contactRepository: ContactRepository
) : ViewModel() {

    val recentCalls: StateFlow<List<CallLogEntry>> = callLogRepository
        .getRecentCalls(RECENT_CALLS_LIMIT)
        .map { list -> list.filter { it.phoneNumber.isNotBlank() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val contacts: StateFlow<List<Contact>> = contactRepository
        .getContacts(100)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
