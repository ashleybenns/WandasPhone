package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.CarerSettingsViewModel
import com.tomsphone.feature.carer.components.*

/**
 * Touch Response settings screen (Level 1)
 * 
 * Fundamental setting for how buttons respond to touch.
 * Carers observe how the user naturally touches and select the best match.
 * 
 * Key insight: We adapt to their behavior, not train new behavior.
 */
@Composable
fun TouchResponseScreen(
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
                    title = "Touch Response",
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
                    // Introduction
                    SettingCard(title = "How should buttons respond?") {
                        Text(
                            text = "Match the mode to how they already touch — don’t expect them to learn a new style.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Activation Presets
                    ButtonActivationPreset.entries.forEach { preset ->
                        ActivationPresetCard(
                            preset = preset,
                            isSelected = settings.buttonActivation == preset,
                            onClick = {
                                viewModel.setButtonActivation(preset)
                                saveToastState.show("Touch mode: ${preset.displayName}")
                            }
                        )
                    }
                    
                    // Accumulated Tap tuning - only visible when that mode is selected
                    if (settings.buttonActivation == ButtonActivationPreset.ACCUMULATED_TAP) {
                        SettingCard(title = "Tune Accumulated Tap") {
                            Text(
                                text = "Short touches add up if each is long enough to count; total must hit the threshold before the idle timeout.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            // Shake protection (debounce) - minimum touch duration
                            Text(
                                text = "Shake protection (minimum touch):",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.wandasColors.onSurface
                            )
                            Text(
                                text = "Shorter touches ignored as brushes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DebouncePresetButton(
                                    label = "None",
                                    valueMs = 0,
                                    isSelected = settings.touchDebounceMs == 0,
                                    onClick = {
                                        viewModel.setTouchDebounceMs(0)
                                        saveToastState.show("No shake protection")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                DebouncePresetButton(
                                    label = "Light",
                                    valueMs = 100,
                                    isSelected = settings.touchDebounceMs in 1..100,
                                    onClick = {
                                        viewModel.setTouchDebounceMs(100)
                                        saveToastState.show("Light shake protection")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                DebouncePresetButton(
                                    label = "Medium",
                                    valueMs = 200,
                                    isSelected = settings.touchDebounceMs in 101..200,
                                    onClick = {
                                        viewModel.setTouchDebounceMs(200)
                                        saveToastState.show("Medium shake protection")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                DebouncePresetButton(
                                    label = "Strong",
                                    valueMs = 350,
                                    isSelected = settings.touchDebounceMs > 200,
                                    onClick = {
                                        viewModel.setTouchDebounceMs(350)
                                        saveToastState.show("Strong shake protection")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Threshold setting
                            Text(
                                text = "Total touch time to activate:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.wandasColors.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AccumulatedPresetButton(
                                    label = "0.3s",
                                    isSelected = settings.accumulatedTapThresholdMs in 250..350,
                                    onClick = {
                                        viewModel.setAccumulatedTapThresholdMs(300)
                                        saveToastState.show("Threshold: 0.3 seconds")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                AccumulatedPresetButton(
                                    label = "0.5s",
                                    isSelected = settings.accumulatedTapThresholdMs in 450..550,
                                    onClick = {
                                        viewModel.setAccumulatedTapThresholdMs(500)
                                        saveToastState.show("Threshold: 0.5 seconds")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                AccumulatedPresetButton(
                                    label = "0.8s",
                                    isSelected = settings.accumulatedTapThresholdMs in 700..900,
                                    onClick = {
                                        viewModel.setAccumulatedTapThresholdMs(800)
                                        saveToastState.show("Threshold: 0.8 seconds")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                AccumulatedPresetButton(
                                    label = "1.2s",
                                    isSelected = settings.accumulatedTapThresholdMs > 1000,
                                    onClick = {
                                        viewModel.setAccumulatedTapThresholdMs(1200)
                                        saveToastState.show("Threshold: 1.2 seconds")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Timeout setting
                            Text(
                                text = "Time before counter resets:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.wandasColors.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AccumulatedPresetButton(
                                    label = "2s",
                                    isSelected = settings.accumulatedTapTimeoutMs in 1500..2500,
                                    onClick = {
                                        viewModel.setAccumulatedTapTimeoutMs(2000)
                                        saveToastState.show("Timeout: 2 seconds")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                AccumulatedPresetButton(
                                    label = "3s",
                                    isSelected = settings.accumulatedTapTimeoutMs in 2500..3500,
                                    onClick = {
                                        viewModel.setAccumulatedTapTimeoutMs(3000)
                                        saveToastState.show("Timeout: 3 seconds")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                AccumulatedPresetButton(
                                    label = "5s",
                                    isSelected = settings.accumulatedTapTimeoutMs in 4000..6000,
                                    onClick = {
                                        viewModel.setAccumulatedTapTimeoutMs(5000)
                                        saveToastState.show("Timeout: 5 seconds")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                AccumulatedPresetButton(
                                    label = "10s",
                                    isSelected = settings.accumulatedTapTimeoutMs > 8000,
                                    onClick = {
                                        viewModel.setAccumulatedTapTimeoutMs(10000)
                                        saveToastState.show("Timeout: 10 seconds")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    
                    // Debounce / Shake Protection - only show for non-accumulated modes
                    if (settings.buttonActivation != ButtonActivationPreset.ACCUMULATED_TAP) {
                        SettingCard(title = "Shake Protection") {
                            Text(
                                text = "Ignore very brief touches (accidental brushes).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            // Show current value
                            Text(
                                text = "Ignore touches under: ${settings.touchDebounceMs}ms",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.wandasColors.onSurface
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Preset buttons instead of confusing slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DebouncePresetButton(
                                    label = "None",
                                    valueMs = 0,
                                    isSelected = settings.touchDebounceMs == 0,
                                    onClick = {
                                        viewModel.setTouchDebounceMs(0)
                                        saveToastState.show("No shake protection")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                DebouncePresetButton(
                                    label = "Light",
                                    valueMs = 100,
                                    isSelected = settings.touchDebounceMs in 1..100,
                                    onClick = {
                                        viewModel.setTouchDebounceMs(100)
                                        saveToastState.show("Light shake protection")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                DebouncePresetButton(
                                    label = "Medium",
                                    valueMs = 200,
                                    isSelected = settings.touchDebounceMs in 101..200,
                                    onClick = {
                                        viewModel.setTouchDebounceMs(200)
                                        saveToastState.show("Medium shake protection")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                DebouncePresetButton(
                                    label = "Strong",
                                    valueMs = 350,
                                    isSelected = settings.touchDebounceMs > 200,
                                    onClick = {
                                        viewModel.setTouchDebounceMs(350)
                                        saveToastState.show("Strong shake protection")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    
                    // Special buttons note
                    SettingCard(title = "Special Buttons") {
                        Text(
                            text = "Always stricter, whatever you choose above:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "• End call: accumulated tap",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.wandasColors.onSurface
                            )
                            Text(
                                text = "• Emergency: 3 taps + confirm",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.wandasColors.onSurface
                            )
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

/**
 * Card for selecting an activation preset
 */
@Composable
private fun ActivationPresetCard(
    preset: ButtonActivationPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.wandasColors.onSurface.copy(alpha = 0.2f)
    }
    
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    } else {
        MaterialTheme.wandasColors.surface
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.wandasColors.onSurface.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.wandasColors.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Button for selecting an accumulated tap preset
 */
@Composable
private fun AccumulatedPresetButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.wandasColors.surface
    }
    
    val textColor = if (isSelected) {
        Color.White
    } else {
        MaterialTheme.wandasColors.onSurface
    }
    
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
        border = if (!isSelected) {
            androidx.compose.foundation.BorderStroke(
                1.dp, 
                MaterialTheme.wandasColors.onSurface.copy(alpha = 0.2f)
            )
        } else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
        }
    }
}

/**
 * Button for selecting a debounce preset
 */
@Composable
private fun DebouncePresetButton(
    label: String,
    valueMs: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.wandasColors.surface
    }
    
    val textColor = if (isSelected) {
        Color.White
    } else {
        MaterialTheme.wandasColors.onSurface
    }
    
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
        border = if (!isSelected) {
            androidx.compose.foundation.BorderStroke(
                1.dp, 
                MaterialTheme.wandasColors.onSurface.copy(alpha = 0.2f)
            )
        } else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
        }
    }
}
