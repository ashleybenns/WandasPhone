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
import com.tomsphone.core.config.MissedCallNagInterval
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.CarerSettingsViewModel
import com.tomsphone.feature.carer.components.*

/**
 * Call handling settings screen.
 * 
 * Contains:
 * - Reject unknown calls
 * - Speakerphone settings
 * - Auto-answer settings (Level 2+ per-contact)
 * - Missed call nag settings
 */
@Composable
fun CallHandlingScreen(
    featureLevel: FeatureLevel,
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
                // Dev level indicator
                DevLevelIndicator(level = featureLevel)
                
                // Breadcrumb
                CarerBreadcrumb(
                    title = "Call Handling",
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
                    // Unknown Callers
                    SettingCard(title = "Unknown Callers") {
                        SettingToggle(
                            title = "Reject Unknown Calls",
                            description = "Silently reject calls not in contacts",
                            checked = settings.rejectUnknownCalls,
                            onCheckedChange = { enabled ->
                                viewModel.setRejectUnknownCalls(enabled)
                                saveToastState.show("Unknown call handling saved")
                            }
                        )
                    }
                    
                    // Speakerphone
                    SettingCard(title = "Speakerphone") {
                        SettingToggle(
                            title = "Always On Speaker",
                            description = "All calls use speakerphone",
                            checked = settings.speakerphoneAlwaysOn,
                            onCheckedChange = { enabled ->
                                viewModel.setSpeakerphoneAlwaysOn(enabled)
                                saveToastState.show("Speakerphone setting saved")
                            }
                        )
                        
                        LevelGatedContent(
                            minLevel = FeatureLevel.BASIC,
                            currentLevel = featureLevel
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "In Comfortable mode: Speaker toggle button is available during calls when this is off",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    
                    // Auto-Answer - Level 2+ only (security/privacy concerns at Level 1)
                    LevelGatedContent(
                        minLevel = FeatureLevel.BASIC,
                        currentLevel = featureLevel
                    ) {
                        SettingCard(title = "Auto-Answer") {
                            SettingToggle(
                                title = "Enable Auto-Answer",
                                description = "Automatically answer calls from carers",
                                checked = settings.autoAnswerEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.setAutoAnswer(enabled, settings.autoAnswerDelaySeconds)
                                    saveToastState.show("Auto-answer ${if (enabled) "enabled" else "disabled"}")
                                }
                            )
                            
                            if (settings.autoAnswerEnabled) {
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = "Delay before answering: ${settings.autoAnswerDelaySeconds} seconds",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.wandasColors.onSurface
                                )
                                
                                Slider(
                                    value = settings.autoAnswerDelaySeconds.toFloat(),
                                    onValueChange = { delay ->
                                        viewModel.setAutoAnswer(true, delay.toInt())
                                    },
                                    onValueChangeFinished = {
                                        saveToastState.show("Auto-answer delay saved")
                                    },
                                    valueRange = 1f..10f,
                                    steps = 8
                                )
                            }
                        }
                    }
                    
                    // Missed Call Nag
                    SettingCard(title = "Missed Call Reminders") {
                        SettingToggle(
                            title = "Enable Reminders",
                            description = "Remind user to call back missed calls from carers",
                            checked = settings.missedCallNagEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setMissedCallNagEnabled(enabled)
                                saveToastState.show("Missed call reminders ${if (enabled) "enabled" else "disabled"}")
                            }
                        )
                        
                        if (settings.missedCallNagEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "Reminder Interval",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.wandasColors.onSurface
                            )
                            
                            MissedCallNagInterval.entries.forEach { interval ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = settings.missedCallNagInterval == interval,
                                        onClick = {
                                            viewModel.setMissedCallNagInterval(interval)
                                            saveToastState.show("Reminder interval saved")
                                        }
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Text(
                                        text = interval.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.wandasColors.onSurface
                                    )
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
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
