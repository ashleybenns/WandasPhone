package com.tomsphone.core.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.tomsphone.core.config.ButtonActivationPreset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Custom gesture detector that respects ButtonActivationPreset settings.
 * 
 * Handles three activation modes:
 * - ON_RELEASE: Standard tap - activates when finger lifts (with debounce)
 * - ON_PRESS: Activates after holding for minHoldMs (no release needed)
 * - ACCUMULATED_TAP: Multiple touches accumulate time - forgiving for motor impairments
 * 
 * @param preset The activation preset from settings
 * @param debounceMs Minimum touch duration to filter accidental brushes
 * @param accumulatedThresholdMs For ACCUMULATED_TAP: total touch time needed to activate
 * @param accumulatedTimeoutMs For ACCUMULATED_TAP: time after first touch before counter resets
 * @param onActivate Called when the button should activate
 * @param interactionSource Optional interaction source for ripple effects
 */
@Composable
fun Modifier.activationGesture(
    preset: ButtonActivationPreset,
    debounceMs: Int = 150,
    accumulatedThresholdMs: Int = 500,
    accumulatedTimeoutMs: Int = 3000,
    onActivate: () -> Unit,
    interactionSource: MutableInteractionSource? = null
): Modifier {
    val coroutineScope = rememberCoroutineScope()
    
    // State for ON_PRESS mode
    var pressStartTime by remember { mutableLongStateOf(0L) }
    var hasActivatedOnPress by remember { mutableStateOf(false) }
    
    // State for ACCUMULATED_TAP mode
    // Track accumulated touch time for this specific button
    var accumulatedTouchTime by remember { mutableLongStateOf(0L) }
    var firstTouchTime by remember { mutableLongStateOf(0L) }
    var hasActivatedAccumulated by remember { mutableStateOf(false) }
    var timeoutJob by remember { mutableStateOf<Job?>(null) }
    
    return this.pointerInput(preset, debounceMs, accumulatedThresholdMs, accumulatedTimeoutMs) {
        detectTapGestures(
            onPress = { offset ->
                pressStartTime = System.currentTimeMillis()
                hasActivatedOnPress = false
                
                // Show press interaction for ripple
                val press = PressInteraction.Press(offset)
                interactionSource?.let {
                    coroutineScope.launch { it.emit(press) }
                }
                
                // For ON_PRESS mode: activate after holding for minHoldMs
                if (preset == ButtonActivationPreset.ON_PRESS) {
                    coroutineScope.launch {
                        delay(preset.minHoldMs.toLong())
                        val holdDuration = System.currentTimeMillis() - pressStartTime
                        if (holdDuration >= preset.minHoldMs && !hasActivatedOnPress) {
                            hasActivatedOnPress = true
                            onActivate()
                        }
                    }
                }
                
                // For ACCUMULATED_TAP mode: start tracking this touch
                if (preset == ButtonActivationPreset.ACCUMULATED_TAP && !hasActivatedAccumulated) {
                    // Cancel any pending timeout
                    timeoutJob?.cancel()
                    
                    // Record first touch time if this is starting fresh
                    if (accumulatedTouchTime == 0L) {
                        firstTouchTime = System.currentTimeMillis()
                    }
                    
                    // Check timeout - if too much time has passed since first touch, reset
                    val timeSinceFirstTouch = System.currentTimeMillis() - firstTouchTime
                    if (timeSinceFirstTouch > accumulatedTimeoutMs && accumulatedTouchTime > 0L) {
                        // Timeout expired, reset
                        accumulatedTouchTime = 0L
                        firstTouchTime = System.currentTimeMillis()
                    }
                }
                
                // Wait for release
                val released = tryAwaitRelease()
                
                // Release interaction for ripple
                interactionSource?.let {
                    coroutineScope.launch { it.emit(PressInteraction.Release(press)) }
                }
                
                val pressDuration = System.currentTimeMillis() - pressStartTime
                
                when (preset) {
                    ButtonActivationPreset.ON_RELEASE -> {
                        // Standard tap: activate on release if held long enough
                        if (released && pressDuration >= debounceMs) {
                            onActivate()
                        }
                    }
                    ButtonActivationPreset.ON_PRESS -> {
                        // Already handled above - activation happens during press
                        // Nothing to do on release
                    }
                    ButtonActivationPreset.ACCUMULATED_TAP -> {
                        // Add this touch duration to accumulated time
                        if (!hasActivatedAccumulated && pressDuration >= debounceMs) {
                            accumulatedTouchTime += pressDuration
                            
                            // Check if we've reached the threshold
                            if (accumulatedTouchTime >= accumulatedThresholdMs) {
                                hasActivatedAccumulated = true
                                onActivate()
                                // Reset for next activation
                                accumulatedTouchTime = 0L
                                hasActivatedAccumulated = false
                            } else {
                                // Start timeout to reset accumulated time if no more touches
                                timeoutJob = coroutineScope.launch {
                                    delay(accumulatedTimeoutMs.toLong())
                                    // Timeout expired without reaching threshold - reset
                                    accumulatedTouchTime = 0L
                                    firstTouchTime = 0L
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

/**
 * Simplified version that uses default values from preset
 */
@Composable
fun Modifier.activationGesture(
    preset: ButtonActivationPreset,
    onActivate: () -> Unit
): Modifier = activationGesture(
    preset = preset,
    debounceMs = preset.minHoldMs.coerceAtMost(150),
    accumulatedThresholdMs = preset.accumulatedTimeMs,
    accumulatedTimeoutMs = preset.accumulatedTimeoutMs,
    onActivate = onActivate,
    interactionSource = null
)
