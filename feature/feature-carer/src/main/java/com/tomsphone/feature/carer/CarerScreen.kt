package com.tomsphone.feature.carer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.ui.components.SecondaryScreenIdleEffect
import kotlinx.coroutines.flow.map
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.billing.CarerPaywallGateViewModel
import com.tomsphone.feature.carer.screens.PaywallScreen

/**
 * Carer configuration screen
 * 
 * Assistant settings: optional PIN after the clock taps (carer chooses in Assistant PIN settings):
 * - Main menu with categories
 * - Individual screens for each category
 * - Level-gated features
 */
@Composable
fun CarerScreen(
    onNavigateBack: () -> Unit,
    onExitApp: () -> Unit,
    viewModel: CarerSettingsViewModel = hiltViewModel(),
    paywallGateViewModel: CarerPaywallGateViewModel = hiltViewModel()
) {
    val isPinVerified by viewModel.isPinVerified.collectAsState()
    val showPinDialog by viewModel.showPinDialog.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val entitlement by paywallGateViewModel.snapshot.collectAsState()
    val hasCarerPin by viewModel.hasCarerPin.collectAsState()
    val assistantPinRequired by viewModel.settings
        .map { it.assistantPinRequired }
        .collectAsState(initial = true)

    LaunchedEffect(assistantPinRequired) {
        if (!assistantPinRequired) {
            viewModel.enterCarerIfPinNotRequired()
        }
    }

    // PIN step only when carer chose to require it (or legacy default)
    if (assistantPinRequired && showPinDialog && !isPinVerified) {
        PinDialog(
            hasStoredPin = hasCarerPin,
            onPinEntered = { viewModel.verifyPin(it) },
            onContinueWithoutPin = { viewModel.skipAssistantPinSetup() },
            onDismiss = onNavigateBack
        )
    }
    
    // Once verified, show paywall or settings (idle timeout → home uses carer inactivity setting).
    if (isPinVerified) {
        val timeoutMs = (settings.inactivityTimeoutSeconds * 1000L).coerceIn(15_000L, 600_000L)
        SecondaryScreenIdleEffect(timeoutMs = timeoutMs, onTimeout = onNavigateBack) {
            if (entitlement.needsPaywall) {
                PaywallScreen(onBack = onNavigateBack)
            } else {
                CarerNavigation(
                    onExitCarerSettings = onNavigateBack,
                    onExitApp = onExitApp
                )
            }
        }
    }
}

@Composable
private fun PinDialog(
    hasStoredPin: Boolean,
    onPinEntered: (String) -> Unit,
    onContinueWithoutPin: () -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    
    // Auto-focus the text field when dialog appears
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
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
                    text = if (hasStoredPin) "Enter Assistant PIN" else "Assistant PIN (optional)",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.wandasColors.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = if (hasStoredPin) {
                        "Enter the 4-digit PIN you chose in Assistant settings."
                    } else {
                        "Optional: a PIN adds protection on top of the clock taps. " +
                            "Without it, anyone who discovers the taps can change settings — that may still suit some households."
                    },
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
                    ),
                    modifier = Modifier.focusRequester(focusRequester)
                )
                
                if (!hasStoredPin) {
                    TextButton(
                        onClick = onContinueWithoutPin,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Continue without PIN",
                            textAlign = TextAlign.Center
                        )
                    }
                }

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
                        Text(if (hasStoredPin) "OK" else "Save PIN")
                    }
                }
            }
        }
    }
}
