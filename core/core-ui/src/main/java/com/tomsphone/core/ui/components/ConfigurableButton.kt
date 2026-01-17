package com.tomsphone.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * 
 * Used for contact buttons, menu buttons, etc.
 */
@Composable
fun ConfigurableButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.wandasColors.primaryButton,
    textColor: Color = MaterialTheme.wandasColors.onPrimaryButton,
    warningText: String? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(WandasDimensions.ContactButtonHeight),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.5f),
            disabledContentColor = textColor.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge),
        contentPadding = PaddingValues(
            start = WandasDimensions.SpacingLarge,
            end = WandasDimensions.SpacingLarge,
            top = if (warningText != null) WandasDimensions.SpacingSmall else WandasDimensions.SpacingLarge,
            bottom = if (warningText != null) WandasDimensions.SpacingSmall else WandasDimensions.SpacingLarge
        ),
        enabled = enabled,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = WandasDimensions.ElevationMedium
        )
    ) {
        if (warningText != null) {
            // Two-line layout: main label + warning at bottom
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxHeight()
            ) {
                // Main label
                Text(
                    text = label,
                    style = WandasTextStyles.ContactName,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Warning badge
                WarningBadge(
                    text = warningText,
                    modifier = Modifier.padding(horizontal = WandasDimensions.SpacingSmall)
                )
            }
        } else {
            // Single label, centered
            Text(
                text = label,
                style = WandasTextStyles.ContactName,
                color = textColor,
                textAlign = TextAlign.Center
            )
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
