package com.tomsphone.feature.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.config.HomeButtonConfig
import com.tomsphone.core.telecom.CallDirection
import com.tomsphone.core.telecom.CallState
import com.tomsphone.core.ui.components.CallingStateButton
import com.tomsphone.core.ui.components.ConfigurableButton
import com.tomsphone.core.ui.components.DisplayOffButton
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
    onNavigateToEmergencyConfirm: () -> Unit,
    batteryLevel: Int = 100,
    isLowBattery: Boolean = false,
    isCharging: Boolean = false,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val displayMessage by viewModel.displayMessage.collectAsState()
    val homeButtons by viewModel.homeButtons.collectAsState()
    val showCarerAccess by viewModel.showCarerAccess.collectAsState()
    val showEmergencyConfirm by viewModel.showEmergencyConfirm.collectAsState()
    val callingContact by viewModel.callingContact.collectAsState()
    val emergencyTestMode by viewModel.emergencyTestMode.collectAsState()
    val unknownCallsAllowed by viewModel.unknownCallsAllowed.collectAsState()
    val displayOffButtonEnabled by viewModel.displayOffButtonEnabled.collectAsState()
    val displayOffButtonActive by viewModel.displayOffButtonActive.collectAsState()
    val isDisplayOff by viewModel.isDisplayOff.collectAsState()
    
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
            // TOP GUTTER: Warning strip for carer alerts (battery, all callers)
            // Small, unobtrusive, doesn't affect button layout
            val hasWarnings = isLowBattery || (isCharging && batteryLevel < 100) || unknownCallsAllowed
            if (hasWarnings) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Battery warning (red) or charging (green)
                    if (isLowBattery) {
                        Surface(
                            color = Color(0xFFD32F2F),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "🔋 $batteryLevel%",
                                style = TextStyle(
                                    fontSize = ScaledDimensions.scaledSp(12f),
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isCharging && batteryLevel < 100) {
                        Surface(
                            color = Color(0xFF4CAF50),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "🔌 $batteryLevel%",
                                style = TextStyle(
                                    fontSize = ScaledDimensions.scaledSp(12f),
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    // Spacer between warnings
                    if ((isLowBattery || (isCharging && batteryLevel < 100)) && unknownCallsAllowed) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    // All callers allowed warning (blue)
                    if (unknownCallsAllowed) {
                        Surface(
                            color = Color(0xFF1976D2),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "📞 All callers",
                                style = TextStyle(
                                    fontSize = ScaledDimensions.scaledSp(12f),
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            
            // Status message box - always shows full 3 lines for user info
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
                        .padding(horizontal = ScaledDimensions.edgePadding)
                        .padding(top = 4.dp, bottom = ScaledDimensions.edgePadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // MIDDLE: Contact/Menu/Utility buttons - each row gets equal weight
                    // Count total button rows for weight distribution
                    val fullWidthContacts = contactButtons.filter { !it.isHalfWidth }
                    val halfWidthContacts = contactButtons.filter { it.isHalfWidth }
                    val halfWidthContactRows = (halfWidthContacts.size + 1) / 2
                    val menuButtonRows = (menuButtons.size + 1) / 2
                    val displayOffRows = if (displayOffButtonEnabled) 1 else 0
                    val totalRows = fullWidthContacts.size + halfWidthContactRows + menuButtonRows + displayOffRows
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (callingContact != null) {
                            // CALLING ANIMATION: Buttons fade to black in place
                            // Must use same row structure as normal mode so nothing moves
                            
                            // Contact button rows (same structure as normal mode)
                            fullWidthContacts.forEach { button ->
                                val isThisContactCalling = callingContact?.id == button.contactId
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isThisContactCalling) {
                                        CallingStateButton(
                                            contactName = button.name,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    // Other buttons: empty space preserves layout
                                }
                            }
                            
                            // Half-width contact button rows (preserve structure)
                            halfWidthContacts.chunked(2).forEach { pair ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Check if either button in pair is calling
                                    val callingButton = pair.find { it.contactId == callingContact?.id }
                                    if (callingButton != null) {
                                        CallingStateButton(
                                            contactName = callingButton.name,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                            
                            // Menu button rows (preserve structure)
                            menuButtons.chunked(2).forEach { _ ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Empty - menu buttons hidden during call
                                }
                            }
                            
                            // Display Off button row (preserve structure)
                            if (displayOffButtonEnabled) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Empty - Display Off hidden during call
                                }
                            }
                        } else {
                            // NORMAL MODE: Render contact buttons with equal weight per row
                            
                            // Full-width contact buttons - each gets equal weight
                            fullWidthContacts.forEach { button ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    RenderContactButton(
                                        button = button,
                                        onClick = { viewModel.onContactButtonTap(button) }
                                    )
                                }
                            }
                            
                            // Half-width contact buttons (paired) - each pair gets equal weight
                            halfWidthContacts.chunked(2).forEach { pair ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
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
                            }
                            
                            // Menu buttons (Level 2+) - each row gets equal weight
                            menuButtons.chunked(2).forEach { pair ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
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
                                        Column {
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
                            
                            // Display Off button (Level 2+) - always takes space when enabled
                            // Invisible during call/nag but reserves layout space
                            if (displayOffButtonEnabled) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Only show and enable when active (not during nag)
                                    if (displayOffButtonActive) {
                                        DisplayOffButton(
                                            onClick = { viewModel.onDisplayOffTap() }
                                        )
                                    }
                                    // Otherwise: empty box reserves space
                                }
                            }
                        }
                    }
                    
                    // BOTTOM: Emergency button - fixed at bottom, close to inert gutter
                    // Tap 3 times = emergency, Long press = carer settings
                    if (emergencyButton != null && callingContact == null) {
                        Spacer(modifier = Modifier.height(ScaledDimensions.buttonSpacing))
                        EmergencyButton(
                            text = if (emergencyTestMode) "${emergencyButton.label} (Test)" else emergencyButton.label,
                            subtitle = "Press 3 times",
                            onClick = { viewModel.onEmergencyButtonTap() },
                            onLongPress = { viewModel.onEmergencyButtonLongPress() },
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
    
    // Carer access (via long press on emergency or hidden tap)
    if (showCarerAccess) {
        viewModel.dismissCarerAccess()
        onNavigateToCarer()
    }
    
    // Emergency confirm navigation (after 3 taps)
    if (showEmergencyConfirm) {
        viewModel.dismissEmergencyConfirm()
        onNavigateToEmergencyConfirm()
    }
    
    // Display Off overlay - black screen, any touch wakes it
    if (isDisplayOff) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    viewModel.wakeDisplay()
                },
            contentAlignment = Alignment.Center
        ) {
            // Completely black - no text to avoid burn-in
        }
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
