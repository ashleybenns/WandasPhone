package com.tomsphone.feature.carer.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tomsphone.core.config.FeatureLevel

/**
 * Level indicator - hidden for production (level system kept in code but not shown in UI).
 */
@Composable
fun DevLevelIndicator(
    level: FeatureLevel,
    modifier: Modifier = Modifier
) {
    // No-op: level system hidden for first production version
}
