package com.tomsphone.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.ui.theme.ScaledDimensions
import com.tomsphone.core.ui.theme.WandasDimensions

/**
 * Layout for list screens (Missed Calls, Contacts)
 * 
 * Matches home screen design:
 * - Pastel background color (matches the button that navigated here)
 * - Inert gutter around edges (same size as home screen)
 * - Back button at top middle
 * - Title below back button
 * - Content area: [contentScrollable] adds vertical scroll (default). When false, no scroll — use
 *   pagination + [footer] (e.g. Next) pinned at the bottom for seniors who cannot scroll reliably.
 */
@Composable
fun ListScreenLayout(
    backgroundColor: Color,
    title: String,
    emptyMessage: String,
    isEmpty: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentScrollable: Boolean = true,
    activationPreset: ButtonActivationPreset = ButtonActivationPreset.ON_RELEASE,
    debounceMs: Int = 150,
    accumulatedThresholdMs: Int = 500,
    accumulatedTimeoutMs: Int = 3000,
    /** Space between list rows; use [Arrangement.Top] when rows supply their own vertical padding (e.g. contacts). */
    listVerticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    /** Optional modifier applied to the list viewport (the weighted content area, excluding header/footer). */
    listViewportModifier: Modifier = Modifier,
    footer: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    // Use larger contact name size for back button and title
    val headerTextSize = ScaledDimensions.contactNameTextSize
    val iconSize = headerTextSize.value.dp * 1.2f  // Icon slightly larger than text
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Inert border padding (same as home screen)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = WandasDimensions.InertBorderWidth,
                    top = WandasDimensions.InertBorderWidth,
                    end = WandasDimensions.InertBorderWidth,
                    bottom = WandasDimensions.InertBorderBottom
                )
        ) {
            // Back button row - uses activation gesture for consistency
            val interactionSource = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .indication(interactionSource, rememberRipple())
                    .activationGesture(
                        preset = activationPreset,
                        debounceMs = debounceMs,
                        accumulatedThresholdMs = accumulatedThresholdMs,
                        accumulatedTimeoutMs = accumulatedTimeoutMs,
                        onActivate = onBack,
                        interactionSource = interactionSource
                    )
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.size(iconSize)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Back",
                    style = TextStyle(
                        fontSize = headerTextSize,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.Black
                )
            }
            
            // Title (only show if not empty)
            if (title.isNotEmpty()) {
                Text(
                    text = if (isEmpty) emptyMessage else title,
                    style = TextStyle(
                        fontSize = headerTextSize,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }
            
            // Main list area — optional scroll; footer stays below and does not scroll
            val listModifier = Modifier
                .weight(1f)
                .fillMaxWidth()
            Column(
                modifier = if (contentScrollable) {
                    listModifier
                        .then(listViewportModifier)
                        .verticalScroll(rememberScrollState())
                } else {
                    listModifier.then(listViewportModifier)
                },
                verticalArrangement = listVerticalArrangement,
                content = content
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = footer
            )
        }
    }
}
