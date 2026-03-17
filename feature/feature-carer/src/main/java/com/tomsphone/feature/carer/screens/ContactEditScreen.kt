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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.config.ButtonColor
import com.tomsphone.core.data.util.PhoneNumberUtils
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
import com.tomsphone.feature.carer.phone.getDefaultPhoneRegion
import com.tomsphone.feature.carer.phone.getPhoneCountries
import com.tomsphone.feature.carer.phone.PhoneCountry
import com.tomsphone.feature.carer.phone.validateAndToE164
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3Api

/**
 * Contact edit screen.
 * 
 * Edit existing contact or add new one.
 * Contact type is passed as parameter (determined by which "Add" button was tapped).
 * 
 * Settings include:
 * - Name
 * - Phone number
 * - Button color
 * - Auto-answer (Carers only, Level 1+)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactEditScreen(
    contactId: Long,
    contactType: ContactType,  // Type is fixed - determined by entry point
    onBack: () -> Unit,
    viewModel: CarerSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val featureLevel = settings.featureLevel
    val contacts by viewModel.contacts.collectAsState()
    val saveToastState = rememberSaveToastState()
    
    val isNewContact = contactId == 0L
    val existingContact = contacts.find { it.id == contactId }
    
    // Use existing contact's type if editing, otherwise use passed type
    val effectiveContactType = existingContact?.contactType ?: contactType
    
    val context = LocalContext.current
    val defaultRegion = remember(context) { getDefaultPhoneRegion(context) }
    val countries = remember { getPhoneCountries() }
    
    // Form state
    var name by remember { mutableStateOf(existingContact?.name ?: "") }
    var selectedRegionCode by remember { mutableStateOf(defaultRegion) }
    var nationalNumber by remember { mutableStateOf("") }
    var autoAnswerEnabled by remember { mutableStateOf(existingContact?.autoAnswerEnabled ?: false) }
    var notifyBatteryAlerts by remember { mutableStateOf(existingContact?.notifyBatteryAlerts ?: false) }
    var selectedColor by remember { mutableStateOf(ButtonColor.fromArgb(existingContact?.buttonColor)) }
    
    // Update form when contact loads
    LaunchedEffect(existingContact, defaultRegion) {
        existingContact?.let {
            name = it.name
            val parsed = PhoneNumberUtils.parseToRegionAndNational(it.phoneNumber, defaultRegion)
            if (parsed != null) {
                selectedRegionCode = parsed.first
                nationalNumber = parsed.second
            } else {
                selectedRegionCode = defaultRegion
                nationalNumber = it.phoneNumber.replace(Regex("[^0-9]"), "")
            }
            autoAnswerEnabled = it.autoAnswerEnabled
            notifyBatteryAlerts = it.notifyBatteryAlerts
            selectedColor = ButtonColor.fromArgb(it.buttonColor)
        } ?: run {
            if (existingContact == null && nationalNumber.isEmpty()) {
                selectedRegionCode = defaultRegion
            }
        }
    }
    
    val phoneNumberE164 = remember(nationalNumber, selectedRegionCode) {
        validateAndToE164(nationalNumber, selectedRegionCode)
    }
    val phoneForSave = phoneNumberE164 ?: existingContact?.phoneNumber ?: ""
    
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
                    parentTitle = getContactTypeDisplayName(effectiveContactType),
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
                                        viewModel, existingContact, name, phoneForSave,
                                        effectiveContactType, autoAnswerEnabled, notifyBatteryAlerts, selectedColor
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
                    
                    // Phone Number: country on one row, number on the next (full width each so field is editable and country doesn't wrap)
                    val selectedCountry = remember(selectedRegionCode, countries) {
                        countries.find { it.regionCode == selectedRegionCode }
                            ?: countries.firstOrNull { it.regionCode == defaultRegion }
                            ?: countries.first()
                    }
                    val phoneError = remember(nationalNumber, selectedRegionCode) {
                        if (nationalNumber.isBlank()) null
                        else if (validateAndToE164(nationalNumber, selectedRegionCode) == null)
                            "Enter a valid phone number for the selected country"
                        else null
                    }
                    var countryDropdownExpanded by remember { mutableStateOf(false) }
                    
                    SettingCard(title = "Phone Number") {
                        // Country row only – full width, single line so it doesn't take 2 lines
                        ExposedDropdownMenuBox(
                            expanded = countryDropdownExpanded,
                            onExpandedChange = { countryDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedCountry?.let { "${it.callingCodeDisplay} ${it.displayName}" } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Country") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                singleLine = true,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = countryDropdownExpanded,
                                onDismissRequest = { countryDropdownExpanded = false }
                            ) {
                                countries.forEach { country ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${country.callingCodeDisplay} ${country.displayName}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        onClick = {
                                            selectedRegionCode = country.regionCode
                                            countryDropdownExpanded = false
                                            if (!isNewContact && validateAndToE164(nationalNumber, country.regionCode) != null) {
                                                saveContact(
                                                    viewModel, existingContact, name,
                                                    validateAndToE164(nationalNumber, country.regionCode)!!,
                                                    effectiveContactType, autoAnswerEnabled, notifyBatteryAlerts, selectedColor
                                                )
                                                saveToastState.show("$name's phone saved")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // Number row only – full width so you can edit and move cursor normally (no per-keystroke save to avoid reset)
                        OutlinedTextField(
                            value = nationalNumber,
                            onValueChange = { newVal ->
                                val digitsOnly = newVal.filter { it.isDigit() }
                                nationalNumber = digitsOnly
                            },
                            label = { Text("Number") },
                            placeholder = { Text("e.g. 7911123456") },
                            singleLine = true,
                            maxLines = 1,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
                    
                    // Button Color (Assistants: home screen; Friends: list screens)
                    SettingCard(title = "Button Color") {
                        val colorDescription = when (effectiveContactType) {
                            ContactType.CARER -> "Choose a color for this assistant's button on the home screen"
                            ContactType.GREY_LIST -> "Choose a color for this friend in list screens"
                        }
                        Text(
                            text = colorDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Color swatches in a row — equal width per swatch so the last is never squashed
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                                viewModel, existingContact, name, phoneForSave,
                                                effectiveContactType, autoAnswerEnabled, notifyBatteryAlerts,
                                                buttonColor
                                            )
                                            saveToastState.show("$name's color: ${buttonColor.displayName}")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    
                    // Auto-Answer (Carers only, Level 1+)
                    LevelGatedContent(
                        minLevel = FeatureLevel.MINIMAL,
                        currentLevel = featureLevel
                    ) {
                        if (effectiveContactType == ContactType.CARER) {
                            SettingCard(title = "Auto-Answer") {
                                SettingToggle(
                                    title = "Auto-Answer for this assistant",
                                    description = "Phone answers automatically when they call",
                                    checked = autoAnswerEnabled,
                                    onCheckedChange = { enabled ->
                                        autoAnswerEnabled = enabled
                                        if (!isNewContact) {
saveContact(
                                        viewModel, existingContact, name, phoneForSave,
                                        effectiveContactType, autoAnswerEnabled, notifyBatteryAlerts, selectedColor
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
                                            text = "⚠️ Privacy warning: This assistant can listen without the user answering",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF795548),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Notify for battery alerts (Carers only, Level 1)
                    if (effectiveContactType == ContactType.CARER) {
                        SettingCard(title = "Battery alerts") {
                            SettingToggle(
                                title = "Notify for battery alerts",
                                description = "Send this assistant a text when battery is low or when the device is plugged in after low battery",
                                checked = notifyBatteryAlerts,
                                onCheckedChange = { enabled ->
                                    notifyBatteryAlerts = enabled
                                    if (!isNewContact) {
saveContact(
                                        viewModel, existingContact, name, phoneForSave,
                                        effectiveContactType, autoAnswerEnabled, notifyBatteryAlerts, selectedColor
                                    )
                                        saveToastState.show("$name's battery alerts ${if (enabled) "on" else "off"}")
                                    }
                                }
                            )
                        }
                    }
                    
                    // Save button for new contacts
                    if (isNewContact) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val canSave = name.isNotBlank() && phoneNumberE164 != null
                        
                        Button(
                            onClick = {
                                if (canSave) {
                                    val newContact = Contact(
                                        id = 0,
                                        name = name.trim(),
                                        phoneNumber = phoneForSave,
                                        photoUri = null,
                                        priority = 0,
                                        contactType = effectiveContactType,
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis(),
                                        buttonColor = if (selectedColor == ButtonColor.DEFAULT) null else selectedColor.argb,
                                        autoAnswerEnabled = autoAnswerEnabled,
                                        notifyBatteryAlerts = notifyBatteryAlerts,
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
    autoAnswerEnabled: Boolean,
    notifyBatteryAlerts: Boolean,
    buttonColor: ButtonColor = ButtonColor.DEFAULT
) {
    existingContact?.let { contact ->
        viewModel.saveContact(
            contact.copy(
                name = name,
                phoneNumber = phoneNumber,
                contactType = contactType,
                autoAnswerEnabled = autoAnswerEnabled,
                notifyBatteryAlerts = notifyBatteryAlerts,
                buttonColor = if (buttonColor == ButtonColor.DEFAULT) null else buttonColor.argb,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}

/**
 * Color swatch for button color selection.
 * Use modifier = Modifier.weight(1f) in a Row for equal-width layout that avoids squashing the last item.
 */
@Composable
private fun ColorSwatch(
    color: ButtonColor,
    isSelected: Boolean,
    themeDefaultColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val swatchColor = if (color == ButtonColor.DEFAULT) {
        themeDefaultColor
    } else {
        Color(color.argb)
    }
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
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
        ContactType.CARER -> "Assistant"
        ContactType.GREY_LIST -> "Friends"
    }
}

private fun getContactTypeDescription(type: ContactType): String {
    return when (type) {
        ContactType.CARER -> "Appears on home screen, triggers missed call reminders"
        ContactType.GREY_LIST -> "Can answer calls. At Level 2+, enable list buttons in Appearance to call back."
    }
}
