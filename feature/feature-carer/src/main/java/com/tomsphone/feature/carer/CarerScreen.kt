package com.tomsphone.feature.carer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors

/**
 * Carer configuration screen
 * 
 * PIN-protected access to the new settings architecture:
 * - Main menu with categories
 * - Individual screens for each category
 * - Level-gated features
 */
@Composable
fun CarerScreen(
    onNavigateBack: () -> Unit,
    onExitApp: () -> Unit,
    viewModel: CarerSettingsViewModel = hiltViewModel()
) {
    val isPinVerified by viewModel.isPinVerified.collectAsState()
    val showPinDialog by viewModel.showPinDialog.collectAsState()
    val settings by viewModel.settings.collectAsState()
    
    // Show PIN dialog if not verified
    if (showPinDialog && !isPinVerified) {
        PinDialog(
            onPinEntered = { viewModel.verifyPin(it) },
            onDismiss = onNavigateBack
        )
    }
    
    // Once verified, show the settings navigation
    if (isPinVerified) {
        CarerNavigation(
            onExitCarerSettings = onNavigateBack,
            onExitApp = onExitApp,
            featureLevel = settings.featureLevel
        )
    }
}

@Composable
private fun PinDialog(
    onPinEntered: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.wandasColors.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(WandasDimensions.SpacingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingMedium)
            ) {
                Text(
                    text = "Enter Carer PIN",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.wandasColors.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "First time? Enter a 4-digit PIN to set it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                
                TextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) pin = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        textAlign = TextAlign.Center
                    )
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingMedium)
                ) {
                    Button(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onPinEntered(pin) },
                        enabled = pin.length == 4
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}
