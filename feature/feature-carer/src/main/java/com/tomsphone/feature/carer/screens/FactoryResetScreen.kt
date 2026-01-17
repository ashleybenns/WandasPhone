package com.tomsphone.feature.carer.screens

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.config.FeatureLevel
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.CarerSettingsViewModel
import com.tomsphone.feature.carer.components.CarerBreadcrumb
import com.tomsphone.feature.carer.components.DevLevelIndicator

/**
 * Factory Reset screen.
 * 
 * Allows carer to completely wipe all app data before giving
 * the phone to a new user.
 * 
 * SECURITY: This is critical for:
 * - Protecting previous user's contacts
 * - Ensuring auto-answer is disabled for new user
 * - Removing any personal data
 */
@Composable
fun FactoryResetScreen(
    featureLevel: FeatureLevel,
    onBack: () -> Unit,
    viewModel: CarerSettingsViewModel = hiltViewModel()
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Dev level indicator
            DevLevelIndicator(level = featureLevel)
            
            // Breadcrumb
            CarerBreadcrumb(
                title = "Factory Reset",
                parentTitle = "Settings",
                onBack = onBack
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(WandasDimensions.SpacingMedium),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingLarge)
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                
                // Warning icon
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFD32F2F)  // Red
                )
                
                // Title
                Text(
                    text = "Reset to Factory Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.wandasColors.onBackground,
                    textAlign = TextAlign.Center
                )
                
                // Explanation
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.wandasColors.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(WandasDimensions.SpacingMedium)
                    ) {
                        Text(
                            text = "This will permanently delete:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.wandasColors.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        listOf(
                            "• All contacts",
                            "• All settings (including PIN)",
                            "• Call history",
                            "• Auto-answer configuration"
                        ).forEach { item ->
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                
                // Carer explanation
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE3F2FD)  // Light blue
                    )
                ) {
                    Text(
                        text = "Use this before giving the phone to a new user. " +
                               "No copies of this data are kept in cloud backups.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1565C0),  // Dark blue
                        modifier = Modifier.padding(WandasDimensions.SpacingMedium)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Reset button
                Button(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),  // Red
                        contentColor = Color.White
                    ),
                    enabled = !isResetting
                ) {
                    if (isResetting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Factory Reset",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // Confirmation dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = "Confirm Factory Reset",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure? This cannot be undone.\n\n" +
                    "The app will restart as if newly installed."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        isResetting = true
                        viewModel.factoryReset {
                            // Restart the app
                            val activity = context as? Activity
                            activity?.let {
                                val intent = it.packageManager.getLaunchIntentForPackage(it.packageName)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                it.startActivity(intent)
                                it.finishAffinity()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F)
                    )
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
