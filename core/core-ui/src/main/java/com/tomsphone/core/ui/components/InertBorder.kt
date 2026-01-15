package com.tomsphone.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.tomsphone.core.ui.theme.WandasDimensions

/**
 * Inert border layout that creates a "dead zone" around screen edges.
 * 
 * This prevents:
 * - Accidental touches from fingers curling onto screen edges
 * - Palm touches from grip
 * - Edge swipe gestures (especially bottom gestures that exit app)
 * 
 * The border area does not respond to touches.
 * Bottom border is extra large (56dp) to prevent triggering gesture navigation.
 */
@Composable
fun InertBorderLayout(
    modifier: Modifier = Modifier,
    borderWidth: Dp = WandasDimensions.InertBorderWidth,
    bottomBorderWidth: Dp = WandasDimensions.InertBorderBottom,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = borderWidth,
                top = borderWidth,
                end = borderWidth,
                bottom = bottomBorderWidth  // Extra large at bottom
            )
    ) {
        content()
    }
}

