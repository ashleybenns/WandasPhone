package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.config.FeatureLevel
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.model.ContactType
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.CarerSettingsViewModel
import com.tomsphone.feature.carer.components.*

/**
 * Contacts list screen — shows one category (Assistants or Friends) or both.
 *
 * @param contactTypeFilter When set, show only that type (Assistants or Friends). When null, show both (legacy).
 * @param onNavigateToContactEdit (contactId, contactType) — contactId 0 for new, type for new contacts
 */
@Composable
fun ContactsScreen(
    contactTypeFilter: ContactType? = null,
    onNavigateToContactEdit: (Long, ContactType) -> Unit,
    onBack: () -> Unit,
    viewModel: CarerSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val featureLevel = settings.featureLevel
    val contacts by viewModel.contacts.collectAsState()

    val showAssistants = contactTypeFilter == null || contactTypeFilter == ContactType.CARER
    val showFriends = contactTypeFilter == null || contactTypeFilter == ContactType.GREY_LIST
    val breadcrumbTitle = when (contactTypeFilter) {
        ContactType.CARER -> "Assistants"
        ContactType.GREY_LIST -> "Friends"
        null -> "Contacts"
    }

    val maxAssistants = when (featureLevel) {
        FeatureLevel.MINIMAL -> 4
        FeatureLevel.BASIC -> 5
    }
    val assistantCount = contacts.count { it.contactType == ContactType.CARER }
    val canAddMoreAssistants = assistantCount < maxAssistants

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            DevLevelIndicator(level = featureLevel)
            CarerBreadcrumb(
                title = breadcrumbTitle,
                parentTitle = "Assistant Settings",
                onBack = onBack
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WandasDimensions.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingSmall)
            ) {
                // ========== ASSISTANTS ==========
                if (showAssistants) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Assistants",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.wandasColors.onSurface
                            )
                            if (maxAssistants != Int.MAX_VALUE) {
                                Text(
                                    text = "$assistantCount / $maxAssistants",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (canAddMoreAssistants)
                                        MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f)
                                    else
                                        MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    val assistants = contacts.filter { it.contactType == ContactType.CARER }
                        .sortedBy { it.buttonPosition }
                    items(assistants, key = { it.id }) { contact ->
                        val index = assistants.indexOf(contact)
                        ContactListItem(
                            contact = contact,
                            onClick = { onNavigateToContactEdit(contact.id, contact.contactType) },
                            showReorderButtons = true,
                            isFirst = index == 0,
                            isLast = index == assistants.size - 1,
                            onMoveUp = { viewModel.moveContactUp(contact, assistants) },
                            onMoveDown = { viewModel.moveContactDown(contact, assistants) }
                        )
                    }

                    item {
                        OutlinedButton(
                            onClick = { onNavigateToContactEdit(0, ContactType.CARER) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canAddMoreAssistants
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (canAddMoreAssistants) "Add Assistant" else "$maxAssistants assistants is enough for this mode")
                        }
                    }
                }

                // ========== FRIENDS ==========
                if (showFriends) {
                    item {
                        Spacer(modifier = Modifier.height(if (showAssistants) 24.dp else 0.dp))
                        Text(
                            text = "Friends",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.wandasColors.onSurface
                        )
                        val friendsDescription = if (featureLevel.level >= 2) {
                            "Can answer calls but no home screen button. At Level 2+, use 'Other Contacts' and 'Missed Calls' in Appearance to allow calling back."
                        } else {
                            "Can answer calls only. No home screen button, no way to call back."
                        }
                        Text(
                            text = friendsDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    val friends = contacts.filter { it.contactType == ContactType.GREY_LIST }
                        .sortedBy { it.buttonPosition }
                    items(friends, key = { it.id }) { contact ->
                        val index = friends.indexOf(contact)
                        ContactListItem(
                            contact = contact,
                            onClick = { onNavigateToContactEdit(contact.id, contact.contactType) },
                            showReorderButtons = true,
                            isFirst = index == 0,
                            isLast = index == friends.size - 1,
                            onMoveUp = { viewModel.moveContactUp(contact, friends) },
                            onMoveDown = { viewModel.moveContactDown(contact, friends) }
                        )
                    }

                    item {
                        OutlinedButton(
                            onClick = { onNavigateToContactEdit(0, ContactType.GREY_LIST) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add to Friends")
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun ContactListItem(
    contact: Contact,
    onClick: () -> Unit,
    showReorderButtons: Boolean = false,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.wandasColors.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WandasDimensions.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reorder buttons (left side)
            if (showReorderButtons) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = !isFirst,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move up",
                            tint = if (isFirst) 
                                MaterialTheme.wandasColors.onSurface.copy(alpha = 0.2f)
                            else 
                                MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = !isLast,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move down",
                            tint = if (isLast) 
                                MaterialTheme.wandasColors.onSurface.copy(alpha = 0.2f)
                            else 
                                MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            // Avatar placeholder
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = contact.buttonColor?.let { 
                    androidx.compose.ui.graphics.Color(it) 
                } ?: MaterialTheme.wandasColors.primaryButton.copy(alpha = 0.2f)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    tint = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.5f)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.wandasColors.onSurface
                    )
                    
                    if (contact.autoAnswerEnabled) {
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.wandasColors.warning.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "AUTO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.wandasColors.warning,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
