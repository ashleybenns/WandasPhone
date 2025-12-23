package com.wandasphone.core.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Dimensions and spacing system for WandasPhone
 * 
 * Design principles:
 * - Minimum 72dp touch targets (prefer 96dp+)
 * - Inert border around screen edges (24-32dp)
 * - Generous spacing to prevent accidental touches
 */

object WandasDimensions {
    // Touch targets
    val MinimumTouchTarget = 72.dp
    val PreferredTouchTarget = 96.dp
    val LargeTouchTarget = 120.dp
    
    // Inert border (dead zone around edges)
    val InertBorderWidth = 28.dp
    val InertBorderWidthLarge = 32.dp
    
    // Spacing
    val SpacingTiny = 4.dp
    val SpacingSmall = 8.dp
    val SpacingMedium = 16.dp
    val SpacingLarge = 24.dp
    val SpacingExtraLarge = 32.dp
    val SpacingHuge = 48.dp
    
    // Button dimensions
    val ButtonHeightSmall = 72.dp
    val ButtonHeightMedium = 96.dp
    val ButtonHeightLarge = 120.dp
    val ButtonHeightExtraLarge = 160.dp
    
    // Contact button dimensions (home screen)
    val ContactButtonHeight = 140.dp
    val ContactButtonHeightLarge = 180.dp
    
    // Emergency button
    val EmergencyButtonHeight = 96.dp
    
    // Corner radius
    val CornerRadiusSmall = 8.dp
    val CornerRadiusMedium = 16.dp
    val CornerRadiusLarge = 24.dp
    
    // Elevation
    val ElevationSmall = 4.dp
    val ElevationMedium = 8.dp
    val ElevationLarge = 12.dp
    
    // Icon sizes
    val IconSizeSmall = 24.dp
    val IconSizeMedium = 32.dp
    val IconSizeLarge = 48.dp
    val IconSizeExtraLarge = 64.dp
    
    // Contact photo sizes
    val ContactPhotoSmall = 64.dp
    val ContactPhotoMedium = 96.dp
    val ContactPhotoLarge = 120.dp
}

