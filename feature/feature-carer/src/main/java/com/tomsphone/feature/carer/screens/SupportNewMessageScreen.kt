package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.components.CarerBreadcrumb
import com.tomsphone.feature.carer.components.DevLevelIndicator
import com.tomsphone.feature.carer.support.SupportSuggestionsViewModel

private const val SUPPORT_CATEGORY_SUPPORT = "support"
private const val SUPPORT_CATEGORY_FEATURE = "feature_suggestion"

@Composable
fun SupportNewMessageScreen(
    onBack: () -> Unit,
    onSent: (threadId: String?) -> Unit,
    viewModel: SupportSuggestionsViewModel = hiltViewModel()
) {
    val submitState by viewModel.submitState.collectAsState()
    var category by remember { mutableStateOf(SUPPORT_CATEGORY_SUPPORT) }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.ensureDeviceId()
    }

    LaunchedEffect(submitState) {
        when (val s = submitState) {
            is SupportSuggestionsViewModel.SubmitState.Success -> {
                viewModel.clearSubmitState()
                onSent(s.threadId)
            }
            else -> {}
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.wandasColors.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            DevLevelIndicator(level = com.tomsphone.core.config.FeatureLevel.MINIMAL)
            CarerBreadcrumb(
                title = "New message",
                parentTitle = "Support & suggestions",
                onBack = onBack
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(WandasDimensions.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingMedium)
            ) {
                Text(
                    text = "We read every message and may reply in the app. Your feedback is anonymous.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SettingCard(title = "Category") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = category == SUPPORT_CATEGORY_SUPPORT,
                            onClick = { category = SUPPORT_CATEGORY_SUPPORT }
                        )
                        Text(
                            text = "Support",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.wandasColors.onSurface,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = category == SUPPORT_CATEGORY_FEATURE,
                            onClick = { category = SUPPORT_CATEGORY_FEATURE }
                        )
                        Text(
                            text = "Feature suggestion",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.wandasColors.onSurface,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                SettingCard(title = "Message") {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        placeholder = { Text("Describe your question or idea…") },
                        minLines = 4,
                        maxLines = 8,
                        enabled = submitState !is SupportSuggestionsViewModel.SubmitState.Sending
                    )
                }
                Button(
                    onClick = { if (message.isNotBlank()) viewModel.submitNewThread(category, message) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = message.isNotBlank() && submitState !is SupportSuggestionsViewModel.SubmitState.Sending
                ) {
                    Text(
                        when (submitState) {
                            is SupportSuggestionsViewModel.SubmitState.Sending -> "Sending…"
                            else -> "Send"
                        }
                    )
                }
                when (submitState) {
                    is SupportSuggestionsViewModel.SubmitState.Error -> Text(
                        text = "Could not send. Check connection and try again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.wandasColors.error
                    )
                    else -> {}
                }
            }
        }
    }
}
