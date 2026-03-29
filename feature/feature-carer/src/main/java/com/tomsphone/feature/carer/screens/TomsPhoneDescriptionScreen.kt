package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.components.CarerBreadcrumb

/**
 * Tom's Phone description — the story and philosophy behind the app.
 * 
 * Read-only informational screen for carers and families.
 */
@Composable
fun TomsPhoneDescriptionScreen(
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            CarerBreadcrumb(
                title = "About",
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
                DescriptionParagraph(
                    "I built this for my mother. Other “senior” phones meant starting over; this one is meant to stay — minimal learning, minimal remembering."
                )

                DescriptionParagraph(
                    "Level 1 keeps the home screen front and centre: one-touch calls to assistants, emergency, and tap styles you can match to the user."
                )

                SectionTitle("Calls")
                DescriptionParagraph("One touch dials an assistant — loud, on speaker.")
                DescriptionParagraph("One touch answers — loud, on speaker.")
                DescriptionParagraph(
                    "The ringtone can use their name (e.g. “Wanda, that’s your phone ringing”)."
                )

                SectionTitle("Missed calls")
                DescriptionParagraph(
                    "If they miss an assistant’s call, a reminder stays until they call back — e.g. “Wanda, you missed a call. Please call Fred now.” One touch returns the call and clears it."
                )

                SectionTitle("Who can call in")
                DescriptionParagraph("You can allow only contacts or allow anyone.")
                DescriptionParagraph("The latest missed call from contacts can be one-tapped from home (when enabled).")

                SectionTitle("Emergency")
                DescriptionParagraph(
                    "Emergency needs confirmation to avoid pocket dials. Medical and address details show when a real emergency call goes out."
                )

                SectionTitle("Level 2")
                DescriptionParagraph(
                    "Adds simple second screens: full contacts list, missed-calls list, screen off, in-call speaker toggle, and more — still with fixed assistant buttons on home."
                )

                SectionTitle("Feedback")
                DescriptionParagraph(
                    "Ideas that help your user usually help everyone. Use Support in settings to send a message."
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun DescriptionParagraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.wandasColors.onBackground,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.wandasColors.primaryButton,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    )
}
