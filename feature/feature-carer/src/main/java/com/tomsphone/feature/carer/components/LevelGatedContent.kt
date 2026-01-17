package com.tomsphone.feature.carer.components

import androidx.compose.runtime.Composable
import com.tomsphone.core.config.FeatureLevel

/**
 * Wrapper for content that should only appear at certain feature levels.
 * 
 * Usage:
 * ```
 * LevelGatedContent(
 *     minLevel = FeatureLevel.BASIC,  // Level 2+
 *     currentLevel = settings.featureLevel
 * ) {
 *     // This content only shows for Level 2 and above
 *     SettingRow("Speaker Toggle", ...)
 * }
 * ```
 * 
 * If content is not visible, no space is taken.
 */
@Composable
fun LevelGatedContent(
    minLevel: FeatureLevel,
    currentLevel: FeatureLevel,
    content: @Composable () -> Unit
) {
    if (currentLevel.level >= minLevel.level) {
        content()
    }
}

/**
 * Check if a feature is available at the current level.
 * 
 * Useful for conditional logic outside of Composables.
 */
fun isFeatureAvailable(
    minLevel: FeatureLevel,
    currentLevel: FeatureLevel
): Boolean {
    return currentLevel.level >= minLevel.level
}
