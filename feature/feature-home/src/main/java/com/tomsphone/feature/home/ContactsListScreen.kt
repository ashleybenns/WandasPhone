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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.ui.theme.wandasColors

/**
 * Contacts List Screen (Level 2+)
 * 
 * Shows list of all carer contacts for the user to call.
 * Tap a contact to call them.
 * 
 * Design: Large touch targets, simple list, prominent names.
 * This allows calling carers that don't fit on the home screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsListScreen(
    onBack: () -> Unit,
    onCallContact: (String, String) -> Unit, // (name, phoneNumber)
    viewModel: ContactsListViewModel = hiltViewModel()
) {
    val contacts by viewModel.contacts.collectAsState()
    
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
                        text = "Contacts",
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
            
            if (contacts.isEmpty()) {
                // No contacts message
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No contacts",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                // Contacts list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(contacts) { contact ->
                        ContactListItem(
                            contact = contact,
                            onClick = {
                                onCallContact(contact.name, contact.phoneNumber)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactListItem(
    contact: Contact,
    onClick: () -> Unit
) {
    val buttonColor = contact.buttonColor?.let { Color(it) }
        ?: MaterialTheme.wandasColors.primaryButton
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = buttonColor,
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
                text = contact.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.wandasColors.onPrimaryButton
            )
        }
    }
}
