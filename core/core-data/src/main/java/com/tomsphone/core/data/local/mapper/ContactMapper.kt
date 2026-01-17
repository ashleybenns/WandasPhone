package com.tomsphone.core.data.local.mapper

import com.tomsphone.core.data.local.entity.ContactEntity
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.model.ContactType

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
        contactType = try {
            ContactType.valueOf(contactType)
        } catch (e: IllegalArgumentException) {
            ContactType.GREY_LIST  // Default to grey list for safety
        },
        createdAt = createdAt,
        updatedAt = updatedAt,
        // Button configuration
        buttonColor = buttonColor,
        autoAnswerEnabled = autoAnswerEnabled,
        buttonPosition = buttonPosition,
        isHalfWidth = isHalfWidth
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
        contactType = contactType.name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        // Button configuration
        buttonColor = buttonColor,
        autoAnswerEnabled = autoAnswerEnabled,
        buttonPosition = buttonPosition,
        isHalfWidth = isHalfWidth
    )
}
