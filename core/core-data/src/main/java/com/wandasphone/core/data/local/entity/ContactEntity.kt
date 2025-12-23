package com.wandasphone.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for contacts (internal to data layer)
 */
@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val photoUri: String?,
    val priority: Int,
    val isPrimary: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

