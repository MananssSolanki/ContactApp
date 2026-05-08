package com.example.contactapp.Model

// ─── Native SyncAdapter entity for soft-deleted contacts ──────────────────────────────────

data class DeletedContact(
    val id: Long = 0,
    val contactId: String,          // The RawContact ID
    val name: String,
    val phoneNumber: String,
    val email: String?,
    val photoUri: String?,
    val accountName: String? = null,
    val accountType: String? = null,
    val photoBytesBase64: String? = null,
    val deletedAt: Long = System.currentTimeMillis(),
    val vCardData: String = ""
)

// ─── Merge duplicate group ───────────────────────────────────────────────────

data class MergeDuplicateGroup(
    val displayKey: String,         // phone number / email / name (lowercase)
    val contacts: List<ContactEnhanced>
)