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
                    // Theme Selection
                    SettingCard(title = "Theme") {
                        ThemeOption.entries.forEach { theme ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.ui.theme == theme,
                                    onClick = {
                                        viewModel.setTheme(theme)
                                        saveToastState.show("Theme saved")
                                    }
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = getThemeDisplayName(theme),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.wandasColors.onSurface
                                )
                            }
                        }
                    }
                    
                    // User Text Size
                    SettingCard(title = "User Text Size") {
                        Text(
                            text = "Controls text and button size on user screens (Home, Call). Carer screens stay at normal size.",
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
                                
                                Column {
                                    Text(
                                        text = textSize.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.wandasColors.onSurface
                                    )
                                    Text(
                                        text = "${(textSize.scale * 100).toInt()}% scale",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Button Size (placeholder)
                    SettingCard(title = "Button Size") {
                        Text(
                            text = "Button size adjustment will be available in a future update.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f)
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
