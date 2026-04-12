package com.tomsphone.feature.carer.transfer

import com.tomsphone.core.config.CarerSettings
import com.tomsphone.core.config.HomeSlotAssignments
import com.tomsphone.core.config.withSlotsSynced
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.model.ContactType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * On-device transfer file: contacts plus carer settings. Contact IDs in the file are **export IDs**
 * (1…N) so the same file can be imported on another device with new Room primary keys.
 */
object AppDataTransfer {

    const val FORMAT_VERSION: Int = 1
    const val FILE_NAME_PREFIX: String = "tomsphone-transfer"

    @Serializable
    data class TomPhoneTransferV1(
        val formatVersion: Int = FORMAT_VERSION,
        val exportedAtMillis: Long,
        val contacts: List<ExportedContactV1>,
        val settings: CarerSettings,
    )

    @Serializable
    data class ExportedContactV1(
        val exportId: Long,
        val name: String,
        val phoneNumber: String,
        val photoUri: String? = null,
        val priority: Int = 0,
        val contactType: String,
        val createdAt: Long,
        val updatedAt: Long,
        val buttonColor: Long? = null,
        val autoAnswerEnabled: Boolean = false,
        val notifyBatteryAlerts: Boolean = false,
        val buttonPosition: Int = 0,
        val isHalfWidth: Boolean = false,
    )

    fun exportJson(contacts: List<Contact>, settings: CarerSettings, json: Json): String {
        val sorted = contacts.sortedBy { it.id }
        val localToExport = sorted.mapIndexed { index, c -> c.id to (index + 1).toLong() }.toMap()
        val exported = sorted.map { c ->
            val eid = localToExport.getValue(c.id)
            ExportedContactV1(
                exportId = eid,
                name = c.name,
                phoneNumber = c.phoneNumber,
                photoUri = c.photoUri,
                priority = c.priority,
                contactType = c.contactType.name,
                createdAt = c.createdAt,
                updatedAt = c.updatedAt,
                buttonColor = c.buttonColor,
                autoAnswerEnabled = c.autoAnswerEnabled,
                notifyBatteryAlerts = c.notifyBatteryAlerts,
                buttonPosition = c.buttonPosition,
                isHalfWidth = c.isHalfWidth,
            )
        }
        fun remapSlot(slot: String): String {
            val lid = HomeSlotAssignments.parseContactId(slot) ?: return slot
            val eid = localToExport[lid] ?: return HomeSlotAssignments.EMPTY
            return HomeSlotAssignments.contactSlot(eid)
        }
        val newSlots = settings.homeSlotAssignments.map { remapSlot(it) }
        val newAutoAnswer = settings.autoAnswerContacts.mapNotNull { localToExport[it] }.toSet()
        val remapped = settings.copy(
            homeSlotAssignments = newSlots,
            autoAnswerContacts = newAutoAnswer,
        ).withSlotsSynced(newSlots)
        val payload = TomPhoneTransferV1(
            formatVersion = FORMAT_VERSION,
            exportedAtMillis = System.currentTimeMillis(),
            contacts = exported,
            settings = remapped,
        )
        return json.encodeToString(TomPhoneTransferV1.serializer(), payload)
    }

    fun parseTransfer(jsonString: String, json: Json): Result<TomPhoneTransferV1> = runCatching {
        val payload = json.decodeFromString(TomPhoneTransferV1.serializer(), jsonString)
        validatePayload(payload)
        payload
    }

    private fun validatePayload(payload: TomPhoneTransferV1) {
        require(payload.formatVersion == FORMAT_VERSION) { "This file is not a supported transfer backup." }
        val ids = payload.contacts.map { it.exportId }
        require(ids.size == ids.toSet().size) { "Transfer file is damaged (duplicate IDs)." }
        require(ids.all { it > 0 }) { "Transfer file is damaged (invalid IDs)." }
        val idSet = ids.toSet()
        for (slot in payload.settings.homeSlotAssignments) {
            val cid = HomeSlotAssignments.parseContactId(slot) ?: continue
            require(cid in idSet) { "Transfer file references a missing contact in home layout." }
        }
        for (aid in payload.settings.autoAnswerContacts) {
            require(aid in idSet) { "Transfer file references a missing contact for auto-answer." }
        }
    }

    fun remapImportedSettings(
        fileSettings: CarerSettings,
        exportIdToLocalId: Map<Long, Long>,
    ): CarerSettings {
        fun remapSlot(slot: String): String {
            val eid = HomeSlotAssignments.parseContactId(slot) ?: return slot
            val lid = exportIdToLocalId[eid] ?: return HomeSlotAssignments.EMPTY
            return HomeSlotAssignments.contactSlot(lid)
        }
        val slots = fileSettings.homeSlotAssignments.map { remapSlot(it) }
        val autoAnswer = fileSettings.autoAnswerContacts.mapNotNull { exportIdToLocalId[it] }.toSet()
        return fileSettings.copy(
            homeSlotAssignments = slots,
            autoAnswerContacts = autoAnswer,
        ).withSlotsSynced(slots)
    }
}

fun AppDataTransfer.ExportedContactV1.toNewContact(): Contact {
    val type = try {
        ContactType.valueOf(contactType)
    } catch (_: IllegalArgumentException) {
        ContactType.GREY_LIST
    }
    return Contact(
        id = 0L,
        name = name,
        phoneNumber = phoneNumber,
        photoUri = photoUri,
        priority = priority,
        contactType = type,
        createdAt = createdAt,
        updatedAt = updatedAt,
        buttonColor = buttonColor,
        autoAnswerEnabled = autoAnswerEnabled,
        notifyBatteryAlerts = notifyBatteryAlerts,
        buttonPosition = buttonPosition,
        isHalfWidth = isHalfWidth,
    )
}
