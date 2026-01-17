package com.tomsphone.feature.carer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomsphone.core.config.FeatureLevel

/**
 * Development indicator showing current feature level.
 * 
 * Displayed at the top of every carer settings screen during development
 * to make it clear which level we're testing/building for.
 * 
 * TODO: Hide in production builds or make toggleable
 */
@Composable
fun DevLevelIndicator(
    level: FeatureLevel,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (level) {
        FeatureLevel.MINIMAL -> Color(0xFF2196F3)   // Blue
        FeatureLevel.BASIC -> Color(0xFF4CAF50)     // Green
        FeatureLevel.STANDARD -> Color(0xFFFF9800) // Orange
        FeatureLevel.EXTENDED -> Color(0xFF9C27B0) // Purple
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Level ${level.level}: ${level.name}",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
