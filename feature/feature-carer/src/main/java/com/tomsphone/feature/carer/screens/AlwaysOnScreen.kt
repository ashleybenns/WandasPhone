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
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.CarerSettingsViewModel
import com.tomsphone.feature.carer.components.*

/**
 * Always On Mode settings screen.
 * 
 * For use on a charging stand - keeps the phone visible and ready.
 */
@Composable
fun AlwaysOnScreen(
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
                    title = "Always On Mode",
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
                    Text(
                        text = "For use on a charging stand. Keeps the phone visible and ready at all times.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f)
                    )
                    
                    // Pinned Mode
                    SettingCard(title = "Pinned Mode") {
                        SettingToggle(
                            title = "Enable Pinned Mode",
                            description = "Prevents accidentally exiting the app",
                            checked = settings.pinnedModeEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setPinnedMode(enabled)
                                saveToastState.show("Pinned mode ${if (enabled) "enabled" else "disabled"}")
                            }
                        )
                    }
                    
                    // Screen Always On
                    SettingCard(title = "Screen") {
                        SettingToggle(
                            title = "Screen Always On",
                            description = "Screen never dims or sleeps",
                            checked = settings.screenAlwaysOn,
                            onCheckedChange = { enabled ->
                                viewModel.setScreenAlwaysOn(enabled)
                                saveToastState.show("Screen always on ${if (enabled) "enabled" else "disabled"}")
                            }
                        )
                    }
                    
                    // Volume Lock
                    SettingCard(title = "Volume") {
                        SettingToggle(
                            title = "Lock Volume Buttons",
                            description = "Prevent accidental volume changes",
                            checked = settings.lockVolumeButtons,
                            onCheckedChange = { enabled ->
                                viewModel.setLockVolumeButtons(enabled)
                                saveToastState.show("Volume lock ${if (enabled) "enabled" else "disabled"}")
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

/**
 * Toggle setting with title and description.
 */
@Composable
fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.wandasColors.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f)
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
