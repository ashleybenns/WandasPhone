package com.tomsphone.feature.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.telecom.CallDirection
import com.tomsphone.core.telecom.CallManager
import com.tomsphone.core.telecom.CallState
import com.tomsphone.core.ui.components.CallingStateButton
import com.tomsphone.core.ui.components.ContactButton
import com.tomsphone.core.ui.components.EmergencyButton
import com.tomsphone.core.ui.components.InertBorderLayout
import com.tomsphone.core.ui.components.StatusMessageBox
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors

/**
 * Level 1 Home Screen - STANDBY STATE ONLY
 * 
 * Shows:
 * - Status message box at top
 * - Contact buttons (tap to call)
 * - Emergency button
 * - Brief calling animation (1 second black button)
 * 
 * End call UI is on separate screens.
 * 
 * IMPORTANT: To prevent "standby flash" during outgoing calls,
 * we check BOTH callingContact AND currentCall. If there's an
 * active outgoing call, we show the calling UI even if callingContact
 * gets cleared due to timing issues.
 */
@Composable
fun HomeScreen(
    onNavigateToCarer: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    callManager: CallManager? = null // Optional for direct call state observation
) {
    val displayMessage by viewModel.displayMessage.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val showCarerAccess by viewModel.showCarerAccess.collectAsState()
    val callingContact by viewModel.callingContact.collectAsState()
    
    // Also observe currentCall directly to prevent standby flash
    val currentCall by viewModel.currentCallForUI.collectAsState()
    
    // Show calling UI if:
    // 1. callingContact is set (user just tapped, animation in progress), OR
    // 2. There's an active outgoing call (prevents flash while navigating to yellow screen)
    val hasActiveOutgoingCall = currentCall?.let { call ->
        call.direction == CallDirection.OUTGOING &&
        (call.state == CallState.DIALING || call.state == CallState.RINGING || 
         call.state == CallState.CONNECTING || call.state == CallState.ACTIVE)
    } ?: false
    
    val isCallingMode = callingContact != null || hasActiveOutgoingCall
    val callingContactName = callingContact?.name ?: currentCall?.contactName ?: "..."
    
    Log.d("HomeScreen", "COMPOSE: callingContact=${callingContact?.name}, hasActiveOutgoing=$hasActiveOutgoingCall, isCallingMode=$isCallingMode")
    
    // When HomeScreen becomes visible and there's no active call, clear stale calling state
    // This handles returning from yellow/green screen after call ends
    LaunchedEffect(hasActiveOutgoingCall) {
        if (!hasActiveOutgoingCall && callingContact == null) {
            // No active call and no animation - this is normal standby
        } else if (!hasActiveOutgoingCall) {
            // Animation was set but no active call - clear it
            viewModel.clearCallingStateIfNoCall()
        }
    }
    
    // If there's an active outgoing call, show a black screen
    // This prevents the standby flash while MainActivity navigates to yellow screen
    if (hasActiveOutgoingCall && callingContact == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // Just show black - navigation to yellow will happen momentarily
        }
        return
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top: Status message box - FULL WIDTH
            StatusMessageBox(
                message = displayMessage,
                onHiddenTap = { if (!isCallingMode) viewModel.onCarerButtonTap() },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Rest of screen has inert border for buttons
            InertBorderLayout(
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WandasDimensions.SpacingLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Middle: Contact buttons OR calling animation
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingLarge),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (callingContact != null) {
                            // CALLING ANIMATION: Brief black button (1 second)
                            contacts.take(2).forEachIndexed { index, contact ->
                                val isThisContactCalling = callingContact?.id == contact.id
                                
                                if (isThisContactCalling) {
                                    CallingStateButton(
                                        contactName = contact.name,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(WandasDimensions.ContactButtonHeight)
                                    )
                                }
                            }
                        } else {
                            // NORMAL MODE: Show contact buttons
                            contacts.take(2).forEach { contact ->
                                ContactButton(
                                    name = contact.name,
                                    phoneNumber = contact.phoneNumber,
                                    onClick = { viewModel.onContactTap(contact) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    
                    // Bottom: Emergency button - HIDE when in calling animation
                    if (callingContact == null) {
                        EmergencyButton(
                            text = "Emergency",
                            onClick = { viewModel.onEmergencyButtonTap() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Spacer(modifier = Modifier.height(WandasDimensions.EmergencyButtonHeight))
                    }
                }
            }
        }
    }
    
    // Carer access dialog
    if (showCarerAccess) {
        viewModel.dismissCarerAccess()
        onNavigateToCarer()
    }
}
