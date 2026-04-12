package com.tomsphone.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

/**
 * ## Fitted label policy (home contact buttons, menu [ListButton], emergency, contacts list)
 *
 * - **Max lines:** Default **2**. Layout never targets a third line; at the minimum font size,
 *   [TextOverflow.Ellipsis] still applies so overflow is clipped with an ellipsis.
 * - **Font range:** Binary search from the carer **user text** scale (`maxSp`) down to
 *   **max(11sp, 50% of maxSp)** for the smallest allowed size.
 * - **Re-layout:** Composition re-runs when user text scale, label string, box size (px), or
 *   `maxLines` change (via [remember] keys in [FittedLabelInBox]).
 * - **Very long strings:** [clipForDisplay] truncates inputs longer than [SOFT_MAX_DISPLAY_CHARS]
 *   before measuring so pathological names do not force extra work or unreadable micro-text;
 *   full names remain in carer settings; tap actions still use the real contact name from data.
 */
object LabelFitPolicy {
    const val DEFAULT_MAX_LINES = 2
    const val LINE_HEIGHT_MULTIPLIER = 1.25f
    const val MIN_FONT_SP_FLOOR = 11f
    const val MIN_FONT_RATIO_OF_MAX = 0.5f

    /** Soft cap before measure; avoids extreme multi-segment names dominating layout. */
    const val SOFT_MAX_DISPLAY_CHARS = 56

    fun clipForDisplay(text: String): String {
        if (text.length <= SOFT_MAX_DISPLAY_CHARS) return text
        return text.take(SOFT_MAX_DISPLAY_CHARS - 1) + "…"
    }
}

/**
 * Largest font size in sp such that [text] fits in the box at most [maxLines] lines, or [minSp]
 * if nothing larger fits.
 */
fun TextMeasurer.findLargestFontSp(
    text: String,
    maxWidthPx: Int,
    maxHeightPx: Int,
    maxSp: Float,
    minSp: Float,
    maxLines: Int,
    fontWeight: FontWeight,
    textAlign: TextAlign,
    lineHeightMultiplier: Float = LabelFitPolicy.LINE_HEIGHT_MULTIPLIER
): Float {
    if (text.isEmpty()) return maxSp.coerceIn(minSp, maxSp)
    val w = maxWidthPx.coerceAtLeast(1)
    val h = maxHeightPx.coerceAtLeast(1)

    fun fits(sp: Float): Boolean {
        if (sp < minSp) return false
        val lineH = sp * lineHeightMultiplier
        val layout = measure(
            text = AnnotatedString(text),
            style = TextStyle(
                fontSize = sp.sp,
                fontWeight = fontWeight,
                lineHeight = lineH.sp,
                textAlign = textAlign
            ),
            constraints = Constraints(maxWidth = w),
            maxLines = maxLines
        )
        val linesOk = layout.lineCount <= maxLines
        val heightOk = layout.size.height <= h
        return linesOk && heightOk
    }

    val loStart = (minSp * 10f).toInt().coerceAtLeast(1)
    val hiStart = (maxSp * 10f).toInt().coerceAtLeast(loStart)
    var lo = loStart
    var hi = hiStart
    var best = loStart
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
    val candidate = best / 10f
    return if (fits(candidate)) candidate else minSp
}

/**
 * Single shared font for two single-line rows (e.g. emergency title + number), both must fit
 * in [maxHeightPx] including [gapPx] between rows.
 */
fun TextMeasurer.findLargestFontSpTwoSingleLineRows(
    line1: String,
    line2: String,
    maxWidthPx: Int,
    maxHeightPx: Int,
    maxSp: Float,
    minSp: Float,
    gapPx: Int,
    fontWeight1: FontWeight,
    fontWeight2: FontWeight,
    textAlign: TextAlign,
    lineHeightMultiplier: Float = 1f
): Float {
    val w = maxWidthPx.coerceAtLeast(1)
    val h = maxHeightPx.coerceAtLeast(1)
    if (line1.isEmpty() && line2.isEmpty()) return maxSp.coerceIn(minSp, maxSp)

    fun fits(sp: Float): Boolean {
        if (sp < minSp) return false
        val lineH = sp * lineHeightMultiplier
        fun measureRow(s: String, weight: FontWeight): Pair<Boolean, Int> {
            if (s.isEmpty()) return true to 0
            val layout = measure(
                text = AnnotatedString(s),
                style = TextStyle(
                    fontSize = sp.sp,
                    fontWeight = weight,
                    lineHeight = lineH.sp,
                    textAlign = textAlign
                ),
                constraints = Constraints(maxWidth = w),
                maxLines = 1
            )
            val ok = layout.lineCount <= 1 && layout.size.width <= w
            return ok to layout.size.height
        }
        val (ok1, height1) = measureRow(line1, fontWeight1)
        val (ok2, height2) = measureRow(line2, fontWeight2)
        if (!ok1 || !ok2) return false
        val gaps = if (line1.isNotEmpty() && line2.isNotEmpty()) gapPx else 0
        return height1 + gaps + height2 <= h
    }

    val loStart = (minSp * 10f).toInt().coerceAtLeast(1)
    val hiStart = (maxSp * 10f).toInt().coerceAtLeast(loStart)
    var lo = loStart
    var hi = hiStart
    var best = loStart
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
    val candidate = best / 10f
    return if (fits(candidate)) candidate else minSp
}

