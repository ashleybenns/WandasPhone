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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.telecom.CallManager
import com.tomsphone.core.telecom.CallState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * End Outgoing Call Screen - YELLOW background
 * 
 * Shown when:
 * - User taps a contact button and the 1-second animation completes
 * - Stays visible while outgoing call is dialing, ringing, or active
 * 
 * Features:
 * - Yellow background for visual distinction
 * - Shows "Calling [name]" or "On call with [name]"
 * - Double-tap protected end call button
 */
@Composable
fun EndOutgoingCallScreen(
    contactName: String,
    onCallEnded: () -> Unit,
    viewModel: EndOutgoingCallViewModel = hiltViewModel()
) {
    val callState by viewModel.callState.collectAsState()
    val confirmPending by viewModel.confirmPending.collectAsState()
    
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
            .background(Color(0xFFFFC107)), // Amber/Yellow
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // Status text
            Text(
                text = when (callState) {
                    CallState.ACTIVE -> "On call with"
                    else -> "Calling"
                },
                fontSize = 24.sp,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = contactName,
                fontSize = 48.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // End call instruction
            Text(
                text = if (confirmPending) "Tap again to end" else "To end call, press twice",
                fontSize = 18.sp,
                color = Color.Black.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // End call button - round, red
            Button(
                onClick = { viewModel.onEndCallTap() },
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F), // Red
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "End",
                    fontSize = 24.sp
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@HiltViewModel
class EndOutgoingCallViewModel @Inject constructor(
    private val callManager: CallManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "EndOutgoingCallVM"
    }
    
    val callState: StateFlow<CallState> = callManager.currentCall
        .map { it?.state ?: CallState.IDLE }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CallState.IDLE)
    
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
}
