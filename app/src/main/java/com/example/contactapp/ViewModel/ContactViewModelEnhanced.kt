package com.example.contactapp.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactapp.Model.*
import com.example.contactapp.Repository.ContactRepositoryEnhanced
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Enhanced Contact ViewModel with StateFlow and comprehensive features
 */
class ContactViewModelEnhanced(app: Application) : AndroidViewModel(app) {
    
    private val repository = ContactRepositoryEnhanced(app.applicationContext)
    
    // State flows for reactive UI
    private val _contactListItems = MutableStateFlow<List<ContactListItemEnhanced>>(emptyList())
    val contactListItems: StateFlow<List<ContactListItemEnhanced>> = _contactListItems.asStateFlow()
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _duplicateGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val duplicateGroups: StateFlow<List<DuplicateGroup>> = _duplicateGroups.asStateFlow()
    
    private val _smartSuggestions = MutableStateFlow<List<SmartSuggestion>>(emptyList())
    val smartSuggestions: StateFlow<List<SmartSuggestion>> = _smartSuggestions.asStateFlow()
    
    private val _availableAccounts = MutableStateFlow<List<Pair<AccountType, String>>>(emptyList())
    val availableAccounts: StateFlow<List<Pair<AccountType, String>>> = _availableAccounts.asStateFlow()
    
    // Section positions for fast scrolling
    private val sectionPositions = mutableMapOf<String, Int>()
    
    init {
        loadAvailableAccounts()
    }
    
    /**
     * Load all phone contacts with alphabetic grouping
     */
    fun loadPhoneContacts(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _errorMessage.value = null
                
                val contacts = repository.getContactsSortedAlphabetically(forceRefresh)
                _contactListItems.value = contacts
                
                // Update section positions
                updateSectionPositions(contacts)
                
                _loading.value = false
            } catch (e: Exception) {
                _loading.value = false
                _errorMessage.value = "Failed to load contacts: ${e.message}"
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Search contacts by query
     */
    fun searchContacts(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            try {
                _loading.value = true
                
                if (query.isEmpty()) {
                    // Show all contacts with headers
                    val contacts = repository.getContactsSortedAlphabetically()
                    _contactListItems.value = contacts
                    updateSectionPositions(contacts)
                } else {
                    // Show search results without headers
                    val results = repository.searchContacts(query)
                    _contactListItems.value = results.map { ContactListItemEnhanced.ContactItem(it) }
                }
                
                _loading.value = false
            } catch (e: Exception) {
                _loading.value = false
                _errorMessage.value = "Search failed: ${e.message}"
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Toggle favorite status for a contact
     */
    fun toggleFavorite(contactId: String) {
        viewModelScope.launch {
            try {
                val isFavorite = repository.toggleFavorite(contactId)
                // Reload contacts to reflect changes
                loadPhoneContacts(forceRefresh = true)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update favorite: ${e.message}"
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Detect duplicate contacts
     */
    fun detectDuplicates() {
        viewModelScope.launch {
            try {
                _loading.value = true
                val duplicates = repository.detectDuplicates()
                _duplicateGroups.value = duplicates
                _loading.value = false
            } catch (e: Exception) {
                _loading.value = false
                _errorMessage.value = "Failed to detect duplicates: ${e.message}"
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Load smart suggestions
     */
    fun loadSmartSuggestions() {
        viewModelScope.launch {
            try {
                val suggestions = repository.getSmartSuggestions()
                _smartSuggestions.value = suggestions
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Add contact to device
     */
    fun addContactToDevice(
        contact: ContactEnhanced,
        accountType: AccountType = AccountType.PHONE,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val success = repository.addContactToDevice(contact, accountType)
                onComplete(success)
                if (success) {
                    // Reload contacts after adding
                    loadPhoneContacts(forceRefresh = true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }
    
    /**
     * Load available accounts for contact storage
     */
    private fun loadAvailableAccounts() {
        viewModelScope.launch {
            try {
                val accounts = repository.getAvailableAccounts()
                _availableAccounts.value = accounts
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Get section position for fast scrolling
     */
    fun getSectionPosition(letter: String): Int? {
        return sectionPositions[letter]
    }
    
    /**
     * Update section positions map
     */
    private fun updateSectionPositions(items: List<ContactListItemEnhanced>) {
        sectionPositions.clear()
        items.forEachIndexed { index, item ->
            if (item is ContactListItemEnhanced.Header) {
                sectionPositions[item.letter] = index
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Refresh contacts
     */
    fun refresh() {
        loadPhoneContacts(forceRefresh = true)
        loadSmartSuggestions()
    }
}