@Composable
fun FittedLabelInBox(
    text: String,
    color: Color,
    textAlign: TextAlign,
    maxFontSize: TextUnit,
    modifier: Modifier = Modifier,
    maxLines: Int = LabelFitPolicy.DEFAULT_MAX_LINES,
    fontWeight: FontWeight = FontWeight.SemiBold
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val displayText = remember(text) { LabelFitPolicy.clipForDisplay(text) }
    BoxWithConstraints(modifier = modifier) {
        val baseMaxSp = maxFontSize.value
        val maxWpx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        // Unbounded height (some measure passes) used to pick max font and drew outside the real box.
        // Cap by line count × font so fitting matches visible clipped buttons (same as calling state).
        val maxHpx = if (maxHeight.value.isFinite()) {
            with(density) { maxHeight.roundToPx() }.coerceAtLeast(1)
        } else {
            with(density) {
                (baseMaxSp * LabelFitPolicy.LINE_HEIGHT_MULTIPLIER * maxLines * 1.15f)
                    .sp
                    .roundToPx()
                    .coerceIn(1, 4096)
            }
        }
        val minSp = max(
            LabelFitPolicy.MIN_FONT_SP_FLOOR,
            baseMaxSp * LabelFitPolicy.MIN_FONT_RATIO_OF_MAX
        ).coerceAtMost(baseMaxSp)
        val chosenSp = remember(
            displayText,
            maxWpx,
            maxHpx,
            baseMaxSp,
            minSp,
            maxLines,
            fontWeight,
            textAlign
        ) {
            textMeasurer.findLargestFontSp(
                text = displayText,
                maxWidthPx = maxWpx,
                maxHeightPx = maxHpx,
                maxSp = baseMaxSp,
                minSp = minSp,
                maxLines = maxLines,
                fontWeight = fontWeight,
                textAlign = textAlign
            )
        }
        val lineHeight = chosenSp * LabelFitPolicy.LINE_HEIGHT_MULTIPLIER
        val labelBoxAlignment = when (textAlign) {
            TextAlign.Start, TextAlign.Left -> Alignment.CenterStart
            TextAlign.End, TextAlign.Right -> Alignment.CenterEnd
            else -> Alignment.Center
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = labelBoxAlignment
        ) {
            Text(
                text = displayText,
                style = TextStyle(
                    fontSize = chosenSp.sp,
                    fontWeight = fontWeight,
                    lineHeight = lineHeight.sp,
                    textAlign = textAlign
                ),
                color = color,
                textAlign = textAlign,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun FittedEmergencyTwoLinesInBox(
    title: String,
    subtitleOrProgress: String?,
    color: Color,
    maxFontSize: TextUnit,
    modifier: Modifier = Modifier,
    secondLineBold: Boolean = false
) {
    val displayTitle = remember(title) { LabelFitPolicy.clipForDisplay(title) }
    val line2 = remember(subtitleOrProgress) {
        subtitleOrProgress?.let { LabelFitPolicy.clipForDisplay(it) } ?: ""
    }
    if (line2.isEmpty()) {
        FittedLabelInBox(
            text = displayTitle,
            color = color,
            textAlign = TextAlign.Center,
            maxFontSize = maxFontSize,
            modifier = modifier,
            maxLines = 1,
            fontWeight = FontWeight.Medium
        )
        return
    }
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier) {
        val maxWpx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val maxHpx = with(density) { maxHeight.roundToPx() }.coerceAtLeast(1)
        val gapPx = with(density) { 2.dp.roundToPx() }
        val baseMaxSp = maxFontSize.value
        val minSp = max(
            LabelFitPolicy.MIN_FONT_SP_FLOOR,
            baseMaxSp * LabelFitPolicy.MIN_FONT_RATIO_OF_MAX
        ).coerceAtMost(baseMaxSp)
        val chosenSp = remember(
            displayTitle,
            line2,
            maxWpx,
            maxHpx,
            baseMaxSp,
            minSp,
            gapPx,
            secondLineBold
        ) {
            textMeasurer.findLargestFontSpTwoSingleLineRows(
                line1 = displayTitle,
                line2 = line2,
                maxWidthPx = maxWpx,
                maxHeightPx = maxHpx,
                maxSp = baseMaxSp,
                minSp = minSp,
                gapPx = gapPx,
                fontWeight1 = FontWeight.Medium,
                fontWeight2 = if (secondLineBold) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeightMultiplier = 1f
            )
        }
        val lineHeight = chosenSp
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = displayTitle,
                style = TextStyle(
                    fontSize = chosenSp.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = lineHeight.sp,
                    textAlign = TextAlign.Center
                ),
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = line2,
                style = TextStyle(
                    fontSize = chosenSp.sp,
                    fontWeight = if (secondLineBold) FontWeight.Bold else FontWeight.Medium,
                    lineHeight = lineHeight.sp,
                    textAlign = TextAlign.Center
                ),
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
