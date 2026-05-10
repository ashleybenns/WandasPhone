package com.tomsphone.feature.phone

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.tomsphone.core.config.ButtonActivationPreset
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
 * End Incoming Call Screen - WHITE background
 * 
 * Shown when an incoming call has been ANSWERED.
 * 
 * Status matches Home scale; action bar text uses device-fit typography ([rememberEndCallFixedActionTypography]).
 */
@Composable
fun EndIncomingCallScreen(
    onCallEnded: () -> Unit,
    viewModel: EndIncomingCallViewModel = hiltViewModel()
) {
    val callState by viewModel.callState.collectAsState()
    val callerName by viewModel.callerName.collectAsState()
    val confirmPending by viewModel.confirmPending.collectAsState()
    val showSpeakerToggle by viewModel.showSpeakerToggle.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val speakerConfirmPending by viewModel.speakerConfirmPending.collectAsState()
    val wasAutoAnswered by viewModel.wasAutoAnswered.collectAsState()
    
    // Touch response settings
    val buttonActivation by viewModel.buttonActivation.collectAsState()
    val touchDebounceMs by viewModel.touchDebounceMs.collectAsState()
    val accumulatedThresholdMs by viewModel.accumulatedTapThresholdMs.collectAsState()
    val accumulatedTimeoutMs by viewModel.accumulatedTapTimeoutMs.collectAsState()
    
    // Status message - ONE LINE
    val displayName = callerName ?: "Caller"
    val statusMessage = "On call with $displayName"
    
    // Watch for call ending
    LaunchedEffect(callState) {
        if (callState == CallState.IDLE || callState == CallState.DISCONNECTED) {
            Log.d("EndIncomingCall", "Call ended, navigating back")
            onCallEnded()
        }
    }
    
    // White background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
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
                    
                    // Auto-answered indicator
                    if (wasAutoAnswered) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "(Auto-answered)",
                            style = TextStyle(
                                fontSize = ScaledDimensions.statusTextSize * 0.7f,
                                fontWeight = FontWeight.Normal
                            ),
                            color = Color.Black.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            EndCallScreenActionArea(
                modifier = Modifier.weight(1f),
                showSpeakerToggle = showSpeakerToggle,
                confirmPending = confirmPending,
                isSpeakerOn = isSpeakerOn,
                speakerConfirmPending = speakerConfirmPending,
                buttonActivation = buttonActivation,
                touchDebounceMs = touchDebounceMs,
                accumulatedThresholdMs = accumulatedThresholdMs,
                accumulatedTimeoutMs = accumulatedTimeoutMs,
                onEndCallTap = { viewModel.onEndCallTap() },
                onSpeakerTap = { viewModel.onSpeakerTap() },
            )
        }
    }
}

@HiltViewModel
class EndIncomingCallViewModel @Inject constructor(
    private val callManager: CallManager,
    private val settingsRepository: com.tomsphone.core.config.SettingsRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "EndIncomingCallVM"
    }
    
    val callState: StateFlow<CallState> = callManager.currentCall
        .map { it?.state ?: CallState.IDLE }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CallState.IDLE)
    
    val callerName: StateFlow<String?> = callManager.currentCall
        .map { it?.contactName ?: it?.phoneNumber }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    
    // Speaker toggle visibility - based on new showSpeakerButton setting (Level 2+)
    val showSpeakerToggle: StateFlow<Boolean> = settingsRepository.getSettings()
        .map { it.showSpeakerButton }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    // Speaker state
    val isSpeakerOn: StateFlow<Boolean> = callManager.currentCall
        .map { it?.isSpeakerOn ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    
    // Auto-answer indicator
    val wasAutoAnswered: StateFlow<Boolean> = callManager.currentCall
        .map { it?.wasAutoAnswered ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    // Touch response settings
    val buttonActivation: StateFlow<ButtonActivationPreset> = settingsRepository.getSettings()
        .map { it.buttonActivation }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ButtonActivationPreset.ON_RELEASE)
    
    val touchDebounceMs: StateFlow<Int> = settingsRepository.getSettings()
        .map { it.touchDebounceMs }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 150)
    
    val accumulatedTapThresholdMs: StateFlow<Int> = settingsRepository.getSettings()
        .map { it.accumulatedTapThresholdMs }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 500)
    
    val accumulatedTapTimeoutMs: StateFlow<Int> = settingsRepository.getSettings()
        .map { it.accumulatedTapTimeoutMs }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3000)
    
    // End call double-tap
    private val _tapCount = MutableStateFlow(0)
    private var resetJob: Job? = null
    
    val confirmPending: StateFlow<Boolean> = _tapCount
        .map { it == 1 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    // Speaker double-tap
    private val _speakerTapCount = MutableStateFlow(0)
    private var speakerResetJob: Job? = null
    
    val speakerConfirmPending: StateFlow<Boolean> = _speakerTapCount
        .map { it == 1 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    init {
        // Reset speaker to default when call ends
        viewModelScope.launch {
            callState.collect { state ->
                if (state == CallState.DISCONNECTED || state == CallState.IDLE) {
                    Log.d(TAG, "Call ended - resetting speaker to default")
                    resetSpeakerToDefault()
                }
            }
        }
    }
    
    private fun resetSpeakerToDefault() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings().first()
            val shouldBeOn = settings.speakerDefaultOn
            val currentlyOn = callManager.currentCall.value?.isSpeakerOn ?: true
            if (currentlyOn != shouldBeOn) {
                callManager.setSpeaker(shouldBeOn)
                Log.d(TAG, "Speaker reset to default: $shouldBeOn")
            }
        }
    }
    
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
    
    fun onSpeakerTap() {
        _speakerTapCount.value++
        
        if (_speakerTapCount.value >= 2) {
            Log.d(TAG, "Speaker double tap confirmed - toggling")
            callManager.toggleSpeaker()
            _speakerTapCount.value = 0
            speakerResetJob?.cancel()
        } else {
            Log.d(TAG, "Speaker first tap - waiting for confirmation")
            speakerResetJob?.cancel()
            speakerResetJob = viewModelScope.launch {
                delay(3000)
                _speakerTapCount.value = 0
                Log.d(TAG, "Speaker tap reset after timeout")
            }
        }
    }
    
    @Deprecated("Use onSpeakerTap for double-tap protection")
    fun toggleSpeaker() {
        Log.d(TAG, "Toggling speaker (legacy)")
        callManager.toggleSpeaker()
    }
}
