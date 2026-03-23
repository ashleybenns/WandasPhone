package com.tomsphone.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.config.ListTextAlignment
import com.tomsphone.core.config.SettingsRepository
import com.tomsphone.core.data.model.CallLogEntry
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.repository.CallLogRepository
import com.tomsphone.core.data.repository.ContactRepository
import com.tomsphone.core.data.util.PhoneNumberUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val MISSED_CALLS_LIST_LIMIT = 200
private const val PHONE_REGION = "GB"

/**
 * ViewModel for the assistant **Missed calls** list: **unique callers** with unread missed/declined,
 * most recent time each. Resolves [Contact] per row for colours; unknown numbers use blue.
 */
@HiltViewModel
class MissedCallsListViewModel @Inject constructor(
    private val callLogRepository: CallLogRepository,
    private val contactRepository: ContactRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val missedCallRows: StateFlow<List<MissedCallListRow>> = combine(
        callLogRepository.getOutstandingMissedCallsPerCaller(MISSED_CALLS_LIST_LIMIT),
        contactRepository.getContacts(500)
    ) { calls, contacts ->
        val byId = contacts.associateBy { it.id }
        calls.map { call ->
            val contact = resolveContact(call, byId, contacts)
            MissedCallListRow(call, contact)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val listTextAlignment: StateFlow<ListTextAlignment> = settingsRepository.getSettings()
        .map { settings -> settings.listTextAlignment }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ListTextAlignment.CENTER
        )

    val buttonActivation: StateFlow<ButtonActivationPreset> = settingsRepository.getSettings()
        .map { it.buttonActivation }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ButtonActivationPreset.ON_RELEASE
        )

    val touchDebounceMs: StateFlow<Int> = settingsRepository.getSettings()
        .map { it.touchDebounceMs }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 150
        )

    val accumulatedTapThresholdMs: StateFlow<Int> = settingsRepository.getSettings()
        .map { it.accumulatedTapThresholdMs }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 500
        )

    val accumulatedTapTimeoutMs: StateFlow<Int> = settingsRepository.getSettings()
        .map { it.accumulatedTapTimeoutMs }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 3000
        )

    private fun resolveContact(
        call: CallLogEntry,
        byId: Map<Long, Contact>,
        contacts: List<Contact>
    ): Contact? {
        call.contactId?.let { id -> byId[id]?.let { return it } }
        val callE164 = PhoneNumberUtils.normalizeToE164(call.phoneNumber, PHONE_REGION)
        if (callE164.isEmpty()) return null
        return contacts.firstOrNull { c ->
            PhoneNumberUtils.normalizeToE164(c.phoneNumber, PHONE_REGION) == callE164
        }
    }
}
