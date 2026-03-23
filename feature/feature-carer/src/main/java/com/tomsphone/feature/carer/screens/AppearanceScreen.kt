package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.config.FeatureLevel
import com.tomsphone.core.config.ListTextAlignment
import com.tomsphone.core.config.ThemeOption
import com.tomsphone.core.config.UserTextSize
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.CarerSettingsViewModel
import com.tomsphone.feature.carer.components.*

/**
 * Appearance settings screen.
 * 
 * Level 2+ only. Contains:
 * - Theme selection
 * - Font scale
 * - Button size
 */
@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    viewModel: CarerSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val featureLevel = settings.featureLevel
    val saveToastState = rememberSaveToastState()
    
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
                    title = "Appearance",
                    parentTitle = "Assistant Settings",
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
                    // Theme Selection - deferred pending user feedback
                    // Device color inversion (Settings > Accessibility) works with this app
                    SettingCard(title = "Theme") {
                        Text(
                            text = "Using High Contrast Light theme.\n\nFor users who need inverted colors, use the device's Accessibility settings (Color Inversion) which works with this app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.wandasColors.onSurface
                        )
                    }
                    
                    // User Text Size
                    SettingCard(title = "User Text Size") {
                        Text(
                            text = "Text size adapts to screen and button count. Use Maximum for short names, reduce for longer names that need to wrap.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        UserTextSize.entries.forEach { textSize ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.ui.userTextSize == textSize,
                                    onClick = {
                                        viewModel.setUserTextSize(textSize)
                                        saveToastState.show("Text size saved: ${textSize.displayName}")
                                    }
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = textSize.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.wandasColors.onSurface
                                )
                            }
                        }
                    }
                    
                    // Button Size - handled automatically by text size setting
                    // Container heights are calculated from text size
                    
                    // List Text Alignment (Level 1 - always visible)
                    SettingCard(title = "List Text Alignment") {
                        Text(
                            text = "Left-aligned text is easier to scan down a list. Center looks more balanced.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        ListTextAlignment.entries.forEach { alignment ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.listTextAlignment == alignment,
                                    onClick = {
                                        viewModel.setListTextAlignment(alignment)
                                        saveToastState.show("Alignment saved: ${alignment.displayName}")
                                    }
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = alignment.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.wandasColors.onSurface
                                )
                            }
                        }
                    }
                    
                    // Time Display (Level 1)
                    SettingCard(title = "Status Display") {
                        SettingToggle(
                            title = "Show Time",
                            description = "Display current time in the status text area",
                            checked = settings.showTimeInStatus,
                            onCheckedChange = { enabled ->
                                viewModel.setShowTimeInStatus(enabled)
                                saveToastState.show(
                                    if (enabled) "Time display enabled"
                                    else "Time display disabled"
                                )
                            }
                        )
                    }
                    
                    // Missed call return (one-tap callback) — not the full recent-calls list
                    SettingCard(title = "Missed call return") {
                        Text(
                            text = "One home button to call back the same top missed or declined caller as the missed-calls count (everyone, including assistants). Different from “Missed calls list”, which opens the two-tap list. Uses one of the 7 slots.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        SettingToggle(
                            title = "Enable missed call return",
                            description = "Shows caller name or “No Missed Calls”. Tapping places that call.",
                            checked = settings.homeShowMissedCallReturnButton,
                            onCheckedChange = { enabled ->
                                viewModel.setShowMissedCallReturnButton(enabled)
                                saveToastState.show(
                                    if (enabled) "Missed call return enabled"
                                    else "Missed call return disabled"
                                )
                            }
                        )
                    }
                    
                    // Two-touch features: list buttons and screen off (each uses one of the 7 home slots)
                    SettingCard(title = "Home Screen Buttons") {
                        Text(
                            text = "Two-touch features (tap once to open list or screen, then tap again to choose): Missed calls list, Contacts (everyone), Screen off. Speaker toggle is available during calls (Call Handling). Each home button uses one of 7 slots.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Add buttons below. Each uses one slot on the home screen (max 7 total including call buttons).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        SettingToggle(
                            title = "Missed calls list",
                                description = "Two taps: missed and declined calls only (not answered or outgoing history)",
                                checked = settings.homeShowMissedCallsButton,
                                onCheckedChange = { enabled ->
                                    viewModel.setShowMissedCallsButton(enabled)
                                    saveToastState.show(
                                        if (enabled) "Missed calls list enabled"
                                        else "Missed calls list disabled"
                                    )
                                }
                            )
                        SettingToggle(
                            title = "Contacts button",
                                description = "Opens Contacts list: everyone (same list as carer settings)",
                                checked = settings.homeShowContactsListButton,
                                onCheckedChange = { enabled ->
                                    viewModel.setShowContactsListButton(enabled)
                                    saveToastState.show(
                                        if (enabled) "Contacts button enabled"
                                        else "Contacts button disabled"
                                    )
                                }
                            )
                            // Sub-option (only visible when contacts button enabled)
                            if (settings.homeShowContactsListButton) {
                                SettingToggle(
                                    title = "Without home button only",
                                    description = "On the Contacts list, hide people who have a home call button (slot)",
                                    checked = settings.homeContactsListShowGreyListOnly,
                                    onCheckedChange = { enabled ->
                                        viewModel.setContactsListShowGreyListOnly(enabled)
                                        saveToastState.show(
                                            if (enabled) "Contacts list: no home button only"
                                            else "Contacts list: everyone"
                                        )
                                    }
                                )
                            }
                            SettingToggle(
                                title = "Screen Off Button",
                                description = "Turn off display, any touch wakes",
                                checked = settings.showDisplayOffButton,
                                onCheckedChange = { enabled ->
                                    viewModel.setShowDisplayOffButton(enabled)
                                    saveToastState.show(
                                        if (enabled) "Screen Off button enabled"
                                        else "Screen Off button disabled"
                                    )
                                }
                            )
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

private fun getThemeDisplayName(theme: ThemeOption): String {
    return when (theme) {
        ThemeOption.HIGH_CONTRAST_LIGHT -> "High Contrast Light"
        ThemeOption.HIGH_CONTRAST_DARK -> "High Contrast Dark"
        ThemeOption.YELLOW_BLACK -> "Yellow on Black"
        ThemeOption.SOFT_CONTRAST -> "Soft Contrast"
    }
}
