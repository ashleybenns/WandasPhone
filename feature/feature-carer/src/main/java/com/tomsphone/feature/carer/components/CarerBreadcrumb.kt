package com.tomsphone.feature.carer.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomsphone.core.ui.theme.wandasColors

/**
 * Big, unsubtle breadcrumb navigation for carer settings.
 * 
 * Shows current location and provides clear back navigation.
 * Large touch target for accessibility.
 */
@Composable
fun CarerBreadcrumb(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    parentTitle: String? = null  // e.g., "Settings" when in "Contacts"
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.wandasColors.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onBack)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back arrow - large touch target
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.wandasColors.primaryButton
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                // Parent path (if provided)
                if (parentTitle != null) {
                    Text(
                        text = "← $parentTitle",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                // Current screen title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.wandasColors.onSurface
                )
            }
        }
    }
}
