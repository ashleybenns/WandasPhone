package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.telecom.contactIdsWithHomeCallButton
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.model.ContactType
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.CarerSettingsViewModel
import com.tomsphone.feature.carer.components.*

/**
 * Single list of all contacts. Assistant vs normal is determined by **Home screen layout**:
 * assign a slot to give a home call button and assistant features (nag, auto-answer options, battery SMS).
 */
@Composable
fun ContactsScreen(
    onNavigateToContactEdit: (Long, ContactType) -> Unit,
    onBack: () -> Unit,
    /** When true, breadcrumb shows parent "Assistants" (opened from hub). */
    openedFromAssistantsHub: Boolean = false,
    viewModel: CarerSettingsViewModel = hiltViewModel()
) {
    val contacts by viewModel.contacts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val onHomeIds = remember(settings, contacts) {
        contactIdsWithHomeCallButton(settings)
    }

    val sortedAll = remember(contacts) {
        contacts.sortedWith(compareBy<Contact> { it.buttonPosition }.thenBy { it.name.lowercase() })
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            CarerBreadcrumb(
                title = if (openedFromAssistantsHub) "All contacts" else "Contacts",
                parentTitle = if (openedFromAssistantsHub) "Contacts" else "Settings",
                onBack = onBack
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WandasDimensions.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingSmall)
            ) {
                item {
                    Text(
                        text = "One list for everyone",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.wandasColors.onSurface
                    )
                    Text(
                        text = "Home Screen Layout assigns call buttons. Others can still call in and show in the on-phone list.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.65f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                }

                itemsIndexed(sortedAll, key = { _, c -> c.id }) { index, contact ->
                    ContactListItem(
                        contact = contact,
                        hasHomeButton = contact.id in onHomeIds,
                        onClick = { onNavigateToContactEdit(contact.id, contact.contactType) },
                        showReorderButtons = true,
                        isFirst = index == 0,
                        isLast = index == sortedAll.lastIndex,
                        onMoveUp = { viewModel.moveContactUp(contact, sortedAll) },
                        onMoveDown = { viewModel.moveContactDown(contact, sortedAll) }
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
                        Text("Add contact")
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
    hasHomeButton: Boolean,
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
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.wandasColors.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (hasHomeButton || contact.autoAnswerEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasHomeButton) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.wandasColors.primaryButton.copy(alpha = 0.28f)
                            ) {
                                Text(
                                    text = "Home button",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.wandasColors.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (contact.autoAnswerEnabled) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.wandasColors.warning.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "AUTO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.wandasColors.warning,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
