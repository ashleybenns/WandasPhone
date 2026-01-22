package com.tomsphone.feature.phone

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.telecom.CallManager
import com.tomsphone.core.telecom.CallState
import com.tomsphone.core.ui.theme.ScaledDimensions
import com.tomsphone.core.ui.theme.WandasDimensions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * End Outgoing Call Screen - YELLOW background
 * 
 * Layout:
 * - Status text at top (same position as HomeScreen)
 * - Button area divided into TWO equal zones:
 *   - Top zone: End call button (always visible)
 *   - Bottom zone: Speaker toggle (may or may not be visible)
 * 
 * The end call button position is FIXED regardless of speaker toggle visibility.
 * This ensures familiarity - user always taps the same spot to end call.
 */
@Composable
fun EndOutgoingCallScreen(
    contactName: String,
    onCallEnded: () -> Unit,
    viewModel: EndOutgoingCallViewModel = hiltViewModel()
) {
    val callState by viewModel.callState.collectAsState()
    val confirmPending by viewModel.confirmPending.collectAsState()
    val showSpeakerToggle by viewModel.showSpeakerToggle.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    
    // Status message - ONE LINE like HomeScreen
    val statusMessage = when (callState) {
        CallState.ACTIVE -> "On call with $contactName"
        else -> "Calling $contactName"
    }
    
    // End call instruction - break at comma for readability
    val instructionText = if (confirmPending) {
        "Tap again to end"
    } else {
        "To end call,\npress twice"
    }
    
    // Watch for call ending
    LaunchedEffect(callState) {
        if (callState == CallState.IDLE || callState == CallState.DISCONNECTED) {
            Log.d("EndOutgoingCall", "Call ended, navigating back")
            onCallEnded()
        }
    }
    
    // Yellow background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFC107)) // Amber/Yellow
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Status text box - SAME as HomeScreen (top, scaled height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ScaledDimensions.statusBoxHeight)
                    .padding(horizontal = WandasDimensions.SpacingLarge),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusMessage,
                    style = TextStyle(
                        fontSize = ScaledDimensions.statusTextSize,
                        fontWeight = FontWeight.Medium,
                        lineHeight = ScaledDimensions.statusTextSize * 1.2f
                    ),
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 3
                )
            }
            
            // Button area - divided into TWO equal zones
            // This ensures end call button stays in same position
            // regardless of speaker toggle visibility
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = WandasDimensions.SpacingLarge)
            ) {
                // TOP ZONE: End call button (always in top half)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // End call instruction - scaled text
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ScaledDimensions.endCallInstructionHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = instructionText,
                                style = TextStyle(
                                    fontSize = ScaledDimensions.statusTextSize,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = ScaledDimensions.statusTextSize * 1.2f
                                ),
                                color = Color.Black.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(WandasDimensions.SpacingMedium))
                        
                        // End call button - round, red, scaled size
                        Button(
                            onClick = { viewModel.onEndCallTap() },
                            modifier = Modifier.size(ScaledDimensions.endCallButtonSize),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "End",
                                style = TextStyle(
                                    fontSize = ScaledDimensions.buttonTextSize,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
                
                // BOTTOM ZONE: Speaker toggle (always takes equal space)
                // Button is only visible if showSpeakerToggle is true
                // But space is ALWAYS reserved to keep end call in same position
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (showSpeakerToggle) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Speaker button - round, grey/green based on state, scaled
                            Button(
                                onClick = { viewModel.toggleSpeaker() },
                                modifier = Modifier.size(ScaledDimensions.endCallButtonSize),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSpeakerOn) Color(0xFF4CAF50) else Color(0xFF757575),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = if (isSpeakerOn) "Speaker\nON" else "Speaker\nOFF",
                                    style = TextStyle(
                                        fontSize = ScaledDimensions.scaledSp(18f),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    // If showSpeakerToggle is false, the Box is empty but still takes space
                }
            }
        }
    }
}

@HiltViewModel
class EndOutgoingCallViewModel @Inject constructor(
    private val callManager: CallManager,
    private val settingsRepository: com.tomsphone.core.config.SettingsRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "EndOutgoingCallVM"
    }
    
    val callState: StateFlow<CallState> = callManager.currentCall
        .map { it?.state ?: CallState.IDLE }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CallState.IDLE)
    
    // Speaker toggle visibility - based on feature level (Level 2+)
    val showSpeakerToggle: StateFlow<Boolean> = settingsRepository.getSettings()
        .map { it.featureLevel.level >= 2 && !it.speakerphoneAlwaysOn }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    // Speaker state
    val isSpeakerOn: StateFlow<Boolean> = callManager.currentCall
        .map { it?.isSpeakerOn ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    
    private val _tapCount = MutableStateFlow(0)
    private var resetJob: Job? = null
    
    val confirmPending: StateFlow<Boolean> = _tapCount
        .map { it == 1 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    fun onEndCallTap() {
        _tapCount.value++
        
        if (_tapCount.value >= 2) {
            Log.d(TAG, "Double tap confirmed - ending call")
            callManager.endCall()
            _tapCount.value = 0
            resetJob?.cancel()
        } else {
            Log.d(TAG, "First tap - waiting for confirmation")
            resetJob?.cancel()
            resetJob = viewModelScope.launch {
                delay(3000)
                _tapCount.value = 0
                Log.d(TAG, "Tap reset after timeout")
            }
        }
    }
    
    fun toggleSpeaker() {
        Log.d(TAG, "Toggling speaker")
        callManager.toggleSpeaker()
    }
}
