package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.config.FeatureLevel
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.CarerSettingsViewModel
import com.tomsphone.feature.carer.components.*

/**
 * Feature level selection screen.
 * 
 * Shows descriptions of each level and allows changing.
 * Placeholder for future paywall integration.
 */
@Composable
fun FeatureLevelScreen(
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
                    title = "Feature Level",
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
                        text = "Match the phone to your user's needs. Simpler is often better — each level is designed for what the user can comfortably handle.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f)
                    )
                    
                    FeatureLevel.entries.forEach { level ->
                        FeatureLevelCard(
                            level = level,
                            isSelected = settings.featureLevel == level,
                            onClick = {
                                viewModel.setFeatureLevel(level)
                                saveToastState.show("Feature level saved")
                            }
                        )
                    }
                    
                    // Paywall placeholder
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.wandasColors.surface.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(WandasDimensions.SpacingMedium)
                        ) {
                            Text(
                                text = "Subscription",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.5f)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Subscription tiers and payment will be available in a future release. All levels are currently available for testing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.5f)
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

@Composable
private fun FeatureLevelCard(
    level: FeatureLevel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (title, description, features) = getLevelInfo(level)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.wandasColors.primaryButton.copy(alpha = 0.1f)
            else 
                MaterialTheme.wandasColors.surface
        ),
        border = if (isSelected) 
            BorderStroke(2.dp, MaterialTheme.wandasColors.primaryButton)
        else 
            null
    ) {
        Column(
            modifier = Modifier.padding(WandasDimensions.SpacingMedium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Level ${level.level}: $title",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.wandasColors.onSurface
                )
                
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.wandasColors.primaryButton
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.wandasColors.primaryButton
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private fun getLevelInfo(level: FeatureLevel): Triple<String, String, List<String>> {
    return when (level) {
        FeatureLevel.MINIMAL -> Triple(
            "Simple",
            "The clearest possible interface. Perfect for users who need one-touch simplicity with no choices to make.",
            listOf(
                "Up to 3 large contact buttons",
                "Emergency button",
                "Always-on speakerphone",
                "Missed call reminders for carers"
            )
        )
        FeatureLevel.BASIC -> Triple(
            "Comfortable",
            "A few more options for users who are comfortable making simple choices.",
            listOf(
                "Up to 4 contact buttons",
                "Speaker toggle during calls",
                "Missed calls list",
                "Appearance options"
            )
        )
        FeatureLevel.STANDARD -> Triple(
            "Capable",
            "List navigation for users who can browse and select from multiple options.",
            listOf(
                "Up to 12 contacts with scrolling",
                "Contact photos",
                "Call history",
                "Basic text notifications"
            )
        )
        FeatureLevel.EXTENDED -> Triple(
            "Independent",
            "Full phone functionality for users who just need larger buttons and clearer design.",
            listOf(
                "Unlimited contacts",
                "Text input",
                "Voicemail access",
                "Full notification support"
            )
        )
    }
}
