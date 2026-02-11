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
 * Missed Calls List Screen (Level 2+)
 * 
 * Shows list of missed calls for the user to return.
 * Tap a contact to call them back.
 * 
 * Design: Matches home screen with pastel blue background,
 * inert gutters, and home-style call buttons.
 * Time text appears below each button.
 */
@Composable
fun MissedCallsListScreen(
    onBack: () -> Unit,
    onCallContact: (String, String) -> Unit, // (name, phoneNumber)
    viewModel: MissedCallsListViewModel = hiltViewModel()
) {
    val missedCalls by viewModel.missedCalls.collectAsState()
    val listTextAlignment by viewModel.listTextAlignment.collectAsState()
    val buttonActivation by viewModel.buttonActivation.collectAsState()
    val touchDebounceMs by viewModel.touchDebounceMs.collectAsState()
    val accumulatedThresholdMs by viewModel.accumulatedTapThresholdMs.collectAsState()
    val accumulatedTimeoutMs by viewModel.accumulatedTapTimeoutMs.collectAsState()
    
    // Auto-dismiss after 30 seconds of inactivity
    LaunchedEffect(Unit) {
        delay(INACTIVITY_TIMEOUT_MS)
        onBack()
    }
    
    ListScreenLayout(
        backgroundColor = PastelColors.lightBlue,
        title = "Missed Calls",
        emptyMessage = "No Missed Calls",
        isEmpty = missedCalls.isEmpty(),
        onBack = onBack,
        activationPreset = buttonActivation,
        debounceMs = touchDebounceMs,
        accumulatedThresholdMs = accumulatedThresholdMs,
        accumulatedTimeoutMs = accumulatedTimeoutMs
    ) {
        missedCalls.forEach { call ->
            MissedCallItem(
                contactName = call.contactName ?: call.phoneNumber,
                timestamp = call.timestamp,
                textAlignment = listTextAlignment,
                activationPreset = buttonActivation,
                debounceMs = touchDebounceMs,
                accumulatedThresholdMs = accumulatedThresholdMs,
                accumulatedTimeoutMs = accumulatedTimeoutMs,
                onClick = {
                    onCallContact(call.contactName ?: call.phoneNumber, call.phoneNumber)
                }
            )
        }
    }
}

/**
 * Missed call item - button with time text below
 * Button matches contacts list style exactly
 * Uses custom activation gesture for consistent touch response
 */
@Composable
private fun MissedCallItem(
    contactName: String,
    timestamp: Long,
    textAlignment: ListTextAlignment,
    activationPreset: ButtonActivationPreset,
    debounceMs: Int,
    accumulatedThresholdMs: Int,
    accumulatedTimeoutMs: Int,
    onClick: () -> Unit
) {
    val textSize = ScaledDimensions.buttonTextSize
    val timeText = formatRelativeTime(timestamp)
    
    // Convert setting to Compose alignment
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
    
    // Interaction source for ripple effect
    val interactionSource = remember { MutableInteractionSource() }
    
    Column(
        horizontalAlignment = columnAlignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Call button - same design as contacts list, with custom activation
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .shadow(WandasDimensions.ElevationMedium, RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
                .clip(RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
                .indication(interactionSource, rememberRipple())
                .activationGesture(
                    preset = activationPreset,
                    debounceMs = debounceMs,
                    accumulatedThresholdMs = accumulatedThresholdMs,
                    accumulatedTimeoutMs = accumulatedTimeoutMs,
                    onActivate = onClick,
                    interactionSource = interactionSource
                ),
            color = MaterialTheme.wandasColors.primaryButton,
            shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = boxAlignment
            ) {
                Text(
                    text = contactName,
                    style = TextStyle(
                        fontSize = textSize,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.wandasColors.onPrimaryButton,
                    textAlign = textAlign
                )
            }
        }
        
        // Time text below button - same size as button text, aligned with button text
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
    }
}

/**
 * Format timestamp as relative time
 * Returns: "X minutes ago", "X hours ago", "yesterday at HH:MM", "X days ago"
 */
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
