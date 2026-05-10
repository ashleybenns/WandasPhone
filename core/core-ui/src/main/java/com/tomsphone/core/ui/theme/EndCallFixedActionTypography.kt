package com.tomsphone.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

/**
 * Typography for end-call action bars (End call / Speaker) that must **not** follow the carer
 * appearance text scale ([LocalUserScale]). Sizes are derived from the available button slot so
 * fixed strings fit on the device; the subtitle is sized so **"Tap again"** stays on one line
 * (longer than "Tap twice").
 *
 * The status strip above these buttons continues to use [ScaledDimensions] like Home.
 * Also used for the emergency active-call “Back to home” bar (after including that title in sizing).
 */
data class EndCallFixedActionTypography(
    val titleStyle: TextStyle,
    val subtitleStyle: TextStyle,
)

private fun noPadStyle() = PlatformTextStyle(includeFontPadding = false)

private fun measureWidth(
    textMeasurer: TextMeasurer,
    text: String,
    style: TextStyle,
    maxWidthPx: Int,
): Int =
    textMeasurer
        .measure(
            text = text,
            style = style,
            maxLines = 1,
            constraints = Constraints(maxWidth = maxWidthPx),
        )
        .size
        .width

private fun measureHeight(
    textMeasurer: TextMeasurer,
    text: String,
    style: TextStyle,
    maxWidthPx: Int,
): Int =
    textMeasurer
        .measure(
            text = text,
            style = style,
            maxLines = 1,
            constraints = Constraints(maxWidth = maxWidthPx),
        )
        .size
        .height

private fun computeEndCallFixedActionTypography(
    textMeasurer: TextMeasurer,
    maxContentWidthPx: Int,
    perButtonMaxHeightPx: Int,
): EndCallFixedActionTypography {
    val widthPx = max(64, maxContentWidthPx)
    val heightPx = max(48, perButtonMaxHeightPx)
    val innerPadV = (heightPx * 0.12f).toInt().coerceIn(6, 24)
    val lineGapPx = (heightPx * 0.06f).toInt().coerceIn(2, 12)
    val innerH = (heightPx - innerPadV).coerceAtLeast(24)

    val maxSubSp = min(30, (innerH / 3.2f).toInt().coerceAtLeast(9))
    var titleStyle: TextStyle? = null
    var subtitleStyle: TextStyle? = null

    for (subSp in maxSubSp downTo 8) {
        val titleSp = min(36, (subSp * 1.22f).toInt().coerceAtLeast(subSp + 1))
        val subStyle =
            TextStyle(
                fontSize = subSp.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = (subSp * 1.2f).sp,
                platformStyle = noPadStyle(),
            )
        val titStyle =
            TextStyle(
                fontSize = titleSp.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = (titleSp * 1.1f).sp,
                platformStyle = noPadStyle(),
            )

        val wSubtitle = measureWidth(textMeasurer, "Tap again", subStyle, widthPx)
        val wTitle =
            maxOf(
                measureWidth(textMeasurer, "Speaker off", titStyle, widthPx),
                measureWidth(textMeasurer, "Speaker on", titStyle, widthPx),
                measureWidth(textMeasurer, "End call", titStyle, widthPx),
                measureWidth(textMeasurer, "Back to home", titStyle, widthPx),
            )
        if (wSubtitle > widthPx || wTitle > widthPx) continue

        val hTitle =
            maxOf(
                measureHeight(textMeasurer, "Speaker off", titStyle, widthPx),
                measureHeight(textMeasurer, "Speaker on", titStyle, widthPx),
                measureHeight(textMeasurer, "End call", titStyle, widthPx),
                measureHeight(textMeasurer, "Back to home", titStyle, widthPx),
            )
        val hSub = measureHeight(textMeasurer, "Tap again", subStyle, widthPx)
        val total = hTitle + hSub + lineGapPx
        if (total <= innerH) {
            titleStyle = titStyle
            subtitleStyle = subStyle
            break
        }
    }

    val fallbackSub = 10f
    val fallbackTitle = 12f
    return EndCallFixedActionTypography(
        titleStyle =
            titleStyle
                ?: TextStyle(
                    fontSize = fallbackTitle.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = (fallbackTitle * 1.1f).sp,
                    platformStyle = noPadStyle(),
                ),
        subtitleStyle =
            subtitleStyle
                ?: TextStyle(
                    fontSize = fallbackSub.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = (fallbackSub * 1.2f).sp,
                    platformStyle = noPadStyle(),
                ),
    )
}

@Composable
fun rememberEndCallFixedActionTypography(
    maxContentWidth: Dp,
    perButtonMaxHeight: Dp,
): EndCallFixedActionTypography {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val widthKey = maxContentWidth.value
    val heightKey = perButtonMaxHeight.value
    return remember(widthKey, heightKey, density.density, density.fontScale) {
        val wPx = with(density) { maxContentWidth.roundToPx() }.coerceAtLeast(1)
        val hPx = with(density) { perButtonMaxHeight.roundToPx() }.coerceAtLeast(1)
        computeEndCallFixedActionTypography(textMeasurer, wPx, hPx)
    }
}

// --- Emergency pre-dial (confirm) screen: dial + instruction, not carer appearance scale ---

data class EmergencyConfirmDialTypography(
    val primaryLine: TextStyle,
    val callLine: TextStyle,
)

