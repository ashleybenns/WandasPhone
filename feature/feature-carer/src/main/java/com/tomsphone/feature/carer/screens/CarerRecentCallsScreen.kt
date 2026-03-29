package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.data.model.CallLogEntry
import com.tomsphone.core.data.model.CallType
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.components.CarerBreadcrumb
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CarerRecentCallsScreen(
    onBack: () -> Unit,
    /** Number not in contacts ([CallLogEntry.contactId] is null): tap opens add-contact with phone pre-filled. */
    onAddUnknownCallerToContacts: (phoneNumber: String) -> Unit = {},
    viewModel: CarerRecentCallsViewModel = hiltViewModel()
) {
    val calls by viewModel.recentCalls.collectAsState()

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
                text = "Matches the user’s phone (one row per event). Tap an unknown number to add a contact.",
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
                    calls.forEach { call ->
                        CarerRecentCallRow(
                            call = call,
                            onAddUnknownCallerToContacts = onAddUnknownCallerToContacts
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CarerRecentCallRow(
    call: CallLogEntry,
    onAddUnknownCallerToContacts: (String) -> Unit
) {
    val unknownNotInContacts = call.contactId == null && call.phoneNumber.isNotBlank()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (unknownNotInContacts) {
                    Modifier.clickable {
                        onAddUnknownCallerToContacts(call.phoneNumber)
                    }
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.wandasColors.surface)
    ) {
        Column(Modifier.padding(WandasDimensions.SpacingMedium)) {
            Text(
                text = carerCallTypeLabel(call.type),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.wandasColors.primaryButton
            )
            Text(
                text = call.contactName ?: call.phoneNumber,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.wandasColors.onSurface
            )
            Text(
                text = call.phoneNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = formatCarerCallTime(call.timestamp) +
                    when {
                        call.duration > 0L && (call.type == CallType.INCOMING || call.type == CallType.OUTGOING) ->
                            " · ${formatDurationShort(call.duration)}"
                        call.type == CallType.OUTGOING_UNANSWERED -> " · Did not connect"
                        else -> ""
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f)
            )
            if (unknownNotInContacts) {
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

private fun carerCallTypeLabel(type: CallType): String = when (type) {
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
