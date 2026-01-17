package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.config.FeatureLevel
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.model.ContactType
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.CarerSettingsViewModel
import com.tomsphone.feature.carer.components.*

/**
 * Contact edit screen.
 * 
 * Edit existing contact or add new one.
 * Settings include:
 * - Name
 * - Phone number
 * - Contact type (Carer/Grey List)
 * - Button color
 * - Auto-answer (per-contact, Level 2+)
 */
@Composable
fun ContactEditScreen(
    contactId: Long,
    featureLevel: FeatureLevel,
    onBack: () -> Unit,
    viewModel: CarerSettingsViewModel = hiltViewModel()
) {
    val contacts by viewModel.contacts.collectAsState()
    val saveToastState = rememberSaveToastState()
    
    val isNewContact = contactId == 0L
    val existingContact = contacts.find { it.id == contactId }
    
    // Form state
    var name by remember { mutableStateOf(existingContact?.name ?: "") }
    var phoneNumber by remember { mutableStateOf(existingContact?.phoneNumber ?: "") }
    var contactType by remember { mutableStateOf(existingContact?.contactType ?: ContactType.CARER) }
    var isPrimary by remember { mutableStateOf(existingContact?.isPrimary ?: false) }
    var autoAnswerEnabled by remember { mutableStateOf(existingContact?.autoAnswerEnabled ?: false) }
    
    // Update form when contact loads
    LaunchedEffect(existingContact) {
        existingContact?.let {
            name = it.name
            phoneNumber = it.phoneNumber
            contactType = it.contactType
            isPrimary = it.isPrimary
            autoAnswerEnabled = it.autoAnswerEnabled
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
                                        contactType, isPrimary, autoAnswerEnabled
                                    )
                                    saveToastState.show("$name saved")
                                }
                            },
                            label = { Text("Contact Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Phone Number
                    SettingCard(title = "Phone Number") {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { newPhone ->
                                phoneNumber = newPhone
                                if (!isNewContact && phoneNumber.isNotBlank()) {
                                    saveContact(
                                        viewModel, existingContact, name, phoneNumber, 
                                        contactType, isPrimary, autoAnswerEnabled
                                    )
                                    saveToastState.show("$name's phone saved")
                                }
                            },
                            label = { Text("Phone Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Contact Type
                    SettingCard(title = "Contact Type") {
                        ContactType.entries.forEach { type ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = contactType == type,
                                    onClick = {
                                        contactType = type
                                        if (!isNewContact) {
                                            saveContact(
                                                viewModel, existingContact, name, phoneNumber, 
                                                contactType, isPrimary, autoAnswerEnabled
                                            )
                                            saveToastState.show("Contact type saved")
                                        }
                                    }
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Column {
                                    Text(
                                        text = getContactTypeDisplayName(type),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.wandasColors.onSurface
                                    )
                                    Text(
                                        text = getContactTypeDescription(type),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Primary Contact
                    if (contactType == ContactType.CARER) {
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
                    
                    // Auto-Answer (Level 2+)
                    LevelGatedContent(
                        minLevel = FeatureLevel.BASIC,
                        currentLevel = featureLevel
                    ) {
                        if (contactType == ContactType.CARER) {
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
                                                contactType, isPrimary, autoAnswerEnabled
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
                        
                        Button(
                            onClick = {
                                if (name.isNotBlank() && phoneNumber.isNotBlank()) {
                                    val newContact = Contact(
                                        id = 0,
                                        name = name,
                                        phoneNumber = phoneNumber,
                                        photoUri = null,
                                        priority = 0,
                                        isPrimary = isPrimary,
                                        contactType = contactType,
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis(),
                                        buttonColor = null,
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
                            enabled = name.isNotBlank() && phoneNumber.isNotBlank()
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
    autoAnswerEnabled: Boolean
) {
    existingContact?.let { contact ->
        viewModel.saveContact(
            contact.copy(
                name = name,
                phoneNumber = phoneNumber,
                contactType = contactType,
                isPrimary = isPrimary,
                autoAnswerEnabled = autoAnswerEnabled,
                updatedAt = System.currentTimeMillis()
            )
        )
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
        ContactType.GREY_LIST -> "Calls allowed but no home button or reminders"
    }
}
