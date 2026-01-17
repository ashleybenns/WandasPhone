package com.tomsphone.feature.carer.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Contextual save feedback toast.
 * 
 * Shows brief confirmation of what was saved:
 * - "Ashley's phone saved"
 * - "Feature level saved"
 * - "User name saved"
 * 
 * Auto-dismisses after a short delay.
 */
@Composable
fun SaveToast(
    message: String?,
    modifier: Modifier = Modifier,
    durationMs: Long = 2000
) {
    var isVisible by remember { mutableStateOf(false) }
    var currentMessage by remember { mutableStateOf("") }
    
    // Show toast when message changes
    LaunchedEffect(message) {
        if (message != null) {
            currentMessage = message
            isVisible = true
            delay(durationMs)
            isVisible = false
        }
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF4CAF50),  // Green success
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Text(
                        text = currentMessage,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * State holder for SaveToast.
 * 
 * Usage:
 * ```
 * val saveToastState = rememberSaveToastState()
 * 
 * // When saving:
 * saveToastState.show("Ashley's phone saved")
 * 
 * // In UI:
 * SaveToast(message = saveToastState.message)
 * ```
 */
class SaveToastState {
    var message by mutableStateOf<String?>(null)
        private set
    
    fun show(text: String) {
        message = null  // Reset to trigger recomposition
        message = text
    }
    
    fun dismiss() {
        message = null
    }
}

@Composable
fun rememberSaveToastState(): SaveToastState {
    return remember { SaveToastState() }
}