private fun computeEmergencyConfirmDialTypography(
    textMeasurer: TextMeasurer,
    dialPrimaryText: String,
    innerWidthPx: Int,
    innerHeightPx: Int,
): EmergencyConfirmDialTypography {
    val wPx = max(48, innerWidthPx)
    val hPx = max(40, innerHeightPx)
    val lineGapPx = (hPx * 0.06f).toInt().coerceIn(2, 10)
    val innerH = (hPx - (hPx * 0.1f).toInt().coerceIn(4, 20)).coerceAtLeast(20)

    val maxCallSp = min(32, (innerH / 3.4f).toInt().coerceAtLeast(8))
    var primaryStyle: TextStyle? = null
    var callStyle: TextStyle? = null

    for (callSp in maxCallSp downTo 8) {
        val titleSp = min(40, (callSp * 1.25f).toInt().coerceAtLeast(callSp + 1))
        val callSt =
            TextStyle(
                fontSize = callSp.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = (callSp * 1.2f).sp,
                platformStyle = noPadStyle(),
            )
        val primSt =
            TextStyle(
                fontSize = titleSp.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = (titleSp * 1.1f).sp,
                platformStyle = noPadStyle(),
            )
        val wPrimary = measureWidth(textMeasurer, dialPrimaryText, primSt, wPx)
        val wCall = measureWidth(textMeasurer, "CALL", callSt, wPx)
        if (wPrimary > wPx || wCall > wPx) continue

        val hPrimary = measureHeight(textMeasurer, dialPrimaryText, primSt, wPx)
        val hCall = measureHeight(textMeasurer, "CALL", callSt, wPx)
        if (hPrimary + hCall + lineGapPx > innerH) continue

        primaryStyle = primSt
        callStyle = callSt
        break
    }

    val fbCall = 11f
    val fbPrim = 14f
    return EmergencyConfirmDialTypography(
        primaryLine =
            primaryStyle
                ?: TextStyle(
                    fontSize = fbPrim.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = (fbPrim * 1.1f).sp,
                    platformStyle = noPadStyle(),
                ),
        callLine =
            callStyle
                ?: TextStyle(
                    fontSize = fbCall.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = (fbCall * 1.2f).sp,
                    platformStyle = noPadStyle(),
                ),
    )
}

@Composable
fun rememberEmergencyConfirmDialTypography(
    dialPrimaryText: String,
    innerContentWidth: Dp,
    innerContentHeight: Dp,
): EmergencyConfirmDialTypography {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val wKey = innerContentWidth.value
    val hKey = innerContentHeight.value
    val textKey = dialPrimaryText
    return remember(wKey, hKey, textKey, density.density, density.fontScale) {
        val wPx = with(density) { innerContentWidth.roundToPx() }.coerceAtLeast(1)
        val hPx = with(density) { innerContentHeight.roundToPx() }.coerceAtLeast(1)
        computeEmergencyConfirmDialTypography(textMeasurer, dialPrimaryText, wPx, hPx)
    }
}

private fun computeEmergencyConfirmInstructionStyle(
    textMeasurer: TextMeasurer,
    maxWidthPx: Int,
    requiredTaps: Int,
): TextStyle {
    val wPx = max(64, maxWidthPx)
    val candidates =
        buildList {
            add("Press $requiredTaps times")
            for (i in 1..requiredTaps) {
                add("$i / $requiredTaps")
            }
        }
    for (sp in 30 downTo 10) {
        val style =
            TextStyle(
                fontSize = sp.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = (sp * 1.25f).sp,
                platformStyle = noPadStyle(),
            )
        val ok = candidates.all { measureWidth(textMeasurer, it, style, wPx) <= wPx }
        if (ok) return style
    }
    return TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 15.sp,
        platformStyle = noPadStyle(),
    )
}

@Composable
fun rememberEmergencyConfirmInstructionStyle(
    maxLineWidth: Dp,
    requiredTaps: Int,
): TextStyle {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val wKey = maxLineWidth.value
    return remember(wKey, requiredTaps, density.density, density.fontScale) {
        val wPx = with(density) { maxLineWidth.roundToPx() }.coerceAtLeast(1)
        computeEmergencyConfirmInstructionStyle(textMeasurer, wPx, requiredTaps)
    }
}

@Composable
fun rememberEmergencyConfirmBackRowStyle(maxLineWidth: Dp): TextStyle {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val wKey = maxLineWidth.value
    return remember(wKey, density.density, density.fontScale) {
        val wPx = with(density) { maxLineWidth.roundToPx() }.coerceAtLeast(1)
        for (sp in 28 downTo 12) {
            val style =
                TextStyle(
                    fontSize = sp.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = (sp * 1.15f).sp,
                    platformStyle = noPadStyle(),
                )
            if (measureWidth(textMeasurer, "Back", style, wPx) <= wPx) return@remember style
        }
        TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 16.sp,
            platformStyle = noPadStyle(),
        )
    }
}

/** Icon size for the pre-dial Back row, derived from screen width (not carer text scale). */
@Composable
fun rememberEmergencyConfirmBackIconSize(): Dp {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        val m = min(configuration.screenWidthDp, configuration.screenHeightDp).toFloat()
        (m * 0.065f).dp.coerceIn(22.dp, 40.dp)
    }
}

/** Round dial diameter for pre-dial emergency confirm (not [LocalUserScale]). */
@Composable
fun rememberEmergencyConfirmDialDiameter(): Dp {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp) {
        ((configuration.screenWidthDp * 0.72f).dp).coerceIn(148.dp, 300.dp)
    }
}
