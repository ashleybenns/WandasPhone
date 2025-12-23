package com.wandasphone.feature.carer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.wandasphone.core.config.FeatureLevel
import com.wandasphone.core.ui.components.LargeButton
import com.wandasphone.core.ui.theme.WandasDimensions
import com.wandasphone.core.ui.theme.WandasTextStyles
import com.wandasphone.core.ui.theme.wandasColors

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
    
    // Add contact dialog state
    var showAddContactDialog by remember { mutableStateOf(false) }
    
    if (showPinDialog && !isPinVerified) {
        PinDialog(
            onPinEntered = { viewModel.verifyPin(it) },
            onDismiss = onNavigateBack
        )
    }
    
    if (showAddContactDialog) {
        AddContactDialog(
            onSave = { name, phone, isPrimary ->
                val now = System.currentTimeMillis()
                viewModel.saveContact(
                    com.wandasphone.core.data.model.Contact(
                        id = 0L,
                        name = name,
                        phoneNumber = phone,
                        photoUri = null,
                        priority = if (isPrimary) 0 else 10,
                        isPrimary = isPrimary,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                showAddContactDialog = false
            },
            onDismiss = { showAddContactDialog = false }
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
                
                // User Details
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
                            text = "User Details",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.wandasColors.onSurface
                        )
                        Spacer(modifier = Modifier.height(WandasDimensions.SpacingSmall))
                        
                        OutlinedTextField(
                            value = settings.userName,
                            onValueChange = { viewModel.setUserName(it) },
                            label = { Text("User's Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(WandasDimensions.SpacingSmall))
                        
                        Text(
                            text = "This name is used in TTS prompts (e.g. \"${settings.userName}, that's your phone ringing\")",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f)
                        )
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
                            onClick = { showAddContactDialog = true },
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
                                    viewModel.setAutoAnswer(enabled, settings.autoAnswerRings)
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
                
                // Volume Settings
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
                            text = "Speaker Volume: ${settings.speakerVolume}/10",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.wandasColors.onSurface
                        )
                        Spacer(modifier = Modifier.height(WandasDimensions.SpacingSmall))
                        
                        Slider(
                            value = settings.speakerVolume.toFloat(),
                            onValueChange = { viewModel.setSpeakerVolume(it.toInt()) },
                            valueRange = 1f..10f,
                            steps = 8,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Emergency Settings
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
                            text = "Emergency",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.wandasColors.onSurface
                        )
                        Spacer(modifier = Modifier.height(WandasDimensions.SpacingSmall))
                        
                        OutlinedTextField(
                            value = settings.emergencyNumber,
                            onValueChange = { viewModel.setEmergencyNumber(it) },
                            label = { Text("Emergency Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(WandasDimensions.SpacingSmall))
                        
                        Text(
                            text = "Requires ${settings.emergencyTapCount} taps to dial",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                // Missed Call Nagging
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
                            text = "Missed Call Reminder",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.wandasColors.onSurface
                        )
                        Spacer(modifier = Modifier.height(WandasDimensions.SpacingSmall))
                        
                        Text(
                            text = "Remind every ${settings.missedCallNagIntervalMinutes} minutes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.wandasColors.onSurface
                        )
                        
                        Slider(
                            value = settings.missedCallNagIntervalMinutes.toFloat(),
                            onValueChange = { viewModel.setMissedCallNagInterval(it.toInt()) },
                            valueRange = 1f..15f,
                            steps = 13,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            text = "Audio reminder for missed calls from carers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f)
                        )
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
    val focusRequester = remember { FocusRequester() }
    
    // Auto-focus the text field when dialog opens
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
                    ),
                    modifier = Modifier.focusRequester(focusRequester)
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

@Composable
private fun AddContactDialog(
    onSave: (name: String, phone: String, isPrimary: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isPrimary by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.wandasColors.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(WandasDimensions.SpacingLarge),
                verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingMedium)
            ) {
                Text(
                    text = "Add Contact",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.wandasColors.onSurface
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isPrimary,
                        onCheckedChange = { isPrimary = it }
                    )
                    Spacer(modifier = Modifier.width(WandasDimensions.SpacingSmall))
                    Text(
                        text = "Primary Carer (shown on home screen)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.wandasColors.onSurface
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingMedium)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { 
                            if (name.isNotBlank() && phone.isNotBlank()) {
                                onSave(name, phone, isPrimary)
                            }
                        },
                        enabled = name.isNotBlank() && phone.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

