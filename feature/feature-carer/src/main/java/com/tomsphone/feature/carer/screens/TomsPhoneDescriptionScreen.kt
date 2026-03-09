package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.CarerSettingsViewModel
import com.tomsphone.feature.carer.components.CarerBreadcrumb
import com.tomsphone.feature.carer.components.DevLevelIndicator

/**
 * Tom's Phone description — the story and philosophy behind the app.
 * 
 * Read-only informational screen for carers and families.
 */
@Composable
fun TomsPhoneDescriptionScreen(
    onBack: () -> Unit,
    viewModel: CarerSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val featureLevel = settings.featureLevel

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            DevLevelIndicator(level = featureLevel)

            CarerBreadcrumb(
                title = "Tom's Phone Description",
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
                DescriptionParagraph(
                    "I originally created this Phone App for my Mother. Mum has struggled with other 'Senior' devices — she can't learn another one, and so this is it. No more learning or remembering what to do."
                )

                DescriptionParagraph(
                    "It's a One Touch phone and on Level 1 the Home Screen is always on. Up to 4 assistants can be called from the Home Screen with One Touch, and there's an Emergency button. Even the tap response can be customised with three different tap responses available."
                )

                SectionTitle("One Touch calling and answering")
                DescriptionParagraph("One Touch makes a phone call to an Assistant. Loud and on Speaker.")
                DescriptionParagraph("One Touch answers a call. Loud and On Speaker.")
                DescriptionParagraph(
                    "The ringtone includes Mum's name: \"Wanda, that's your phone ringing\" to grab her attention."
                )

                SectionTitle("Missed call reminders")
                DescriptionParagraph(
                    "If Mum misses a call from an Assistant there's a persistent reminder until she returns the call: \"Wanda, you missed a call. Please Call Fred Now\". One Touch will return the call and cancel the reminder."
                )
                DescriptionParagraph(
                    "This works great — as Mum said, she can follow an instruction, she knows to call back and knows how to call back."
                )

                SectionTitle("Incoming calls and contacts")
                DescriptionParagraph("Incoming calls can be allowed or restricted to a contacts list.")
                DescriptionParagraph("The most recent missed call from the contact list can be returned from the Home Screen.")

                SectionTitle("Emergency")
                DescriptionParagraph(
                    "The Emergency button opens a confirmation screen to prevent accidental calls. The Emergency information screen opens when an emergency call is made."
                )

                SectionTitle("Level 2 — Two Touch")
                DescriptionParagraph(
                    "Now at Level 2 I've added Two Touch complexity. This allows a separate screen for a bigger contacts list and the missed calls list, plus toggle buttons such as screen-off and speaker on/off controls."
                )
                DescriptionParagraph(
                    "Everything is customisable but an elderly person's capability can vary a lot during the day, so the Assistant call buttons are always in the same place, always One Touch from the Home Screen and the phone returns to its Home Screen and default settings. Wanda doesn't use Level 2 but others find it useful."
                )

                SectionTitle("Feedback")
                DescriptionParagraph(
                    "If you think of an adjustment or setting that would make the phone easier for your Senior, let me know and I'll incorporate it. Your feedback will improve the experience for everyone. More information and suggestions — contact me."
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
