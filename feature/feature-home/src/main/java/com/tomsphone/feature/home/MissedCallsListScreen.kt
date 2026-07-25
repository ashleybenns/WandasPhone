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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.config.ListTextAlignment
import com.tomsphone.core.data.model.CallType
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.ui.components.FittedLabelInBox
import com.tomsphone.core.ui.components.SecondaryScreenIdleEffect
import com.tomsphone.core.ui.components.activationGesture
import com.tomsphone.core.ui.components.ListScreenLayout
import com.tomsphone.core.ui.theme.PastelColors
import com.tomsphone.core.ui.theme.ScaledDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.core.ui.theme.WandasDimensions
import java.text.SimpleDateFormat
import java.util.*

private const val INACTIVITY_TIMEOUT_MS = 30_000L

/** Extra vertical space per entry: gap + single caption line below the call button (not tappable). */
private val MISSED_ENTRY_BELOW_BUTTON_DP = 30.dp

/** Unknown / not-in-app number — solid blue row, white label. */
private val UnknownAllowedRowBlue = Color(0xFF1976D2)

/**
 * **Missed calls list**: paginated like [ContactsListScreen]; call buttons match home row height;
 * a smaller non-interactive “how long ago” line sits below each button. Like [ContactsListScreen],
 * there is no list title when there are entries; when empty, “No missed calls” is shown in the content area.
 */
