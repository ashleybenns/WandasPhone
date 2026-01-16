package com.tomsphone.feature.carer

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.config.FeatureLevel
import com.tomsphone.core.ui.components.LargeButton
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.WandasTextStyles
import com.tomsphone.core.ui.theme.wandasColors

/**
 * Carer configuration screen
 * 
 * PIN-protected access to:
 * - Feature level selection
 * - Contact management
 * - Theme selection
 * - Auto-answer settings
 * - User name
 */
@Composable
fun CarerScreen(
    onNavigateBack: () -> Unit,
    viewModel: CarerViewModel = hiltViewModel()
) {
    val isPinVerified by viewModel.isPinVerified.collectAsState()
    val showPinDialog by viewModel.showPinDialog.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    
    if (showPinDialog && !isPinVerified) {
        PinDialog(
            onPinEntered = { viewModel.verifyPin(it) },
            onDismiss = onNavigateBack
        )
    }
    
    if (isPinVerified) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.wandasColors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WandasDimensions.SpacingMedium)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingMedium)
            ) {
                // Title
                Text(
                    text = "Carer Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.wandasColors.onBackground,
                    modifier = Modifier.padding(WandasDimensions.SpacingMedium)
                )
                
                // User Name Setting
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
                            text = "User Name",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.wandasColors.onSurface
                        )
                        Spacer(modifier = Modifier.height(WandasDimensions.SpacingSmall))
                        
                        var userNameInput by remember { mutableStateOf(settings.userName) }
                        
                        // Update when settings change
                        LaunchedEffect(settings.userName) {
                            userNameInput = settings.userName
                        }
                        
                        OutlinedTextField(
                            value = userNameInput,
                            onValueChange = { userNameInput = it },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(WandasDimensions.SpacingSmall))
                        
                        Button(
                            onClick = { viewModel.setUserName(userNameInput) },
                            enabled = userNameInput != settings.userName && userNameInput.isNotBlank()
                        ) {
                            Text("Save Name")
                        }
                    }
                }
                
                // Feature Level Selection
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
                            text = "Feature Level",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.wandasColors.onSurface
                        )
                        Spacer(modifier = Modifier.height(WandasDimensions.SpacingSmall))
                        
                        FeatureLevel.entries.forEach { level ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.featureLevel == level,
                                    onClick = { viewModel.setFeatureLevel(level) }
                                )
                                Text(
                                    text = "Level ${level.level}: ${getLevelDescription(level)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.wandasColors.onSurface
                                )
                            }
                        }
                    }
                }
                
                // Contact Management
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
                            text = "Contacts (${contacts.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.wandasColors.onSurface
                        )
                        Spacer(modifier = Modifier.height(WandasDimensions.SpacingSmall))
                        
                        contacts.forEach { contact ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = contact.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.wandasColors.onSurface
                                    )
                                    Text(
                                        text = contact.phoneNumber,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                                if (contact.isPrimary) {
                                    Text(
                                        text = "PRIMARY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.wandasColors.success
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(WandasDimensions.SpacingSmall))
                        }
                        
                        Button(
                            onClick = { /* TODO: Add contact dialog */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add Contact")
                        }
                    }
                }
                
                // Auto-answer Settings
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
                            text = "Auto-Answer",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.wandasColors.onSurface
                        )
                        Spacer(modifier = Modifier.height(WandasDimensions.SpacingSmall))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                checked = settings.autoAnswerEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.setAutoAnswer(enabled, settings.autoAnswerDelaySeconds)
                                }
                            )
                            Spacer(modifier = Modifier.width(WandasDimensions.SpacingMedium))
                            Text(
                                text = if (settings.autoAnswerEnabled) "Enabled" else "Disabled",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.wandasColors.onSurface
                            )
                        }
                    }
                }
                
                // Always On Mode Settings
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
                            text = "Always On Mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.wandasColors.onSurface
                        )
                        Text(
                            text = "For use on a charging stand",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(WandasDimensions.SpacingMedium))
                        
                        // Pinned Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pinned Mode",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.wandasColors.onSurface
                                )
                                Text(
                                    text = "Prevents exiting the app",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            Switch(
                                checked = settings.pinnedModeEnabled,
                                onCheckedChange = { viewModel.setPinnedMode(it) }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(WandasDimensions.SpacingSmall))
                        
                        // Screen Always On
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Screen Always On",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.wandasColors.onSurface
                                )
                                Text(
                                    text = "Screen never dims or sleeps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            Switch(
                                checked = settings.screenAlwaysOn,
                                onCheckedChange = { viewModel.setScreenAlwaysOn(it) }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(WandasDimensions.SpacingSmall))
                        
                        // Lock Volume Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Lock Volume Buttons",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.wandasColors.onSurface
                                )
                                Text(
                                    text = "Prevent accidental volume changes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            Switch(
                                checked = settings.lockVolumeButtons,
                                onCheckedChange = { viewModel.setLockVolumeButtons(it) }
                            )
                        }
                    }
                }
                
                // Back button
                LargeButton(
                    text = "Done",
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
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

private fun getLevelDescription(level: FeatureLevel): String {
    return when (level) {
        FeatureLevel.MINIMAL -> "One-touch (2 contacts)"
        FeatureLevel.BASIC -> "Toggles (4 contacts, speaker/mute)"
        FeatureLevel.STANDARD -> "Lists (12 contacts, navigation)"
        FeatureLevel.EXTENDED -> "Full (unlimited, text input)"
    }
}

