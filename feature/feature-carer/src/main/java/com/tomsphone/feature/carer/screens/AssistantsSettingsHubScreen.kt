package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.CarerSettingsViewModel
import com.tomsphone.feature.carer.components.CarerBreadcrumb
import com.tomsphone.feature.carer.components.CarerMenuButton
import com.tomsphone.feature.carer.components.DevLevelIndicator

/**
 * Assistant-related settings grouped under one place:
 * managing assistant contacts vs reviewing the call log.
 */
@Composable
fun AssistantsSettingsHubScreen(
    onNavigateToAssistantContacts: () -> Unit,
    onNavigateToRecentCalls: () -> Unit,
    onBack: () -> Unit,
    viewModel: CarerSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val featureLevel = settings.featureLevel

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Column(Modifier.fillMaxSize()) {
            DevLevelIndicator(level = featureLevel)
            CarerBreadcrumb(
                title = "Assistants",
                parentTitle = "Assistant Settings",
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
                    title = "Assistant contacts",
                    description = "Home screen buttons, reorder, colors",
                    onClick = onNavigateToAssistantContacts,
                    currentLevel = featureLevel
                )
                CarerMenuButton(
                    title = "Recent calls",
                    description = "Full call history: answered, missed, declined, blocked (repeat callers visible)",
                    onClick = onNavigateToRecentCalls,
                    currentLevel = featureLevel
                )
            }
        }
    }
}