@Composable
fun MissedCallsListScreen(
    onBack: () -> Unit,
    onCallContact: (String, String) -> Unit,
    onAddBlockedToApp: (String, String?) -> Unit = { _, _ -> },
    viewModel: MissedCallsListViewModel = hiltViewModel(),
) {
    val missedCallRows by viewModel.missedCallRows.collectAsState()
    val isMissedCallsEmpty by viewModel.isMissedCallsEmpty.collectAsState()
    val hasNextPage by viewModel.hasNextPage.collectAsState()
    val listTextAlignment by viewModel.listTextAlignment.collectAsState()
    val buttonActivation by viewModel.buttonActivation.collectAsState()
    val touchDebounceMs by viewModel.touchDebounceMs.collectAsState()
    val accumulatedThresholdMs by viewModel.accumulatedTapThresholdMs.collectAsState()
    val accumulatedTimeoutMs by viewModel.accumulatedTapTimeoutMs.collectAsState()
    val homeRowCount by viewModel.homeButtonRowCountForLayout.collectAsState()

    val density = LocalDensity.current
    var listViewportHeightPx by remember { mutableIntStateOf(0) }

    val rowSlotHeightDp =
        ScaledDimensions.homeContactRowInnerHeight(homeRowCount) + 8.dp
    val rowSlotHeightPx = with(density) { rowSlotHeightDp.roundToPx() }.coerceAtLeast(1)
    val entrySlotDp = rowSlotHeightDp + MISSED_ENTRY_BELOW_BUTTON_DP
    val entrySlotHeightPx = with(density) { entrySlotDp.roundToPx() }.coerceAtLeast(1)

    val rowsThatFit = remember(listViewportHeightPx, entrySlotHeightPx) {
        if (listViewportHeightPx <= 0) return@remember 0
        (listViewportHeightPx / entrySlotHeightPx).coerceAtLeast(1)
    }

    LaunchedEffect(rowsThatFit) {
        if (rowsThatFit > 0) {
            viewModel.setMissedCallsPerPageFromLayout(rowsThatFit)
        }
    }

    val handleBack = {
        if (!viewModel.goBackWithinList()) {
            onBack()
        }
    }

    SecondaryScreenIdleEffect(timeoutMs = INACTIVITY_TIMEOUT_MS, onTimeout = onBack) {
        ListScreenLayout(
            backgroundColor = PastelColors.lightBlue,
            title = "",
            emptyMessage = "No missed calls",
            isEmpty = isMissedCallsEmpty,
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
                    MissedCallsNextPageButton(
                        activationPreset = buttonActivation,
                        debounceMs = touchDebounceMs,
                        accumulatedThresholdMs = accumulatedThresholdMs,
                        accumulatedTimeoutMs = accumulatedTimeoutMs,
                        onClick = { viewModel.nextPage() },
                    )
                } else {
                    Box(modifier = Modifier.alpha(0f)) {
                        MissedCallsNextPageButton(
                            activationPreset = buttonActivation,
                            debounceMs = touchDebounceMs,
                            accumulatedThresholdMs = accumulatedThresholdMs,
                            accumulatedTimeoutMs = accumulatedTimeoutMs,
                            onClick = {},
                        )
                    }
                }
            },
        ) {
            if (isMissedCallsEmpty) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No missed calls",
                        style =
                            TextStyle(
                                fontSize = ScaledDimensions.contactNameTextSize,
                                fontWeight = FontWeight.Bold,
                            ),
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                missedCallRows.forEach { row ->
                    val call = row.call
                    MissedCallListItem(
                        contact = row.contact,
                        // Senior-facing: unknown callers show as "Unknown", never the raw number.
                        mainLabel = call.contactName ?: "Unknown",
                        timestamp = call.timestamp,
                        rowSlotHeightDp = rowSlotHeightDp,
                        textAlignment = listTextAlignment,
                        activationPreset = buttonActivation,
                        debounceMs = touchDebounceMs,
                        accumulatedThresholdMs = accumulatedThresholdMs,
                        accumulatedTimeoutMs = accumulatedTimeoutMs,
                        onCall = {
                            // Display name for the call-back ("Calling …"); never the raw number.
                            val label = call.contactName ?: "Unknown"
                            onCallContact(label, call.phoneNumber)
                        },
                        onAddBlockedToApp =
                            if (call.contactId == null && call.type == CallType.BLOCKED) {
                                { onAddBlockedToApp(call.phoneNumber, call.contactName) }
                            } else {
                                null
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun MissedCallListItem(
    contact: Contact?,
    mainLabel: String,
    timestamp: Long,
    rowSlotHeightDp: Dp,
    textAlignment: ListTextAlignment,
    activationPreset: ButtonActivationPreset,
    debounceMs: Int,
    accumulatedThresholdMs: Int,
    accumulatedTimeoutMs: Int,
    onCall: () -> Unit,
    onAddBlockedToApp: (() -> Unit)?,
) {
    val nameMaxSize = ScaledDimensions.contactNameTextSize
    val captionSizeSp = (nameMaxSize.value * 0.72f).sp
    val timeText = formatRelativeTimeMissed(timestamp)
    val rowColors = missedCallRowColors(contact)

    val boxAlignment = when (textAlignment) {
        ListTextAlignment.LEFT -> Alignment.CenterStart
        ListTextAlignment.CENTER -> Alignment.Center
    }
    val textAlign = when (textAlignment) {
        ListTextAlignment.LEFT -> TextAlign.Start
        ListTextAlignment.CENTER -> TextAlign.Center
    }
    val columnAlignment = when (textAlignment) {
        ListTextAlignment.LEFT -> Alignment.Start
        ListTextAlignment.CENTER -> Alignment.CenterHorizontally
    }

    val callInteractionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = columnAlignment,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = columnAlignment,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(rowSlotHeightDp)
                        .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .shadow(WandasDimensions.ElevationMedium, RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
                            .clip(RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
                            .indication(callInteractionSource, rememberRipple())
                            .activationGesture(
                                preset = activationPreset,
                                debounceMs = debounceMs,
                                accumulatedThresholdMs = accumulatedThresholdMs,
                                accumulatedTimeoutMs = accumulatedTimeoutMs,
                                onActivate = onCall,
                                interactionSource = callInteractionSource,
                            ),
                    color = rowColors.background,
                    shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = boxAlignment,
                    ) {
                        FittedLabelInBox(
                            text = mainLabel,
                            color = rowColors.onBackground,
                            textAlign = textAlign,
                            maxFontSize = nameMaxSize,
                            modifier = Modifier.fillMaxSize(),
                            maxLines = 2,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Text(
                text = timeText,
                style =
                    TextStyle(
                        fontSize = captionSizeSp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = captionSizeSp * 1.15f,
                    ),
                color = Color.Black.copy(alpha = 0.62f),
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (onAddBlockedToApp != null) {
            val addBlockedInteraction = remember { MutableInteractionSource() }
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(rowSlotHeightDp)
                        .padding(bottom = 8.dp)
                        .shadow(WandasDimensions.ElevationMedium, RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
                        .clip(RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
                        .indication(addBlockedInteraction, rememberRipple())
                        .activationGesture(
                            preset = activationPreset,
                            debounceMs = debounceMs,
                            accumulatedThresholdMs = accumulatedThresholdMs,
                            accumulatedTimeoutMs = accumulatedTimeoutMs,
                            onActivate = onAddBlockedToApp,
                            interactionSource = addBlockedInteraction,
                        ),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = boxAlignment,
                ) {
                    FittedLabelInBox(
                        text = "Add to Contacts or Assistants (in this app)",
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = textAlign,
                        maxFontSize = nameMaxSize,
                        modifier = Modifier.fillMaxSize(),
                        maxLines = 2,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

private data class MissedRowColors(val background: Color, val onBackground: Color)

@Composable
private fun missedCallRowColors(contact: Contact?): MissedRowColors {
    val w = MaterialTheme.wandasColors
    return if (contact != null) {
        val bg = contact.buttonColor?.let { Color(it) } ?: w.primaryButton
        val on = if (contact.buttonColor != null) Color.White else w.onPrimaryButton
        MissedRowColors(bg, on)
    } else {
        MissedRowColors(UnknownAllowedRowBlue, Color.White)
    }
}

private fun formatRelativeTimeMissed(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        days < 2 -> {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            "Yesterday ${timeFormat.format(Date(timestamp))}"
        }
        days < 7 -> "$days days ago"
        else -> {
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}

@Composable
private fun MissedCallsNextPageButton(
    activationPreset: ButtonActivationPreset,
    debounceMs: Int,
    accumulatedThresholdMs: Int,
    accumulatedTimeoutMs: Int,
    onClick: () -> Unit,
) {
    val headerTextSize = ScaledDimensions.contactNameTextSize
    val iconSize = headerTextSize.value.dp * 1.2f

    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .indication(interactionSource, rememberRipple())
                .activationGesture(
                    preset = activationPreset,
                    debounceMs = debounceMs,
                    accumulatedThresholdMs = accumulatedThresholdMs,
                    accumulatedTimeoutMs = accumulatedTimeoutMs,
                    onActivate = onClick,
                    interactionSource = interactionSource,
                )
                .padding(top = 8.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Next",
            style =
                TextStyle(
                    fontSize = headerTextSize,
                    fontWeight = FontWeight.Bold,
                ),
            color = Color.Black,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Next",
            tint = Color.Black,
            modifier = Modifier.size(iconSize),
        )
    }
}
