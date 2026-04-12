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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.config.ListTextAlignment
import com.tomsphone.core.data.model.CallType
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.ui.components.FittedLabelInBox
import com.tomsphone.core.ui.components.SecondaryScreenIdleEffect
import com.tomsphone.core.ui.components.activationGesture
import com.tomsphone.core.ui.components.ListScreenLayout
import com.tomsphone.core.ui.theme.PastelColors
import com.tomsphone.core.ui.theme.ScaledDimensions
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import java.text.SimpleDateFormat
import java.util.*

private const val INACTIVITY_TIMEOUT_MS = 30_000L

/** Unknown / not-in-app number (call was allowed through screening) — solid blue row, white label. */
private val UnknownAllowedRowBlue = Color(0xFF1976D2)

/**
 * **Missed calls list** (two-tap home button): **one row per caller** with an outstanding missed/declined
 * call; returning a call removes that caller. Rows use each contact’s button colour; unknown allowed numbers use blue.
 */
@Composable
fun MissedCallsListScreen(
    onBack: () -> Unit,
    onCallContact: (String, String) -> Unit,
    onAddToContacts: (String) -> Unit = {},
    onAddBlockedToApp: (String, String?) -> Unit = { _, _ -> },
    viewModel: MissedCallsListViewModel = hiltViewModel()
) {
    val missedCallRows by viewModel.missedCallRows.collectAsState()
    val listTextAlignment by viewModel.listTextAlignment.collectAsState()
    val buttonActivation by viewModel.buttonActivation.collectAsState()
    val touchDebounceMs by viewModel.touchDebounceMs.collectAsState()
    val accumulatedThresholdMs by viewModel.accumulatedTapThresholdMs.collectAsState()
    val accumulatedTimeoutMs by viewModel.accumulatedTapTimeoutMs.collectAsState()
    val homeRowCount by viewModel.homeButtonRowCountForLayout.collectAsState()
    val callRowHeight = ScaledDimensions.homeContactRowInnerHeight(homeRowCount)

    SecondaryScreenIdleEffect(timeoutMs = INACTIVITY_TIMEOUT_MS, onTimeout = onBack) {
    ListScreenLayout(
        backgroundColor = PastelColors.lightBlue,
        title = "Missed calls",
        emptyMessage = "No missed calls",
        isEmpty = missedCallRows.isEmpty(),
        onBack = onBack,
        activationPreset = buttonActivation,
        debounceMs = touchDebounceMs,
        accumulatedThresholdMs = accumulatedThresholdMs,
        accumulatedTimeoutMs = accumulatedTimeoutMs
    ) {
        missedCallRows.forEach { row ->
            val call = row.call
            MissedCallListItem(
                contact = row.contact,
                mainLabel = call.contactName ?: call.phoneNumber,
                timestamp = call.timestamp,
                callRowHeight = callRowHeight,
                textAlignment = listTextAlignment,
                activationPreset = buttonActivation,
                debounceMs = touchDebounceMs,
                accumulatedThresholdMs = accumulatedThresholdMs,
                accumulatedTimeoutMs = accumulatedTimeoutMs,
                onCall = {
                    val label = call.contactName ?: call.phoneNumber
                    onCallContact(label, call.phoneNumber)
                },
                onAddToDeviceContacts = if (row.contact == null && call.type != CallType.BLOCKED) {
                    { onAddToContacts(call.phoneNumber) }
                } else {
                    null
                },
                onAddBlockedToApp = if (call.contactId == null && call.type == CallType.BLOCKED) {
                    { onAddBlockedToApp(call.phoneNumber, call.contactName) }
                } else {
                    null
                }
            )
        }
    }
    }
}

@Composable
private fun MissedCallListItem(
    contact: Contact?,
    mainLabel: String,
    timestamp: Long,
    callRowHeight: Dp,
    textAlignment: ListTextAlignment,
    activationPreset: ButtonActivationPreset,
    debounceMs: Int,
    accumulatedThresholdMs: Int,
    accumulatedTimeoutMs: Int,
    onCall: () -> Unit,
    onAddToDeviceContacts: (() -> Unit)?,
    onAddBlockedToApp: (() -> Unit)?
) {
    val nameMaxSize = ScaledDimensions.contactNameTextSize
    val timeText = formatRelativeTimeMissed(timestamp)
    val timeFontSp = nameMaxSize.value * 0.72f
    val rowColors = missedCallRowColors(contact)

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
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(callRowHeight)
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
            color = rowColors.background,
            shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = columnAlignment
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = boxAlignment
                ) {
                    FittedLabelInBox(
                        text = mainLabel,
                        color = rowColors.onBackground,
                        textAlign = textAlign,
                        maxFontSize = nameMaxSize,
                        modifier = Modifier.fillMaxSize(),
                        maxLines = 2,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = timeText,
                    style = TextStyle(
                        fontSize = timeFontSp.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = timeFontSp.sp
                    ),
                    color = rowColors.onBackground.copy(alpha = 0.88f),
                    textAlign = textAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (onAddBlockedToApp != null) {
            val addBlockedInteraction = remember { MutableInteractionSource() }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(callRowHeight)
                    .padding(bottom = 8.dp)
                    .shadow(WandasDimensions.ElevationMedium, RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
                    .clip(RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
                    .indication(addBlockedInteraction, rememberRipple())
                    .activationGesture(
                        preset = activationPreset,
                        debounceMs = debounceMs,
                        accumulatedThresholdMs = accumulatedThresholdMs,
                        accumulatedTimeoutMs = accumulatedTimeoutMs,
                        onActivate = onAddBlockedToApp,
                        interactionSource = addBlockedInteraction
                    ),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = boxAlignment
                ) {
                    FittedLabelInBox(
                        text = "Add to Contacts or Assistants (in this app)",
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = textAlign,
                        maxFontSize = nameMaxSize,
                        modifier = Modifier.fillMaxSize(),
                        maxLines = 2,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        if (onAddToDeviceContacts != null) {
            val addInteraction = remember { MutableInteractionSource() }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(callRowHeight)
                    .padding(bottom = 8.dp)
                    .shadow(WandasDimensions.ElevationMedium, RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
                    .clip(RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
                    .indication(addInteraction, rememberRipple())
                    .activationGesture(
                        preset = activationPreset,
                        debounceMs = debounceMs,
                        accumulatedThresholdMs = accumulatedThresholdMs,
                        accumulatedTimeoutMs = accumulatedTimeoutMs,
                        onActivate = onAddToDeviceContacts,
                        interactionSource = addInteraction
                    ),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = boxAlignment
                ) {
                    FittedLabelInBox(
                        text = "Add to phone contacts",
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = textAlign,
                        maxFontSize = nameMaxSize,
                        modifier = Modifier.fillMaxSize(),
                        maxLines = 2,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private data class MissedRowColors(val background: Color, val onBackground: Color)

@Composable
private fun missedCallRowColors(contact: Contact?): MissedRowColors {
    val w = MaterialTheme.wandasColors
    return if (contact != null) {
        val bg = contact.buttonColor?.let { Color(it) } ?: w.primaryButton
        val on = if (contact.buttonColor != null) Color.White else w.onPrimaryButton
        MissedRowColors(bg, on)
    } else {
        MissedRowColors(UnknownAllowedRowBlue, Color.White)
    }
}

private fun formatRelativeTimeMissed(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        days < 2 -> {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            "Yesterday ${timeFormat.format(Date(timestamp))}"
        }
        days < 7 -> "$days days ago"
        else -> {
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}
