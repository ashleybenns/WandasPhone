package com.tomsphone.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for contacts (internal to data layer)
 * 
 * Each field is a separate DB column for:
 * - Individual remote sync
 * - Subscription tier gating
 * - Granular carer configuration
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
    val contactType: String,  // "CARER" or "GREY_LIST"
    val createdAt: Long,
    val updatedAt: Long,
    
    // ========== BUTTON CONFIGURATION ==========
    // Each is a separate column for individual addressability
    
    /** Button background color (ARGB), null = use theme default */
    val buttonColor: Long? = null,
    
    /** Whether this contact has auto-answer enabled */
    val autoAnswerEnabled: Boolean = false,
    
    /** Position on home screen (0 = top, higher = lower) */
    val buttonPosition: Int = 0,
    
    /** Whether button should be half-width (for split layouts) */
    val isHalfWidth: Boolean = false
)
