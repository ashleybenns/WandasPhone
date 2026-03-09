package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.CarerSettingsViewModel
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
    onNavigateToTomsPhoneDescription: () -> Unit,
    onNavigateToUserProfile: () -> Unit,
    onNavigateToAssistants: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToCallHandling: () -> Unit,
    onNavigateToTouchResponse: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToFeatureLevel: () -> Unit,
    onNavigateToAlwaysOn: () -> Unit,
    onNavigateToFactoryReset: () -> Unit,
    onNavigateToSupportSuggestions: () -> Unit,
    onExitApp: () -> Unit,
    onBack: () -> Unit,
    viewModel: CarerSettingsViewModel = hiltViewModel(),
    supportViewModel: com.tomsphone.feature.carer.support.SupportSuggestionsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val featureLevel = settings.featureLevel
    val supportUnreadCount by supportViewModel.unreadCount.collectAsState(initial = 0)

    LaunchedEffect(Unit) {
        supportViewModel.ensureDeviceId()
        supportViewModel.refreshUnreadCount()
    }
    
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
                title = "Assistant Settings",
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
                // Tom's Phone Description - first, informational
                CarerMenuButton(
                    title = "Tom's Phone Description",
                    description = "The story behind this phone app for seniors",
                    onClick = onNavigateToTomsPhoneDescription,
                    currentLevel = featureLevel
                )

                // User Profile - always visible
                CarerMenuButton(
                    title = "User Profile",
                    description = "Name, emergency info",
                    onClick = onNavigateToUserProfile,
                    currentLevel = featureLevel
                )
                
                // Assistants - home screen buttons, reorder, colors
                CarerMenuButton(
                    title = "Assistants",
                    description = "Home screen buttons, reorder, colors",
                    onClick = onNavigateToAssistants,
                    currentLevel = featureLevel
                )

                // Friends - answer-only contacts
                CarerMenuButton(
                    title = "Friends",
                    description = "Answer-only contacts, call back from lists",
                    onClick = onNavigateToFriends,
                    currentLevel = featureLevel
                )
                
                // Call Handling - always visible
                CarerMenuButton(
                    title = "Call Handling",
                    description = "Auto-answer, speakerphone, missed calls",
                    onClick = onNavigateToCallHandling,
                    currentLevel = featureLevel
                )
                
                // Touch Response - always visible (fundamental accessibility)
                CarerMenuButton(
                    title = "Touch Response",
                    description = "How buttons respond: tap, press, or double-tap",
                    onClick = onNavigateToTouchResponse,
                    currentLevel = featureLevel
                )
                
                // Appearance - always visible (accessibility is essential)
                CarerMenuButton(
                    title = "Appearance",
                    description = "Theme, text size, list alignment",
                    onClick = onNavigateToAppearance,
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
                
                // Support & suggestions - anonymous feedback, unread badge
                CarerMenuButton(
                    title = "Support & suggestions",
                    description = "Get support or suggest improvements (anonymous)",
                    onClick = onNavigateToSupportSuggestions,
                    currentLevel = featureLevel,
                    unreadCount = supportUnreadCount
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Exit App - unpin and close
                OutlinedButton(
                    onClick = onExitApp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(WandasDimensions.CornerRadiusMedium),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.wandasColors.onBackground
                    )
                ) {
                    Text(
                        text = "Exit App",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Text(
                    text = "Unpin and close the app (assistant escape)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.wandasColors.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
