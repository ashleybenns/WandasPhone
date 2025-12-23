package com.wandasphone.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wandasphone.core.ui.theme.WandasDimensions
import com.wandasphone.core.ui.theme.WandasTextStyles
import com.wandasphone.core.ui.theme.wandasColors

/**
 * Large button component for WandasPhone
 * 
 * Features:
 * - Minimum 96dp touch target (exceeds accessibility requirements)
 * - High contrast colors from theme
 * - Large, readable text
 * - Generous padding to prevent accidental touches
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
        modifier = modifier
            .height(height),
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
 * Contact button for home screen - extra large, full width
 * 
 * Readability principles:
 * - Always full width to prevent text breaking mid-word
 * - Single line text, no wrapping
 * - Large, clear typography
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
    Button(
        onClick = onClick,
        modifier = modifier
            .height(WandasDimensions.ContactButtonHeight),
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
        // Single line, no word breaking - readability is critical
        Text(
            text = name,
            style = WandasTextStyles.ContactName,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Emergency button with distinct styling
 */
@Composable
fun EmergencyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(WandasDimensions.EmergencyButtonHeight),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.wandasColors.emergencyButton,
            contentColor = MaterialTheme.wandasColors.onEmergencyButton
        ),
        shape = RoundedCornerShape(WandasDimensions.CornerRadiusMedium),
        contentPadding = PaddingValues(
            horizontal = WandasDimensions.SpacingLarge,
            vertical = WandasDimensions.SpacingMedium
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = WandasDimensions.ElevationLarge
        )
    ) {
        Text(
            text = text,
            style = WandasTextStyles.ButtonMedium,
            textAlign = TextAlign.Center
        )
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
        modifier = modifier
            .height(WandasDimensions.ButtonHeightExtraLarge),
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

