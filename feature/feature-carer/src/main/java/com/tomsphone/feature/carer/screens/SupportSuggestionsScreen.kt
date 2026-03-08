package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.components.CarerBreadcrumb
import com.tomsphone.feature.carer.components.DevLevelIndicator
import com.tomsphone.feature.carer.support.SupportAnnouncement
import com.tomsphone.feature.carer.support.SupportThread
import com.tomsphone.feature.carer.support.SupportSuggestionsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SupportSuggestionsScreen(
    onBack: () -> Unit,
    onThreadClick: (String) -> Unit,
    onNewMessage: () -> Unit,
    viewModel: SupportSuggestionsViewModel = hiltViewModel()
) {
    val threads by viewModel.threads.collectAsState()
    val announcements by viewModel.announcements.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.ensureDeviceId()
        viewModel.markVisited()
        viewModel.loadThreads()
        viewModel.loadAnnouncements()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNewMessage,
                    containerColor = MaterialTheme.wandasColors.primaryButton,
                    contentColor = MaterialTheme.wandasColors.onPrimaryButton
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New message")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                DevLevelIndicator(level = com.tomsphone.core.config.FeatureLevel.MINIMAL)
                CarerBreadcrumb(
                    title = "Support & suggestions",
                    parentTitle = "Settings",
                    onBack = onBack
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(WandasDimensions.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingMedium)
                ) {
                    item {
                        Text(
                            text = "Anonymous. Your threads are only visible to you and support.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    if (announcements.isNotEmpty()) {
                        item {
                            SettingCard(title = "Updates & announcements") {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    announcements.sortedByDescending { it.createdAt }.forEach { a ->
                                        Column {
                                            Text(
                                                text = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(a.createdAt)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = a.body,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.wandasColors.onSurface,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            text = "Your threads",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.wandasColors.onSurface,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    if (threads.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.wandasColors.surface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("No threads yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.wandasColors.onSurface)
                                    Text("Tap + to send support or a feature suggestion.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f))
                                }
                            }
                        }
                    } else {
                        items(threads) { thread ->
                            ThreadCard(thread = thread, onClick = { onThreadClick(thread.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadCard(thread: SupportThread, onClick: () -> Unit) {
    val categoryLabel = when ((thread.category ?: "").lowercase()) {
        "support" -> "Support"
        "feature_suggestion" -> "Feature suggestion"
        else -> thread.category
    }
    val hasReplies = thread.replies.isNotEmpty()
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (hasReplies) MaterialTheme.wandasColors.primaryButton.copy(alpha = 0.08f)
            else MaterialTheme.wandasColors.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = categoryLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.wandasColors.primaryButton
                )
                Text(
                    text = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(thread.updatedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f)
                )
            }
            Text(
                text = thread.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.wandasColors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (thread.replies.isNotEmpty()) {
                Text(
                    text = "💬 ${thread.replies.size} reply",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.wandasColors.primaryButton
                )
            }
        }
    }
}
