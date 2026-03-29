package com.tomsphone.feature.carer.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.config.HomeSlotAssignments
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.model.ContactType
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.CarerSettingsViewModel
import com.tomsphone.feature.carer.components.CarerBreadcrumb
@Composable
fun HomeScreenLayoutScreen(
    onBack: () -> Unit,
    /** After assigning an existing contact to this slot, open their details (battery, auto-answer). */
    onNavigateToContactAfterSlot: (contactId: Long, contactType: ContactType) -> Unit = { _, _ -> },
    /** Open add-contact for this slot; slot is assigned after a successful save. */
    onNavigateToNewContactForSlot: (slotIndex: Int) -> Unit = {},
    viewModel: CarerSettingsViewModel = hiltViewModel()
) {
    val slots by viewModel.homeSlotAssignments.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    /** Any contact may be assigned a home slot (assistant or elevated answer-only). */
    val contactsForSlots = remember(contacts) {
        contacts.sortedWith(compareBy<Contact> { it.buttonPosition }.thenBy { it.name.lowercase() })
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
        value.isEmpty() -> "No button"
        HomeSlotAssignments.isContact(value) -> {
            val id = HomeSlotAssignments.parseContactId(value)
            if (id == null) "Contact"
            else contactsForSlots.find { it.id == id }?.name ?: "Contact (removed)"
        }
        value == HomeSlotAssignments.MISSED_CALL_RETURN -> "Missed call return"
        value == HomeSlotAssignments.MISSED_CALLS_LIST -> "Missed calls list"
        value == HomeSlotAssignments.OTHER_CONTACTS -> "Contacts"
        value == HomeSlotAssignments.DIALER -> "Dial"
        value == HomeSlotAssignments.SCREEN_OFF -> "Screen off"
        else -> "Unknown"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CarerBreadcrumb(
                title = "Home Screen Layout",
                parentTitle = "Settings",
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
                    text = "Slots 1–7: your buttons. Slot 8: Emergency (fixed).",
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
        val pickerScroll = rememberScrollState()
        AlertDialog(
            onDismissRequest = { slotIndexToEdit = null },
            // Slightly gentler radius so less content sits in the curved clip zone (glyphs were cropped).
            shape = RoundedCornerShape(20.dp),
            title = { Text("Slot ${index + 1}") },
            text = {
                // Special actions first so “Recent calls” / missed list isn’t buried below many assistants;
                // scroll so everything stays reachable on small screens.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        // Inset from dialog surface: rounded clip was cutting label/subtitle ascenders & line ends.
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .verticalScroll(pickerScroll),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SlotOption(
                        label = "No button",
                        subtitle = "Leave this row empty so other buttons can be larger with bigger text.",
                        value = HomeSlotAssignments.EMPTY,
                        current = if (index in list.indices) list[index] else HomeSlotAssignments.EMPTY,
                        onSelect = {
                            slotIndexToEdit = null
                            viewModel.setHomeSlotAt(index, HomeSlotAssignments.EMPTY)
                        }
                    )
                    SlotOption(
                        label = "Missed call return",
                        subtitle = "One tap to call back the latest missed answer-only contact",
                        value = HomeSlotAssignments.MISSED_CALL_RETURN,
                        current = if (index in list.indices) list[index] else HomeSlotAssignments.EMPTY,
                        onSelect = {
                            slotIndexToEdit = null
                            viewModel.setHomeSlotAt(index, HomeSlotAssignments.MISSED_CALL_RETURN)
                        }
                    )
                    SlotOption(
                        label = "Missed calls list",
                        subtitle = "Two taps: open list, then tap a row to call back. Shows missed and declined only.",
                        value = HomeSlotAssignments.MISSED_CALLS_LIST,
                        current = if (index in list.indices) list[index] else HomeSlotAssignments.EMPTY,
                        onSelect = {
                            slotIndexToEdit = null
                            viewModel.setHomeSlotAt(index, HomeSlotAssignments.MISSED_CALLS_LIST)
                        }
                    )
                    SlotOption(
                        label = "Contacts",
                        subtitle = "Opens on-phone list of everyone",
                        value = HomeSlotAssignments.OTHER_CONTACTS,
                        current = if (index in list.indices) list[index] else HomeSlotAssignments.EMPTY,
                        onSelect = {
                            slotIndexToEdit = null
                            viewModel.setHomeSlotAt(index, HomeSlotAssignments.OTHER_CONTACTS)
                        }
                    )
                    SlotOption(
                        label = "Dial",
                        subtitle = "Keypad to enter a number, then a large Call button",
                        value = HomeSlotAssignments.DIALER,
                        current = if (index in list.indices) list[index] else HomeSlotAssignments.EMPTY,
                        onSelect = {
                            slotIndexToEdit = null
                            viewModel.setHomeSlotAt(index, HomeSlotAssignments.DIALER)
                        }
                    )
                    SlotOption(
                        label = "Screen off",
                        subtitle = null,
                        value = HomeSlotAssignments.SCREEN_OFF,
                        current = if (index in list.indices) list[index] else HomeSlotAssignments.EMPTY,
                        onSelect = {
                            slotIndexToEdit = null
                            viewModel.setHomeSlotAt(index, HomeSlotAssignments.SCREEN_OFF)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Contacts (home call button)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    SlotOption(
                        label = "Add new contact…",
                        subtitle = "Save a valid name and number, then return here with the slot filled",
                        value = "__new_contact__",
                        current = if (index in list.indices) list[index] else HomeSlotAssignments.EMPTY,
                        onSelect = {
                            slotIndexToEdit = null
                            onNavigateToNewContactForSlot(index)
                        }
                    )
                    contactsForSlots.forEach { c ->
                        SlotOption(
                            label = c.name,
                            subtitle = "Set slot, then open details for battery & auto-answer",
                            value = HomeSlotAssignments.contactSlot(c.id),
                            current = if (index in list.indices) list[index] else HomeSlotAssignments.EMPTY,
                            onSelect = {
                                slotIndexToEdit = null
                                viewModel.setHomeSlotAt(index, HomeSlotAssignments.contactSlot(c.id))
                                onNavigateToContactAfterSlot(c.id, c.contactType)
                            }
                        )
                    }
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
    subtitle: String?,
    value: String,
    current: String,
    onSelect: () -> Unit
) {
    val selected = value == current
    // Extra padding so text doesn’t sit under the dialog’s rounded clip; room for ascenders/descenders.
    TextButton(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                val labelStyle = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = (MaterialTheme.typography.bodyLarge.fontSize.value * 1.35f).sp,
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.None
                    )
                )
                Text(
                    text = label,
                    style = labelStyle,
                    color = MaterialTheme.wandasColors.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    val subStyle = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = (MaterialTheme.typography.bodySmall.fontSize.value * 1.45f).sp,
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None
                        )
                    )
                    Text(
                        text = subtitle,
                        style = subStyle,
                        color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.65f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
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
