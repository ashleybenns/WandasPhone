package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.CarerSettingsViewModel
import com.tomsphone.feature.carer.components.*

private fun pinFilter(raw: String): String =
    raw.filter { it.isDigit() }.take(4)

/**
 * Set or change the 4-digit Assistant PIN used to open carer settings.
 */
@Composable
fun AssistantPinScreen(
    onBack: () -> Unit,
    viewModel: CarerSettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val hasPin by viewModel.hasCarerPin.collectAsState()
    val saveToastState = rememberSaveToastState()

    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showDisablePinConfirm by remember { mutableStateOf(false) }

    if (showDisablePinConfirm) {
        AlertDialog(
            onDismissRequest = { showDisablePinConfirm = false },
            title = { Text("Turn off PIN requirement?") },
            text = {
                Text(
                    "Only the clock tap sequence will be needed to open Assistant settings. " +
                        "Anyone who discovers those taps can change contacts, call handling, and other setup. " +
                        "Any saved PIN will be removed."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setAssistantPinRequired(false)
                        showDisablePinConfirm = false
                        saveToastState.show("PIN requirement off — taps only")
                    }
                ) {
                    Text("Turn off")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisablePinConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                CarerBreadcrumb(
                    title = "Assistant PIN",
                    parentTitle = "Settings",
                    onBack = onBack
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(WandasDimensions.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingMedium)
                ) {
                    SettingCard(title = "Require PIN at login") {
                        Text(
                            text = "When on, Assistant settings ask for a PIN (or offer to set one) after the clock taps. " +
                                "When off, taps alone open settings — simpler for you, easier for anyone else who discovers the taps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.75f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (settings.assistantPinRequired) "PIN required" else "PIN not required",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.wandasColors.onSurface
                            )
                            Switch(
                                checked = settings.assistantPinRequired,
                                onCheckedChange = { on ->
                                    if (on) {
                                        viewModel.setAssistantPinRequired(true)
                                        saveToastState.show("PIN step enabled for next visit")
                                    } else {
                                        showDisablePinConfirm = true
                                    }
                                }
                            )
                        }
                    }

                    SettingCard(title = if (hasPin) "Change PIN" else "Set PIN") {
                        Text(
                            text = when {
                                !settings.assistantPinRequired -> {
                                    "PIN is optional while the switch above is off. Saving a PIN here turns the requirement on " +
                                        "and applies it the next time you open Assistant settings."
                                }
                                hasPin -> "Enter your current PIN, then choose a new 4-digit PIN."
                                else ->
                                    "Choose a 4-digit PIN. You’ll need it each time you open Assistant settings (after the clock taps)."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.75f)
                        )
                    }

                    if (hasPin) {
                        OutlinedTextField(
                            value = currentPin,
                            onValueChange = { currentPin = pinFilter(it) },
                            label = { Text("Current PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = pinFilter(it) },
                        label = { Text(if (hasPin) "New PIN" else "PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = pinFilter(it) },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    errorText?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = {
                            errorText = null
                            viewModel.updateCarerPinFromSettings(
                                currentPin = currentPin,
                                newPin = newPin,
                                confirmPin = confirmPin
                            ) { ok, message ->
                                if (ok) {
                                    saveToastState.show(
                                        if (hasPin) "Assistant PIN updated"
                                        else "Assistant PIN saved"
                                    )
                                    currentPin = ""
                                    newPin = ""
                                    confirmPin = ""
                                } else {
                                    errorText = message
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = newPin.length == 4 && confirmPin.length == 4 &&
                            (!hasPin || currentPin.length == 4)
                    ) {
                        Text(
                            text = if (hasPin) "Update PIN" else "Save PIN",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            SaveToast(
                message = saveToastState.message,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
