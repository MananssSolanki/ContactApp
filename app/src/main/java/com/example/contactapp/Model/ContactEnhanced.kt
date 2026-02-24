package com.example.contactapp.Model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContactEnhanced(
    val contactId: String,
    val rawContactId: String? = null,
    // Name
    val name: String,
    val namePrefix: String? = null,
    val firstName: String? = null,
    val middleName: String? = null,
    val lastName: String? = null,
    val nameSuffix: String? = null,
    val phoneticName: String? = null,
    val nickname: String? = null,
    // Contact data
    val phones: List<PhoneEntry> = emptyList(),
    val emails: List<EmailEntry> = emptyList(),
    val photoUri: String? = null,
    // Work
    val jobTitle: String? = null,
    val department: String? = null,
    val company: String? = null,
    // Other
    val addresses: List<AddressEntry> = emptyList(),
    val importantDates: List<DateEntry> = emptyList(),
    val relationships: List<RelationshipEntry> = emptyList(),
    val notes: String? = null,
    val websites: List<String> = emptyList(),
    val imAccounts: List<ImEntry> = emptyList(),
    // Meta
    val isFavorite: Boolean = false,
    val lastContactedTime: Long = 0L,
    val timesContacted: Int = 0,
    val accountType: AccountType = AccountType.PHONE,
    val accountName: String? = null,
    // Legacy compat fields (used by existing adapters/viewmodels)
    val phoneNumber: String = "",
    val email: String? = null,
    val additionalPhones: List<String> = emptyList(),
    val additionalEmails: List<String> = emptyList()
) : Parcelable {

    fun getSectionCharacter(): String {
        if (name.isEmpty()) return "#"
        val firstChar = name.first().uppercaseChar()
        return if (firstChar in 'A'..'Z') firstChar.toString() else "#"
    }

    fun getAllPhoneNumbers(): List<String> = phones.map { it.number }
    fun getAllEmails(): List<String> = emails.map { it.address }
}

@Parcelize
data class PhoneEntry(
    val number: String,
    val typeLabel: String = "Mobile",
    val customLabel: String = ""
) : Parcelable

@Parcelize
data class EmailEntry(
    val address: String,
    val typeLabel: String = "Home",
    val customLabel: String = ""
) : Parcelable

@Parcelize
data class AddressEntry(
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val postcode: String = "",
    val country: String = "",
    val typeLabel: String = "Home"
) : Parcelable

@Parcelize
data class DateEntry(
    val year: Int = 0,
    val month: Int = 0,
    val day: Int = 0,
    val typeLabel: String = "Birthday"
) : Parcelable

@Parcelize
data class RelationshipEntry(
    val name: String,
    val typeLabel: String = "Parent"
) : Parcelable

@Parcelize
data class ImEntry(
    val account: String,
    val protocol: String = "Hangouts"
) : Parcelable

@Parcelize
enum class AccountType : Parcelable {
    PHONE, GOOGLE, SIM, EXCHANGE, OTHER
}

sealed class ContactListItemEnhanced {
    data class Header(val title: String, val isFavorite: Boolean = false) : ContactListItemEnhanced()
    data object GroupsItem : ContactListItemEnhanced()
    data class ContactItem(val contact: ContactEnhanced) : ContactListItemEnhanced()
}

data class DuplicateGroup(
    val contacts: List<ContactEnhanced>,
    val similarityScore: Float
) {
    val primaryContact: ContactEnhanced
        get() = contacts.maxByOrNull { it.timesContacted } ?: contacts.first()
}

data class SearchHistoryItem(val query: String, val timestamp: Long = System.currentTimeMillis())

data class SmartSuggestion(val contact: ContactEnhanced, val reason: SuggestionReason, val score: Float)

enum class SuggestionReason {
    FREQUENTLY_CONTACTED, RECENTLY_CONTACTED, FAVORITE, SEARCH_HISTORY
}