package com.tomsphone.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.config.FeatureLevel
import com.tomsphone.core.ui.components.CallingStateButton
import com.tomsphone.core.ui.components.ContactButton
import com.tomsphone.core.ui.components.EmergencyButton
import com.tomsphone.core.ui.components.EndCallButton
import com.tomsphone.core.ui.components.InertBorderLayout
import com.tomsphone.core.ui.components.StatusMessageBox
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.WandasTextStyles
import com.tomsphone.core.ui.theme.wandasColors

/**
 * Level 1 Home Screen
 * 
 * Features:
 * - Status message box at top (fixed 3-line height)
 *   - Default: "[User]'s phone"
 *   - Shows: "Calling [name]", "Missed call from [name]", etc.
 * - 2 carer contact buttons (tap to call)
 * - Emergency button (3-tap protection, dev mode: settings shortcut)
 * - Hidden carer access (7 taps on status message box)
 * - Inert border to prevent accidental touches
 * - Full screen (no status bar - cleaner interface)
 */
@Composable
fun HomeScreen(
    onNavigateToCall: () -> Unit,
    onNavigateToCarer: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val featureLevel by viewModel.featureLevel.collectAsState()
    val displayMessage by viewModel.displayMessage.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val showCarerAccess by viewModel.showCarerAccess.collectAsState()
    val callingContact by viewModel.callingContact.collectAsState()
    val showEndCallButton by viewModel.showEndCallButton.collectAsState()
    val endCallConfirmPending by viewModel.endCallConfirmPending.collectAsState()
    
    // Calling mode: when user taps a contact, that button fades to black
    // and all other buttons disappear
    val isCallingMode = callingContact != null
    
    // Clear any stale status when screen is shown
    LaunchedEffect(Unit) {
        viewModel.clearStatus()
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top: Status message box - FULL WIDTH (no inert border)
            // Hidden carer access via 7 taps (disabled during calling)
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
                    // Middle: Contact buttons OR in-call UI
                    // IMPORTANT: Maintain stable layout - buttons don't move
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingLarge),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    if (showEndCallButton) {
                        // END CALL MODE: Show end call button
                        // Works for both outgoing calls (after dialing) and incoming calls (after answering)
                        Spacer(modifier = Modifier.height(WandasDimensions.ContactButtonHeight))
                        
                        EndCallButton(
                            onClick = { viewModel.onEndCallTap() },
                            confirmPending = endCallConfirmPending
                        )
                        
                        Spacer(modifier = Modifier.height(WandasDimensions.ContactButtonHeight))
                    } else if (isCallingMode) {
                        // CALLING ANIMATION: Brief black button before end call appears (outgoing only)
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
                
                    // Bottom: Emergency button - HIDE when in call or calling
                    if (!isCallingMode && !showEndCallButton) {
                        EmergencyButton(
                            text = "Emergency",
                            onClick = { viewModel.onEmergencyButtonTap() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Keep space so layout doesn't jump
                        Spacer(modifier = Modifier.height(WandasDimensions.EmergencyButtonHeight))
                    }
                }
            }
        }
    }
    
    // Carer access dialog
    if (showCarerAccess) {
        // TODO: Show carer PIN dialog
        // For now just dismiss
        viewModel.dismissCarerAccess()
        onNavigateToCarer()
    }
}

