package com.tomsphone.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/**
 * User-facing screen scaling system
 * 
 * Scales text and container dimensions based on carer-configured text scale.
 * This is SEPARATE from Android's system font scaling.
 * 
 * IMPORTANT: Scale is automatically capped to ensure MIN_ROWS (6) always fit.
 * This prevents larger text from pushing buttons off screen on shorter devices.
 * 
 * Usage:
 * 1. Wrap user-facing screens with UserScalingProvider
 * 2. Use LocalUserScale.current to get the scale factor
 * 3. Use ScaledDimensions for pre-scaled common dimensions
 * 
 * Carer settings screens should NOT use this - they stay at normal size (scale = 1.0f).
 */

/**
 * CompositionLocal providing the current user text scale
 */
val LocalUserScale = compositionLocalOf { 1.0f }

/**
 * Provider that sets up user scaling for a screen
 * 
 * Calculates optimal scale based on:
 * - Available screen height
 * - Number of button rows to display
 * - Status box and emergency button (always present)
 * 
 * Scale is capped to ensure 12 characters fit per line (readability).
 * Carer can reduce from max via userScaleReduction (1.0 = max, 0.8 = 80% of max).
 * 
 * @param buttonRowCount Number of button rows (contact + menu + screen off)
 * @param userScaleReduction Carer's reduction factor (1.0 = use max, lower = smaller text)
 */
@Composable
fun UserScalingProvider(
    buttonRowCount: Int,
    userScaleReduction: Float = 1.0f,
    content: @Composable () -> Unit
) {
    // Calculate optimal scale for this button count
    val optimalScale = calculateOptimalScale(buttonRowCount)
    
    // Apply carer's reduction preference
    val rawEffective = optimalScale * userScaleReduction.coerceIn(0.5f, 1.0f)
    val effectiveScale =
        if (rawEffective.isFinite() && rawEffective > 0f) rawEffective else 1f

    CompositionLocalProvider(LocalUserScale provides effectiveScale) {
        content()
    }
}

/**
 * Legacy provider for screens that don't have button counts (call screens, etc.)
 * Uses fixed scale with basic capping.
 */
@Composable
fun UserScalingProvider(
    scale: Float,
    content: @Composable () -> Unit
) {
    // For non-home screens, use a reasonable fixed scale
    val cappedScale = scale.coerceIn(0.7f, 1.5f)
    
    CompositionLocalProvider(LocalUserScale provides cappedScale) {
        content()
    }
}

/**
 * Calculate optimal scale based on screen height and button count
 * 
 * This is a SCREEN-FIRST approach:
 * 1. Calculate available height after fixed elements
 * 2. Divide by weighted line equivalents (accounting for different text sizes)
 * 3. Convert to scale factor based on contact name text (32sp)
 * 4. Cap at max for readability
 * 
 * Text sizes (base):
 * - Status: 28sp (3 lines)
 * - Contact name: 32sp (2 lines per button)
 * - Emergency button: 24sp (2 lines)
 */
@Composable
fun calculateOptimalScale(buttonRowCount: Int): Float {
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.toFloat()
    
    // Fixed elements (dp) - these don't scale
    val topGutterDp = 40f           // Warning strip area
    val edgePaddingDp = 24f         // Top + bottom padding
    val minRowSpacingDp = 4f * (buttonRowCount + 2)  // Spacing between all rows
    
    // Available for content (status + buttons + emergency)
    val availableHeight = screenHeightDp - topGutterDp - edgePaddingDp - minRowSpacingDp
    
    // Weighted line equivalents - normalized to 32sp (contact name size)
    // This correctly accounts for different text sizes
    val statusWeight = 28f / 32f    // Status uses 28sp
    val buttonWeight = 32f / 32f    // Contact name uses 32sp (reference)
    val emergencyWeight = 24f / 32f // Emergency uses 24sp
    
    val statusLineEquiv = 3f * statusWeight      // 3 lines × 0.875 = 2.625
    val buttonLineEquiv = 2f * buttonRowCount.coerceAtLeast(1) * buttonWeight  // 2 lines each
    val emergencyLineEquiv = 2f * emergencyWeight // 2 lines × 0.75 = 1.5
    
    val totalLineEquivalents = statusLineEquiv + buttonLineEquiv + emergencyLineEquiv
    
    // Height per "32sp equivalent line" (in dp)
    val heightPerLine = availableHeight / totalLineEquivalents
    
    // Convert to scale factor
    // Base: 32sp text with minimal line spacing for single-line fit
    // Using 1.1 instead of 1.3 allows larger text for single-line names
    // Carer can reduce if they need 2-line names
    val baseLineHeight = 32f * 1.1f
    val calculatedScale = heightPerLine / baseLineHeight
    
    // Max scale based on button count (no artificial caps - let screen size decide)
    // These are safety caps for very tall screens
    val maxScale = when {
        buttonRowCount >= 6 -> 1.5f   // 6+ buttons: reasonable single-line size
        buttonRowCount >= 4 -> 2.0f   // 4-5 buttons: can go bigger
        buttonRowCount >= 2 -> 2.8f   // 2-3 buttons: much bigger text possible
        else -> 3.5f                  // 1 button: very large
    }
    
    // Minimum for readability
    val minScale = 0.7f
    
    val bounded = calculatedScale.coerceIn(minScale, maxScale)
    return if (bounded.isFinite()) bounded else minScale
}

