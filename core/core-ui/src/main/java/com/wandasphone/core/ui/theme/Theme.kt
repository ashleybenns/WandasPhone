package com.wandasphone.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Theme options for WandasPhone
 */
enum class ThemeOption {
    HIGH_CONTRAST_LIGHT,
    HIGH_CONTRAST_DARK,
    YELLOW_BLACK,
    SOFT_CONTRAST
}

/**
 * CompositionLocal for WandasPhone color scheme
 */
val LocalWandasColors = compositionLocalOf { HighContrastLightColors }

/**
 * Main theme composable for WandasPhone
 * 
 * Provides:
 * - 4 pre-built high-contrast themes
 * - Automatic contrast validation
 * - Custom color scheme with semantic naming
 * - Typography optimized for accessibility
 */
@Composable
fun WandasPhoneTheme(
    themeOption: ThemeOption = ThemeOption.HIGH_CONTRAST_LIGHT,
    content: @Composable () -> Unit
) {
    // Select color scheme based on theme option
    val wandasColors = when (themeOption) {
        ThemeOption.HIGH_CONTRAST_LIGHT -> HighContrastLightColors
        ThemeOption.HIGH_CONTRAST_DARK -> HighContrastDarkColors
        ThemeOption.YELLOW_BLACK -> YellowBlackColors
        ThemeOption.SOFT_CONTRAST -> SoftContrastColors
    }
    
    // Map WandasColorScheme to Material3 ColorScheme
    val materialColors = if (themeOption == ThemeOption.HIGH_CONTRAST_LIGHT || 
                             themeOption == ThemeOption.SOFT_CONTRAST) {
        lightColorScheme(
            primary = wandasColors.primaryButton,
            onPrimary = wandasColors.onPrimaryButton,
            secondary = wandasColors.secondaryButton,
            onSecondary = wandasColors.onSecondaryButton,
            background = wandasColors.background,
            onBackground = wandasColors.onBackground,
            surface = wandasColors.surface,
            onSurface = wandasColors.onSurface,
            error = wandasColors.error,
            onError = wandasColors.onError
        )
    } else {
        darkColorScheme(
            primary = wandasColors.primaryButton,
            onPrimary = wandasColors.onPrimaryButton,
            secondary = wandasColors.secondaryButton,
            onSecondary = wandasColors.onSecondaryButton,
            background = wandasColors.background,
            onBackground = wandasColors.onBackground,
            surface = wandasColors.surface,
            onSurface = wandasColors.onSurface,
            error = wandasColors.error,
            onError = wandasColors.onError
        )
    }
    
    // Provide both Material theme and custom Wandas colors
    CompositionLocalProvider(LocalWandasColors provides wandasColors) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = WandasTypography,
            content = content
        )
    }
}

/**
 * Extension property to access WandasPhone colors from any Composable
 */
val MaterialTheme.wandasColors: WandasColorScheme
    @Composable
    get() = LocalWandasColors.current

