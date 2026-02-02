package com.tomsphone.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.ui.theme.ScaledDimensions
import com.tomsphone.core.ui.theme.wandasColors
import android.util.Log

// #region agent log helper
private fun debugLog(location: String, hypothesisId: String, message: String, data: Map<String, Any?> = emptyMap()) {
    Log.d("DEBUG_NAV", "[$hypothesisId] $location: $message | $data")
}
// #endregion

/**
 * Missed Calls List Screen (Level 2+)
 * 
 * Shows list of missed calls for the user to return.
 * Tap a contact to call them back.
 * 
 * Design: Large touch targets, simple list, prominent names.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissedCallsListScreen(
    onBack: () -> Unit,
    onCallContact: (String, String) -> Unit, // (name, phoneNumber)
    viewModel: MissedCallsListViewModel = hiltViewModel()
) {
    val missedCalls by viewModel.missedCalls.collectAsState()
    val userName by viewModel.userName.collectAsState()
    
    // #region agent log
    LaunchedEffect(Unit) { debugLog("MissedCallsListScreen.kt:40", "H5", "MissedCallsListScreen composed", mapOf("missedCallsCount" to missedCalls.size)) }
    DisposableEffect(Unit) {
        onDispose { debugLog("MissedCallsListScreen.kt:42", "H3", "MissedCallsListScreen disposed", emptyMap()) }
    }
    // #endregion
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar with back button
            TopAppBar(
                title = {
                    Text(
                        text = "Missed Calls",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.wandasColors.surface
                )
            )
            
            if (missedCalls.isEmpty()) {
                // No missed calls message
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No missed calls",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                // Missed calls list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(missedCalls) { call ->
                        MissedCallItem(
                            contactName = call.contactName ?: call.phoneNumber,
                            phoneNumber = call.phoneNumber,
                            onClick = {
                                onCallContact(call.contactName ?: call.phoneNumber, call.phoneNumber)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MissedCallItem(
    contactName: String,
    phoneNumber: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.wandasColors.primaryButton,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contactName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.wandasColors.onPrimaryButton
            )
        }
    }
}
