package com.tomsphone.feature.phone

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.telecom.CallDirection
import com.tomsphone.core.telecom.CallManager
import com.tomsphone.core.telecom.CallState
import com.tomsphone.core.ui.components.InertBorderLayout
import com.tomsphone.core.ui.components.activationGesture
import com.tomsphone.core.ui.theme.ScaledDimensions
import com.tomsphone.core.ui.theme.WandasDimensions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * End Outgoing Call Screen - WHITE background
 *
 * Layout matches [IncomingCallScreen]: status strip, then full-width rounded action buttons
 * ([ScaledDimensions.contactButtonHeight]) with two-line labels (title + Tap twice / Tap again).
 */
@Composable
fun EndOutgoingCallScreen(
    onCallEnded: () -> Unit,
    viewModel: EndOutgoingCallViewModel = hiltViewModel()
) {
    val callState by viewModel.callState.collectAsState()
    val callerLabel by viewModel.outgoingCallerLabel.collectAsState()
    val confirmPending by viewModel.confirmPending.collectAsState()
    val showSpeakerToggle by viewModel.showSpeakerToggle.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    
    // Touch response settings
    val buttonActivation by viewModel.buttonActivation.collectAsState()
    val touchDebounceMs by viewModel.touchDebounceMs.collectAsState()
    val accumulatedThresholdMs by viewModel.accumulatedTapThresholdMs.collectAsState()
    val accumulatedTimeoutMs by viewModel.accumulatedTapTimeoutMs.collectAsState()
    
    // Status message - ONE LINE like HomeScreen (label follows live CallInfo, not stale nav arg)
    val statusMessage = when (callState) {
        CallState.ACTIVE -> "On call with $callerLabel"
        else -> "Calling $callerLabel"
    }
    
    // Watch for call ending
    LaunchedEffect(callState) {
        if (callState == CallState.IDLE || callState == CallState.DISCONNECTED) {
            Log.d("EndOutgoingCall", "Call ended, navigating back")
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
            
            InertBorderLayout(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WandasDimensions.SpacingLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingLarge),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val endCallInteractionSource = remember { MutableInteractionSource() }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ScaledDimensions.contactButtonHeight)
                                .clip(RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
                                .indication(endCallInteractionSource, rememberRipple())
                                .activationGesture(
                                    preset = buttonActivation,
                                    debounceMs = touchDebounceMs,
                                    accumulatedThresholdMs = accumulatedThresholdMs,
                                    accumulatedTimeoutMs = accumulatedTimeoutMs,
                                    onActivate = { viewModel.onEndCallTap() },
                                    interactionSource = endCallInteractionSource
                                ),
                            shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge),
                            color = Color(0xFFD32F2F),
                            shadowElevation = WandasDimensions.ElevationMedium,
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "End call",
                                        style = TextStyle(
                                            fontSize = ScaledDimensions.contactNameTextSize,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = ScaledDimensions.contactNameTextSize * 1.1f,
                                            platformStyle = PlatformTextStyle(
                                                includeFontPadding = false
                                            ),
                                        ),
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                    )
                                    Text(
                                        text = if (confirmPending) "Tap again" else "Tap twice",
                                        style = TextStyle(
                                            fontSize = ScaledDimensions.statusTextSize,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = ScaledDimensions.statusTextSize * 1.2f,
                                            platformStyle = PlatformTextStyle(
                                                includeFontPadding = false
                                            ),
                                        ),
                                        color = if (confirmPending) {
                                            Color(0xFFFFEB3B)
                                        } else {
                                            Color.White.copy(alpha = 0.9f)
                                        },
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }

                        if (showSpeakerToggle) {
                            val speakerConfirmPending by viewModel.speakerConfirmPending.collectAsState()
                            val speakerInteractionSource = remember { MutableInteractionSource() }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(ScaledDimensions.contactButtonHeight)
                                    .clip(RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
                                    .indication(speakerInteractionSource, rememberRipple())
                                    .activationGesture(
                                        preset = buttonActivation,
                                        debounceMs = touchDebounceMs,
                                        accumulatedThresholdMs = accumulatedThresholdMs,
                                        accumulatedTimeoutMs = accumulatedTimeoutMs,
                                        onActivate = { viewModel.onSpeakerTap() },
                                        interactionSource = speakerInteractionSource
                                    ),
                                shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge),
                                color = Color(0xFF455A64),
                                shadowElevation = WandasDimensions.ElevationMedium,
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (isSpeakerOn) "Speaker on" else "Speaker off",
                                            style = TextStyle(
                                                fontSize = ScaledDimensions.contactNameTextSize,
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = ScaledDimensions.contactNameTextSize * 1.1f,
                                                platformStyle = PlatformTextStyle(
                                                    includeFontPadding = false
                                                ),
                                            ),
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                        )
                                        Text(
                                            text = if (speakerConfirmPending) "Tap again" else "Tap twice",
                                            style = TextStyle(
                                                fontSize = ScaledDimensions.statusTextSize,
                                                fontWeight = FontWeight.Medium,
                                                lineHeight = ScaledDimensions.statusTextSize * 1.2f,
                                                platformStyle = PlatformTextStyle(
                                                    includeFontPadding = false
                                                ),
                                            ),
                                            color = if (speakerConfirmPending) {
                                                Color(0xFFFFEB3B)
                                            } else {
                                                Color.White.copy(alpha = 0.9f)
                                            },
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(ScaledDimensions.emergencyButtonHeight))
                }
            }
        }
    }
}

@HiltViewModel
class EndOutgoingCallViewModel @Inject constructor(
    private val callManager: CallManager,
    private val settingsRepository: com.tomsphone.core.config.SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    companion object {
        private const val TAG = "EndOutgoingCallVM"
    }

    private val navArgFallback: String =
        savedStateHandle.get<String>("contactName").orEmpty().ifBlank { "Caller" }
    
    val callState: StateFlow<CallState> = callManager.currentCall
        .map { it?.state ?: CallState.IDLE }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CallState.IDLE)

    /**
     * Display name for status text — always from the current outgoing [CallInfo] when present
     * (contact name, else phone number). Avoids showing a previous call's name after emergency flow.
     */
    val outgoingCallerLabel: StateFlow<String> = callManager.currentCall
        .map { call ->
            if (call == null || call.direction != CallDirection.OUTGOING) return@map navArgFallback
            call.contactName?.takeIf { it.isNotBlank() }
                ?: call.phoneNumber.takeIf { it.isNotBlank() && it != "Unknown" }
                ?: navArgFallback
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), navArgFallback)
    
    // Speaker toggle visibility - based on new showSpeakerButton setting (Level 2+)
    val showSpeakerToggle: StateFlow<Boolean> = settingsRepository.getSettings()
        .map { it.showSpeakerButton }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    // Speaker state
    val isSpeakerOn: StateFlow<Boolean> = callManager.currentCall
        .map { it?.isSpeakerOn ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    
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
