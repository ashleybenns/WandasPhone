package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.config.ButtonColor
import com.tomsphone.core.config.FeatureLevel
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.model.ContactType
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.CarerSettingsViewModel
import com.tomsphone.feature.carer.components.*

/**
 * Contact edit screen.
 * 
 * Edit existing contact or add new one.
 * Contact type is passed as parameter (determined by which "Add" button was tapped).
 * 
 * Settings include:
 * - Name
 * - Phone number
 * - Primary contact toggle (Carers only)
 * - Auto-answer (Carers only, Level 2+)
 */
@Composable
fun ContactEditScreen(
    contactId: Long,
    contactType: ContactType,  // Type is fixed - determined by entry point
    featureLevel: FeatureLevel,
    onBack: () -> Unit,
    viewModel: CarerSettingsViewModel = hiltViewModel()
) {
    val contacts by viewModel.contacts.collectAsState()
    val saveToastState = rememberSaveToastState()
    
    val isNewContact = contactId == 0L
    val existingContact = contacts.find { it.id == contactId }
    
    // Use existing contact's type if editing, otherwise use passed type
    val effectiveContactType = existingContact?.contactType ?: contactType
    
    // Form state
    var name by remember { mutableStateOf(existingContact?.name ?: "") }
    var phoneNumber by remember { mutableStateOf(existingContact?.phoneNumber ?: "") }
    var isPrimary by remember { mutableStateOf(existingContact?.isPrimary ?: false) }
    var autoAnswerEnabled by remember { mutableStateOf(existingContact?.autoAnswerEnabled ?: false) }
    var selectedColor by remember { mutableStateOf(ButtonColor.fromArgb(existingContact?.buttonColor)) }
    
    // Update form when contact loads
    LaunchedEffect(existingContact) {
        existingContact?.let {
            name = it.name
            phoneNumber = it.phoneNumber
            isPrimary = it.isPrimary
            autoAnswerEnabled = it.autoAnswerEnabled
            selectedColor = ButtonColor.fromArgb(it.buttonColor)
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Dev level indicator
                DevLevelIndicator(level = featureLevel)
                
                // Breadcrumb
                CarerBreadcrumb(
                    title = if (isNewContact) "Add Contact" else name.ifEmpty { "Edit Contact" },
                    parentTitle = "Contacts",
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
                    // Name
                    SettingCard(title = "Name") {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { newName ->
                                name = newName
                                if (!isNewContact && name.isNotBlank()) {
                                    saveContact(
                                        viewModel, existingContact, name, phoneNumber, 
                                        effectiveContactType, isPrimary, autoAnswerEnabled, selectedColor
                                    )
                                    saveToastState.show("$name saved")
                                }
                            },
                            label = { Text("Contact Name") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words  // Title Case
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Phone Number
                    val phoneError = remember(phoneNumber) { 
                        if (phoneNumber.isBlank()) null
                        else if (!isValidPhoneNumber(phoneNumber)) "Enter a valid UK phone number"
                        else null
                    }
                    
                    SettingCard(title = "Phone Number") {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { newPhone ->
                                // Only allow digits, spaces, and common phone chars
                                val filtered = newPhone.filter { it.isDigit() || it in " +-" }
                                phoneNumber = filtered
                                if (!isNewContact && isValidPhoneNumber(filtered)) {
                                    saveContact(
                                        viewModel, existingContact, name, phoneNumber, 
                                        effectiveContactType, isPrimary, autoAnswerEnabled, selectedColor
                                    )
                                    saveToastState.show("$name's phone saved")
                                }
                            },
                            label = { Text("Phone Number") },
                            placeholder = { Text("07xxx xxxxxx") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone  // Phone keyboard
                            ),
                            isError = phoneError != null,
                            supportingText = phoneError?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Show contact type (read-only info)
                    SettingCard(title = "Contact Type") {
                        Text(
                            text = getContactTypeDisplayName(effectiveContactType),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.wandasColors.onSurface
                        )
                        Text(
                            text = getContactTypeDescription(effectiveContactType),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    // Button Color (Carers only - they appear on home screen)
                    if (effectiveContactType == ContactType.CARER) {
                        SettingCard(title = "Button Color") {
                            Text(
                                text = "Choose a color for this contact's button on the home screen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            // Color swatches in a row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ButtonColor.entries.forEach { buttonColor ->
                                    ColorSwatch(
                                        color = buttonColor,
                                        isSelected = selectedColor == buttonColor,
                                        themeDefaultColor = MaterialTheme.wandasColors.primaryButton,
                                        onClick = {
                                            selectedColor = buttonColor
                                            if (!isNewContact) {
                                                saveContact(
                                                    viewModel, existingContact, name, phoneNumber,
                                                    effectiveContactType, isPrimary, autoAnswerEnabled,
                                                    buttonColor
                                                )
                                                saveToastState.show("$name's color: ${buttonColor.displayName}")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Primary Contact (Carers only)
                    if (effectiveContactType == ContactType.CARER) {
                        SettingCard(title = "Primary Contact") {
                            SettingToggle(
                                title = "Set as Primary",
                                description = "First contact to call, used for emergency",
                                checked = isPrimary,
                                onCheckedChange = { primary ->
                                    isPrimary = primary
                                    if (!isNewContact) {
                                        if (primary) {
                                            viewModel.setPrimaryContact(contactId)
                                        }
                                        saveToastState.show(if (primary) "$name set as primary" else "Primary removed")
                                    }
                                }
                            )
                        }
                    }
                    
                    // Auto-Answer (Carers only, Level 2+)
                    LevelGatedContent(
                        minLevel = FeatureLevel.BASIC,
                        currentLevel = featureLevel
                    ) {
                        if (effectiveContactType == ContactType.CARER) {
                            SettingCard(title = "Auto-Answer") {
                                SettingToggle(
                                    title = "Auto-Answer for this contact",
                                    description = "Phone answers automatically when they call",
                                    checked = autoAnswerEnabled,
                                    onCheckedChange = { enabled ->
                                        autoAnswerEnabled = enabled
                                        if (!isNewContact) {
                                            saveContact(
                                                viewModel, existingContact, name, phoneNumber, 
                                                contactType, isPrimary, autoAnswerEnabled, selectedColor
                                            )
                                            saveToastState.show("$name's auto-answer ${if (enabled) "enabled" else "disabled"}")
                                        }
                                    }
                                )
                                
                                if (autoAnswerEnabled) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = Color(0xFFFFEB3B).copy(alpha = 0.3f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "⚠️ Privacy warning: This contact can listen without the user answering",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF795548),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Save button for new contacts
                    if (isNewContact) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val canSave = name.isNotBlank() && isValidPhoneNumber(phoneNumber)
                        
                        Button(
                            onClick = {
                                if (canSave) {
                                    val newContact = Contact(
                                        id = 0,
                                        name = name.trim(),
                                        phoneNumber = phoneNumber.trim(),
                                        photoUri = null,
                                        priority = 0,
                                        isPrimary = isPrimary,
                                        contactType = effectiveContactType,
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis(),
                                        buttonColor = if (selectedColor == ButtonColor.DEFAULT) null else selectedColor.argb,
                                        autoAnswerEnabled = autoAnswerEnabled,
                                        buttonPosition = 0,
                                        isHalfWidth = false
                                    )
                                    viewModel.saveContact(newContact)
                                    saveToastState.show("$name added")
                                    onBack()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canSave
                        ) {
                            Text("Save Contact")
                        }
                    }
                    
                    // Delete button for existing contacts
                    if (!isNewContact) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedButton(
                            onClick = {
                                viewModel.deleteContact(contactId)
                                onBack()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete Contact")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
            
            // Save toast
            SaveToast(
                message = saveToastState.message,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

private fun saveContact(
    viewModel: CarerSettingsViewModel,
    existingContact: Contact?,
    name: String,
    phoneNumber: String,
    contactType: ContactType,
    isPrimary: Boolean,
    autoAnswerEnabled: Boolean,
    buttonColor: ButtonColor = ButtonColor.DEFAULT
) {
    existingContact?.let { contact ->
        viewModel.saveContact(
            contact.copy(
                name = name,
                phoneNumber = phoneNumber,
                contactType = contactType,
                isPrimary = isPrimary,
                autoAnswerEnabled = autoAnswerEnabled,
                buttonColor = if (buttonColor == ButtonColor.DEFAULT) null else buttonColor.argb,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}

/**
 * Color swatch for button color selection
 */
@Composable
private fun ColorSwatch(
    color: ButtonColor,
    isSelected: Boolean,
    themeDefaultColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val swatchColor = if (color == ButtonColor.DEFAULT) {
        themeDefaultColor
    } else {
        Color(color.argb)
    }
    
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(swatchColor, CircleShape)
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, MaterialTheme.wandasColors.onBackground, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun getContactTypeDisplayName(type: ContactType): String {
    return when (type) {
        ContactType.CARER -> "Carer"
        ContactType.GREY_LIST -> "Grey List"
    }
}

private fun getContactTypeDescription(type: ContactType): String {
    return when (type) {
        ContactType.CARER -> "Appears on home screen, triggers missed call reminders"
        ContactType.GREY_LIST -> "Allows incoming calls only. No way to call back and no missed call notification."
    }
}

/**
 * Validate UK phone number
 * Must have at least 10 digits after removing formatting
 */
private fun isValidPhoneNumber(phone: String): Boolean {
    val digitsOnly = phone.filter { it.isDigit() }
    // UK numbers: 10-11 digits (with or without leading 0)
    // International: starts with + and at least 10 digits
    return digitsOnly.length >= 10
}
