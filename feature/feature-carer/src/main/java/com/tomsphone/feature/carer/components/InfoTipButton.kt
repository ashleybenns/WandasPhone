package com.tomsphone.feature.carer.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Info button that shows a tip dialog when clicked.
 * Used in carer settings to provide contextual help.
 */
@Composable
fun InfoTipButton(
    tipId: String,
    tipTitle: String,
    tipContent: String?,
    modifier: Modifier = Modifier,
    onTipViewed: (String) -> Unit = {}
) {
    var showDialog by remember { mutableStateOf(false) }
    
    // Only show button if there's content
    if (tipContent != null) {
        IconButton(
            onClick = {
                showDialog = true
                onTipViewed(tipId)
            },
            modifier = modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info about $tipTitle",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        
        if (showDialog) {
            InfoTipDialog(
                title = tipTitle,
                content = tipContent,
                onDismiss = { showDialog = false }
            )
        }
    }
}

/**
 * Dialog showing the tip content.
 */
@Composable
fun InfoTipDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .widthIn(max = 400.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Got it")
                }
            }
        }
    }
}

/**
 * Setting row with an integrated info tip button.
 */
@Composable
fun SettingWithTip(
    title: String,
    tipId: String,
    tipContent: String?,
    onTipViewed: (String) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            
            InfoTipButton(
                tipId = tipId,
                tipTitle = title,
                tipContent = tipContent,
                onTipViewed = onTipViewed
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        content()
    }
}
