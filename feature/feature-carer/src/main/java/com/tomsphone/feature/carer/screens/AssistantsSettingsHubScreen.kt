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
import androidx.compose.ui.Modifier
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.components.CarerBreadcrumb
import com.tomsphone.feature.carer.components.CarerMenuButton
/**
 * Assistant-related settings grouped under one place:
 * managing assistant contacts vs reviewing the call log.
 */
@Composable
fun AssistantsSettingsHubScreen(
    onNavigateToAllContacts: () -> Unit,
    onNavigateToRecentCalls: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Column(Modifier.fillMaxSize()) {
            CarerBreadcrumb(
                title = "Contacts",
                parentTitle = "Settings",
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
                    title = "All contacts",
                    description = "Everyone; home slots add call buttons",
                    onClick = onNavigateToAllContacts
                )
                CarerMenuButton(
                    title = "Recent calls",
                    description = "Answered, missed, declined — tap unknown to add",
                    onClick = onNavigateToRecentCalls
                )
            }
        }
    }
}
