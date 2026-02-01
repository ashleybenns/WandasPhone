package com.tomsphone.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tomsphone.core.ui.theme.ScaledDimensions
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
 * 
 * @param text Main button text (e.g., "Emergency")
 * @param subtitle Optional subtitle (e.g., "Press 3 times")
 * @param onLongPress Optional long-press handler (e.g., for carer settings access)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmergencyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onLongPress: (() -> Unit)? = null
) {
    // Use scaled dimensions
    val buttonHeight = ScaledDimensions.emergencyButtonHeight
    val textSize = ScaledDimensions.buttonTextSize
    
    Surface(
        modifier = modifier
            .height(buttonHeight)
            .then(
                if (onLongPress != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongPress
                    )
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            ),
        color = MaterialTheme.wandasColors.emergencyButton,
        shape = RoundedCornerShape(WandasDimensions.CornerRadiusMedium),
        shadowElevation = WandasDimensions.ElevationLarge
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text(
                    text = text,
                    style = TextStyle(
                        fontSize = textSize,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.wandasColors.onEmergencyButton,
                    textAlign = TextAlign.Center
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = TextStyle(
                            fontSize = textSize * 0.7f,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.wandasColors.onEmergencyButton,
                        textAlign = TextAlign.Center
                    )
                }
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
 * Fades to black, shows "Calling [name]"
 * Uses scaled dimensions based on user text size setting
 */
@Composable
fun CallingStateButton(
    contactName: String,
    modifier: Modifier = Modifier
) {
    // Use scaled dimensions
    val buttonHeight = ScaledDimensions.contactButtonHeight
    val textSize = ScaledDimensions.contactNameTextSize
    
    val backgroundColor by animateColorAsState(
        targetValue = Color.Black,
        animationSpec = tween(durationMillis = 300),
        label = "calling_fade"
    )
    
    Box(
        modifier = modifier
            .height(buttonHeight)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = contactName,
            style = TextStyle(
                fontSize = textSize,
                fontWeight = FontWeight.SemiBold
            ),
            color = Color.White,
            textAlign = TextAlign.Center
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
 * 
 * Design: Outlined button with text inside, sized to text width
 * - Visually distinct from filled circular call buttons
 * - Smaller/less prominent to reduce accidental taps
 * - No icon, text-only for readability
 * 
 * LAYOUT: Fills parent container height (use weight(1f) on parent for equal distribution)
 * Hidden during calls and missed call nag
 */
@Composable
fun DisplayOffButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textSize = ScaledDimensions.buttonTextSize
    val borderColor = MaterialTheme.wandasColors.onBackground.copy(alpha = 0.6f)
    
    // Container fills parent, centers the outlined button
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        // Outlined button - sized to text width, proportional height
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .wrapContentWidth()
                .fillMaxHeight(0.6f),  // Take 60% of row height for proportional look
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(2.dp, borderColor),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.wandasColors.onBackground
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Screen Off",
                style = TextStyle(
                    fontSize = textSize,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
