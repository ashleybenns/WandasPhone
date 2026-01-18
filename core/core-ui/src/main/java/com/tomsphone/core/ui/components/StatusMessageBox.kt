package com.tomsphone.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.tomsphone.core.ui.theme.ScaledDimensions
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors

/**
 * Status message box displayed at top of home screen
 * 
 * Features:
 * - Height scales with user text size setting
 * - NOT a button - no visual feedback on tap
 * - Hidden carer access: requires 7 taps (configurable)
 * - Displays: "[User]'s phone", "Calling [name]", "Missed call..."
 */
@Composable
fun StatusMessageBox(
    message: String,
    onHiddenTap: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.wandasColors.onBackground
) {
    // Height scales with user text size setting
    val scaledHeight = ScaledDimensions.statusBoxHeight
    val scaledTextSize = ScaledDimensions.statusTextSize
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(scaledHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null  // No visual feedback - it's not a button
            ) {
                onHiddenTap()
            }
            .padding(horizontal = WandasDimensions.SpacingLarge),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = TextStyle(
                fontSize = scaledTextSize,
                fontWeight = FontWeight.Medium,
                lineHeight = scaledTextSize * 1.2f
            ),
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 3
        )
    }
}
