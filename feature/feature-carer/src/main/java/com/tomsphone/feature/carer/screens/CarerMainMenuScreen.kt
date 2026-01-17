package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tomsphone.core.config.FeatureLevel
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.components.CarerBreadcrumb
import com.tomsphone.feature.carer.components.CarerMenuButton
import com.tomsphone.feature.carer.components.DevLevelIndicator

/**
 * Main menu for carer settings.
 * 
 * Shows category buttons with descriptions.
 * Some categories are level-gated.
 */
@Composable
fun CarerMainMenuScreen(
    featureLevel: FeatureLevel,
    onNavigateToUserProfile: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToCallHandling: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToFeatureLevel: () -> Unit,
    onNavigateToAlwaysOn: () -> Unit,
    onNavigateToFactoryReset: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Dev level indicator
            DevLevelIndicator(level = featureLevel)
            
            // Breadcrumb / title
            CarerBreadcrumb(
                title = "Carer Settings",
                onBack = onBack
            )
            
            // Menu items
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(WandasDimensions.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingMedium)
            ) {
                // User Profile - always visible
                CarerMenuButton(
                    title = "User Profile",
                    description = "Name, emergency info",
                    onClick = onNavigateToUserProfile,
                    currentLevel = featureLevel
                )
                
                // Contacts - always visible
                CarerMenuButton(
                    title = "Contacts",
                    description = "Manage carers, button colors",
                    onClick = onNavigateToContacts,
                    currentLevel = featureLevel
                )
                
                // Call Handling - always visible
                CarerMenuButton(
                    title = "Call Handling",
                    description = "Auto-answer, speakerphone, missed calls",
                    onClick = onNavigateToCallHandling,
                    currentLevel = featureLevel
                )
                
                // Appearance - Level 2+
                CarerMenuButton(
                    title = "Appearance",
                    description = "Theme, text size, button size",
                    onClick = onNavigateToAppearance,
                    minLevel = FeatureLevel.BASIC,
                    currentLevel = featureLevel
                )
                
                // Feature Level - always visible (how else would they upgrade?)
                CarerMenuButton(
                    title = "Feature Level",
                    description = "Choose plan, see what's available",
                    onClick = onNavigateToFeatureLevel,
                    currentLevel = featureLevel
                )
                
                // Always On Mode - always visible
                CarerMenuButton(
                    title = "Always On Mode",
                    description = "Charging stand, pinned mode",
                    onClick = onNavigateToAlwaysOn,
                    currentLevel = featureLevel
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Factory Reset - always visible, red to indicate danger
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Button(
                    onClick = onNavigateToFactoryReset,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(WandasDimensions.CornerRadiusMedium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),  // Red
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Factory Reset",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Text(
                    text = "Wipe all data before giving phone to new user",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.wandasColors.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
