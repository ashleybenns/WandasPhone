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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.WandasTextStyles
import com.tomsphone.core.ui.theme.wandasColors

/**
 * Status message box displayed at top of home screen
 * 
 * Features:
 * - Fixed height for 3 lines of text
 * - NOT a button - no visual feedback on tap
 * - Hidden carer access: requires 7 taps (configurable)
 * - Displays: "[User]'s phone", "Calling [name]", "Missed call..."
 */
@Composable
fun StatusMessageBox(
    message: String,
    onHiddenTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Fixed height calculated for 3 lines of StatusMessage style
    // Line height is 40.sp, so 3 lines = 120.sp + padding
    val fixedHeight = 140.dp
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(fixedHeight)
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
            style = WandasTextStyles.StatusMessage,
            color = MaterialTheme.wandasColors.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 3
        )
    }
}
