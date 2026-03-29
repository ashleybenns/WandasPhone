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
import com.tomsphone.core.config.ListTextAlignment
import com.tomsphone.core.config.UserTextSize
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.CarerSettingsViewModel
import com.tomsphone.feature.carer.components.*

/**
 * Appearance settings screen.
 * 
 * Contains:
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
    val saveToastState = rememberSaveToastState()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                CarerBreadcrumb(
                    title = "Appearance",
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
                    // Theme Selection - deferred pending user feedback
                    // Device color inversion (Settings > Accessibility) works with this app
                    SettingCard(title = "Theme") {
                        Text(
                            text = "High Contrast Light. For inverted colours, use the device Accessibility colour inversion — it works with this app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.wandasColors.onSurface
                        )
                    }
                    
                    // User Text Size
                    SettingCard(title = "User Text Size") {
                        Text(
                            text = "Larger suits short names; smaller fits longer names that wrap.",
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
                            text = "Left is easier to scan; centre looks more even.",
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
                            description = "Time in the status line",
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
