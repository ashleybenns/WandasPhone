package com.tomsphone.feature.carer.screens

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import coil.request.ImageRequest
import com.tomsphone.core.telecom.EmergencyNumberResolver
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.CarerSettingsViewModel
import com.tomsphone.feature.carer.components.*
import java.io.File

/**
 * User profile settings screen.
 * 
 * Contains:
 * - User name (all levels)
 * - Emergency number (all levels)
 * - Future: blood type, meds, etc. (Level 2+)
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun UserProfileScreen(
    onNavigateToPhotoCapture: () -> Unit,
    onBack: () -> Unit,
    viewModel: CarerSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val saveToastState = rememberSaveToastState()
    val context = LocalContext.current
    val smsPermissionState = rememberPermissionState(Manifest.permission.SEND_SMS)

    LaunchedEffect(Unit) {
        viewModel.syncEmergencyNumberWithRegionIfNeeded()
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                CarerBreadcrumb(
                    title = "User Profile",
                    parentTitle = "Settings",
                    onBack = onBack
                )
                
                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(WandasDimensions.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingMedium)
                ) {
                    // User Name
                    SettingCard(title = "User Name") {
                        var name by remember { mutableStateOf(settings.userName) }
                        var surname by remember { mutableStateOf(settings.userSurname) }
                        
                        LaunchedEffect(settings.userName) {
                            name = settings.userName
                        }
                        LaunchedEffect(settings.userSurname) {
                            surname = settings.userSurname
                        }
                        
                        OutlinedTextField(
                            value = name,
                            onValueChange = { newName ->
                                name = newName
                                viewModel.setUserName(newName)
                                saveToastState.show("First name saved")
                            },
                            label = { Text("First Name") },
                            singleLine = true,
                            keyboardOptions =
                                KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = surname,
                            onValueChange = { newSurname ->
                                surname = newSurname
                                viewModel.setUserSurname(newSurname)
                                saveToastState.show("Surname saved")
                            },
                            label = { Text("Surname") },
                            singleLine = true,
                            keyboardOptions =
                                KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            text = "Used for emergency ID and spoken prompts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    // Emergency Number
                    SettingCard(title = "Emergency Number") {
                        var number by remember { mutableStateOf(settings.emergencyNumber) }
                        val emergencyResolution = remember(settings.emergencyNumber) {
                            EmergencyNumberResolver.resolve(context, settings.emergencyNumber)
                        }
                        
                        LaunchedEffect(settings.emergencyNumber) {
                            number = settings.emergencyNumber
                        }
                        
                        OutlinedTextField(
                            value = number,
                            onValueChange = { newNumber ->
                                number = newNumber
                                viewModel.setEmergencyNumber(newNumber)
                                saveToastState.show("Emergency number saved")
                            },
                            label = { Text("Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            text = "Default is 112. The app picks a local code when the phone or SIM region is known.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        emergencyResolution.assistantNotice?.let { notice ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "For carers / assistants: $notice",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                    
                    // Emergency Test Mode
                    SettingCard(title = "Emergency Test Mode") {
                        SettingToggle(
                            title = "Test Mode Enabled",
                            description = "Emergency button simulates a call — no real dial",
                            checked = settings.emergencyTestMode,
                            onCheckedChange = { enabled ->
                                viewModel.setEmergencyTestMode(enabled)
                                if (enabled) {
                                    saveToastState.show("Test mode enabled - no real calls")
                                } else {
                                    saveToastState.show("⚠️ Test mode OFF - real calls enabled!")
                                }
                            }
                        )
                        
                        if (!settings.emergencyTestMode) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "⚠️ Emergency button will place REAL calls to the emergency number",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    // SOS: text assistants when SOS is pressed
                    SettingCard(title = "SOS — text assistants") {
                        SettingToggle(
                            title = "Send SMS to all assistants when SOS is pressed",
                            description =
                                "Texts every saved contact that has a phone number as soon as SOS is tapped " +
                                    "(even if the emergency call is not completed). Requires SMS permission.",
                            checked = settings.sosSmsNotifyAssistantsEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setSosSmsNotifyAssistantsEnabled(enabled)
                                if (enabled) {
                                    smsPermissionState.launchPermissionRequest()
                                    saveToastState.show("Grant SMS when prompted so SOS texts can be sent")
                                } else {
                                    saveToastState.show("SOS SMS to assistants off")
                                }
                            }
                        )
                        if (settings.sosSmsNotifyAssistantsEnabled && !smsPermissionState.status.isGranted) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "SMS permission is still off — SOS cannot send texts until you allow SMS for this app (system Settings → Apps → Tom's Phone → Permissions).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.65f),
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        var sosSmsBody by remember { mutableStateOf(settings.sosSmsNotifyAssistantsMessage) }
                        LaunchedEffect(settings.sosSmsNotifyAssistantsMessage) {
                            sosSmsBody = settings.sosSmsNotifyAssistantsMessage
                        }
                        OutlinedTextField(
                            value = sosSmsBody,
                            onValueChange = {
                                sosSmsBody = it
                                viewModel.setSosSmsNotifyAssistantsMessage(it)
                            },
                            label = { Text("Custom message (optional)") },
                            placeholder = {
                                Text(
                                    "Leave blank for default text including user name. Use {userName} in your own message.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 5
                        )
                        Text(
                            text = "Blank message uses a default English SMS with the user’s name.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    // Address for EMTs
                    SettingCard(title = "Address") {
                        var address by remember { mutableStateOf(settings.userAddress) }
                        
                        LaunchedEffect(settings.userAddress) {
                            address = settings.userAddress
                        }
                        
                        OutlinedTextField(
                            value = address,
                            onValueChange = { newAddress ->
                                address = newAddress
                                viewModel.setUserAddress(newAddress)
                                saveToastState.show("Address saved")
                            },
                            label = { Text("Home address") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                        
                        Text(
                            text = "Shown on the emergency screen for responders",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    // Medical Information
                    SettingCard(title = "Medical Information") {
                        Text(
                            text = "Shown during emergency calls for responders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Blood Type
                        var bloodType by remember { mutableStateOf(settings.userBloodType) }
                        LaunchedEffect(settings.userBloodType) { bloodType = settings.userBloodType }
                        OutlinedTextField(
                            value = bloodType,
                            onValueChange = { 
                                bloodType = it
                                viewModel.setUserBloodType(it)
                                saveToastState.show("Blood type saved")
                            },
                            label = { Text("Blood Type (e.g., A+, B-, O+)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Allergies
                        var allergies by remember { mutableStateOf(settings.userAllergies) }
                        LaunchedEffect(settings.userAllergies) { allergies = settings.userAllergies }
                        OutlinedTextField(
                            value = allergies,
                            onValueChange = { 
                                allergies = it
                                viewModel.setUserAllergies(it)
                                saveToastState.show("Allergies saved")
                            },
                            label = { Text("Allergies (drug, food, etc.)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Medications
                        var medications by remember { mutableStateOf(settings.userMedications) }
                        LaunchedEffect(settings.userMedications) { medications = settings.userMedications }
                        OutlinedTextField(
                            value = medications,
                            onValueChange = { 
                                medications = it
                                viewModel.setUserMedications(it)
                                saveToastState.show("Medications saved")
                            },
                            label = { Text("Current Medications") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Medical Conditions
                        var conditions by remember { mutableStateOf(settings.userMedicalConditions) }
                        LaunchedEffect(settings.userMedicalConditions) { conditions = settings.userMedicalConditions }
                        OutlinedTextField(
                            value = conditions,
                            onValueChange = { 
                                conditions = it
                                viewModel.setUserMedicalConditions(it)
                                saveToastState.show("Conditions saved")
                            },
                            label = { Text("Conditions") },
                            placeholder = { Text("Dementia, diabetes, heart condition...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Emergency Notes
                        var notes by remember { mutableStateOf(settings.userEmergencyNotes) }
                        LaunchedEffect(settings.userEmergencyNotes) { notes = settings.userEmergencyNotes }
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { 
                                notes = it
                                viewModel.setUserEmergencyNotes(it)
                                saveToastState.show("Emergency notes saved")
                            },
                            label = { Text("Additional Notes for EMTs") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                    
                    // Emergency Contacts (people to notify, not 999)
                    SettingCard(title = "Emergency Contacts") {
                        Text(
                            text = "People to contact in an emergency (family, assistants). Displayed on the emergency screen for attending help.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Contact 1
                        Text(
                            text = "Contact 1",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.wandasColors.onSurface
                        )
                        
                        var contact1Name by remember { mutableStateOf(settings.emergencyContact1Name) }
                        LaunchedEffect(settings.emergencyContact1Name) { contact1Name = settings.emergencyContact1Name }
                        OutlinedTextField(
                            value = contact1Name,
                            onValueChange = { 
                                contact1Name = it
                                viewModel.setEmergencyContact1Name(it)
                                saveToastState.show("Contact 1 name saved")
                            },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        var contact1Phone by remember { mutableStateOf(settings.emergencyContact1Phone) }
                        LaunchedEffect(settings.emergencyContact1Phone) { contact1Phone = settings.emergencyContact1Phone }
                        OutlinedTextField(
                            value = contact1Phone,
                            onValueChange = { 
                                contact1Phone = it
                                viewModel.setEmergencyContact1Phone(it)
                                saveToastState.show("Contact 1 phone saved")
                            },
                            label = { Text("Phone") },
                            placeholder = { Text("07xxx xxxxxx") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Contact 2
                        Text(
                            text = "Contact 2",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.wandasColors.onSurface
                        )
                        
                        var contact2Name by remember { mutableStateOf(settings.emergencyContact2Name) }
                        LaunchedEffect(settings.emergencyContact2Name) { contact2Name = settings.emergencyContact2Name }
                        OutlinedTextField(
                            value = contact2Name,
                            onValueChange = { 
                                contact2Name = it
                                viewModel.setEmergencyContact2Name(it)
                                saveToastState.show("Contact 2 name saved")
                            },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        var contact2Phone by remember { mutableStateOf(settings.emergencyContact2Phone) }
                        LaunchedEffect(settings.emergencyContact2Phone) { contact2Phone = settings.emergencyContact2Phone }
                        OutlinedTextField(
                            value = contact2Phone,
                            onValueChange = { 
                                contact2Phone = it
                                viewModel.setEmergencyContact2Phone(it)
                                saveToastState.show("Contact 2 phone saved")
                            },
                            label = { Text("Phone") },
                            placeholder = { Text("07xxx xxxxxx") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // User Photo
                    SettingCard(title = "User Photo") {
                        Text(
                            text = "A photo helps EMTs verify they have the right patient's information.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Check if photo exists - track with state for reactivity
                        val photoFile = File(context.filesDir, "emergency_photo.jpg")
                        var hasPhoto by remember { mutableStateOf(photoFile.exists()) }
                        var photoTimestamp by remember { mutableStateOf(photoFile.lastModified()) }
                        
                        // Re-check when settings.userPhotoUri changes (triggered after capture)
                        LaunchedEffect(settings.userPhotoUri) {
                            hasPhoto = photoFile.exists()
                            photoTimestamp = photoFile.lastModified()
                            android.util.Log.d("UserProfile", "Photo check: exists=${photoFile.exists()}, uri=${settings.userPhotoUri}, path=${photoFile.absolutePath}")
                        }
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Photo display - 240dp for better visibility
                            if (hasPhoto) {
                                // Use key to force reload when photo changes
                                key(photoTimestamp) {
                                    Box(
                                        modifier = Modifier
                                            .size(240.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.wandasColors.surface)
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(photoFile)
                                                .crossfade(true)
                                                // Add timestamp to bust cache
                                                .setParameter("timestamp", photoTimestamp)
                                                .build(),
                                            contentDescription = "User photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Stacked buttons for cleaner layout
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Button(
                                        onClick = onNavigateToPhotoCapture,
                                        modifier = Modifier.width(200.dp)
                                    ) {
                                        Text("Retake Photo")
                                    }
                                    
                                    OutlinedButton(
                                        onClick = {
                                            photoFile.delete()
                                            viewModel.setUserPhotoUri(null)
                                            saveToastState.show("Photo removed")
                                        },
                                        modifier = Modifier.width(200.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Remove Photo")
                                    }
                                }
                            } else {
                                // No photo - show placeholder and Take Photo button
                                Box(
                                    modifier = Modifier
                                        .size(240.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.wandasColors.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = settings.userName.firstOrNull()?.uppercase() ?: "?",
                                        style = MaterialTheme.typography.displayLarge,
                                        color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Button(
                                    onClick = onNavigateToPhotoCapture,
                                    modifier = Modifier.width(200.dp)
                                ) {
                                    Text("Take Photo")
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
            
            // Save toast
            SaveToast(
                message = saveToastState.message,
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Card wrapper for a setting group.
 */
@Composable
fun SettingCard(
    title: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.wandasColors.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(WandasDimensions.SpacingMedium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.wandasColors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                trailingContent?.invoke()
            }
            
            Spacer(modifier = Modifier.height(WandasDimensions.SpacingSmall))
            
            content()
        }
    }
}
