package com.wandasphone.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wandasphone.core.config.FeatureLevel
import com.wandasphone.core.ui.components.ContactButton
import com.wandasphone.core.ui.components.EmergencyButton
import com.wandasphone.core.ui.components.InertBorderLayout
import com.wandasphone.core.ui.theme.WandasDimensions
import com.wandasphone.core.ui.theme.WandasTextStyles
import com.wandasphone.core.ui.theme.wandasColors
import java.text.SimpleDateFormat
import java.util.*

// DEV MODE: Set to false for production
private const val DEV_MODE = true

/**
 * Level 1 Home Screen
 * 
 * Features:
 * - Clock display
 * - 2 carer contact buttons (tap to call)
 * - Emergency button (3-tap protection)
 * - Hidden carer access (7 taps on clock)
 * - Inert border
 */
@Composable
fun HomeScreen(
    onNavigateToCall: () -> Unit,
    onNavigateToCarer: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val featureLevel by viewModel.featureLevel.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val showCarerAccess by viewModel.showCarerAccess.collectAsState()
    
    // Time updates
    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    var currentDate by remember { mutableStateOf(getCurrentDate()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTime()
            currentDate = getCurrentDate()
            kotlinx.coroutines.delay(1000)  // Update every second
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        InertBorderLayout {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WandasDimensions.SpacingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Clock and Date
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            viewModel.onCarerButtonTap()
                        }
                        .padding(WandasDimensions.SpacingExtraLarge),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentTime,
                        style = WandasTextStyles.Clock,
                        color = MaterialTheme.wandasColors.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(WandasDimensions.SpacingSmall))
                    Text(
                        text = currentDate,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.wandasColors.onBackground,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Middle: Contact buttons - always full width for readability
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingLarge)
                ) {
                    when (featureLevel) {
                        FeatureLevel.MINIMAL -> {
                            // Level 1: 2 contacts, full width stacked
                            contacts.take(2).forEach { contact ->
                                ContactButton(
                                    name = contact.name,
                                    phoneNumber = contact.phoneNumber,
                                    onClick = { viewModel.onContactTap(contact) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        FeatureLevel.BASIC -> {
                            // Level 2: 4 contacts, still full width stacked for readability
                            contacts.take(4).forEach { contact ->
                                ContactButton(
                                    name = contact.name,
                                    phoneNumber = contact.phoneNumber,
                                    onClick = { viewModel.onContactTap(contact) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        else -> {
                            // Level 3+: Show contacts button (implemented later)
                            Text(
                                text = "Tap here to see contacts",
                                style = WandasTextStyles.ButtonLarge,
                                color = MaterialTheme.wandasColors.onBackground
                            )
                        }
                    }
                }
                
                // Bottom: Emergency button
                // DEV MODE: Emergency button opens carer settings directly
                EmergencyButton(
                    text = if (DEV_MODE) "⚙️ Carer Settings (Dev)" else "Emergency",
                    onClick = { 
                        if (DEV_MODE) {
                            onNavigateToCarer()
                        } else {
                            viewModel.onEmergencyButtonTap()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    
    // Carer access dialog
    if (showCarerAccess) {
        // TODO: Show carer PIN dialog
        // For now just dismiss
        viewModel.dismissCarerAccess()
        onNavigateToCarer()
    }
}

private fun getCurrentTime(): String {
    val formatter = SimpleDateFormat("h:mm", Locale.getDefault())
    return formatter.format(Date())
}

private fun getCurrentDate(): String {
    val formatter = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    return formatter.format(Date())
}

