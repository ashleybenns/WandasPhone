package com.wandasphone.core.data.local.mapper

import com.wandasphone.core.data.local.entity.ContactEntity
import com.wandasphone.core.data.model.Contact

/**
 * Mapper between Contact domain model and ContactEntity
 */

fun ContactEntity.toContact(): Contact {
    return Contact(
        id = id,
        name = name,
        phoneNumber = phoneNumber,
        photoUri = photoUri,
        priority = priority,
        isPrimary = isPrimary,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Contact.toEntity(): ContactEntity {
    return ContactEntity(
        id = id,
        name = name,
        phoneNumber = phoneNumber,
        photoUri = photoUri,
        priority = priority,
        isPrimary = isPrimary,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

