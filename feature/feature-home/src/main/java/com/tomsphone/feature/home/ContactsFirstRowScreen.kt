package com.tomsphone.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.ui.components.ConfigurableButton
import com.tomsphone.core.ui.components.ListScreenLayout
import com.tomsphone.core.ui.theme.PastelColors
import com.tomsphone.core.ui.theme.ScaledDimensions
import com.tomsphone.core.ui.theme.wandasColors

/**
 * Minimal contacts route: first contact in [sortedForContactList] order as a single home-style row
 * at the top of the list area.
 */
@Composable
fun ContactsFirstRowScreen(
    onBack: () -> Unit,
    onCallContact: (String, String) -> Unit,
    viewModel: ContactsFirstRowViewModel = hiltViewModel()
) {
    val first by viewModel.firstContact.collectAsState()
    val autoAnswerEnabled by viewModel.showAutoAnswerWarning.collectAsState()
    val homeRows by viewModel.homeButtonRowCountForLayout.collectAsState()
    val listTextAlignment by viewModel.listTextAlignment.collectAsState()
    val buttonActivation by viewModel.buttonActivation.collectAsState()
    val touchDebounceMs by viewModel.touchDebounceMs.collectAsState()
    val accumulatedThresholdMs by viewModel.accumulatedTapThresholdMs.collectAsState()
    val accumulatedTimeoutMs by viewModel.accumulatedTapTimeoutMs.collectAsState()

    val contactRowHeight = ScaledDimensions.homeContactRowInnerHeight(homeRows)
    val rowSlotHeightDp = contactRowHeight + 8.dp

    ListScreenLayout(
        backgroundColor = PastelColors.lightYellow,
        title = "",
        emptyMessage = "",
        isEmpty = first == null,
        onBack = onBack,
        contentScrollable = false,
        activationPreset = buttonActivation,
        debounceMs = touchDebounceMs,
        accumulatedThresholdMs = accumulatedThresholdMs,
        accumulatedTimeoutMs = accumulatedTimeoutMs,
        listVerticalArrangement = Arrangement.Top
    ) {
        val contact = first ?: return@ListScreenLayout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowSlotHeightDp)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            ConfigurableButton(
                label = contact.name,
                onClick = { onCallContact(contact.name, contact.phoneNumber) },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                backgroundColor = contact.buttonColor?.let { Color(it) }
                    ?: MaterialTheme.wandasColors.primaryButton,
                textColor = MaterialTheme.wandasColors.onPrimaryButton,
                warningText = if (autoAnswerEnabled && contact.autoAnswerEnabled) "Auto-Answer" else null,
                textAlignment = listTextAlignment,
                activationPreset = buttonActivation,
                debounceMs = touchDebounceMs,
                accumulatedThresholdMs = accumulatedThresholdMs,
                accumulatedTimeoutMs = accumulatedTimeoutMs
            )
        }
    }
}
