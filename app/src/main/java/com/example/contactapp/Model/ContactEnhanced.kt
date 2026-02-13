package com.example.contactapp.Model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Enhanced Contact Model with support for favorites, multiple accounts, and metadata
 */
@Parcelize
data class ContactEnhanced(
    val contactId: String,
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null,
    val email: String? = null,
    val additionalPhones: List<String> = emptyList(),
    val additionalEmails: List<String> = emptyList(),
    val accountType: AccountType = AccountType.PHONE,
    val accountName: String? = null,
    val isFavorite: Boolean = false,
    val lastContactedTime: Long = 0L,
    val timesContacted: Int = 0,
    val notes: String? = null
) : Parcelable {

    fun getSectionCharacter(): String {
        if (name.isEmpty()) return "#"
        
        val firstChar = name.first().uppercaseChar()
        return when {
            firstChar in 'A'..'Z' -> firstChar.toString()
            else -> "#" // Special characters and non-English
        }
    }

    fun getAllPhoneNumbers(): List<String> {
        return listOf(phoneNumber) + additionalPhones
    }

    fun getAllEmails(): List<String> {
        val emails = mutableListOf<String>()
        email?.let { emails.add(it) }
        emails.addAll(additionalEmails)
        return emails
    }

    fun calculateSimilarity(other: ContactEnhanced): Float {
        var score = 0f
        var factors = 0
        
        // Name similarity (most important)
        if (name.isNotEmpty() && other.name.isNotEmpty()) {
            val nameSimilarity = calculateStringSimilarity(
                name.lowercase(),
                other.name.lowercase()
            )
            score += nameSimilarity * 3 // Weight: 3
            factors += 3
        }
        
        // Phone number match (very important)
        val allPhones = getAllPhoneNumbers().map { normalizePhoneNumber(it) }
        val otherPhones = other.getAllPhoneNumbers().map { normalizePhoneNumber(it) }
        if (allPhones.any { it in otherPhones }) {
            score += 2 // Weight: 2
        }
        factors += 2
        
        // Email match
        val allEmails = getAllEmails().map { it.lowercase() }
        val otherEmails = other.getAllEmails().map { it.lowercase() }
        if (allEmails.any { it in otherEmails }) {
            score += 1 // Weight: 1
        }
        factors += 1
        
        return if (factors > 0) score / factors else 0f
    }

    private fun normalizePhoneNumber(phone: String): String {
        return phone.replace(Regex("[^0-9+]"), "")
    }

    private fun calculateStringSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        if (s1.isEmpty() || s2.isEmpty()) return 0.0f
        
        val maxLength = maxOf(s1.length, s2.length)
        val distance = levenshteinDistance(s1, s2)
        return 1.0f - (distance.toFloat() / maxLength)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[s1.length][s2.length]
    }
}


@Parcelize
enum class AccountType : Parcelable {
    PHONE,      // Device storage
    GOOGLE,     // Google account
    SIM,        // SIM card
    EXCHANGE,   // Exchange account
    OTHER       // Other account types
}


sealed class ContactListItemEnhanced {
    data class Header(val letter: String) : ContactListItemEnhanced()
    data class ContactItem(val contact: ContactEnhanced) : ContactListItemEnhanced()
}


data class DuplicateGroup(
    val contacts: List<ContactEnhanced>,
    val similarityScore: Float
) {
    val primaryContact: ContactEnhanced
        get() = contacts.maxByOrNull { it.timesContacted } ?: contacts.first()
}


data class SearchHistoryItem(
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)


data class SmartSuggestion(
    val contact: ContactEnhanced,
    val reason: SuggestionReason,
    val score: Float
)

enum class SuggestionReason {
    FREQUENTLY_CONTACTED,
    RECENTLY_CONTACTED,
    FAVORITE,
    SEARCH_HISTORY
}
