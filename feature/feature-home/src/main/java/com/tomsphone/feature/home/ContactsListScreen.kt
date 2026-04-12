package com.tomsphone.feature.home

import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.ui.components.SecondaryScreenIdleEffect
import com.tomsphone.core.ui.components.activationGesture
import com.tomsphone.core.ui.components.ConfigurableButton
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.config.ListTextAlignment
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.ui.components.ListScreenLayout
import com.tomsphone.core.ui.theme.PastelColors
import com.tomsphone.core.ui.theme.ScaledDimensions
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
private const val INACTIVITY_TIMEOUT_MS = 30_000L

/**
 * Contacts list (Level 2+)
 *
 * Shows assistants and answer-only contacts (unless carer enabled “answer-only only”).
 * Tap a row to call.
 *
 * Design: Pastel yellow background, inert gutters, home-style call buttons.
 */
@Composable
fun ContactsListScreen(
    onBack: () -> Unit,
    onCallContact: (String, String) -> Unit, // (name, phoneNumber)
    viewModel: ContactsListViewModel = hiltViewModel()
) {
    val contacts by viewModel.contacts.collectAsState()
    val screenTitle by viewModel.screenTitle.collectAsState()
    val emptyMessage by viewModel.emptyMessage.collectAsState()
    val hasNextPage by viewModel.hasNextPage.collectAsState()
    val listTextAlignment by viewModel.listTextAlignment.collectAsState()
    val buttonActivation by viewModel.buttonActivation.collectAsState()
    val autoAnswerEnabled by viewModel.autoAnswerEnabled.collectAsState()
    val touchDebounceMs by viewModel.touchDebounceMs.collectAsState()
    val accumulatedThresholdMs by viewModel.accumulatedTapThresholdMs.collectAsState()
    val accumulatedTimeoutMs by viewModel.accumulatedTapTimeoutMs.collectAsState()
    // Match home: compute rows-per-page from viewport / home row count.
    val homeRows by viewModel.homeButtonRowCountForLayout.collectAsState()

    val density = LocalDensity.current
    var listViewportHeightPx by remember { mutableIntStateOf(0) }

    val rowsThatFit = remember(listViewportHeightPx, homeRows) {
        if (listViewportHeightPx <= 0) return@remember 0
        homeRows.coerceAtLeast(1)
    }

    LaunchedEffect(rowsThatFit) {
        if (rowsThatFit > 0) {
            viewModel.setContactsPerPageFromLayout(rowsThatFit)
        }
    }

    val fallbackRowSlotHeightDp = ScaledDimensions.contactButtonHeight + 8.dp
    val rowSlotHeightDp =
        if (listViewportHeightPx > 0 && rowsThatFit > 0) {
            with(density) { (listViewportHeightPx / rowsThatFit).toDp() }
        } else {
            fallbackRowSlotHeightDp
        }
    
    val handleBack = {
        if (!viewModel.goBackWithinList()) {
            onBack()
        }
    }
    
    SecondaryScreenIdleEffect(timeoutMs = INACTIVITY_TIMEOUT_MS, onTimeout = onBack) {
    ListScreenLayout(
        backgroundColor = PastelColors.lightYellow,
        title = screenTitle,
        emptyMessage = emptyMessage,
        isEmpty = contacts.isEmpty(),
        onBack = handleBack,
        contentScrollable = false,
        activationPreset = buttonActivation,
        debounceMs = touchDebounceMs,
        accumulatedThresholdMs = accumulatedThresholdMs,
        accumulatedTimeoutMs = accumulatedTimeoutMs,
        listVerticalArrangement = Arrangement.Top,
        listViewportModifier = Modifier.onSizeChanged { listViewportHeightPx = it.height },
        footer = {
            if (hasNextPage) {
                NextPageButton(
                    activationPreset = buttonActivation,
                    debounceMs = touchDebounceMs,
                    accumulatedThresholdMs = accumulatedThresholdMs,
                    accumulatedTimeoutMs = accumulatedTimeoutMs,
                    onClick = { viewModel.nextPage() }
                )
            } else {
                // Reserve footer height so page size doesn't change when Next disappears.
                Box(modifier = Modifier.alpha(0f)) {
                    NextPageButton(
                        activationPreset = buttonActivation,
                        debounceMs = touchDebounceMs,
                        accumulatedThresholdMs = accumulatedThresholdMs,
                        accumulatedTimeoutMs = accumulatedTimeoutMs,
                        onClick = {}
                    )
                }
            }
        }
    ) {
        // Same slot pattern as [HomeScreen]: outer height = inner + 8.dp padding, button fillMaxHeight
        // so every row gets identical constraints (avoids uneven-looking blocks).
        contacts.forEach { contact ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowSlotHeightDp)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                ConfigurableButton(
                    label = contact.name,
                    onClick = {
                        onCallContact(contact.name, contact.phoneNumber)
                    },
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
    }
}

/**
 * Next page button - matches Back button style; pinned in [ListScreenLayout] footer above bottom inset.
 */
@Composable
private fun NextPageButton(
    activationPreset: ButtonActivationPreset,
    debounceMs: Int,
    accumulatedThresholdMs: Int,
    accumulatedTimeoutMs: Int,
    onClick: () -> Unit
) {
    val headerTextSize = ScaledDimensions.contactNameTextSize
    val iconSize = headerTextSize.value.dp * 1.2f
    
    val interactionSource = remember { MutableInteractionSource() }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .indication(interactionSource, rememberRipple())
            .activationGesture(
                preset = activationPreset,
                debounceMs = debounceMs,
                accumulatedThresholdMs = accumulatedThresholdMs,
                accumulatedTimeoutMs = accumulatedTimeoutMs,
                onActivate = onClick,
                interactionSource = interactionSource
            )
            .padding(top = 8.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Next",
            style = TextStyle(
                fontSize = headerTextSize,
                fontWeight = FontWeight.Bold
            ),
            color = Color.Black
        )
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Next",
            tint = Color.Black,
            modifier = Modifier.size(iconSize)
        )
    }
}
