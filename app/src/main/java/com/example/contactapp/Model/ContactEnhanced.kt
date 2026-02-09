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
     * Get all phone numbers (primary + additional)
     */
    fun getAllPhoneNumbers(): List<String> {
        return listOf(phoneNumber) + additionalPhones
    }
    
    /**
     * Get all emails (primary + additional)
     */
    fun getAllEmails(): List<String> {
        val emails = mutableListOf<String>()
        email?.let { emails.add(it) }
        emails.addAll(additionalEmails)
        return emails
    }
    
    /**
     * Calculate similarity score with another contact for duplicate detection
     * Returns a score from 0.0 (completely different) to 1.0 (identical)
     */
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
    
    /**
     * Normalize phone number for comparison (remove spaces, dashes, etc.)
     */
    private fun normalizePhoneNumber(phone: String): String {
        return phone.replace(Regex("[^0-9+]"), "")
    }
    
    /**
     * Calculate string similarity using Levenshtein distance
     */
    private fun calculateStringSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        if (s1.isEmpty() || s2.isEmpty()) return 0.0f
        
        val maxLength = maxOf(s1.length, s2.length)
        val distance = levenshteinDistance(s1, s2)
        return 1.0f - (distance.toFloat() / maxLength)
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     */
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

/**
 * Account types for contacts
 */
@Parcelize
enum class AccountType : Parcelable {
    PHONE,      // Device storage
    GOOGLE,     // Google account
    SIM,        // SIM card
    EXCHANGE,   // Exchange account
    OTHER       // Other account types
}

/**
 * Wrapper class to support both contact items and section headers
 */
sealed class ContactListItemEnhanced {
    data class Header(val letter: String) : ContactListItemEnhanced()
    data class ContactItem(val contact: ContactEnhanced) : ContactListItemEnhanced()
}

/**
 * Duplicate contact group
 */
data class DuplicateGroup(
    val contacts: List<ContactEnhanced>,
    val similarityScore: Float
) {
    val primaryContact: ContactEnhanced
        get() = contacts.maxByOrNull { it.timesContacted } ?: contacts.first()
}

/**
 * Search history item
 */
data class SearchHistoryItem(
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Smart suggestion based on user behavior
 */
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
