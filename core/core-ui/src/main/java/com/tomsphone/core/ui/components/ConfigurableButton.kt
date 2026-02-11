package com.tomsphone.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.config.ListTextAlignment
import com.tomsphone.core.ui.theme.ScaledDimensions
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.WandasTextStyles
import com.tomsphone.core.ui.theme.wandasColors

/**
 * Unified configurable button for home screen.
 * 
 * Supports:
 * - Custom background/text colors
 * - Optional warning badge at bottom (e.g., "Auto-Answer")
 * - Full-width or half-width layouts
 * - Scales with user text size setting
 * 
 * LAYOUT: Button fills its parent container (use weight(1f) on parent for equal distribution).
 * Text size is from ScaledDimensions which adapts to screen size and button count.
 * 
 * Used for contact buttons, menu buttons, etc.
 */
/**
 * Configurable button with custom activation gesture support.
 * 
 * @param activationPreset How the button responds to touch (ON_RELEASE, ON_PRESS, DOUBLE_TAP)
 * @param debounceMs Minimum touch duration to filter accidental brushes
 */
@Composable
fun ConfigurableButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.wandasColors.primaryButton,
    textColor: Color = MaterialTheme.wandasColors.onPrimaryButton,
    warningText: String? = null,
    enabled: Boolean = true,
    textAlignment: ListTextAlignment = ListTextAlignment.CENTER,
    activationPreset: ButtonActivationPreset = ButtonActivationPreset.ON_RELEASE,
    debounceMs: Int = 150,
    accumulatedThresholdMs: Int = 500,
    accumulatedTimeoutMs: Int = 3000
) {
    // Text size adapts to screen height and button count
    val textSize = ScaledDimensions.contactNameTextSize
    
    // Convert setting to Compose alignment
    val boxAlignment = when (textAlignment) {
        ListTextAlignment.LEFT -> Alignment.CenterStart
        ListTextAlignment.CENTER -> Alignment.Center
    }
    val textAlign = when (textAlignment) {
        ListTextAlignment.LEFT -> TextAlign.Start
        ListTextAlignment.CENTER -> TextAlign.Center
    }
    
    // Interaction source for ripple effect
    val interactionSource = remember { MutableInteractionSource() }
    
    val actualBackgroundColor = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f)
    val actualTextColor = if (enabled) textColor else textColor.copy(alpha = 0.5f)
    
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .shadow(WandasDimensions.ElevationMedium, RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
            .clip(RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
            .indication(interactionSource, rememberRipple())
            .then(
                if (enabled) {
                    Modifier.activationGesture(
                        preset = activationPreset,
                        debounceMs = debounceMs,
                        accumulatedThresholdMs = accumulatedThresholdMs,
                        accumulatedTimeoutMs = accumulatedTimeoutMs,
                        onActivate = onClick,
                        interactionSource = interactionSource
                    )
                } else {
                    Modifier
                }
            ),
        color = actualBackgroundColor,
        shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = boxAlignment
        ) {
            if (warningText != null) {
                // Two-line layout: main label + warning at bottom
                Column(
                    horizontalAlignment = when (textAlignment) {
                        ListTextAlignment.LEFT -> Alignment.Start
                        ListTextAlignment.CENTER -> Alignment.CenterHorizontally
                    },
                    verticalArrangement = Arrangement.Center
                ) {
                    // Main label
                    Text(
                        text = label,
                        style = TextStyle(
                            fontSize = textSize,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = textSize  // Tight line height
                        ),
                        color = actualTextColor,
                        textAlign = textAlign
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Warning badge
                    WarningBadge(
                        text = warningText,
                        modifier = Modifier.padding(horizontal = WandasDimensions.SpacingSmall)
                    )
                }
            } else {
                // Single label
                Text(
                    text = label,
                    style = TextStyle(
                        fontSize = textSize,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = textSize  // Tight line height
                    ),
                    color = actualTextColor,
                    textAlign = textAlign
                )
            }
        }
    }
}

/**
 * Warning badge shown at bottom of button.
 * 
 * Used for:
 * - "Auto-Answer" warning
 * - Other important status indicators
 */
@Composable
fun WarningBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = Color(0xFFFFEB3B).copy(alpha = 0.9f), // Yellow warning
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            ),
            color = Color.Black,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Half-width button row for split layouts (Level 2+).
 * 
 * Shows two buttons side by side with equal width.
 */
@Composable
fun HalfWidthButtonRow(
    leftButton: @Composable (Modifier) -> Unit,
    rightButton: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingMedium)
    ) {
        leftButton(Modifier.weight(1f))
        rightButton(Modifier.weight(1f))
    }
}
