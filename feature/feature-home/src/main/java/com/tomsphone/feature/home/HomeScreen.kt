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
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.config.HomeButtonConfig
import com.tomsphone.core.config.ListTextAlignment
import com.tomsphone.core.telecom.CallDirection
import com.tomsphone.core.telecom.CallState
import com.tomsphone.core.ui.components.CallingStateButton
import com.tomsphone.core.ui.components.ConfigurableButton
import com.tomsphone.core.ui.components.DisplayOffButton
import com.tomsphone.core.ui.components.EmergencyButton
import com.tomsphone.core.ui.components.SettingsAccessButton
import com.tomsphone.core.ui.components.HalfWidthButtonRow
import com.tomsphone.core.ui.components.InertBorderLayout
import com.tomsphone.core.ui.components.ListButton
import com.tomsphone.core.ui.components.StatusMessageBox
import com.tomsphone.core.ui.theme.PastelColors
import com.tomsphone.core.ui.theme.ScaledDimensions
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
// #region agent log helper
private fun debugLog(location: String, hypothesisId: String, message: String, data: Map<String, Any?> = emptyMap()) {
    Log.d("DEBUG_NAV", "[$hypothesisId] $location: $message | $data")
}
// #endregion

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
    onNavigateToMissedCalls: () -> Unit = {},
    onNavigateToContactsList: () -> Unit = {},
    batteryLevel: Int = 100,
    isLowBattery: Boolean = false,
    isCharging: Boolean = false,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val displayMessage by viewModel.displayMessage.collectAsState()
    val homeButtons by viewModel.homeButtons.collectAsState()
    val showCarerAccess by viewModel.showCarerAccess.collectAsState()
    val showEmergencyConfirm by viewModel.showEmergencyConfirm.collectAsState()
    val showMissedCallsList by viewModel.showMissedCallsList.collectAsState()
    val showContactsList by viewModel.showContactsList.collectAsState()
    
    // #region agent log
    LaunchedEffect(Unit) { debugLog("HomeScreen.kt:72", "H2", "HomeScreen composed", mapOf("buttonCount" to homeButtons.size)) }
    DisposableEffect(Unit) {
        onDispose { debugLog("HomeScreen.kt:74", "H3", "HomeScreen disposed", emptyMap()) }
    }
    // #endregion
    val callingContact by viewModel.callingContact.collectAsState()
    val emergencyTestMode by viewModel.emergencyTestMode.collectAsState()
    val emergencyTapCount by viewModel.emergencyTapCount.collectAsState()
    val emergencyRequiredTaps by viewModel.emergencyRequiredTaps.collectAsState()
    val unknownCallsAllowed by viewModel.unknownCallsAllowed.collectAsState()
    val textAlignment by viewModel.listTextAlignment.collectAsState()
    val buttonActivation by viewModel.buttonActivation.collectAsState()
    val touchDebounceMs by viewModel.touchDebounceMs.collectAsState()
    val accumulatedThresholdMs by viewModel.accumulatedTapThresholdMs.collectAsState()
    val accumulatedTimeoutMs by viewModel.accumulatedTapTimeoutMs.collectAsState()
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
            // TOP GUTTER: Status strip showing battery and warnings
            // Always visible, small, unobtrusive, doesn't affect button layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Battery status - always visible
                // Color: red if low, green if charging, gray if normal
                val batteryColor = when {
                    isLowBattery -> Color(0xFFD32F2F)  // Red
                    isCharging -> Color(0xFF4CAF50)    // Green
                    else -> Color(0xFF757575)          // Gray
                }
                val batteryIcon = when {
                    isCharging -> "🔌"
                    isLowBattery -> "🔋"
                    batteryLevel >= 80 -> "🔋"
                    batteryLevel >= 40 -> "🔋"
                    else -> "🔋"
                }
                Surface(
                    color = batteryColor,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "$batteryIcon $batteryLevel%",
                        style = TextStyle(
                            fontSize = ScaledDimensions.scaledSp(12f),
                            fontWeight = if (isLowBattery) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                
                // All callers allowed warning (blue) - separate indicator
                if (unknownCallsAllowed) {
                    Spacer(modifier = Modifier.width(8.dp))
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
            
            // Status message box - always shows full 3 lines for user info
            StatusMessageBox(
                message = displayMessage,
                onHiddenTap = { if (!isCallingMode) viewModel.onCarerButtonTap() },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Rest of screen has inert border for buttons
            // No top border - status box is in the "dead zone" (not a button)
            InertBorderLayout(
                modifier = Modifier.weight(1f),
                topBorderWidth = 0.dp
            ) {
                // Separate buttons by type
                val contactButtons = homeButtons.filterIsInstance<HomeButtonConfig.ContactButton>()
                val missedCallReturnButton = homeButtons.filterIsInstance<HomeButtonConfig.MissedCallReturnButton>().firstOrNull()
                val menuButtons = homeButtons.filterIsInstance<HomeButtonConfig.MenuButton>()
                val emergencyButton = homeButtons.filterIsInstance<HomeButtonConfig.EmergencyButton>().firstOrNull()
                
                // Layout: Contact buttons in middle (distributed), Emergency fixed at bottom
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = ScaledDimensions.edgePadding)
                        .padding(bottom = ScaledDimensions.edgePadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // MIDDLE: Contact/Menu/Utility buttons - each row gets equal weight
                    // Count total button rows for weight distribution
                    val fullWidthContacts = contactButtons.filter { !it.isHalfWidth }
                    val halfWidthContacts = contactButtons.filter { it.isHalfWidth }
                    val halfWidthContactRows = (halfWidthContacts.size + 1) / 2
                    val missedCallReturnRows = if (missedCallReturnButton != null) 1 else 0
                    val fullWidthMenus = menuButtons.filter { !it.isHalfWidth }
                    val halfWidthMenus = menuButtons.filter { it.isHalfWidth }
                    val menuButtonRows = fullWidthMenus.size + (halfWidthMenus.size + 1) / 2
                    val displayOffRows = if (displayOffButtonEnabled) 1 else 0
                    val totalRows = fullWidthContacts.size + halfWidthContactRows + missedCallReturnRows + menuButtonRows + displayOffRows
                    
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
                            
                            // Missed Call Return button row (preserve structure)
                            // Show calling animation if this is the button being used to call
                            if (missedCallReturnButton != null) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Check if we're calling from this button (callingContact id would be 0)
                                    val isCallingFromMissedReturn = callingContact?.id == 0L && 
                                        callingContact?.phoneNumber == missedCallReturnButton.phoneNumber
                                    if (isCallingFromMissedReturn) {
                                        CallingStateButton(
                                            contactName = missedCallReturnButton.label,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                            
                            // Menu button rows (preserve structure for layout consistency)
                            // Full-width menu buttons - each needs a row
                            val callingFullWidthMenus = menuButtons.filter { !it.isHalfWidth }
                            val callingHalfWidthMenus = menuButtons.filter { it.isHalfWidth }
                            
                            callingFullWidthMenus.forEach { _ ->
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
                            
                            // Half-width menu buttons - paired rows
                            callingHalfWidthMenus.chunked(2).forEach { _ ->
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
                                        textAlignment = textAlignment,
                                        activationPreset = buttonActivation,
                                        debounceMs = touchDebounceMs,
                                        accumulatedThresholdMs = accumulatedThresholdMs,
                                        accumulatedTimeoutMs = accumulatedTimeoutMs,
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
                                                    textAlignment = textAlignment,
                                                    activationPreset = buttonActivation,
                                                    debounceMs = touchDebounceMs,
                                                    accumulatedThresholdMs = accumulatedThresholdMs,
                                                    accumulatedTimeoutMs = accumulatedTimeoutMs,
                                                    onClick = { viewModel.onContactButtonTap(pair[0]) },
                                                    modifier = modifier
                                                )
                                            },
                                            rightButton = { modifier ->
                                                RenderContactButton(
                                                    button = pair[1],
                                                    textAlignment = textAlignment,
                                                    activationPreset = buttonActivation,
                                                    debounceMs = touchDebounceMs,
                                                    accumulatedThresholdMs = accumulatedThresholdMs,
                                                    accumulatedTimeoutMs = accumulatedTimeoutMs,
                                                    onClick = { viewModel.onContactButtonTap(pair[1]) },
                                                    modifier = modifier
                                                )
                                            }
                                        )
                                    } else {
                                        RenderContactButton(
                                            button = pair[0],
                                            textAlignment = textAlignment,
                                            activationPreset = buttonActivation,
                                            debounceMs = touchDebounceMs,
                                            accumulatedThresholdMs = accumulatedThresholdMs,
                                            accumulatedTimeoutMs = accumulatedTimeoutMs,
                                            onClick = { viewModel.onContactButtonTap(pair[0]) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                            
                            // Missed Call Return button (Level 1) - same styling as contact buttons
                            if (missedCallReturnButton != null) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    RenderMissedCallReturnButton(
                                        button = missedCallReturnButton,
                                        textAlignment = textAlignment,
                                        activationPreset = buttonActivation,
                                        debounceMs = touchDebounceMs,
                                        accumulatedThresholdMs = accumulatedThresholdMs,
                                        accumulatedTimeoutMs = accumulatedTimeoutMs,
                                        onClick = { viewModel.onMissedCallReturnButtonTap(missedCallReturnButton) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            
                            // Menu buttons (Level 2+) - separate full-width and half-width
                            val fullWidthMenuButtons = menuButtons.filter { !it.isHalfWidth }
                            val halfWidthMenuButtons = menuButtons.filter { it.isHalfWidth }
                            
                            // Full-width menu buttons - each gets its own row
                            fullWidthMenuButtons.forEach { button ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    RenderMenuButton(
                                        button = button,
                                        textAlignment = textAlignment,
                                        activationPreset = buttonActivation,
                                        debounceMs = touchDebounceMs,
                                        accumulatedThresholdMs = accumulatedThresholdMs,
                                        accumulatedTimeoutMs = accumulatedTimeoutMs,
                                        onClick = { viewModel.onMenuButtonTap(button) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            
                            // Half-width menu buttons - paired in rows
                            halfWidthMenuButtons.chunked(2).forEach { pair ->
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
                                                RenderMenuButton(
                                                    button = pair[0],
                                                    textAlignment = textAlignment,
                                                    activationPreset = buttonActivation,
                                                    debounceMs = touchDebounceMs,
                                                    accumulatedThresholdMs = accumulatedThresholdMs,
                                                    accumulatedTimeoutMs = accumulatedTimeoutMs,
                                                    onClick = { viewModel.onMenuButtonTap(pair[0]) },
                                                    modifier = modifier
                                                )
                                            },
                                            rightButton = { modifier ->
                                                RenderMenuButton(
                                                    button = pair[1],
                                                    textAlignment = textAlignment,
                                                    activationPreset = buttonActivation,
                                                    debounceMs = touchDebounceMs,
                                                    accumulatedThresholdMs = accumulatedThresholdMs,
                                                    accumulatedTimeoutMs = accumulatedTimeoutMs,
                                                    onClick = { viewModel.onMenuButtonTap(pair[1]) },
                                                    modifier = modifier
                                                )
                                            }
                                        )
                                    } else {
                                        RenderMenuButton(
                                            button = pair[0],
                                            textAlignment = textAlignment,
                                            activationPreset = buttonActivation,
                                            debounceMs = touchDebounceMs,
                                            accumulatedThresholdMs = accumulatedThresholdMs,
                                            accumulatedTimeoutMs = accumulatedTimeoutMs,
                                            onClick = { viewModel.onMenuButtonTap(pair[0]) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
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
                                            onClick = { viewModel.onDisplayOffTap() },
                                            activationPreset = buttonActivation,
                                            debounceMs = touchDebounceMs,
                                            accumulatedThresholdMs = accumulatedThresholdMs,
                                            accumulatedTimeoutMs = accumulatedTimeoutMs
                                        )
                                    }
                                    // Otherwise: empty box reserves space
                                }
                            }
                        }
                    }
                    
                    // BOTTOM: Emergency button + Settings access button
                    // Emergency: Tap 3 times = emergency
                    // Settings: Tap 7-10 times = carer settings access
                    if (emergencyButton != null && callingContact == null) {
                        Spacer(modifier = Modifier.height(ScaledDimensions.buttonSpacing))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Emergency button - takes remaining space
                            EmergencyButton(
                                text = if (emergencyTestMode) "${emergencyButton.label} (Test)" else emergencyButton.label,
                                subtitle = "Press $emergencyRequiredTaps times",
                                tapCount = emergencyTapCount,
                                requiredTaps = emergencyRequiredTaps,
                                onClick = { viewModel.onEmergencyButtonTap() },
                                activationPreset = buttonActivation,
                                debounceMs = touchDebounceMs,
                                accumulatedThresholdMs = accumulatedThresholdMs,
                                accumulatedTimeoutMs = accumulatedTimeoutMs,
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Settings access button - square, to the right
                            SettingsAccessButton(
                                onSettingsAccess = { viewModel.onSettingsAccessTap() },
                                activationPreset = buttonActivation,
                                debounceMs = touchDebounceMs,
                                accumulatedThresholdMs = accumulatedThresholdMs,
                                accumulatedTimeoutMs = accumulatedTimeoutMs
                            )
                        }
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
    
    // List screen navigation (Level 2+)
    if (showMissedCallsList) {
        viewModel.dismissMissedCallsList()
        onNavigateToMissedCalls()
    }
    
    if (showContactsList) {
        viewModel.dismissContactsList()
        onNavigateToContactsList()
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
    textAlignment: ListTextAlignment,
    activationPreset: ButtonActivationPreset,
    debounceMs: Int,
    accumulatedThresholdMs: Int,
    accumulatedTimeoutMs: Int,
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
        warningText = if (button.showAutoAnswerWarning) "Auto-Answer" else null,
        textAlignment = textAlignment,
        activationPreset = activationPreset,
        debounceMs = debounceMs,
        accumulatedThresholdMs = accumulatedThresholdMs,
        accumulatedTimeoutMs = accumulatedTimeoutMs
    )
}

/**
 * Render a Missed Call Return button (Level 1)
 * 
 * Styled like a contact button (same color scheme) since it's a call action.
 * Shows caller name if there's a missed call, or "No Missed Calls" if not.
 */
@Composable
private fun RenderMissedCallReturnButton(
    button: HomeButtonConfig.MissedCallReturnButton,
    textAlignment: ListTextAlignment,
    activationPreset: ButtonActivationPreset,
    debounceMs: Int,
    accumulatedThresholdMs: Int,
    accumulatedTimeoutMs: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConfigurableButton(
        label = button.label,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        // Use primary button colors (same as contact buttons)
        backgroundColor = MaterialTheme.wandasColors.primaryButton,
        textColor = MaterialTheme.wandasColors.onPrimaryButton,
        warningText = null,
        textAlignment = textAlignment,
        activationPreset = activationPreset,
        debounceMs = debounceMs,
        accumulatedThresholdMs = accumulatedThresholdMs,
        accumulatedTimeoutMs = accumulatedTimeoutMs
    )
}

/**
 * Render a menu/list button from HomeButtonConfig
 * 
 * Uses ListButton with pastel colors:
 * - Missed Calls: Light blue
 * - Contacts: Light yellow
 * - Other: Light gray (fallback)
 */
@Composable
private fun RenderMenuButton(
    button: HomeButtonConfig.MenuButton,
    textAlignment: ListTextAlignment,
    activationPreset: ButtonActivationPreset,
    debounceMs: Int,
    accumulatedThresholdMs: Int,
    accumulatedTimeoutMs: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Select pastel color based on button type
    val fillColor = when (button.id) {
        HomeButtonConfig.MenuButton.ID_MISSED_CALLS -> PastelColors.lightBlue
        HomeButtonConfig.MenuButton.ID_CONTACTS_LIST -> PastelColors.lightYellow
        else -> PastelColors.lightGray  // Fallback for future button types
    }
    
    ListButton(
        label = button.label,
        fillColor = fillColor,
        onClick = onClick,
        modifier = modifier,
        textAlignment = textAlignment,
        activationPreset = activationPreset,
        debounceMs = debounceMs,
        accumulatedThresholdMs = accumulatedThresholdMs,
        accumulatedTimeoutMs = accumulatedTimeoutMs
    )
}
