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
import com.tomsphone.feature.carer.components.CarerBreadcrumb
import com.tomsphone.feature.carer.components.CarerMenuButton

/**
 * Main menu for carer settings.
 */
@Composable
fun CarerMainMenuScreen(
    onNavigateToTomsPhoneDescription: () -> Unit,
    onNavigateToUserProfile: () -> Unit,
    onNavigateToContactsHub: () -> Unit,
    onNavigateToCallHandling: () -> Unit,
    onNavigateToTouchResponse: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToHomeLayout: () -> Unit,
    onNavigateToAlwaysOn: () -> Unit,
    onNavigateToFactoryReset: () -> Unit,
    onNavigateToSupportSuggestions: () -> Unit,
    onExitApp: () -> Unit,
    onBack: () -> Unit,
    supportViewModel: com.tomsphone.feature.carer.support.SupportSuggestionsViewModel = hiltViewModel()
) {
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
            CarerBreadcrumb(
                title = "Settings",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(WandasDimensions.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingMedium)
            ) {
                CarerMenuButton(
                    title = "User Profile",
                    description = "User Name, Emergency, Medical info, Photo",
                    onClick = onNavigateToUserProfile
                )
                CarerMenuButton(
                    title = "Home Screen Layout",
                    description = "Slots for up to Seven Buttons",
                    onClick = onNavigateToHomeLayout
                )
                CarerMenuButton(
                    title = "Contacts",
                    description = "Add, reorder or delete contacts, Recent Calls record",
                    onClick = onNavigateToContactsHub
                )
                CarerMenuButton(
                    title = "Call Handling",
                    description = "Volumes, Unknown Callers, Speakerphone, Auto-answer, Reminders, Battery texts, Voice Announcements",
                    onClick = onNavigateToCallHandling
                )
                CarerMenuButton(
                    title = "Touch Click Response",
                    description = "Tap, Press or Accumulated Tap",
                    onClick = onNavigateToTouchResponse
                )
                CarerMenuButton(
                    title = "Appearance",
                    description = "Text size, alignment, time display",
                    onClick = onNavigateToAppearance
                )
                CarerMenuButton(
                    title = "Always On",
                    description = "Pin app, screen awake, volume lock",
                    onClick = onNavigateToAlwaysOn
                )
                CarerMenuButton(
                    title = "Support",
                    description = "Messaging for Support or Suggestions",
                    onClick = onNavigateToSupportSuggestions,
                    unreadCount = supportUnreadCount
                )
                CarerMenuButton(
                    title = "About",
                    description = "Why this app exists and how it works",
                    onClick = onNavigateToTomsPhoneDescription
                )

                Spacer(modifier = Modifier.height(24.dp))

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Button(
                    onClick = onNavigateToFactoryReset,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(WandasDimensions.CornerRadiusMedium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Factory Reset",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Text(
                    text = "Wipes data before giving the phone to a new user",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.wandasColors.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                    text = "Unpin (if needed) and close",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.wandasColors.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
