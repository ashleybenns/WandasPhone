package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.config.FeatureLevel
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.CarerSettingsViewModel
import com.tomsphone.feature.carer.components.*

/**
 * User profile settings screen.
 * 
 * Contains:
 * - User name (all levels)
 * - Emergency number (all levels)
 * - Future: blood type, meds, etc. (Level 2+)
 */
@Composable
fun UserProfileScreen(
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
                    title = "User Profile",
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
                    // User Name
                    SettingCard(title = "User Name") {
                        var name by remember { mutableStateOf(settings.userName) }
                        
                        LaunchedEffect(settings.userName) {
                            name = settings.userName
                        }
                        
                        OutlinedTextField(
                            value = name,
                            onValueChange = { newName ->
                                name = newName
                                viewModel.setUserName(newName)
                                saveToastState.show("User name saved")
                            },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            text = "This name is used in voice announcements and on-screen messages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    // Emergency Number
                    SettingCard(title = "Emergency Number") {
                        var number by remember { mutableStateOf(settings.emergencyNumber) }
                        
                        LaunchedEffect(settings.emergencyNumber) {
                            number = settings.emergencyNumber
                        }
                        
                        OutlinedTextField(
                            value = number,
                            onValueChange = { newNumber ->
                                number = newNumber
                                viewModel.setEmergencyNumber(newNumber)
                                saveToastState.show("Emergency number saved")
                            },
                            label = { Text("Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            text = "UK default: 999",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    // Future: Level 2+ fields
                    LevelGatedContent(
                        minLevel = FeatureLevel.BASIC,
                        currentLevel = featureLevel
                    ) {
                        SettingCard(title = "Medical Information") {
                            Text(
                                text = "Blood type, medications, and allergies will be available in a future update.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
            
            // Save toast
            SaveToast(
                message = saveToastState.message,
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Card wrapper for a setting group.
 */
@Composable
fun SettingCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.wandasColors.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(WandasDimensions.SpacingMedium)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.wandasColors.onSurface
            )
            
            Spacer(modifier = Modifier.height(WandasDimensions.SpacingSmall))
            
            content()
        }
    }
}
