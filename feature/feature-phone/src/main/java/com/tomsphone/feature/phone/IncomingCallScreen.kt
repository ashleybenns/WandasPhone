package com.tomsphone.feature.phone

import android.media.AudioManager
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.data.model.ContactType
import com.tomsphone.core.data.repository.ContactRepository
import com.tomsphone.core.telecom.CallManager
import com.tomsphone.core.telecom.MissedCallNagManager
import com.tomsphone.core.telecom.RingtonePlayer
import com.tomsphone.core.tts.TTSScripts
import com.tomsphone.core.tts.WandasTTS
import com.tomsphone.core.ui.components.InertBorderLayout
import com.tomsphone.core.ui.theme.ScaledDimensions
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Incoming call screen - shown when phone is ringing
 * 
 * Layout matches HomeScreen:
 * - Status text at top (3 lines, 140dp, full width)
 * - Answer/Reject buttons in same position as contact buttons
 */
@Composable
fun IncomingCallScreen(
    onCallAnswered: () -> Unit,
    onCallRejected: () -> Unit,
    viewModel: IncomingCallViewModel = hiltViewModel()
) {
    val callerName by viewModel.callerName.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    
    // Status message - one line, matching HomeScreen format
    val displayName = callerName ?: "Unknown Caller"
    val statusMessage = "$displayName is calling"
    
    // Start ringtone when screen appears
    LaunchedEffect(Unit) {
        viewModel.startRingtone()
    }
    
    // Stop ringtone when screen disappears
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopRingtone()
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top: Status text box - SAME as HomeScreen (scaled)
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
                    color = MaterialTheme.wandasColors.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 3
                )
            }
            
            // Rest of screen has inert border for buttons - SAME as HomeScreen
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
                    // Top: Answer and Reject buttons - SAME position as HomeScreen contact buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingLarge),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Answer button - GREEN, large (same size as contact buttons, scaled)
                        Button(
                            onClick = {
                                viewModel.answerCall()
                                onCallAnswered()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ScaledDimensions.contactButtonHeight),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50), // Green
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge),
                            contentPadding = PaddingValues(WandasDimensions.SpacingLarge),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = WandasDimensions.ElevationMedium
                            )
                        ) {
                            Text(
                                text = "Answer",
                                style = TextStyle(
                                    fontSize = ScaledDimensions.contactNameTextSize,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        // Reject button - RED, same size as contact buttons, scaled
                        Button(
                            onClick = {
                                viewModel.rejectCall()
                                onCallRejected()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ScaledDimensions.contactButtonHeight),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F), // Red
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge),
                            contentPadding = PaddingValues(WandasDimensions.SpacingLarge),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = WandasDimensions.ElevationMedium
                            )
                        ) {
                            Text(
                                text = "Reject",
                                style = TextStyle(
                                    fontSize = ScaledDimensions.contactNameTextSize,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    // Spacer where Emergency button would be on HomeScreen
                    Spacer(modifier = Modifier.height(ScaledDimensions.emergencyButtonHeight))
                }
            }
        }
    }
}

/**
 * ViewModel for incoming call screen
 */
@HiltViewModel
class IncomingCallViewModel @Inject constructor(
    private val callManager: CallManager,
    private val tts: WandasTTS,
    private val ringtonePlayer: RingtonePlayer,
    private val contactRepository: ContactRepository,
    private val missedCallNagManager: MissedCallNagManager,
    private val settingsRepository: com.tomsphone.core.config.SettingsRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "IncomingCallVM"
    }
    
    private var ringtoneJob: Job? = null
    
    // Use incomingRingingCall (not currentCall) - this is the dedicated flow for ringing calls
    val phoneNumber: StateFlow<String?> = callManager.incomingRingingCall
        .map { it?.phoneNumber }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    // Caller name - try from call info first, then do our own lookup
    private val _callerName = MutableStateFlow<String?>(null)
    val callerName: StateFlow<String?> = _callerName.asStateFlow()
    
    init {
        // Monitor incomingRingingCall for contact name, with backup lookup
        viewModelScope.launch {
            callManager.incomingRingingCall.collect { callInfo ->
                if (callInfo?.contactName != null) {
                    _callerName.value = callInfo.contactName
                } else if (callInfo?.phoneNumber != null && _callerName.value == null) {
                    // Service didn't find contact - try our own lookup
                    val contact = contactRepository.getContactByPhone(callInfo.phoneNumber).first()
                    if (contact != null) {
                        _callerName.value = contact.name
                        Log.d(TAG, "Found contact via backup lookup: ${contact.name}")
                    }
                }
            }
        }
    }
    
    /**
     * Start ringtone and TTS announcement loop
     * 
     * Pattern:
     * 1. Play short_twobell_ringtone.mp3
     * 2. TTS: "[userName]"
     * 3. 500ms pause
     * 4. TTS: "That's your phone ringing."
     * 5. 500ms pause
     * 6. TTS: "[callerName] is calling" (if known)
     * 7. Repeat
     */
    fun startRingtone() {
        if (ringtoneJob?.isActive == true) return
        
        ringtoneJob = viewModelScope.launch {
            Log.d(TAG, "Starting ringtone loop")
            
            // Get userName from settings
            val userName = settingsRepository.getUserName().first()
            
            // Wait briefly for contact lookup to complete before first announcement
            delay(300)
            
            while (isActive) {
                try {
                    // 1. Play short two-bell ringtone sound
                    ringtonePlayer.playAndWait(RingtonePlayer.Ringtone.SHORT_TWOBELL)
                    
                    // 2. TTS: "[userName]"
                    tts.speakAndWait(userName)
                    
                    // 3. 500ms pause
                    delay(500)
                    
                    // 4. TTS: "That's your phone ringing."
                    tts.speakAndWait("That's your phone ringing.")
                    
                    // 5. 500ms pause
                    delay(500)
                    
                    // 6. Announce caller (if known)
                    val name = callerName.value
                    if (name != null) {
                        tts.speakAndWait("$name is calling.")
                        delay(500)
                    }
                    
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error in ringtone loop: ${e.message}")
                    delay(2000)
                }
            }
        }
    }
    
    /**
     * Stop ringtone and TTS
     */
    fun stopRingtone() {
        Log.d(TAG, "Stopping ringtone")
        ringtoneJob?.cancel()
        ringtoneJob = null
        ringtonePlayer.stop()
        tts.stop()
    }
    
    /**
     * Answer the incoming call
     */
    fun answerCall() {
        Log.d(TAG, "Answering call")
        stopRingtone()
        callManager.answerCall()
    }
    
    /**
     * Reject the incoming call
     */
    fun rejectCall() {
        Log.d(TAG, "Rejecting call")
        stopRingtone()
        callManager.endCall()
        
        // Check if this is a CARER contact - if so, trigger missed call nag
        viewModelScope.launch {
            try {
                val number = phoneNumber.value ?: return@launch
                val contact = contactRepository.getContactByPhone(number).first()
                
                if (contact?.contactType == ContactType.CARER) {
                    Log.d(TAG, "Rejected call from CARER ${contact.name} - triggering nag")
                    missedCallNagManager.onMissedCall(number, contact.name)
                } else {
                    Log.d(TAG, "Rejected call from non-carer - no nag needed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking contact type: ${e.message}")
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopRingtone()
    }
}
