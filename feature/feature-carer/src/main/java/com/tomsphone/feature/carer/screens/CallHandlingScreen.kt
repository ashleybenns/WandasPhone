package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import android.Manifest
import com.tomsphone.core.config.CarerSettings
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
    onBack: () -> Unit,
    viewModel: CarerSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val featureLevel = settings.featureLevel
    val saveToastState = rememberSaveToastState()

    // When opening this screen, sync device volumes to saved settings
    LaunchedEffect(Unit) {
        viewModel.syncVolumesToDevice()
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
                    title = "Call Handling",
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
                    // Volume - ringtone and call volume without leaving the app
                    SettingCard(title = "Volume") {
                        Text(
                            text = "Adjust ringtone and call volume here so you don't need to leave the app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = "Ringtone volume",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.wandasColors.onSurface
                        )
                        Text(
                            text = "Volume for incoming call ring. ${settings.ringtoneVolume}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Slider(
                            value = settings.ringtoneVolume.toFloat(),
                            onValueChange = { percent ->
                                viewModel.setRingtoneVolume(percent.toInt())
                            },
                            onValueChangeFinished = {
                                saveToastState.show("Ringtone volume saved")
                            },
                            valueRange = 0f..100f,
                            steps = 19
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Call (speaker) volume",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.wandasColors.onSurface
                        )
                        Text(
                            text = "Volume during calls. Restored when each call ends. ${settings.speakerVolume}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Slider(
                            value = settings.speakerVolume.toFloat(),
                            onValueChange = { percent ->
                                viewModel.setSpeakerVolume(percent.toInt())
                            },
                            onValueChangeFinished = {
                                saveToastState.show("Call volume saved")
                            },
                            valueRange = 0f..100f,
                            steps = 19
                        )
                    }

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

                    // Battery alert SMS (Level 1)
                    BatteryAlertSmsCard(
                        settings = settings,
                        viewModel = viewModel,
                        saveToastState = saveToastState
                    )
                    
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
                    }
                    
                    // Voice Announcements (Level 1)
                    SettingCard(title = "Voice Announcements") {
                        Text(
                            text = "Spoken feedback for actions like calling, speaker toggle, and battery alerts. Separate from ringtone and missed call reminders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        SettingToggle(
                            title = "Enable Announcements",
                            description = "Greeting, calling, call ended, speaker, mute, battery",
                            checked = settings.ttsAnnouncementsEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setTtsAnnouncementsEnabled(enabled)
                                saveToastState.show(
                                    if (enabled) "Voice announcements enabled"
                                    else "Voice announcements disabled"
                                )
                            }
                        )
                    }
                    
                    // Speaker Toggle Button - Level 2+ only
                    LevelGatedContent(
                        minLevel = FeatureLevel.BASIC,
                        currentLevel = featureLevel
                    ) {
                        SettingCard(title = "Speaker Button") {
                            Text(
                                text = "Show a button during calls to toggle speaker on/off. Double-tap required to prevent accidents. Speaker returns to default after each call.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            SettingToggle(
                                title = "Show Speaker Button",
                                description = "Display speaker toggle on call screens",
                                checked = settings.showSpeakerButton,
                                onCheckedChange = { enabled ->
                                    viewModel.setShowSpeakerButton(enabled)
                                    saveToastState.show("Speaker button ${if (enabled) "enabled" else "disabled"}")
                                }
                            )
                            
                            if (settings.showSpeakerButton) {
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                SettingToggle(
                                    title = "Speaker On by Default",
                                    description = "Start each call with speaker on",
                                    checked = settings.speakerDefaultOn,
                                    onCheckedChange = { enabled ->
                                        viewModel.setSpeakerDefaultOn(enabled)
                                        saveToastState.show("Default speaker ${if (enabled) "on" else "off"}")
                                    }
                                )
                            }
                        }
                    }
                    
                    // Auto-Answer - Available at Level 1 (requires no user interaction)
                    SettingCard(
                        title = "Auto-Answer",
                        trailingContent = {
                            InfoTipButton(
                                tipId = "auto_answer",
                                tipTitle = "Auto-Answer",
                                tipContent = viewModel.getOnboardingTip("auto_answer"),
                                onTipViewed = { viewModel.onTipViewed(it) }
                            )
                        }
                    ) {
                            // Privacy warning
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFFFFF3E0), // Light orange/warning
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = "⚠️ Privacy Notice",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100) // Dark orange
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Auto-Answer allows assistants to call and listen without the user pressing Answer. " +
                                               "The user will hear a ringtone and announcement when a call is answered automatically. " +
                                               "This feature requires the user's informed consent.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF5D4037) // Brown text
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            SettingToggle(
                                title = "Enable Auto-Answer",
                                description = "Automatically answer calls from enabled assistants",
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
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Configure which assistants have auto-answer in Assistants.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    
                    // Missed Call Nag
                    SettingCard(title = "Missed Call Reminders") {
                        SettingToggle(
                            title = "Enable Reminders",
                            description = "Remind user to call back missed calls from assistants",
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun BatteryAlertSmsCard(
    settings: CarerSettings,
    viewModel: CarerSettingsViewModel,
    saveToastState: SaveToastState
) {
    val smsPermissionState = rememberPermissionState(Manifest.permission.SEND_SMS)
    val batteryAlertStatus by viewModel.batteryAlertStatus.collectAsState()
    val (hasSmsPermission, recipientCount) = batteryAlertStatus
    var testSending by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshBatteryAlertStatus()
    }
    LaunchedEffect(smsPermissionState.status) {
        viewModel.refreshBatteryAlertStatus()
    }

    SettingCard(title = "Battery alert texts") {
        Text(
            text = "Send a text to assistants when battery is low or when the device is plugged in after low battery. Turn on \"Notify for battery alerts\" for each assistant in Assistants.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingToggle(
            title = "Send battery alert texts",
            description = "Low battery and device connected after low battery",
            checked = settings.batteryAlertSmsEnabled,
            onCheckedChange = { enabled ->
                if (enabled) {
                    viewModel.setBatteryAlertSmsEnabled(true)
                    smsPermissionState.launchPermissionRequest()
                    saveToastState.show("Battery alerts on. Grant SMS when prompted.")
                } else {
                    viewModel.setBatteryAlertSmsEnabled(false)
                    saveToastState.show("Battery alert texts disabled")
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "SMS permission: ${if (hasSmsPermission) "granted" else "denied"}",
            style = MaterialTheme.typography.bodySmall,
            color = if (hasSmsPermission) MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f) else Color(0xFFB00020),
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = "Battery alert recipients: $recipientCount",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Button(
            onClick = {
                if (testSending) return@Button
                testSending = true
                viewModel.sendTestBatteryAlertSms { message ->
                    saveToastState.show(message)
                    testSending = false
                }
            },
            enabled = !testSending,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (testSending) "Sending…" else "Send test text")
        }
    }
}
