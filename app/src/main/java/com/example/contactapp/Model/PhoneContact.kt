package com.example.contactapp.Model

import android.net.Uri

data class PhoneContact(
    val contactId: String,
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null,
    val email: String? = null,
    val additionalPhones: List<String> = emptyList(),
    val sectionHeader: String = "" // For alphabetic grouping
) {
    /**
     * Get the first character for section headers
     * Returns "#" for special characters and non-English names
     */
    fun getSectionCharacter(): String {
        if (name.isEmpty()) return "#"
        
        val firstChar = name.first().uppercaseChar()
        return when {
            firstChar in 'A'..'Z' -> firstChar.toString()
            else -> "#" // Special characters and non-English
        }
    }
    
    /**
     * Check if this is a section header item
     */
    val isHeader: Boolean
        get() = sectionHeader.isNotEmpty()
}

/**
 * Wrapper class to support both contact items and section headers
 */
sealed class ContactListItem {
    data class Header(val letter: String) : ContactListItem()
    data class ContactItem(val contact: PhoneContact) : ContactListItem()
}
