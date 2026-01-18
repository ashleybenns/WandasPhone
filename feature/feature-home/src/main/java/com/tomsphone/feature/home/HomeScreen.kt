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
import com.tomsphone.core.config.HomeButtonConfig
import com.tomsphone.core.telecom.CallDirection
import com.tomsphone.core.telecom.CallState
import com.tomsphone.core.ui.components.CallingStateButton
import com.tomsphone.core.ui.components.ConfigurableButton
import com.tomsphone.core.ui.components.EmergencyButton
import com.tomsphone.core.ui.components.HalfWidthButtonRow
import com.tomsphone.core.ui.components.InertBorderLayout
import com.tomsphone.core.ui.components.StatusMessageBox
import com.tomsphone.core.ui.theme.ScaledDimensions
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors

/**
 * Home Screen - Data-driven button rendering
 * 
 * Shows:
 * - Status message box at top
 * - Configurable buttons (contact, menu, emergency)
 * - Brief calling animation (1 second black button)
 * 
 * Buttons are built from:
 * - Contact data (stored in Room DB)
 * - CarerSettings (stored in DataStore)
 * 
 * Each setting is individually addressable for remote sync and paywall gating.
 */
@Composable
fun HomeScreen(
    onNavigateToCarer: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val displayMessage by viewModel.displayMessage.collectAsState()
    val homeButtons by viewModel.homeButtons.collectAsState()
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
    
    Log.d("HomeScreen", "COMPOSE: callingContact=${callingContact?.name}, hasActiveOutgoing=$hasActiveOutgoingCall, isCallingMode=$isCallingMode, buttons=${homeButtons.size}")
    
    // When HomeScreen becomes visible and there's no active call, clear stale calling state
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
                // Separate buttons by type
                val contactButtons = homeButtons.filterIsInstance<HomeButtonConfig.ContactButton>()
                val menuButtons = homeButtons.filterIsInstance<HomeButtonConfig.MenuButton>()
                val emergencyButton = homeButtons.filterIsInstance<HomeButtonConfig.EmergencyButton>().firstOrNull()
                
                // Layout: Contact buttons in middle (distributed), Emergency fixed at bottom
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(ScaledDimensions.edgePadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // MIDDLE: Contact/Menu buttons - fill available space with even distribution
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (callingContact != null) {
                            // CALLING ANIMATION: Show black button for calling contact
                            contactButtons.forEach { button ->
                                val isThisContactCalling = callingContact?.id == button.contactId
                                
                                if (isThisContactCalling) {
                                    CallingStateButton(
                                        contactName = button.name,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(ScaledDimensions.contactButtonHeight)
                                    )
                                }
                            }
                        } else {
                            // NORMAL MODE: Render contact buttons
                            
                            // Full-width contact buttons
                            contactButtons.filter { !it.isHalfWidth }.forEach { button ->
                                RenderContactButton(
                                    button = button,
                                    onClick = { viewModel.onContactButtonTap(button) }
                                )
                            }
                            
                            // Half-width contact buttons (paired)
                            val halfWidthContacts = contactButtons.filter { it.isHalfWidth }
                            halfWidthContacts.chunked(2).forEach { pair ->
                                if (pair.size == 2) {
                                    HalfWidthButtonRow(
                                        leftButton = { modifier ->
                                            RenderContactButton(
                                                button = pair[0],
                                                onClick = { viewModel.onContactButtonTap(pair[0]) },
                                                modifier = modifier
                                            )
                                        },
                                        rightButton = { modifier ->
                                            RenderContactButton(
                                                button = pair[1],
                                                onClick = { viewModel.onContactButtonTap(pair[1]) },
                                                modifier = modifier
                                            )
                                        }
                                    )
                                } else {
                                    RenderContactButton(
                                        button = pair[0],
                                        onClick = { viewModel.onContactButtonTap(pair[0]) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            
                            // Menu buttons (Level 2+)
                            menuButtons.chunked(2).forEach { pair ->
                                if (pair.size == 2 && pair[0].isHalfWidth && pair[1].isHalfWidth) {
                                    HalfWidthButtonRow(
                                        leftButton = { modifier ->
                                            RenderMenuButton(
                                                button = pair[0],
                                                onClick = { viewModel.onMenuButtonTap(pair[0]) },
                                                modifier = modifier
                                            )
                                        },
                                        rightButton = { modifier ->
                                            RenderMenuButton(
                                                button = pair[1],
                                                onClick = { viewModel.onMenuButtonTap(pair[1]) },
                                                modifier = modifier
                                            )
                                        }
                                    )
                                } else {
                                    pair.forEach { button ->
                                        RenderMenuButton(
                                            button = button,
                                            onClick = { viewModel.onMenuButtonTap(button) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // BOTTOM: Emergency button - fixed at bottom, close to inert gutter
                    if (emergencyButton != null && callingContact == null) {
                        Spacer(modifier = Modifier.height(ScaledDimensions.buttonSpacing))
                        EmergencyButton(
                            text = emergencyButton.label,
                            onClick = { viewModel.onEmergencyButtonTap() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (emergencyButton != null) {
                        // Maintain layout space during calling animation
                        Spacer(modifier = Modifier.height(
                            ScaledDimensions.buttonSpacing + ScaledDimensions.emergencyButtonHeight
                        ))
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

/**
 * Render a contact button from HomeButtonConfig
 */
@Composable
private fun RenderContactButton(
    button: HomeButtonConfig.ContactButton,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConfigurableButton(
        label = button.name,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        backgroundColor = button.color?.let { Color(it) } 
            ?: MaterialTheme.wandasColors.primaryButton,
        textColor = MaterialTheme.wandasColors.onPrimaryButton,
        warningText = if (button.showAutoAnswerWarning) "Auto-Answer" else null
    )
}

/**
 * Render a menu button from HomeButtonConfig
 */
@Composable
private fun RenderMenuButton(
    button: HomeButtonConfig.MenuButton,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConfigurableButton(
        label = button.label,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        backgroundColor = button.color?.let { Color(it) } 
            ?: MaterialTheme.wandasColors.secondaryButton,
        textColor = MaterialTheme.wandasColors.onSecondaryButton
    )
}
