package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.components.CarerBreadcrumb
import com.tomsphone.feature.carer.support.SupportThread
import com.tomsphone.feature.carer.support.SupportThreadReply
import com.tomsphone.feature.carer.support.SupportSuggestionsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportThreadDetailScreen(
    threadId: String,
    onBack: () -> Unit,
    viewModel: SupportSuggestionsViewModel = hiltViewModel()
) {
    val thread by viewModel.selectedThread.collectAsState()
    var replyText by remember { mutableStateOf("") }

    LaunchedEffect(threadId) {
        viewModel.ensureDeviceId()
        viewModel.loadThread(threadId)
    }

    LaunchedEffect(thread?.replies?.size) {
        thread?.replies?.let { /* scroll to end could go here */ }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.wandasColors.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            CarerBreadcrumb(
                title = "Thread",
                parentTitle = "Support",
                onBack = onBack
            )
            when {
                thread == null && threadId.isNotBlank() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                thread == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Thread not found", color = MaterialTheme.wandasColors.onSurface)
                }
                else -> {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            OriginalMessageCard(thread!!)
                        }
                        items(thread!!.replies) { reply ->
                            ReplyBubble(reply = reply)
                        }
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 3.dp,
                        color = MaterialTheme.wandasColors.surface
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            OutlinedTextField(
                                value = replyText,
                                onValueChange = { replyText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Add a follow-up…") },
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 3
                            )
                            IconButton(
                                onClick = {
                                    if (replyText.isNotBlank()) {
                                        viewModel.addReply(threadId, replyText.trim())
                                        replyText = ""
                                    }
                                },
                                enabled = replyText.isNotBlank()
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = if (replyText.isNotBlank()) MaterialTheme.wandasColors.primaryButton else MaterialTheme.wandasColors.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OriginalMessageCard(thread: SupportThread) {
    val categoryLabel = when ((thread.category ?: "").lowercase()) {
        "support" -> "Support"
        "feature_suggestion" -> "Feature suggestion"
        else -> thread.category
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.wandasColors.surface.copy(alpha = 0.6f)),
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
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.wandasColors.primaryButton
                )
                Text(
                    text = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(thread.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f)
                )
            }
            Text(
                text = thread.body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.wandasColors.onSurface
            )
        }
    }
}

@Composable
private fun ReplyBubble(reply: SupportThreadReply) {
    val isAdmin = reply.isAdmin
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isAdmin) Arrangement.Start else Arrangement.End
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isAdmin) MaterialTheme.wandasColors.primaryButton.copy(alpha = 0.15f)
                else MaterialTheme.wandasColors.surface
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isAdmin) 4.dp else 16.dp,
                bottomEnd = if (isAdmin) 16.dp else 4.dp
            )
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isAdmin) {
                    Text(
                        text = "Support",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.wandasColors.primaryButton
                    )
                }
                Text(
                    text = reply.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.wandasColors.onSurface
                )
                Text(
                    text = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(reply.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
