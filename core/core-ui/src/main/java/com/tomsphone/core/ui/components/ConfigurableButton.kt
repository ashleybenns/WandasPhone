package com.tomsphone.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.config.ListTextAlignment
import com.tomsphone.core.ui.theme.ScaledDimensions
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors

/**
 * Unified configurable button for home screen.
 *
 * Supports:
 * - Custom background/text colors
 * - Optional warning badge at bottom (e.g., "Auto-Answer")
 * - Full-width or half-width layouts
 * - Scales with user text size setting
 *
 * LAYOUT: Caller supplies height — e.g. [Modifier.fillMaxHeight] inside a weighted home row, or
 * [Modifier.height] on list screens. Do not use [fillMaxHeight] here: in a [Column] it would expand
 * into all space below each row and break fixed-height lists.
 * Label sizing follows [LabelFitPolicy] (fit-to-box, max lines, ellipsis).
 *
 * Used for contact buttons, menu buttons, etc.
 *
 * @param labelMaxLines Usually 2; see [LabelFitPolicy].
 * @param activationPreset How the button responds to touch (ON_RELEASE, ON_PRESS, DOUBLE_TAP)
 * @param debounceMs Minimum touch duration to filter accidental brushes
 */
@Composable
fun ConfigurableButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.wandasColors.primaryButton,
    textColor: Color = MaterialTheme.wandasColors.onPrimaryButton,
    warningText: String? = null,
    enabled: Boolean = true,
    textAlignment: ListTextAlignment = ListTextAlignment.CENTER,
    activationPreset: ButtonActivationPreset = ButtonActivationPreset.ON_RELEASE,
    debounceMs: Int = 150,
    accumulatedThresholdMs: Int = 500,
    accumulatedTimeoutMs: Int = 3000,
    labelMaxLines: Int = LabelFitPolicy.DEFAULT_MAX_LINES
) {
    val textSize = ScaledDimensions.contactNameTextSize

    val boxAlignment = when (textAlignment) {
        ListTextAlignment.LEFT -> Alignment.CenterStart
        ListTextAlignment.CENTER -> Alignment.Center
    }
    val textAlign = when (textAlignment) {
        ListTextAlignment.LEFT -> TextAlign.Start
        ListTextAlignment.CENTER -> TextAlign.Center
    }

    val interactionSource = remember { MutableInteractionSource() }

    val actualBackgroundColor = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f)
    val actualTextColor = if (enabled) textColor else textColor.copy(alpha = 0.5f)

    val columnHorizontalAlignment = when (textAlignment) {
        ListTextAlignment.LEFT -> Alignment.Start
        ListTextAlignment.CENTER -> Alignment.CenterHorizontally
    }

    Surface(
        modifier = modifier
            .shadow(WandasDimensions.ElevationMedium, RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
            .clip(RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
            .indication(interactionSource, rememberRipple())
            .then(
                if (enabled) {
                    Modifier.activationGesture(
                        preset = activationPreset,
                        debounceMs = debounceMs,
                        accumulatedThresholdMs = accumulatedThresholdMs,
                        accumulatedTimeoutMs = accumulatedTimeoutMs,
                        onActivate = onClick,
                        interactionSource = interactionSource
                    )
                } else {
                    Modifier
                }
            ),
        color = actualBackgroundColor,
        shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge)
    ) {
        if (warningText != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = columnHorizontalAlignment,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = boxAlignment
                ) {
                    FittedLabelInBox(
                        text = label,
                        color = actualTextColor,
                        textAlign = textAlign,
                        maxFontSize = textSize,
                        modifier = Modifier.fillMaxSize(),
                        maxLines = labelMaxLines.coerceIn(1, 4)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                WarningBadge(
                    text = warningText,
                    modifier = Modifier.padding(horizontal = WandasDimensions.SpacingSmall)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = boxAlignment
            ) {
                FittedLabelInBox(
                    text = label,
                    color = actualTextColor,
                    textAlign = textAlign,
                    maxFontSize = textSize,
                    modifier = Modifier.fillMaxSize(),
                    maxLines = labelMaxLines.coerceIn(1, 4)
                )
            }
        }
    }
}

/**
 * Warning badge shown at bottom of button.
 *
 * Used for:
 * - "Auto-Answer" warning
 * - Other important status indicators
 */
@Composable
fun WarningBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = Color(0xFFFFEB3B).copy(alpha = 0.9f), // Yellow warning
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            ),
            color = Color.Black,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Half-width button row for split layouts (Level 2+).
 *
 * Shows two buttons side by side with equal width.
 */
@Composable
fun HalfWidthButtonRow(
    leftButton: @Composable (Modifier) -> Unit,
    rightButton: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingMedium)
    ) {
        leftButton(Modifier.weight(1f))
        rightButton(Modifier.weight(1f))
    }
}
