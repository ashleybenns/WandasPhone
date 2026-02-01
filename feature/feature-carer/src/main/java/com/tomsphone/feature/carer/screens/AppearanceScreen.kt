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
                    
                    // Screen Off Button (Level 2+)
                    LevelGatedContent(
                        minLevel = FeatureLevel.BASIC,
                        currentLevel = featureLevel
                    ) {
                        SettingCard(title = "Screen Off Button") {
                            Text(
                                text = "Shows a button to turn off the screen. Useful for bedside use when the user can't find the power button. Any touch wakes the screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            SettingToggle(
                                title = "Show Screen Off Button",
                                description = "Appears below contact buttons",
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
