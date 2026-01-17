package com.tomsphone.feature.carer.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomsphone.core.config.FeatureLevel
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors

/**
 * Large menu button for carer settings main menu.
 * 
 * Features:
 * - Title + description for clarity
 * - Level gating (only shows if currentLevel >= minLevel)
 * - Big touch target for accessibility
 */
@Composable
fun CarerMenuButton(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minLevel: FeatureLevel = FeatureLevel.MINIMAL,
    currentLevel: FeatureLevel = FeatureLevel.MINIMAL
) {
    // Level gating
    if (currentLevel.level < minLevel.level) {
        return
    }
    
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp),
        shape = RoundedCornerShape(WandasDimensions.CornerRadiusMedium),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.wandasColors.surface,
            contentColor = MaterialTheme.wandasColors.onSurface
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.wandasColors.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
