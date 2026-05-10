package com.tomsphone.feature.phone

import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.ui.components.InertBorderLayout
import com.tomsphone.core.ui.components.activationGesture
import com.tomsphone.core.ui.theme.ScaledDimensions
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.rememberEndCallFixedActionTypography

/**
 * End-call action stack: [InertBorderLayout], fixed-layout typography (not carer appearance scale),
 * and the same full-width rounded bars as Answer/Reject.
 */
@Composable
internal fun EndCallScreenActionArea(
    modifier: Modifier = Modifier,
    showSpeakerToggle: Boolean,
    confirmPending: Boolean,
    isSpeakerOn: Boolean,
    speakerConfirmPending: Boolean,
    buttonActivation: ButtonActivationPreset,
    touchDebounceMs: Int,
    accumulatedThresholdMs: Int,
    accumulatedTimeoutMs: Int,
    onEndCallTap: () -> Unit,
    onSpeakerTap: () -> Unit,
) {
    InertBorderLayout(modifier = modifier) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(WandasDimensions.SpacingLarge),
        ) {
            val between = WandasDimensions.SpacingLarge
            val emergencyH = ScaledDimensions.emergencyButtonHeight
            val buttonCount = if (showSpeakerToggle) 2 else 1
            val stackMaxH =
                maxHeight - emergencyH - between * (buttonCount - 1).coerceAtLeast(0)
            val perButtonH = (stackMaxH / buttonCount).coerceAtLeast(48.dp)
            val innerTextWidth = (maxWidth - 40.dp).coerceAtLeast(120.dp)
            val typography =
                rememberEndCallFixedActionTypography(
                    maxContentWidth = innerTextWidth,
                    perButtonMaxHeight = perButtonH,
                )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val endCallInteractionSource = remember { MutableInteractionSource() }
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(perButtonH)
                                .clip(RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
                                .indication(endCallInteractionSource, rememberRipple())
                                .activationGesture(
                                    preset = buttonActivation,
                                    debounceMs = touchDebounceMs,
                                    accumulatedThresholdMs = accumulatedThresholdMs,
                                    accumulatedTimeoutMs = accumulatedTimeoutMs,
                                    onActivate = onEndCallTap,
                                    interactionSource = endCallInteractionSource,
                                ),
                        shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge),
                        color = Color(0xFFD32F2F),
                        shadowElevation = WandasDimensions.ElevationMedium,
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "End call",
                                    style = typography.titleStyle,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                )
                                Text(
                                    text = if (confirmPending) "Tap again" else "Tap twice",
                                    style = typography.subtitleStyle,
                                    color =
                                        if (confirmPending) {
                                            Color(0xFFFFEB3B)
                                        } else {
                                            Color.White.copy(alpha = 0.9f)
                                        },
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                )
                            }
                        }
                    }

                    if (showSpeakerToggle) {
                        val speakerInteractionSource = remember { MutableInteractionSource() }
                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(perButtonH)
                                    .clip(RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
                                    .indication(speakerInteractionSource, rememberRipple())
                                    .activationGesture(
                                        preset = buttonActivation,
                                        debounceMs = touchDebounceMs,
                                        accumulatedThresholdMs = accumulatedThresholdMs,
                                        accumulatedTimeoutMs = accumulatedTimeoutMs,
                                        onActivate = onSpeakerTap,
                                        interactionSource = speakerInteractionSource,
                                    ),
                            shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge),
                            color = Color(0xFF455A64),
                            shadowElevation = WandasDimensions.ElevationMedium,
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (isSpeakerOn) "Speaker on" else "Speaker off",
                                        style = typography.titleStyle,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = if (speakerConfirmPending) "Tap again" else "Tap twice",
                                        style = typography.subtitleStyle,
                                        color =
                                            if (speakerConfirmPending) {
                                                Color(0xFFFFEB3B)
                                            } else {
                                                Color.White.copy(alpha = 0.9f)
                                            },
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(emergencyH))
            }
        }
    }
}
