package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
 * Contacts list screen.
 * 
 * Shows all contacts with ability to:
 * - View/edit existing contacts
 * - Add new contacts
 * - Set primary contact
 */
@Composable
fun ContactsScreen(
    featureLevel: FeatureLevel,
    onNavigateToContactEdit: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: CarerSettingsViewModel = hiltViewModel()
) {
    val contacts by viewModel.contacts.collectAsState()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Dev level indicator
            DevLevelIndicator(level = featureLevel)
            
            // Breadcrumb
            CarerBreadcrumb(
                title = "Contacts",
                parentTitle = "Settings",
                onBack = onBack
            )
            
            // Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WandasDimensions.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingSmall)
            ) {
                // Carers section
                item {
                    Text(
                        text = "Carers",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.wandasColors.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                val carers = contacts.filter { it.contactType == ContactType.CARER }
                items(carers, key = { it.id }) { contact ->
                    ContactListItem(
                        contact = contact,
                        onClick = { onNavigateToContactEdit(contact.id) }
                    )
                }
                
                // Grey List section (Level 2+)
                if (featureLevel.level >= 2) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Grey List",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.wandasColors.onSurface,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        Text(
                            text = "Calls allowed but no missed call reminders",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    
                    val greyList = contacts.filter { it.contactType == ContactType.GREY_LIST }
                    items(greyList, key = { it.id }) { contact ->
                        ContactListItem(
                            contact = contact,
                            onClick = { onNavigateToContactEdit(contact.id) }
                        )
                    }
                }
                
                // Add contact button
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { onNavigateToContactEdit(0) },  // 0 = new contact
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text("Add Contact")
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
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
                    
                    if (contact.isPrimary) {
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.wandasColors.primaryButton.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "PRIMARY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.wandasColors.primaryButton,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
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
