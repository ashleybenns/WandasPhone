package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.data.model.CallLogEntry
import com.tomsphone.core.data.model.CallType
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.util.PhoneNumberUtils
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.components.CarerBreadcrumb
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CarerRecentCallsScreen(
    onBack: () -> Unit,
    /** Number not in contacts: tap the card to add with phone pre-filled. */
    onAddUnknownCallerToContacts: (phoneNumber: String) -> Unit = {},
    viewModel: CarerRecentCallsViewModel = hiltViewModel()
) {
    val calls by viewModel.recentCalls.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val groups = remember(calls, contacts) { buildRecentCallGroups(calls, contacts) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Column(Modifier.fillMaxSize()) {
            CarerBreadcrumb(
                title = "Recent calls",
                parentTitle = "Contacts",
                onBack = onBack
            )
            Text(
                text = "Matches the user’s phone. History is grouped by person (most recent activity first). Tap an unknown caller to add name and button colour.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.75f),
                modifier = Modifier.padding(horizontal = WandasDimensions.SpacingMedium)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(WandasDimensions.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingSmall)
            ) {
                if (calls.isEmpty()) {
                    Text(
                        text = "No calls recorded yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.wandasColors.onSurface
                    )
                } else {
                    groups.forEach { group ->
                        CarerRecentCallPersonCard(
                            group = group,
                            onAddUnknownCallerToContacts = onAddUnknownCallerToContacts
                        )
                    }
                }
            }
        }
    }
}

private data class RecentCallPersonGroup(
    val sortKey: Long,
    val displayName: String,
    val representativePhone: String,
    val contact: Contact?,
    val events: List<CallLogEntry>,
)

private fun buildRecentCallGroups(
    calls: List<CallLogEntry>,
    contacts: List<Contact>
): List<RecentCallPersonGroup> {
    val contactById = contacts.associateBy { it.id }

    fun resolveContact(call: CallLogEntry): Contact? {
        call.contactId?.let { id -> contactById[id]?.let { return it } }
        return contacts.find { c -> PhoneNumberUtils.isMatch(c.phoneNumber, call.phoneNumber) }
    }

    fun groupKey(call: CallLogEntry): String {
        val c = resolveContact(call)
        if (c != null) return "id:${c.id}"
        val norm = PhoneNumberUtils.normalizeToE164(call.phoneNumber, "GB").ifBlank {
            call.phoneNumber.trim()
        }
        return "phone:$norm"
    }

    return calls
        .groupBy(::groupKey)
        .values
        .map { entries ->
            val sorted = entries.sortedByDescending { it.timestamp }
            val head = sorted.first()
            val contact = resolveContact(head)
            val displayName = contact?.name ?: head.contactName ?: head.phoneNumber
            RecentCallPersonGroup(
                sortKey = sorted.maxOf { it.timestamp },
                displayName = displayName,
                representativePhone = head.phoneNumber,
                contact = contact,
                events = sorted,
            )
        }
        .sortedByDescending { it.sortKey }
}

@Composable
private fun CarerRecentCallPersonCard(
    group: RecentCallPersonGroup,
    onAddUnknownCallerToContacts: (String) -> Unit,
) {
    val unknown = group.contact == null && group.representativePhone.isNotBlank()
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (unknown) {
                        Modifier.clickable {
                            onAddUnknownCallerToContacts(group.representativePhone)
                        }
                    } else {
                        Modifier
                    }
                ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.wandasColors.surface)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            val stripeColor = group.contact?.buttonColor?.let { Color(it) }
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(stripeColor ?: Color.Transparent)
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(WandasDimensions.SpacingMedium)
            ) {
                Text(
                    text = group.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.wandasColors.onSurface
                )
                Text(
                    text = group.representativePhone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(8.dp))
                group.events.forEachIndexed { index, event ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.12f)
                        )
                    }
                    Text(
                        text = carerCallTypeLabel(event.type),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.wandasColors.primaryButton
                    )
                    Text(
                        text =
                            formatCarerCallTime(event.timestamp) +
                                when {
                                    event.duration > 0L &&
                                        (event.type == CallType.INCOMING || event.type == CallType.OUTGOING) ->
                                        " · ${formatDurationShort(event.duration)}"
                                    event.type == CallType.OUTGOING_UNANSWERED -> " · Did not connect"
                                    else -> ""
                                },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f)
                    )
                }
                if (unknown) {
                    Text(
                        text = "Tap to add to contacts — set name and button colour",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.wandasColors.primaryButton,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

private fun carerCallTypeLabel(type: CallType): String =
    when (type) {
        CallType.INCOMING -> "Answered incoming"
        CallType.OUTGOING -> "Outgoing (answered)"
        CallType.OUTGOING_UNANSWERED -> "Outgoing (no answer)"
        CallType.MISSED -> "Missed"
        CallType.REJECTED -> "Declined"
        CallType.BLOCKED -> "Blocked"
    }

private fun formatCarerCallTime(timestamp: Long): String {
    val fmt = SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault())
    return fmt.format(Date(timestamp))
}

private fun formatDurationShort(durationMs: Long): String {
    val totalSec = durationMs / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}
