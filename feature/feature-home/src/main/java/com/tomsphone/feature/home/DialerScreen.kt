package com.tomsphone.feature.home

import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.data.util.PhoneNumberUtils
import com.tomsphone.core.ui.components.SecondaryScreenIdleEffect
import com.tomsphone.core.ui.components.activationGesture
import com.tomsphone.core.ui.theme.PastelColors
import com.tomsphone.core.ui.theme.WandasDimensions
import java.util.Locale
import kotlin.math.roundToInt

private const val INACTIVITY_TIMEOUT_MS = 45_000L

/** Max keypad characters (digits, *, #) so users can review a full number before Call. */
private const val DIALER_MAX_INPUT_CHARS = 12

/**
 * Fit string: max dialer length plus trailing caret (each slot as wide as "0" at this font).
 * Use [DIALER_MAX_INPUT_CHARS] + 1 so font size matches “full number + underline” width.
 */
private val DISPLAY_FIT_SAMPLE = "0".repeat(DIALER_MAX_INPUT_CHARS + 1)

/**
 * Keypad dialer: typography and layout scale from **device screen** only (not home button scaling).
 * Number line is sized so up to [DIALER_MAX_INPUT_CHARS] digits plus the caret fit on one row when possible;
 * longer input scrolls horizontally.
 */
