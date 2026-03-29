package com.tomsphone.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.delay

/**
 * Invoked when a control using [activationGesture] (or anything that reads this local) successfully
 * activates. Default is no-op (e.g. home screen).
 */
val LocalSecondaryScreenIdleReset = staticCompositionLocalOf<() -> Unit> { { } }

/**
 * Starts an idle timer that calls [onTimeout] after [timeoutMs] with no successful button activation.
 * Child composables reset the timer via [LocalSecondaryScreenIdleReset] (wired inside [activationGesture]).
 */
@Composable
fun SecondaryScreenIdleEffect(
    timeoutMs: Long,
    onTimeout: () -> Unit,
    content: @Composable () -> Unit
) {
    val generation = remember { mutableIntStateOf(0) }
    LaunchedEffect(generation.intValue) {
        delay(timeoutMs)
        onTimeout()
    }
    CompositionLocalProvider(
        LocalSecondaryScreenIdleReset provides { generation.intValue++ }
    ) {
        content()
    }
}
