package com.tomsphone.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.config.ListTextAlignment
import com.tomsphone.core.ui.theme.ScaledDimensions
import com.tomsphone.core.ui.theme.PastelColors
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.WandasTextStyles
import com.tomsphone.core.ui.theme.wandasColors

/**
 * Large button component for WandasPhone
 */
@Composable
fun LargeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = WandasDimensions.ButtonHeightMedium,
    backgroundColor: Color = MaterialTheme.wandasColors.primaryButton,
    textColor: Color = MaterialTheme.wandasColors.onPrimaryButton,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.5f),
            disabledContentColor = textColor.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(WandasDimensions.CornerRadiusMedium),
        contentPadding = PaddingValues(
            horizontal = WandasDimensions.SpacingLarge,
            vertical = WandasDimensions.SpacingMedium
        ),
        enabled = enabled,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = WandasDimensions.ElevationMedium,
            pressedElevation = WandasDimensions.ElevationSmall
        )
    ) {
        Text(
            text = text,
            style = WandasTextStyles.ButtonLarge,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Contact button for home screen
 * Uses scaled dimensions based on user text size setting
 */
@Composable
fun ContactButton(
    name: String,
    phoneNumber: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.wandasColors.primaryButton,
    textColor: Color = MaterialTheme.wandasColors.onPrimaryButton
) {
    // Use scaled dimensions
    val buttonHeight = ScaledDimensions.contactButtonHeight
    val textSize = ScaledDimensions.contactNameTextSize
    
    Button(
        onClick = onClick,
        modifier = modifier.height(buttonHeight),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge),
        contentPadding = PaddingValues(WandasDimensions.SpacingLarge),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = WandasDimensions.ElevationMedium
        )
    ) {
        Text(
            text = name,
            style = TextStyle(
                fontSize = textSize,
                fontWeight = FontWeight.SemiBold
            ),
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Emergency button with distinct styling
 * Uses scaled dimensions based on user text size setting
 * Uses activation gesture for consistent touch response
 * 
 * Shows tap progress when user starts tapping (e.g., "1 / 3", "2 / 3")
 * 
 * @param text Main button text (e.g., "Emergency")
 * @param subtitle Optional subtitle (e.g., "Press 3 times") - shown when tapCount is 0
 * @param tapCount Current number of taps registered (0 = no taps yet)
 * @param requiredTaps Total taps required to activate (default 3)
 * @param activationPreset How the button responds to touch
 * @param debounceMs Minimum touch duration to filter accidental brushes
 */
@Composable
fun EmergencyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tapCount: Int = 0,
    requiredTaps: Int = 3,
    activationPreset: ButtonActivationPreset = ButtonActivationPreset.ON_RELEASE,
    debounceMs: Int = 150,
    accumulatedThresholdMs: Int = 500,
    accumulatedTimeoutMs: Int = 3000
) {
    // Use scaled dimensions
    val buttonHeight = ScaledDimensions.emergencyButtonHeight
    val textSize = ScaledDimensions.buttonTextSize
    
    // Interaction source for ripple effect
    val interactionSource = remember { MutableInteractionSource() }
    
    // Determine what to show as subtitle - progress when tapping, default otherwise
    val displaySubtitle = when {
        tapCount > 0 -> "$tapCount / $requiredTaps"
        else -> subtitle
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(buttonHeight)
            .clip(RoundedCornerShape(WandasDimensions.CornerRadiusMedium))
            .indication(interactionSource, rememberRipple())
            .activationGesture(
                preset = activationPreset,
                debounceMs = debounceMs,
                accumulatedThresholdMs = accumulatedThresholdMs,
                accumulatedTimeoutMs = accumulatedTimeoutMs,
                onActivate = onClick,
                interactionSource = interactionSource
            ),
        color = MaterialTheme.wandasColors.emergencyButton,
        shape = RoundedCornerShape(WandasDimensions.CornerRadiusMedium),
        shadowElevation = WandasDimensions.ElevationLarge
    ) {
        FittedEmergencyTwoLinesInBox(
            title = text,
            subtitleOrProgress = displaySubtitle,
            color = MaterialTheme.wandasColors.onEmergencyButton,
            maxFontSize = textSize,
            secondLineBold = tapCount > 0,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
        )
    }
}

/**
 * Settings access button - square button with settings icon
 * 
 * Requires 7-10 taps to activate (countdown from 7)
 * More than 10 taps cancels and resets
 * 
 * Design:
 * - Square (width = height = emergency button height)
 * - Solid grey background
 * - Settings gear icon
 * - Shows countdown number after first tap
 * - Rounded corners matching emergency button
 * 
 * @param onSettingsAccess Called when 7-10 taps are registered within timeout
 * @param activationPreset How each individual tap is recognized
 */
@Composable
fun SettingsAccessButton(
    onSettingsAccess: () -> Unit,
    modifier: Modifier = Modifier,
    activationPreset: ButtonActivationPreset = ButtonActivationPreset.ON_RELEASE,
    debounceMs: Int = 150,
    accumulatedThresholdMs: Int = 500,
    accumulatedTimeoutMs: Int = 3000
) {
    // Square button - size is emergency button height
    val buttonSize = ScaledDimensions.emergencyButtonHeight
    val iconSize = buttonSize * 0.4f
    val countdownTextSize = ScaledDimensions.buttonTextSize
    
    // Tap tracking state
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableIntStateOf(0) }
    
    // Reset tap count after 3 seconds of inactivity
    LaunchedEffect(tapCount) {
        if (tapCount > 0) {
            delay(3000)
            tapCount = 0
        }
    }
    
    // Handle tap result
    LaunchedEffect(tapCount) {
        if (tapCount >= 7 && tapCount <= 10) {
            // Wait a brief moment to see if more taps are coming
            delay(500)
            if (tapCount in 7..10) {
                onSettingsAccess()
                tapCount = 0
            }
        } else if (tapCount > 10) {
            // Too many taps - cancel
            tapCount = 0
        }
    }
    
    // Interaction source for ripple effect
    val interactionSource = remember { MutableInteractionSource() }
    
    // Calculate remaining taps needed (counting down from 7)
    val remainingTaps = (7 - tapCount).coerceAtLeast(0)
    val showCountdown = tapCount in 1..6
    
    Surface(
        modifier = modifier
            .size(buttonSize)
            .clip(RoundedCornerShape(WandasDimensions.CornerRadiusMedium))
            .indication(interactionSource, rememberRipple())
            .activationGesture(
                preset = activationPreset,
                debounceMs = debounceMs,
                accumulatedThresholdMs = accumulatedThresholdMs,
                accumulatedTimeoutMs = accumulatedTimeoutMs,
                onActivate = { tapCount++ },
                interactionSource = interactionSource
            ),
        color = Color(0xFF757575), // Grey
        shape = RoundedCornerShape(WandasDimensions.CornerRadiusMedium),
        shadowElevation = WandasDimensions.ElevationMedium
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (showCountdown) {
                // Show countdown number
                Text(
                    text = remainingTaps.toString(),
                    style = TextStyle(
                        fontSize = countdownTextSize,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            } else {
                // Show settings icon
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(iconSize),
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Hang up button with distinct styling
 */
@Composable
fun HangUpButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(WandasDimensions.ButtonHeightExtraLarge),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.wandasColors.hangUpButton,
            contentColor = MaterialTheme.wandasColors.onHangUpButton
        ),
        shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge),
        contentPadding = PaddingValues(WandasDimensions.SpacingExtraLarge),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = WandasDimensions.ElevationLarge
        )
    ) {
        Text(
            text = text,
            style = WandasTextStyles.ButtonLarge,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Button shown when a call is being placed (dialing)
 * Fades to black - text stays visible (white, same size and position)
 * Button does not change size or position - fillMaxHeight matches ConfigurableButton
 * Uses scaled dimensions based on user text size setting
 */
@Composable
fun CallingStateButton(
    contactName: String,
    modifier: Modifier = Modifier,
    textAlignment: ListTextAlignment = ListTextAlignment.CENTER,
    initialBackgroundColor: Color = MaterialTheme.wandasColors.primaryButton
) {
    val textSize = ScaledDimensions.contactNameTextSize
    var animateToBlack by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateToBlack = true }
    val backgroundColor by animateColorAsState(
        targetValue = if (animateToBlack) Color.Black else initialBackgroundColor,
        animationSpec = tween(300),
        label = "calling_fade"
    )

    val textAlign = when (textAlignment) {
        ListTextAlignment.LEFT -> TextAlign.Start
        ListTextAlignment.CENTER -> TextAlign.Center
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .shadow(WandasDimensions.ElevationMedium, RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
            .clip(RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge)
            )
    ) {
        // Same label fitting as [ConfigurableButton] so home standby and calling animation match.
        FittedLabelInBox(
            text = contactName,
            color = Color.White,
            textAlign = textAlign,
            maxFontSize = textSize,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            maxLines = LabelFitPolicy.DEFAULT_MAX_LINES,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * End call button with double-tap protection
 * 
 * - Red, round, smaller than contact buttons
 * - Label ABOVE the button for readability
 * - Requires 2 taps to end call (protection from accidental hangups)
 */
@Composable
fun EndCallButton(
    onClick: () -> Unit,
    confirmPending: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingMedium)
    ) {
        // Instruction label ABOVE the button
        Text(
            text = if (confirmPending) {
                "Tap again to end call"
            } else {
                "To end call, press twice"
            },
            style = WandasTextStyles.Instruction,
            color = if (confirmPending) Color.Red else Color.Black,
            textAlign = TextAlign.Center
        )
        
        // Round red button
        Button(
            onClick = onClick,
            modifier = Modifier.size(WandasDimensions.EndCallButtonSize),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red,
                contentColor = Color.White
            ),
            shape = CircleShape,
            contentPadding = PaddingValues(WandasDimensions.SpacingSmall),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = WandasDimensions.ElevationLarge
            )
        ) {
            Text(
                text = "End",
                style = WandasTextStyles.ButtonMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Screen Off button - turns screen off, any touch wakes it
 * Level 2+ feature for users who can't find the power button
 * Uses activation gesture for consistent touch response
 * 
 * Matches list button styling but with transparent fill:
 * - Outlined with transparent background
 * - Square corners (4dp radius)
 * - Black border and text
 * - Same text size and width as list buttons (12 chars)
 * 
 * LAYOUT: Fills parent container height (use weight(1f) on parent for equal distribution)
 * Hidden during calls and missed call nag
 */
@Composable
fun DisplayOffButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activationPreset: ButtonActivationPreset = ButtonActivationPreset.ON_RELEASE,
    debounceMs: Int = 150,
    accumulatedThresholdMs: Int = 500,
    accumulatedTimeoutMs: Int = 3000
) {
    val textSize = ScaledDimensions.buttonTextSize
    val borderColor = Color.Black
    
    // Calculate width for 12 characters (same as list buttons)
    val charWidth = textSize.value * 0.6f
    val buttonWidth = (charWidth * 12 + 40).dp
    
    // Interaction source for ripple effect
    val interactionSource = remember { MutableInteractionSource() }
    
    // Container fills parent, centers the button
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(buttonWidth)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(4.dp))
                .indication(interactionSource, rememberRipple(color = Color.White))
                .activationGesture(
                    preset = activationPreset,
                    debounceMs = debounceMs,
                    accumulatedThresholdMs = accumulatedThresholdMs,
                    accumulatedTimeoutMs = accumulatedTimeoutMs,
                    onActivate = onClick,
                    interactionSource = interactionSource
                ),
            shape = RoundedCornerShape(4.dp),
            color = Color.Black,  // Solid black - looks like an off screen
            border = BorderStroke(2.dp, Color.Black)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Screen Off",
                    style = TextStyle(
                        fontSize = textSize,
                        fontWeight = FontWeight.Bold,
                        lineHeight = textSize
                    ),
                    color = Color.White,  // White text on black
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * List Button - for navigation to list screens (Missed Calls, Contacts)
 * 
 * Distinct from call buttons:
 * - Outlined with pastel fill
 * - Square corners (4dp radius)
 * - Black border and text
 * - Same text size as call buttons for readability
 * - Full width of the row; label follows [LabelFitPolicy] (up to 2 lines, ellipsis, soft char cap).
 *
 * @param label Button text (long strings are clipped per [LabelFitPolicy.SOFT_MAX_DISPLAY_CHARS])
 * @param fillColor Pastel background color (use PastelColors.lightBlue, etc.)
 * @param onClick Action when tapped
 * @param textAlignment Left or center alignment for text
 * @param trailingContent Optional icon (etc.) to the right of [label]
 * @param activationPreset How the button responds to touch
 * @param debounceMs Minimum touch duration to filter accidental brushes
 */
@Composable
fun ListButton(
    label: String,
    fillColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textAlignment: ListTextAlignment = ListTextAlignment.CENTER,
    trailingContent: (@Composable () -> Unit)? = null,
    activationPreset: ButtonActivationPreset = ButtonActivationPreset.ON_RELEASE,
    debounceMs: Int = 150,
    accumulatedThresholdMs: Int = 500,
    accumulatedTimeoutMs: Int = 3000
) {
    val textSize = ScaledDimensions.buttonTextSize
    val borderColor = Color.Black

    val textAlign = when (textAlignment) {
        ListTextAlignment.LEFT -> TextAlign.Start
        ListTextAlignment.CENTER -> TextAlign.Center
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(4.dp))
                .indication(interactionSource, rememberRipple())
                .activationGesture(
                    preset = activationPreset,
                    debounceMs = debounceMs,
                    accumulatedThresholdMs = accumulatedThresholdMs,
                    accumulatedTimeoutMs = accumulatedTimeoutMs,
                    onActivate = onClick,
                    interactionSource = interactionSource
                ),
            shape = RoundedCornerShape(4.dp),
            color = fillColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            if (trailingContent == null) {
                FittedLabelInBox(
                    text = label,
                    color = Color.Black,
                    textAlign = textAlign,
                    maxFontSize = textSize,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    maxLines = LabelFitPolicy.DEFAULT_MAX_LINES,
                    fontWeight = FontWeight.Bold
                )
            } else {
                val rowArrangement = when (textAlignment) {
                    ListTextAlignment.LEFT -> Arrangement.Start
                    ListTextAlignment.CENTER -> Arrangement.Center
                }
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = rowArrangement
                ) {
                    FittedLabelInBox(
                        text = label,
                        color = Color.Black,
                        textAlign = if (textAlignment == ListTextAlignment.LEFT) TextAlign.Start else TextAlign.Center,
                        maxFontSize = textSize,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        maxLines = LabelFitPolicy.DEFAULT_MAX_LINES,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier.wrapContentWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        trailingContent()
                    }
                }
            }
        }
    }
}
