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
import com.tomsphone.core.ui.components.activationGesture
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.config.ListTextAlignment
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.ui.components.ListScreenLayout
import com.tomsphone.core.ui.theme.PastelColors
import com.tomsphone.core.ui.theme.ScaledDimensions
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import kotlinx.coroutines.delay

private const val INACTIVITY_TIMEOUT_MS = 30_000L

/**
 * Contacts List Screen (Level 2+)
 * 
 * Shows list of all carer contacts for the user to call.
 * Tap a contact to call them.
 * 
 * Design: Matches home screen with pastel yellow background,
 * inert gutters, and home-style call buttons.
 * This allows calling carers that don't fit on the home screen.
 */
@Composable
fun ContactsListScreen(
    onBack: () -> Unit,
    onCallContact: (String, String) -> Unit, // (name, phoneNumber)
    viewModel: ContactsListViewModel = hiltViewModel()
) {
    val contacts by viewModel.contacts.collectAsState()
    val screenTitle by viewModel.screenTitle.collectAsState()
    val emptyMessage by viewModel.emptyMessage.collectAsState()
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
        backgroundColor = PastelColors.lightYellow,
        title = screenTitle,
        emptyMessage = emptyMessage,
        isEmpty = contacts.isEmpty(),
        onBack = onBack,
        activationPreset = buttonActivation,
        debounceMs = touchDebounceMs,
        accumulatedThresholdMs = accumulatedThresholdMs,
        accumulatedTimeoutMs = accumulatedTimeoutMs
    ) {
        contacts.forEach { contact ->
            ContactButton(
                contact = contact,
                textAlignment = listTextAlignment,
                activationPreset = buttonActivation,
                debounceMs = touchDebounceMs,
                accumulatedThresholdMs = accumulatedThresholdMs,
                accumulatedTimeoutMs = accumulatedTimeoutMs,
                onClick = {
                    onCallContact(contact.name, contact.phoneNumber)
                }
            )
        }
    }
}

/**
 * Contact button - matches home screen call button style
 * Uses custom activation gesture for consistent touch response
 */
@Composable
private fun ContactButton(
    contact: Contact,
    textAlignment: ListTextAlignment,
    activationPreset: ButtonActivationPreset,
    debounceMs: Int,
    accumulatedThresholdMs: Int,
    accumulatedTimeoutMs: Int,
    onClick: () -> Unit
) {
    val textSize = ScaledDimensions.buttonTextSize
    val buttonColor = contact.buttonColor?.let { Color(it) }
        ?: MaterialTheme.wandasColors.primaryButton
    
    // Convert setting to Compose alignment
    val alignment = when (textAlignment) {
        ListTextAlignment.LEFT -> Alignment.CenterStart
        ListTextAlignment.CENTER -> Alignment.Center
    }
    val textAlign = when (textAlignment) {
        ListTextAlignment.LEFT -> TextAlign.Start
        ListTextAlignment.CENTER -> TextAlign.Center
    }
    
    // Interaction source for ripple effect
    val interactionSource = remember { MutableInteractionSource() }
    
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
        color = buttonColor,
        shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = alignment
        ) {
            Text(
                text = contact.name,
                style = TextStyle(
                    fontSize = textSize,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.wandasColors.onPrimaryButton,
                textAlign = textAlign
            )
        }
    }
}
