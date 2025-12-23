package com.wandasphone.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.wandasphone.core.ui.theme.WandasDimensions

/**
 * Inert border layout that creates a "dead zone" around screen edges.
 * 
 * This prevents:
 * - Accidental touches from fingers curling onto screen edges
 * - Palm touches from grip
 * - Edge swipe gestures
 * 
 * The border area does not respond to touches.
 */
@Composable
fun InertBorderLayout(
    borderWidth: Dp = WandasDimensions.InertBorderWidth,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(borderWidth)
    ) {
        content()
    }
}

