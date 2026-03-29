package com.tomsphone.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.analytics.AnalyticsEvent
import com.tomsphone.core.analytics.AnalyticsManager
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.model.ContactType
import com.tomsphone.core.data.repository.ContactRepository
import com.tomsphone.core.data.util.PhoneNumberUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MAX_ASSISTANTS = 7

@HiltViewModel
class AddBlockedCallerViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val analytics: AnalyticsManager
) : ViewModel() {

    fun addContact(
        rawPhone: String,
        displayName: String,
        type: ContactType,
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            val result = runCatching {
                val trimmedName = displayName.trim()
                if (trimmedName.isEmpty()) {
                    error("Please enter a name")
                }
                val normalized = PhoneNumberUtils.normalizeToE164(rawPhone, "GB")
                    .ifBlank { rawPhone.trim() }
                if (normalized.isBlank()) {
                    error("Invalid phone number")
                }
                val all = contactRepository.getContacts(200).first()
                if (all.any { PhoneNumberUtils.isMatch(it.phoneNumber, normalized) }) {
                    error("This number is already in Wanda's Phone")
                }
                if (type == ContactType.CARER) {
                    val carers = all.filter { it.contactType == ContactType.CARER }
                    if (carers.size >= MAX_ASSISTANTS) {
                        error("You already have $MAX_ASSISTANTS assistants. Remove one in Settings first.")
                    }
                }
                val sameType = all.filter { it.contactType == type }
                val nextPos = sameType.maxOfOrNull { it.buttonPosition }?.plus(1) ?: 0
                val now = System.currentTimeMillis()
                val contact = Contact(
                    id = 0L,
                    name = trimmedName,
                    phoneNumber = normalized,
                    photoUri = null,
                    priority = 0,
                    contactType = type,
                    createdAt = now,
                    updatedAt = now,
                    buttonColor = null,
                    autoAnswerEnabled = false,
                    notifyBatteryAlerts = false,
                    buttonPosition = nextPos,
                    isHalfWidth = false
                )
                contactRepository.addContact(contact).getOrThrow()
                val contactTypeLabel = if (type == ContactType.CARER) "carer" else "grey_list"
                analytics.logEvent(AnalyticsEvent.ContactAdded(contactType = contactTypeLabel))
                "Added ${trimmedName}"
            }
            onResult(result)
        }
    }
}