@Composable
fun DialerScreen(
    onBack: () -> Unit,
    onPlaceCall: (String) -> Unit,
    buttonActivation: ButtonActivationPreset,
    touchDebounceMs: Int,
    accumulatedThresholdMs: Int,
    accumulatedTimeoutMs: Int
) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val hDp = config.screenHeightDp.toFloat().coerceAtLeast(400f)
    val wDp = config.screenWidthDp.toFloat().coerceAtLeast(320f)

    // Heights / fonts derived from screen (independent of UserScalingProvider)
    val backTextSp = (wDp * 0.055f).coerceIn(22f, 30f).sp
    val backIconDp = (wDp * 0.09f).coerceIn(32f, 44f).dp
    // Enough height for one line; font is capped to this box so glyphs aren’t clipped.
    val inputAreaMinDp = (hDp * 0.09f).coerceIn(50f, 72f).dp
    val keypadGapDp = (wDp * 0.018f).coerceIn(5f, 9f).dp
    val callBarHeightDp = (hDp * 0.11f).coerceIn(60f, 88f).dp
    val callTextSp = (hDp * 0.038f).coerceIn(22f, 32f).sp
    val deleteWidthDp = (wDp * 0.42f).coerceIn(140f, 200f).dp
    val deleteHeightDp = (hDp * 0.072f).coerceIn(48f, 62f).dp

    val defaultRegion = remember(context) {
        try {
            val tm = context.getSystemService(TelephonyManager::class.java)
            tm?.networkCountryIso?.uppercase(Locale.ROOT)?.takeIf { it.length == 2 } ?: "GB"
        } catch (_: Exception) {
            "GB"
        }
    }

    var digits by remember { mutableStateOf("") }

    val keysEnabled = digits.length < DIALER_MAX_INPUT_CHARS

    // Key label size scales with screen; row height comes from weighted keypad area
    val keyFontMaxSp = (hDp * 0.052f).coerceIn(24f, 36f)

    SecondaryScreenIdleEffect(timeoutMs = INACTIVITY_TIMEOUT_MS, onTimeout = onBack) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PastelColors.lightGreen)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = WandasDimensions.InertBorderWidth,
                    top = WandasDimensions.InertBorderWidth,
                    end = WandasDimensions.InertBorderWidth,
                    bottom = WandasDimensions.InertBorderBottom
                )
        ) {
            val backInteraction = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .indication(backInteraction, rememberRipple())
                    .activationGesture(
                        preset = buttonActivation,
                        debounceMs = touchDebounceMs,
                        accumulatedThresholdMs = accumulatedThresholdMs,
                        accumulatedTimeoutMs = accumulatedTimeoutMs,
                        onActivate = onBack,
                        interactionSource = backInteraction
                    )
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.size(backIconDp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Back",
                    style = TextStyle(
                        fontSize = backTextSp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.Black
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(inputAreaMinDp)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.9f),
                shadowElevation = 2.dp
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    val textMeasurer = rememberTextMeasurer()
                    val maxWpx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
                    val maxHpx = with(density) { maxHeight.roundToPx() }.coerceAtLeast(1)
                    val displayFontSp = remember(maxWpx, maxHpx, textMeasurer) {
                        fitSingleLineFontSp(
                            textMeasurer = textMeasurer,
                            sample = DISPLAY_FIT_SAMPLE,
                            maxWidthPx = maxWpx,
                            maxHeightPx = maxHpx,
                            minSp = 14f,
                            maxSp = 44f
                        )
                    }
                    val displayStyle = TextStyle(
                        fontSize = displayFontSp,
                        lineHeight = displayFontSp,
                        fontWeight = FontWeight.SemiBold,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                    val caretWidthPx = remember(displayStyle, textMeasurer) {
                        textMeasurer.measure(
                            AnnotatedString("0"),
                            style = displayStyle,
                            constraints = Constraints()
                        ).size.width.coerceAtLeast(1)
                    }
                    val scrollState = rememberScrollState()
                    LaunchedEffect(digits, scrollState.maxValue) {
                        scrollState.scrollTo(scrollState.maxValue)
                    }
                    val minLineDp = with(density) { displayFontSp.toDp() }
                    Box(Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .horizontalScroll(scrollState),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = digits,
                                modifier = Modifier.heightIn(min = minLineDp),
                                style = displayStyle,
                                color = Color.Black,
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Visible
                            )
                            Box(
                                Modifier
                                    .width(with(density) { caretWidthPx.toDp() })
                                    .height(3.dp)
                                    .background(Color.Black)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DialerSmallActionButton(
                    label = "Delete",
                    modifier = Modifier
                        .width(deleteWidthDp)
                        .height(deleteHeightDp),
                    fontSizeSp = (deleteHeightDp.value * 0.36f).coerceIn(16f, 22f).sp,
                    enabled = digits.isNotEmpty(),
                    buttonActivation = buttonActivation,
                    touchDebounceMs = touchDebounceMs,
                    accumulatedThresholdMs = accumulatedThresholdMs,
                    accumulatedTimeoutMs = accumulatedTimeoutMs,
                    onPress = {
                        if (digits.isNotEmpty()) digits = digits.dropLast(1)
                    }
                )
            }

            val rows = remember {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("*", "0", "#")
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(keypadGapDp)
            ) {
                rows.forEach { rowKeys ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(keypadGapDp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowKeys.forEach { key ->
                            DialerKey(
                                label = key,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                maxKeyFontSp = keyFontMaxSp,
                                enabled = keysEnabled,
                                buttonActivation = buttonActivation,
                                touchDebounceMs = touchDebounceMs,
                                accumulatedThresholdMs = accumulatedThresholdMs,
                                accumulatedTimeoutMs = accumulatedTimeoutMs,
                                onPress = {
                                    if (digits.length < DIALER_MAX_INPUT_CHARS) digits += key
                                }
                            )
                        }
                    }
                }
            }

            val callInteraction = remember { MutableInteractionSource() }
            val callModifier = Modifier
                .fillMaxWidth()
                .height(callBarHeightDp)
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .clip(RoundedCornerShape(WandasDimensions.CornerRadiusMedium))
                .indication(callInteraction, rememberRipple())
                .activationGesture(
                    preset = buttonActivation,
                    debounceMs = touchDebounceMs,
                    accumulatedThresholdMs = accumulatedThresholdMs,
                    accumulatedTimeoutMs = accumulatedTimeoutMs,
                    onActivate = {
                        val trimmed = digits.trim()
                        if (trimmed.isEmpty()) {
                            Toast.makeText(
                                context,
                                "Enter a number",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@activationGesture
                        }
                        val normalized = PhoneNumberUtils.dialerInputToE164OrNull(trimmed, defaultRegion)
                            ?: PhoneNumberUtils.normalizeToE164(trimmed, defaultRegion)
                        val toDial = normalized.ifBlank { trimmed }
                        onPlaceCall(toDial)
                    },
                    interactionSource = callInteraction
                )
            Surface(
                modifier = callModifier,
                color = Color(0xFF2E7D32),
                shape = RoundedCornerShape(WandasDimensions.CornerRadiusMedium),
                shadowElevation = WandasDimensions.ElevationMedium
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Call",
                        style = TextStyle(
                            fontSize = callTextSp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
    }
}

/**
 * Largest font size so [sample] fits on one line within [maxWidthPx] and [maxHeightPx]
 * (avoids choosing a width-only fit that clips vertically in a short field).
 */
private fun fitSingleLineFontSp(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    sample: String,
    maxWidthPx: Int,
    maxHeightPx: Int,
    minSp: Float,
    maxSp: Float
): TextUnit {
    fun fits(sp: Float): Boolean {
        if (sp < minSp) return false
        // Measure intrinsic single-line width; do NOT pass maxWidth here — that caps reported width
        // and makes the search accept fonts that overflow the field (only ~9 digits visible).
        val layout = textMeasurer.measure(
            text = AnnotatedString(sample),
            style = TextStyle(
                fontSize = sp.sp,
                lineHeight = sp.sp,
                fontWeight = FontWeight.SemiBold,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            overflow = TextOverflow.Visible,
            softWrap = false,
            maxLines = 1,
            constraints = Constraints(
                maxWidth = Constraints.Infinity,
                maxHeight = Constraints.Infinity
            )
        )
        return layout.lineCount <= 1 &&
            layout.size.width <= maxWidthPx &&
            layout.size.height <= maxHeightPx
    }
    var lo = (minSp * 10f).roundToInt().coerceAtLeast(1)
    var hi = (maxSp * 10f).roundToInt().coerceAtLeast(lo)
    var best = lo
    while (lo <= hi) {
        val mid = (lo + hi) / 2
        val sp = mid / 10f
        if (fits(sp)) {
            best = mid
            lo = mid + 1
        } else {
            hi = mid - 1
        }
    }
    return (best / 10f).coerceAtLeast(minSp).sp
}

@Composable
private fun DialerSmallActionButton(
    label: String,
    modifier: Modifier,
    fontSizeSp: TextUnit,
    enabled: Boolean,
    buttonActivation: ButtonActivationPreset,
    touchDebounceMs: Int,
    accumulatedThresholdMs: Int,
    accumulatedTimeoutMs: Int,
    onPress: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(12.dp)
    val base = modifier.clip(shape)
    Surface(
        modifier = if (enabled) {
            base
                .indication(interaction, rememberRipple())
                .activationGesture(
                    preset = buttonActivation,
                    debounceMs = touchDebounceMs,
                    accumulatedThresholdMs = accumulatedThresholdMs,
                    accumulatedTimeoutMs = accumulatedTimeoutMs,
                    onActivate = onPress,
                    interactionSource = interaction
                )
        } else {
            base
        }.alpha(if (enabled) 1f else 0.4f),
        color = if (enabled) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.45f),
        shape = shape,
        shadowElevation = if (enabled) 1.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = fontSizeSp,
                    fontWeight = FontWeight.Bold
                ),
                color = if (enabled) Color.Black else Color.Black.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
private fun DialerKey(
    label: String,
    modifier: Modifier,
    maxKeyFontSp: Float,
    enabled: Boolean,
    buttonActivation: ButtonActivationPreset,
    touchDebounceMs: Int,
    accumulatedThresholdMs: Int,
    accumulatedTimeoutMs: Int,
    onPress: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    BoxWithConstraints(modifier = modifier) {
        // Row height comes from weighted keypad area — tie key digit size to row height (no aspectRatio).
        val keySp = (maxHeight.value * 0.42f).coerceIn(20f, maxKeyFontSp).sp
        val shape = RoundedCornerShape(12.dp)
        val base = Modifier
            .fillMaxSize()
            .clip(shape)
        Surface(
            modifier = if (enabled) {
                base
                    .indication(interaction, rememberRipple())
                    .activationGesture(
                        preset = buttonActivation,
                        debounceMs = touchDebounceMs,
                        accumulatedThresholdMs = accumulatedThresholdMs,
                        accumulatedTimeoutMs = accumulatedTimeoutMs,
                        onActivate = onPress,
                        interactionSource = interaction
                    )
            } else {
                base
            }.alpha(if (enabled) 1f else 0.38f),
            color = if (enabled) Color.White.copy(alpha = 0.78f) else Color.White.copy(alpha = 0.4f),
            shape = shape,
            shadowElevation = if (enabled) 1.dp else 0.dp
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = label,
                    style = TextStyle(
                        fontSize = keySp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (enabled) Color.Black else Color.Black.copy(alpha = 0.4f)
                )
            }
        }
    }
}
