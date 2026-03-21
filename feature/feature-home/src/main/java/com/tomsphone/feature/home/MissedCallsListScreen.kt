package com.tomsphone.feature.home

import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.config.ListTextAlignment
import com.tomsphone.core.data.model.CallLogEntry
import com.tomsphone.core.data.model.CallType
import com.tomsphone.core.ui.components.activationGesture
import com.tomsphone.core.ui.components.ListScreenLayout
import com.tomsphone.core.ui.theme.PastelColors
import com.tomsphone.core.ui.theme.ScaledDimensions
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private const val INACTIVITY_TIMEOUT_MS = 30_000L

/**
 * Recent calls for the assistant: incoming, outgoing, missed, and declined.
 * Each log row is listed separately (duplicates allowed). Unknown numbers can
 * open the system "new contact" screen with the number filled in.
 */
@Composable
fun MissedCallsListScreen(
    onBack: () -> Unit,
    onCallContact: (String, String) -> Unit, // (name, phoneNumber)
    onAddToContacts: (String) -> Unit = {},
    viewModel: RecentCallsListViewModel = hiltViewModel()
) {
    val recentCalls by viewModel.recentCalls.collectAsState()
    val listTextAlignment by viewModel.listTextAlignment.collectAsState()
    val buttonActivation by viewModel.buttonActivation.collectAsState()
    val touchDebounceMs by viewModel.touchDebounceMs.collectAsState()
    val accumulatedThresholdMs by viewModel.accumulatedTapThresholdMs.collectAsState()
    val accumulatedTimeoutMs by viewModel.accumulatedTapTimeoutMs.collectAsState()

    LaunchedEffect(Unit) {
        delay(INACTIVITY_TIMEOUT_MS)
        onBack()
    }

    ListScreenLayout(
        backgroundColor = PastelColors.lightBlue,
        title = "Recent calls",
        emptyMessage = "No recent calls",
        isEmpty = recentCalls.isEmpty(),
        onBack = onBack,
        activationPreset = buttonActivation,
        debounceMs = touchDebounceMs,
        accumulatedThresholdMs = accumulatedThresholdMs,
        accumulatedTimeoutMs = accumulatedTimeoutMs
    ) {
        recentCalls.forEach { call ->
            RecentCallItem(
                call = call,
                textAlignment = listTextAlignment,
                activationPreset = buttonActivation,
                debounceMs = touchDebounceMs,
                accumulatedThresholdMs = accumulatedThresholdMs,
                accumulatedTimeoutMs = accumulatedTimeoutMs,
                onCall = {
                    val label = call.contactName ?: call.phoneNumber
                    onCallContact(label, call.phoneNumber)
                },
                onAddToContacts = if (call.contactId == null) {
                    { onAddToContacts(call.phoneNumber) }
                } else {
                    null
                }
            )
        }
    }
}

private fun callTypeLabel(type: CallType): String = when (type) {
    CallType.INCOMING -> "Answered incoming"
    CallType.OUTGOING -> "Outgoing"
    CallType.MISSED -> "Missed"
    CallType.REJECTED -> "Declined"
    CallType.BLOCKED -> "Blocked"
}

@Composable
private fun RecentCallItem(
    call: CallLogEntry,
    textAlignment: ListTextAlignment,
    activationPreset: ButtonActivationPreset,
    debounceMs: Int,
    accumulatedThresholdMs: Int,
    accumulatedTimeoutMs: Int,
    onCall: () -> Unit,
    onAddToContacts: (() -> Unit)?
) {
    val textSize = ScaledDimensions.buttonTextSize
    val typeSize = textSize * 0.72f
    val timeText = formatRelativeTime(call.timestamp)
    val typeLabel = callTypeLabel(call.type)
    val mainLabel = call.contactName ?: call.phoneNumber
    val subtitle = buildString {
        append(typeLabel)
        if (call.duration > 0L &&
            (call.type == CallType.INCOMING || call.type == CallType.OUTGOING)
        ) {
            append(" · ")
            append(formatDuration(call.duration))
        }
    }

    val boxAlignment = when (textAlignment) {
        ListTextAlignment.LEFT -> Alignment.CenterStart
        ListTextAlignment.CENTER -> Alignment.Center
    }
    val textAlign = when (textAlignment) {
        ListTextAlignment.LEFT -> TextAlign.Start
        ListTextAlignment.CENTER -> TextAlign.Center
    }
    val columnAlignment = when (textAlignment) {
        ListTextAlignment.LEFT -> Alignment.Start
        ListTextAlignment.CENTER -> Alignment.CenterHorizontally
    }

    val callInteractionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = columnAlignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        val rowBg = callRowColors(call.type)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .shadow(WandasDimensions.ElevationMedium, RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
                .clip(RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
                .indication(callInteractionSource, rememberRipple())
                .activationGesture(
                    preset = activationPreset,
                    debounceMs = debounceMs,
                    accumulatedThresholdMs = accumulatedThresholdMs,
                    accumulatedTimeoutMs = accumulatedTimeoutMs,
                    onActivate = onCall,
                    interactionSource = callInteractionSource
                ),
            color = rowBg.background,
            shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = boxAlignment
            ) {
                Column(horizontalAlignment = columnAlignment) {
                    Text(
                        text = subtitle,
                        style = TextStyle(
                            fontSize = typeSize,
                            fontWeight = FontWeight.Medium
                        ),
                        color = rowBg.onBackground.copy(alpha = 0.92f),
                        textAlign = textAlign
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mainLabel,
                        style = TextStyle(
                            fontSize = textSize,
                            fontWeight = FontWeight.Bold
                        ),
                        color = rowBg.onBackground,
                        textAlign = textAlign
                    )
                }
            }
        }

        Text(
            text = timeText,
            style = TextStyle(
                fontSize = textSize,
                fontWeight = FontWeight.Normal
            ),
            color = Color.Black,
            textAlign = textAlign,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
        )

        if (onAddToContacts != null) {
            val addInteraction = remember { MutableInteractionSource() }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .shadow(WandasDimensions.ElevationMedium, RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
                    .clip(RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
                    .indication(addInteraction, rememberRipple())
                    .activationGesture(
                        preset = activationPreset,
                        debounceMs = debounceMs,
                        accumulatedThresholdMs = accumulatedThresholdMs,
                        accumulatedTimeoutMs = accumulatedTimeoutMs,
                        onActivate = onAddToContacts,
                        interactionSource = addInteraction
                    ),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge)
            ) {
                Text(
                    text = "Add to phone contacts",
                    style = TextStyle(
                        fontSize = typeSize,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = textAlign,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

private data class CallRowColors(val background: Color, val onBackground: Color)

@Composable
private fun callRowColors(type: CallType): CallRowColors {
    val c = MaterialTheme.wandasColors
    return when (type) {
        CallType.MISSED, CallType.REJECTED -> CallRowColors(c.missedCall, c.onMissedCall)
        CallType.BLOCKED -> CallRowColors(c.hangUpButton, c.onHangUpButton)
        CallType.INCOMING, CallType.OUTGOING -> CallRowColors(c.primaryButton, c.onPrimaryButton)
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSec = durationMs / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000

    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
        hours < 24 -> "$hours hour${if (hours > 1) "s" else ""} ago"
        days < 2 -> {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            "yesterday at ${timeFormat.format(Date(timestamp))}"
        }
        days < 7 -> "$days days ago"
        else -> {
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}
