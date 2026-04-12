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
import com.tomsphone.core.config.HomeSlotAssignments
import com.tomsphone.core.telecom.contactIdsWithHomeCallButton
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
import android.Manifest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ContactEditScreen(
    contactId: Long,
    contactType: ContactType,  // Type is fixed - determined by entry point
    /** If 0–6, assign this home slot after a new contact is saved (from Home screen layout). */
    homeSlotPendingIndex: Int = -1,
    /** When adding from Assistants → Recent calls: pre-fill phone (E164 or raw). */
    initialPhoneFromCallLog: String = "",
    /** Override breadcrumb parent (e.g. "Recent calls"); empty = Contacts / Home layout. */
    parentBreadcrumbTitle: String = "",
    onBack: () -> Unit,
    viewModel: CarerSettingsViewModel = hiltViewModel()
) {
    val contacts by viewModel.contacts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val onHomeIds = remember(settings, contacts) {
        contactIdsWithHomeCallButton(settings)
    }
    val saveToastState = rememberSaveToastState()
    
    val isNewContact = contactId == 0L
    val existingContact = contacts.find { it.id == contactId }
    
    // Use existing contact's type if editing, otherwise use passed type
    val effectiveContactType = existingContact?.contactType ?: contactType

    /** Auto-answer: existing contact on a slot, or new contact being added for a layout slot. */
    val showAssistantDeviceOptions = remember(isNewContact, existingContact, onHomeIds, homeSlotPendingIndex) {
        (!isNewContact && existingContact != null && existingContact.id in onHomeIds) ||
            (isNewContact && homeSlotPendingIndex >= 0)
    }
    /** Battery SMS: offered for every Assistant contact (no separate Call Handling master switch). */
    val showBatteryAlertsOption = effectiveContactType == ContactType.CARER
    val usesHomeButton = !isNewContact && existingContact != null && existingContact.id in onHomeIds

    val smsPermissionState = rememberPermissionState(Manifest.permission.SEND_SMS)

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
    
    // Update form when contact loads, or seed new contact from recent-calls / deep link
    LaunchedEffect(contactId, existingContact, defaultRegion, initialPhoneFromCallLog) {
        when {
            contactId != 0L && existingContact != null -> {
                val it = existingContact
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
            }
            contactId == 0L && initialPhoneFromCallLog.isNotBlank() -> {
                name = ""
                autoAnswerEnabled = false
                notifyBatteryAlerts = false
                selectedColor = ButtonColor.DEFAULT
                val parsed = PhoneNumberUtils.parseToRegionAndNational(
                    initialPhoneFromCallLog.trim(),
                    defaultRegion
                )
                if (parsed != null) {
                    selectedRegionCode = parsed.first
                    nationalNumber = parsed.second
                } else {
                    selectedRegionCode = defaultRegion
                    nationalNumber = initialPhoneFromCallLog.filter { it.isDigit() }
                }
            }
            contactId == 0L -> {
                if (name.isEmpty() && nationalNumber.isEmpty()) {
                    selectedRegionCode = defaultRegion
                }
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
                // Breadcrumb
                CarerBreadcrumb(
                    title = if (isNewContact) "Add Contact" else name.ifEmpty { "Edit Contact" },
                    parentTitle = when {
                        homeSlotPendingIndex >= 0 -> "Home screen layout"
                        parentBreadcrumbTitle.isNotBlank() -> parentBreadcrumbTitle
                        else -> "Contacts"
                    },
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
                        // Number row – save when digits form a valid E.164 for the selected country (same as country change),
                        // so we don't persist invalid partial numbers or spam saves on every keystroke while invalid.
                        OutlinedTextField(
                            value = nationalNumber,
                            onValueChange = { newVal ->
                                val digitsOnly = newVal.filter { it.isDigit() }
                                nationalNumber = digitsOnly
                                if (!isNewContact && existingContact != null) {
                                    val e164 = validateAndToE164(digitsOnly, selectedRegionCode)
                                    if (e164 != null && e164 != existingContact.phoneNumber) {
                                        saveContact(
                                            viewModel,
                                            existingContact,
                                            name,
                                            e164,
                                            effectiveContactType,
                                            autoAnswerEnabled,
                                            notifyBatteryAlerts,
                                            selectedColor
                                        )
                                        saveToastState.show("$name's phone saved")
                                    }
                                }
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
                    
                    // Home button = assistant for this app (slot-based)
                    SettingCard(title = "Home screen") {
                        Text(
                            text = when {
                                usesHomeButton ->
                                    "This contact has a home call button. Assistant options (auto-answer, battery texts, missed-call reminders) apply while they stay on a slot."
                                isNewContact && homeSlotPendingIndex >= 0 ->
                                    "You're adding this person for home slot ${homeSlotPendingIndex + 1}. After you save, that slot will get their call button and the options below will apply."
                                else ->
                                    "Add the contact to a slot on the Home Screen promote it to an Assistant. Remove from the slot to demote it."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.85f)
                        )
                    }
                    
                    SettingCard(title = "Button Color") {
                        val colorDescription = when {
                            usesHomeButton || isNewContact ->
                                "Used on the home call button when they have a slot; also in on-phone contact lists"
                            else -> "Shown in on-phone contact lists (and on home if you assign a slot later)"
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
                    
                    if (showAssistantDeviceOptions) {
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

                    // Notify for battery alerts (all Assistant contacts; SMS only sent when on a home slot + permission)
                    if (showBatteryAlertsOption) {
                        SettingCard(title = "Battery alerts") {
                            SettingToggle(
                                title = "Notify for battery alerts",
                                description = "Send this assistant a text when battery is low or when the device is plugged in after low battery",
                                checked = notifyBatteryAlerts,
                                onCheckedChange = { enabled ->
                                    notifyBatteryAlerts = enabled
                                    if (enabled) {
                                        smsPermissionState.launchPermissionRequest()
                                        saveToastState.show("Grant SMS when prompted so alerts can be sent")
                                    }
                                    if (!isNewContact) {
                                        saveContact(
                                            viewModel, existingContact, name, phoneForSave,
                                            effectiveContactType, autoAnswerEnabled, notifyBatteryAlerts, selectedColor
                                        )
                                        if (!enabled) {
                                            saveToastState.show("$name's battery alerts off")
                                        }
                                    }
                                }
                            )
                            if (!smsPermissionState.status.isGranted) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "SMS permission is required on the phone for low-battery texts to be sent.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.65f)
                                )
                            }
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
                                    viewModel.saveContact(newContact) { newId ->
                                        if (homeSlotPendingIndex in 0 until HomeSlotAssignments.SLOT_COUNT) {
                                            viewModel.setHomeSlotAt(
                                                homeSlotPendingIndex,
                                                HomeSlotAssignments.contactSlot(newId)
                                            )
                                        }
                                        saveToastState.show("$name added")
                                        onBack()
                                    }
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

