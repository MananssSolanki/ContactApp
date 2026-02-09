package com.example.contactapp.Repository

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.provider.ContactsContract
import com.example.contactapp.Model.*
import com.example.contactapp.Utils.ContactCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Enhanced Contact Repository with advanced features:
 * - Multi-account support (Phone, Google, SIM)
 * - Favorites management
 * - Duplicate detection
 * - Search history
 * - Smart suggestions
 * - Performance optimization with caching
 */
class ContactRepositoryEnhanced(private val context: Context) {
    
    private val cache = ContactCache.getInstance()
    private val prefs: SharedPreferences = context.getSharedPreferences("contacts_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val PREF_FAVORITES = "favorites"
        private const val PREF_SEARCH_HISTORY = "search_history"
        private const val MAX_SEARCH_HISTORY = 20
        private const val DUPLICATE_THRESHOLD = 0.7f // 70% similarity
    }
    
    /**
     * Get all contacts with complete details and caching
     */
    suspend fun getContacts(forceRefresh: Boolean = false): List<ContactEnhanced> = withContext(Dispatchers.IO) {
        // Return cached data if valid and not forcing refresh
        if (!forceRefresh && cache.isCacheValid()) {
            val cached = cache.getAllContacts()
            if (cached.isNotEmpty()) return@withContext cached
        }
        
        val contactsMap = mutableMapOf<String, ContactEnhanced>()
        val favorites = getFavoriteIds()
        
        // Query contacts with all details
        val cursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.Data.DISPLAY_NAME,
                ContactsContract.Data.MIMETYPE,
                ContactsContract.Data.DATA1,
                ContactsContract.Data.DATA2,
                ContactsContract.Data.PHOTO_URI,
                ContactsContract.Data.TIMES_CONTACTED,
                ContactsContract.Data.LAST_TIME_CONTACTED
            ),
            "${ContactsContract.Data.MIMETYPE} IN (?, ?)",
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
            ),
            ContactsContract.Data.DISPLAY_NAME + " ASC"
        )
        
        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.Data.CONTACT_ID)
            val nameIndex = it.getColumnIndex(ContactsContract.Data.DISPLAY_NAME)
            val mimeIndex = it.getColumnIndex(ContactsContract.Data.MIMETYPE)
            val data1Index = it.getColumnIndex(ContactsContract.Data.DATA1)
            val data2Index = it.getColumnIndex(ContactsContract.Data.DATA2)
            val photoIndex = it.getColumnIndex(ContactsContract.Data.PHOTO_URI)
            val timesContactedIndex = it.getColumnIndex(ContactsContract.Data.TIMES_CONTACTED)
            val lastContactedIndex = it.getColumnIndex(ContactsContract.Data.LAST_TIME_CONTACTED)
            
            while (it.moveToNext()) {
                val contactId = if (idIndex != -1) it.getString(idIndex) else continue
                val name = if (nameIndex != -1) it.getString(nameIndex) ?: "Unknown" else "Unknown"
                val mimeType = if (mimeIndex != -1) it.getString(mimeIndex) else continue
                val data1 = if (data1Index != -1) it.getString(data1Index) else null
                val photoUri = if (photoIndex != -1) it.getString(photoIndex) else null
                val timesContacted = if (timesContactedIndex != -1) it.getInt(timesContactedIndex) else 0
                val lastContacted = if (lastContactedIndex != -1) it.getLong(lastContactedIndex) else 0L
                
                // Get or create contact
                val contact = contactsMap.getOrPut(contactId) {
                    ContactEnhanced(
                        contactId = contactId,
                        name = name,
                        phoneNumber = "",
                        photoUri = photoUri,
                        isFavorite = contactId in favorites,
                        timesContacted = timesContacted,
                        lastContactedTime = lastContacted,
                        accountType = getAccountType(contactId)
                    )
                }
                
                // Add phone or email based on MIME type
                when (mimeType) {
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                        data1?.let { phone ->
                            if (contact.phoneNumber.isEmpty()) {
                                contactsMap[contactId] = contact.copy(phoneNumber = phone)
                            } else {
                                contactsMap[contactId] = contact.copy(
                                    additionalPhones = contact.additionalPhones + phone
                                )
                            }
                        }
                    }
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                        data1?.let { email ->
                            if (contact.email == null) {
                                contactsMap[contactId] = contact.copy(email = email)
                            } else {
                                contactsMap[contactId] = contact.copy(
                                    additionalEmails = contact.additionalEmails + email
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Filter out contacts without phone numbers
        val contacts = contactsMap.values.filter { it.phoneNumber.isNotEmpty() }
        
        // Update cache
        cache.updateContacts(contacts)
        
        contacts
    }
    
    /**
     * Get contacts sorted alphabetically with section headers
     */
    suspend fun getContactsSortedAlphabetically(forceRefresh: Boolean = false): List<ContactListItemEnhanced> = withContext(Dispatchers.IO) {
        val contacts = getContacts(forceRefresh)
        val sortedContacts = contacts.sortedWith(
            compareByDescending<ContactEnhanced> { it.isFavorite }
                .thenBy { it.name.uppercase() }
        )
        
        val result = mutableListOf<ContactListItemEnhanced>()
        var currentSection = ""
        var addedFavoriteHeader = false
        
        sortedContacts.forEach { contact ->
            // Add favorite header if needed
            if (contact.isFavorite && !addedFavoriteHeader) {
                result.add(ContactListItemEnhanced.Header("★ Favorites"))
                addedFavoriteHeader = true
                currentSection = "★"
            }
            
            // Add alphabetic section header
            if (!contact.isFavorite) {
                val section = contact.getSectionCharacter()
                if (section != currentSection) {
                    currentSection = section
                    result.add(ContactListItemEnhanced.Header(section))
                }
            }
            
            result.add(ContactListItemEnhanced.ContactItem(contact))
        }
        
        result
    }
    
    /**
     * Search contacts by name or phone number
     */
    suspend fun searchContacts(query: String): List<ContactEnhanced> = withContext(Dispatchers.IO) {
        if (query.isEmpty()) {
            return@withContext getContacts()
        }
        
        // Add to search history
        addToSearchHistory(query)
        
        val contacts = getContacts()
        contacts.filter { contact ->
            contact.name.contains(query, ignoreCase = true) ||
            contact.phoneNumber.contains(query) ||
            contact.additionalPhones.any { it.contains(query) } ||
            contact.email?.contains(query, ignoreCase = true) == true ||
            contact.additionalEmails.any { it.contains(query, ignoreCase = true) }
        }
    }
    
    /**
     * Toggle favorite status for a contact
     */
    suspend fun toggleFavorite(contactId: String): Boolean = withContext(Dispatchers.IO) {
        val favorites = getFavoriteIds().toMutableSet()
        val isFavorite = if (contactId in favorites) {
            favorites.remove(contactId)
            false
        } else {
            favorites.add(contactId)
            true
        }
        
        saveFavoriteIds(favorites)
        
        // Update cache
        cache.getContact(contactId)?.let { contact ->
            cache.updateContact(contact.copy(isFavorite = isFavorite))
        }
        
        isFavorite
    }
    
    /**
     * Detect duplicate contacts
     */
    suspend fun detectDuplicates(): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val contacts = getContacts()
        val duplicates = mutableListOf<DuplicateGroup>()
        val processed = mutableSetOf<String>()
        
        contacts.forEach { contact1 ->
            if (contact1.contactId in processed) return@forEach
            
            val similarContacts = mutableListOf(contact1)
            
            contacts.forEach { contact2 ->
                if (contact1.contactId != contact2.contactId && 
                    contact2.contactId !in processed) {
                    val similarity = contact1.calculateSimilarity(contact2)
                    if (similarity >= DUPLICATE_THRESHOLD) {
                        similarContacts.add(contact2)
                        processed.add(contact2.contactId)
                    }
                }
            }
            
            if (similarContacts.size > 1) {
                val avgSimilarity = similarContacts.drop(1).map { 
                    contact1.calculateSimilarity(it) 
                }.average().toFloat()
                
                duplicates.add(DuplicateGroup(similarContacts, avgSimilarity))
                processed.add(contact1.contactId)
            }
        }
        
        duplicates.sortedByDescending { it.similarityScore }
    }
    
    /**
     * Get smart suggestions based on user behavior
     */
    suspend fun getSmartSuggestions(limit: Int = 5): List<SmartSuggestion> = withContext(Dispatchers.IO) {
        val contacts = getContacts()
        val suggestions = mutableListOf<SmartSuggestion>()
        
        // Favorites
        contacts.filter { it.isFavorite }.forEach {
            suggestions.add(SmartSuggestion(it, SuggestionReason.FAVORITE, 1.0f))
        }
        
        // Frequently contacted
        contacts.sortedByDescending { it.timesContacted }
            .take(5)
            .forEach {
                if (it.timesContacted > 0) {
                    val score = (it.timesContacted / 100f).coerceAtMost(0.9f)
                    suggestions.add(SmartSuggestion(it, SuggestionReason.FREQUENTLY_CONTACTED, score))
                }
            }
        
        // Recently contacted
        contacts.sortedByDescending { it.lastContactedTime }
            .take(5)
            .forEach {
                if (it.lastContactedTime > 0) {
                    val daysSince = (System.currentTimeMillis() - it.lastContactedTime) / (24 * 60 * 60 * 1000)
                    val score = (1.0f / (daysSince + 1)).coerceAtMost(0.8f)
                    suggestions.add(SmartSuggestion(it, SuggestionReason.RECENTLY_CONTACTED, score))
                }
            }
        
        suggestions.sortedByDescending { it.score }.take(limit)
    }
    
    /**
     * Add contact to device with account support
     */
    suspend fun addContactToDevice(
        contact: ContactEnhanced,
        accountType: AccountType = AccountType.PHONE
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val ops = ArrayList<ContentProviderOperation>()
            val account = getAccountForType(accountType)
            
            // Create new raw contact
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, account?.type)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, account?.name)
                    .build()
            )
            
            // Add name
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                    .build()
            )
            
            // Add phone numbers
            contact.getAllPhoneNumbers().forEach { phone ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                        .withValue(
                            ContactsContract.CommonDataKinds.Phone.TYPE,
                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                        )
                        .build()
                )
            }
            
            // Add emails
            contact.getAllEmails().forEach { email ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Email.DATA, email)
                        .withValue(
                            ContactsContract.CommonDataKinds.Email.TYPE,
                            ContactsContract.CommonDataKinds.Email.TYPE_HOME
                        )
                        .build()
                )
            }
            
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            cache.invalidate()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Get available accounts for contact storage
     */
    fun getAvailableAccounts(): List<Pair<AccountType, String>> {
        val accountManager = AccountManager.get(context)
        val accounts = accountManager.accounts
        val result = mutableListOf<Pair<AccountType, String>>()
        
        // Always add phone storage
        result.add(Pair(AccountType.PHONE, "Phone"))
        
        accounts.forEach { account ->
            when {
                account.type.contains("google", ignoreCase = true) -> {
                    result.add(Pair(AccountType.GOOGLE, account.name))
                }
                account.type.contains("sim", ignoreCase = true) -> {
                    result.add(Pair(AccountType.SIM, "SIM Card"))
                }
                account.type.contains("exchange", ignoreCase = true) -> {
                    result.add(Pair(AccountType.EXCHANGE, account.name))
                }
                else -> {
                    result.add(Pair(AccountType.OTHER, account.name))
                }
            }
        }
        
        return result
    }
    
    /**
     * Get search history
     */
    fun getSearchHistory(): List<SearchHistoryItem> {
        val json = prefs.getString(PREF_SEARCH_HISTORY, "[]") ?: "[]"
        // Simple parsing - in production use Gson or kotlinx.serialization
        return emptyList() // Simplified for now
    }
    
    /**
     * Clear search history
     */
    fun clearSearchHistory() {
        prefs.edit().remove(PREF_SEARCH_HISTORY).apply()
    }
    
    // Private helper methods
    
    private fun getFavoriteIds(): Set<String> {
        return prefs.getStringSet(PREF_FAVORITES, emptySet()) ?: emptySet()
    }
    
    private fun saveFavoriteIds(favorites: Set<String>) {
        prefs.edit().putStringSet(PREF_FAVORITES, favorites).apply()
    }
    
    private fun addToSearchHistory(query: String) {
        // Simplified - in production, maintain a proper list with timestamps
        val history = getSearchHistory().toMutableList()
        history.add(0, SearchHistoryItem(query))
        // Keep only recent items
        val trimmed = history.take(MAX_SEARCH_HISTORY)
        // Save back (simplified)
    }
    
    private fun getAccountType(contactId: String): AccountType {
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts.ACCOUNT_TYPE),
                "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val typeIndex = it.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
                    val type = if (typeIndex != -1) it.getString(typeIndex) else null
                    
                    return when {
                        type == null -> AccountType.PHONE
                        type.contains("google", ignoreCase = true) -> AccountType.GOOGLE
                        type.contains("sim", ignoreCase = true) -> AccountType.SIM
                        type.contains("exchange", ignoreCase = true) -> AccountType.EXCHANGE
                        else -> AccountType.OTHER
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return AccountType.PHONE
    }
    
    private fun getAccountForType(accountType: AccountType): Account? {
        if (accountType == AccountType.PHONE) return null
        
        val accountManager = AccountManager.get(context)
        val accounts = accountManager.accounts
        
        return accounts.firstOrNull { account ->
            when (accountType) {
                AccountType.GOOGLE -> account.type.contains("google", ignoreCase = true)
                AccountType.SIM -> account.type.contains("sim", ignoreCase = true)
                AccountType.EXCHANGE -> account.type.contains("exchange", ignoreCase = true)
                else -> false
            }
        }
    }
}
