package com.tomsphone.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * User-facing screen scaling system
 * 
 * Scales text and container dimensions based on carer-configured text scale.
 * This is SEPARATE from Android's system font scaling.
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
 * @param scale The scaling factor (1.0 = normal, 1.25 = large, 1.5 = extra large, etc.)
 */
@Composable
fun UserScalingProvider(
    scale: Float,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalUserScale provides scale) {
        content()
    }
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
        return (baseSp * scale).sp
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
     * End call button size - square, fits 2 lines
     */
    val endCallButtonSize: Dp
        @Composable get() = heightForLines(2, BUTTON_TEXT_BASE_SP)
    
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
}
