package com.wandasphone.core.data.model

/**
 * Domain model for a contact
 */
data class Contact(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val photoUri: String?,
    val priority: Int,
    val isPrimary: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

