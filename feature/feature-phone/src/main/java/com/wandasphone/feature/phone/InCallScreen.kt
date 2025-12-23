package com.wandasphone.feature.phone

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.wandasphone.core.config.FeatureLevel
import com.wandasphone.core.telecom.CallState
import com.wandasphone.core.ui.components.HangUpButton
import com.wandasphone.core.ui.components.InertBorderLayout
import com.wandasphone.core.ui.components.LargeButton
import com.wandasphone.core.ui.theme.WandasDimensions
import com.wandasphone.core.ui.theme.WandasTextStyles
import com.wandasphone.core.ui.theme.wandasColors

/**
 * In-Call Screen
 * 
 * Level 1: Just shows who's calling and End Call button
 * Level 2+: Adds speaker, mute, volume controls
 */
@Composable
fun InCallScreen(
    onNavigateBack: () -> Unit,
    viewModel: InCallViewModel = hiltViewModel()
) {
    val currentCall by viewModel.currentCall.collectAsState()
    val contactName by viewModel.contactName.collectAsState()
    val featureLevel by viewModel.featureLevel.collectAsState()
    
    val call = currentCall
    
    // Auto-navigate back when call ends
    if (call == null || call.state == CallState.DISCONNECTED) {
        onNavigateBack()
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        InertBorderLayout {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WandasDimensions.SpacingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top spacer
                Spacer(modifier = Modifier.height(WandasDimensions.SpacingExtraLarge))
                
                // Middle: Call info
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Contact name or phone number
                    Text(
                        text = contactName ?: call?.phoneNumber ?: "Unknown",
                        style = WandasTextStyles.ContactName,
                        color = MaterialTheme.wandasColors.onBackground,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(WandasDimensions.SpacingMedium))
                    
                    // Call state
                    Text(
                        text = getCallStateText(call?.state),
                        style = WandasTextStyles.CallStatus,
                        color = MaterialTheme.wandasColors.onBackground,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Bottom: Controls
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingLarge)
                ) {
                    // Level 2+ controls
                    if (featureLevel.level >= FeatureLevel.BASIC.level && call?.state == CallState.ACTIVE) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingMedium)
                        ) {
                            LargeButton(
                                text = if (call.isSpeakerOn) "Speaker\nON" else "Speaker\nOFF",
                                onClick = { viewModel.onToggleSpeaker() },
                                modifier = Modifier.weight(1f),
                                backgroundColor = if (call.isSpeakerOn) {
                                    MaterialTheme.wandasColors.success
                                } else {
                                    MaterialTheme.wandasColors.secondaryButton
                                },
                                textColor = if (call.isSpeakerOn) {
                                    MaterialTheme.wandasColors.onSuccess
                                } else {
                                    MaterialTheme.wandasColors.onSecondaryButton
                                }
                            )
                            
                            LargeButton(
                                text = if (call.isMuted) "Muted" else "Mute",
                                onClick = { viewModel.onToggleMute() },
                                modifier = Modifier.weight(1f),
                                backgroundColor = if (call.isMuted) {
                                    MaterialTheme.wandasColors.warning
                                } else {
                                    MaterialTheme.wandasColors.secondaryButton
                                },
                                textColor = if (call.isMuted) {
                                    MaterialTheme.wandasColors.onWarning
                                } else {
                                    MaterialTheme.wandasColors.onSecondaryButton
                                }
                            )
                        }
                    }
                    
                    // End call button (always shown)
                    HangUpButton(
                        text = "End Call",
                        onClick = { viewModel.onEndCall() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(WandasDimensions.SpacingLarge))
            }
        }
    }
}

private fun getCallStateText(state: CallState?): String {
    return when (state) {
        CallState.DIALING -> "Calling..."
        CallState.RINGING -> "Incoming call"
        CallState.CONNECTING -> "Connecting..."
        CallState.ACTIVE -> "Connected"
        CallState.HOLDING -> "On hold"
        CallState.DISCONNECTING -> "Ending..."
        CallState.DISCONNECTED -> "Call ended"
        else -> ""
    }
}

