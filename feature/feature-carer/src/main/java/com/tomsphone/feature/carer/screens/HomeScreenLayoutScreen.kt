package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.config.HomeSlotAssignments
import com.tomsphone.core.data.model.ContactType
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.CarerSettingsViewModel
import com.tomsphone.feature.carer.components.CarerBreadcrumb
import com.tomsphone.feature.carer.components.DevLevelIndicator

@Composable
fun HomeScreenLayoutScreen(
    onBack: () -> Unit,
    viewModel: CarerSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val featureLevel = settings.featureLevel
    val slots by viewModel.homeSlotAssignments.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val assistants = remember(contacts) {
        contacts.filter { it.contactType == ContactType.CARER }.sortedBy { it.buttonPosition }
    }

    LaunchedEffect(Unit) {
        viewModel.ensureMigrationOnLayoutOpen()
    }

    var slotIndexToEdit by remember { mutableStateOf<Int?>(null) }
    val list = remember(slots) {
        slots.toMutableList().apply {
            while (size < HomeSlotAssignments.SLOT_COUNT) add(HomeSlotAssignments.EMPTY)
        }.take(HomeSlotAssignments.SLOT_COUNT) // Ensure exactly SLOT_COUNT items
    }

    fun slotLabel(value: String): String = when {
        value.isEmpty() -> "Empty"
        HomeSlotAssignments.isContact(value) -> {
            val id = HomeSlotAssignments.parseContactId(value)
            if (id == null) "Assistant"
            else assistants.find { it.id == id }?.name ?: "Assistant (deleted)"
        }
        value == HomeSlotAssignments.MISSED_CALL_RETURN -> "Missed Call Return"
        value == HomeSlotAssignments.MISSED_CALLS_LIST -> "Recent calls"
        value == HomeSlotAssignments.OTHER_CONTACTS -> "Other Contacts"
        value == HomeSlotAssignments.SCREEN_OFF -> "Screen off"
        else -> "Unknown"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DevLevelIndicator(level = featureLevel)
            CarerBreadcrumb(
                title = "Home screen layout",
                parentTitle = "Assistant Settings",
                onBack = onBack
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(WandasDimensions.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingSmall)
            ) {
                Text(
                    text = "Slots 1–7 are assignable. Slot 8 is always Emergency.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                list.forEachIndexed { index, value ->
                    SlotRow(
                        slotNumber = index + 1,
                        label = slotLabel(value),
                        isFirst = index == 0,
                        isLast = index == list.lastIndex,
                        onClick = { slotIndexToEdit = index },
                        onMoveUp = { viewModel.moveHomeSlotUp(index) },
                        onMoveDown = { viewModel.moveHomeSlotDown(index) }
                    )
                }
                // Slot 8 – Emergency (fixed)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.wandasColors.surface.copy(alpha = 0.7f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(WandasDimensions.SpacingMedium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "8",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.width(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Emergency (always on)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.wandasColors.onSurface
                        )
                    }
                }
            }
        }
    }

    slotIndexToEdit?.let { index ->
        AlertDialog(
            onDismissRequest = { slotIndexToEdit = null },
            title = { Text("Slot ${index + 1}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SlotOption(
                        label = "Empty",
                        value = HomeSlotAssignments.EMPTY,
                        current = if (index in list.indices) list[index] else HomeSlotAssignments.EMPTY,
                        onSelect = {
                            slotIndexToEdit = null
                            viewModel.setHomeSlotAt(index, HomeSlotAssignments.EMPTY)
                        }
                    )
                    assistants.forEach { c ->
                        SlotOption(
                            label = c.name,
                            value = HomeSlotAssignments.contactSlot(c.id),
                            current = if (index in list.indices) list[index] else HomeSlotAssignments.EMPTY,
                            onSelect = {
                                slotIndexToEdit = null
                                viewModel.setHomeSlotAt(index, HomeSlotAssignments.contactSlot(c.id))
                            }
                        )
                    }
                    SlotOption(
                        label = "Missed Call Return",
                        value = HomeSlotAssignments.MISSED_CALL_RETURN,
                        current = if (index in list.indices) list[index] else HomeSlotAssignments.EMPTY,
                        onSelect = {
                            slotIndexToEdit = null
                            viewModel.setHomeSlotAt(index, HomeSlotAssignments.MISSED_CALL_RETURN)
                        }
                    )
                    SlotOption(
                        label = "Missed Calls List",
                        value = HomeSlotAssignments.MISSED_CALLS_LIST,
                        current = if (index in list.indices) list[index] else HomeSlotAssignments.EMPTY,
                        onSelect = {
                            slotIndexToEdit = null
                            viewModel.setHomeSlotAt(index, HomeSlotAssignments.MISSED_CALLS_LIST)
                        }
                    )
                    SlotOption(
                        label = "Other Contacts",
                        value = HomeSlotAssignments.OTHER_CONTACTS,
                        current = if (index in list.indices) list[index] else HomeSlotAssignments.EMPTY,
                        onSelect = {
                            slotIndexToEdit = null
                            viewModel.setHomeSlotAt(index, HomeSlotAssignments.OTHER_CONTACTS)
                        }
                    )
                    SlotOption(
                        label = "Screen off",
                        value = HomeSlotAssignments.SCREEN_OFF,
                        current = if (index in list.indices) list[index] else HomeSlotAssignments.EMPTY,
                        onSelect = {
                            slotIndexToEdit = null
                            viewModel.setHomeSlotAt(index, HomeSlotAssignments.SCREEN_OFF)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { slotIndexToEdit = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SlotRow(
    slotNumber: Int,
    label: String,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
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
            Text(
                text = "$slotNumber",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.width(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.wandasColors.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SlotOption(
    label: String,
    value: String,
    current: String,
    onSelect: () -> Unit
) {
    val selected = value == current
    TextButton(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.wandasColors.onSurface
            )
            if (selected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.wandasColors.primaryButton
                )
            }
        }
    }
}
