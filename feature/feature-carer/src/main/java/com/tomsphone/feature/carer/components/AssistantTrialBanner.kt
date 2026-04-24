package com.tomsphone.feature.carer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tomsphone.core.billing.EntitlementSnapshot
import com.tomsphone.core.ui.theme.WandasDimensions

@Composable
fun AssistantTrialBanner(snapshot: EntitlementSnapshot) {
    if (!snapshot.shouldNudgeAssistantsAboutTrial) return
    val days = snapshot.trialDaysRemainingInclusive ?: return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = WandasDimensions.SpacingSmall),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0),
            contentColor = Color(0xFF5D4037)
        ),
        shape = RoundedCornerShape(WandasDimensions.CornerRadiusMedium)
    ) {
        Column(
            modifier = Modifier.padding(WandasDimensions.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingSmall)
        ) {
            Text(
                text = when (days) {
                    1 -> "Trial ends tomorrow"
                    else -> "About $days days left on the free trial"
                },
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "After the trial, assistants must complete the one-time purchase to keep changing settings. " +
                    "The person using the phone can still call people on their home screen if those buttons are already set up — " +
                    "but new setup needs an unlocked assistant area.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
