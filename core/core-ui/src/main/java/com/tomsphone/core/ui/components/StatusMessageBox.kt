package com.tomsphone.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
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
 * - Battery warning replaces first line when battery is low
 */
@Composable
fun StatusMessageBox(
    message: String,
    onHiddenTap: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.wandasColors.onBackground,
    batteryLevel: Int = 100,
    isLowBattery: Boolean = false,
    isCharging: Boolean = false
) {
    // Height scales with user text size setting
    val scaledHeight = ScaledDimensions.statusBoxHeight
    val scaledTextSize = ScaledDimensions.statusTextSize
    
    // Split message into lines
    val lines = message.split("\n")
    
    // Determine battery status line (replaces first line when active)
    val batteryLine: String? = when {
        isLowBattery -> "🔋 Battery low ($batteryLevel%)"
        isCharging && batteryLevel < 100 -> "🔌 Charging ($batteryLevel%)"
        else -> null
    }
    
    val batteryColor: Color? = when {
        isLowBattery -> Color(0xFFD32F2F)  // Red
        isCharging && batteryLevel < 100 -> Color(0xFF4CAF50)  // Green
        else -> null
    }
    
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
            .padding(horizontal = WandasDimensions.SpacingSmall),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // First line - battery status or normal text
            if (batteryLine != null && batteryColor != null) {
                // Battery warning line with colored background
                Text(
                    text = batteryLine,
                    style = TextStyle(
                        fontSize = scaledTextSize,
                        fontWeight = FontWeight.Bold,
                        lineHeight = scaledTextSize * 1.2f
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .background(batteryColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            } else if (lines.isNotEmpty()) {
                // Normal first line
                Text(
                    text = lines[0],
                    style = TextStyle(
                        fontSize = scaledTextSize,
                        fontWeight = FontWeight.Medium,
                        lineHeight = scaledTextSize * 1.2f
                    ),
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
            
            // Remaining lines
            if (lines.size > 1) {
                Text(
                    text = lines.drop(1).joinToString("\n"),
                    style = TextStyle(
                        fontSize = scaledTextSize,
                        fontWeight = FontWeight.Medium,
                        lineHeight = scaledTextSize * 1.2f
                    ),
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}