/**
 * Scaled dimensions object for use in user-facing screens
 * 
 * Heights are calculated based on text size and line count.
 * This ensures containers are always tall enough for their content.
 * 
 * Formula: height = (textSize * lineHeight * lineCount) + padding
 * Where lineHeight = 1.3 (standard for readability)
 */
object ScaledDimensions {
    
    // Line height multiplier (1.3 = 30% extra space between lines)
    private const val LINE_HEIGHT_MULTIPLIER = 1.3f
    
    // Vertical padding inside containers
    private const val CONTAINER_PADDING_DP = 16f
    
    /**
     * Get a scaled Dp value based on current user scale
     */
    @Composable
    fun scaledDp(baseDp: Float): Dp {
        val scale = LocalUserScale.current
        return (baseDp * scale).dp
    }
    
    /**
     * Get a scaled TextUnit (sp) based on current user scale
     */
    @Composable
    fun scaledSp(baseSp: Float): TextUnit {
        val scale = LocalUserScale.current
        val safe = if (scale.isFinite() && scale > 0f) scale else 1f
        return (baseSp * safe).sp
    }
    
    /**
     * Calculate container height for N lines of text at given base font size
     * Height = (fontSize * scale * lineHeight * lines) + padding
     */
    @Composable
    fun heightForLines(lines: Int, baseFontSizeSp: Float): Dp {
        val scale = LocalUserScale.current
        val textHeight = baseFontSizeSp * scale * LINE_HEIGHT_MULTIPLIER * lines
        return (textHeight + CONTAINER_PADDING_DP).dp
    }
    
    // ========== BASE TEXT SIZES (before scaling) ==========
    
    private const val STATUS_TEXT_BASE_SP = 28f
    private const val CONTACT_NAME_BASE_SP = 32f
    private const val BUTTON_TEXT_BASE_SP = 24f
    
    // ========== CONTAINER HEIGHTS (calculated from text) ==========
    
    /**
     * Status message box height - fits 3 lines of status text
     * Used for: "Jim.\nYou missed a call.\nCall Ashley now."
     */
    val statusBoxHeight: Dp
        @Composable get() = heightForLines(3, STATUS_TEXT_BASE_SP)
    
    /**
     * Contact button height - fits 2 lines of contact name
     * Allows for long names or names with warning badges
     */
    val contactButtonHeight: Dp
        @Composable get() = heightForLines(2, CONTACT_NAME_BASE_SP)
    
    /**
     * Emergency button height - fits 2 lines of button text
     */
    val emergencyButtonHeight: Dp
        @Composable get() = heightForLines(2, BUTTON_TEXT_BASE_SP)
    
    /**
     * End call button size - circular, needs extra width for short words like "End"
     * Uses larger multiplier to ensure text fits horizontally in circle
     */
    val endCallButtonSize: Dp
        @Composable get() {
            val scale = LocalUserScale.current
            // Base ~128dp — slightly larger than contact-name line height so bold "End" fits with descenders
            return (128f * scale).dp
        }
    
    /**
     * End call instruction height - fits 2 lines of instruction text
     */
    val endCallInstructionHeight: Dp
        @Composable get() = heightForLines(2, STATUS_TEXT_BASE_SP)
    
    // ========== SCALED TEXT SIZES ==========
    
    /**
     * Status message text size (base: 28sp, scales with user setting)
     */
    val statusTextSize: TextUnit
        @Composable get() = scaledSp(STATUS_TEXT_BASE_SP)
    
    /**
     * Button text size (base: 24sp, scales with user setting)
     */
    val buttonTextSize: TextUnit
        @Composable get() = scaledSp(BUTTON_TEXT_BASE_SP)
    
    /**
     * Contact name text size (base: 32sp, scales with user setting)
     */
    val contactNameTextSize: TextUnit
        @Composable get() = scaledSp(CONTACT_NAME_BASE_SP)
    
    // ========== SPACING (fixed) ==========
    
    /**
     * Edge padding around button area
     */
    val edgePadding: Dp
        @Composable get() = 12.dp
    
    /**
     * Minimum spacing between buttons
     */
    val buttonSpacing: Dp
        @Composable get() = 8.dp

    /**
     * Inner height of one home contact row (the drawable button), derived from the same vertical budget
     * as the home screen: battery strip, status box, inert bottom inset, inner padding, emergency row +
     * spacer, then equal [weight(1f)] slots with 4.dp vertical padding above and below each slot.
     *
     * [buttonRowCount] must be the **actual** number of weighted rows on home (from settings’
     * home button row count), not the 2–6 cap used only for [UserScalingProvider] scale — otherwise
     * list pages divide the same pool by too few rows and look oversized.
     */
    @Composable
    fun homeContactRowInnerHeight(buttonRowCount: Int): Dp {
        val rows = buttonRowCount.coerceIn(1, 12)
        val configuration = LocalConfiguration.current
        val screenH = configuration.screenHeightDp.dp
        // Matches calculateOptimalScale top gutter (battery / warnings strip)
        val belowStatus = screenH - 40.dp - statusBoxHeight
        val inertContentH = belowStatus - WandasDimensions.InertBorderBottom
        val innerColumnMax = inertContentH - edgePadding
        val bottomSection = buttonSpacing + emergencyButtonHeight
        val middlePool = innerColumnMax - bottomSection
        if (middlePool <= 0.dp) return contactButtonHeight
        val perSlot = middlePool / rows
        val inner = perSlot - 8.dp
        return if (inner > 0.dp) inner else contactButtonHeight
    }
}
