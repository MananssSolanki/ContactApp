package com.example.contactapp.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.contactapp.Model.ContactListItem
import com.example.contactapp.Model.PhoneContact
import com.example.contactapp.Repository.ContactRepository
import com.example.contactapp.RoomDatabase.Contact
import com.example.contactapp.RoomDatabase.ContactDatabase
import kotlinx.coroutines.launch

class ContactViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: ContactRepository
    val contact: LiveData<List<Contact>>

    private val _phoneContacts = MutableLiveData<List<PhoneContact>>()
    val phoneContacts: LiveData<List<PhoneContact>> = _phoneContacts

    private val _contactListItems = MutableLiveData<List<ContactListItem>>()
    val contactListItems: LiveData<List<ContactListItem>> = _contactListItems

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        val dao = ContactDatabase.getDatabase(app).contactDao()
        repository = ContactRepository(dao, app.applicationContext)
        contact = repository.contact
    }

    fun addContact(name: String, number: String) {
        viewModelScope.launch {
            repository.add(Contact(name = name, number = number))
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            repository.delete(contact)
        }
    }

    /**
     * Load all phone contacts with alphabetic grouping
     */
    fun loadPhoneContacts() {
        viewModelScope.launch {
            try {
                _loading.value = true
                _errorMessage.value = null
                val contacts = repository.getContactsSortedAlphabetically()
                _contactListItems.value = contacts
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
                val results = repository.searchContacts(query)
                
                // Convert search results to list items without headers for cleaner search results
                if (query.isEmpty()) {
                    // Show with headers when not searching
                    _contactListItems.value = repository.getContactsSortedAlphabetically()
                } else {
                    // Show without headers when searching
                    _contactListItems.value = results.map { ContactListItem.ContactItem(it) }
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
     * Add contact to device
     */
    fun addContactToDevice(contact: PhoneContact, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = repository.addContactToDevice(contact)
                onComplete(success)
                if (success) {
                    // Reload contacts after adding
                    loadPhoneContacts()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    /**
     * Delete contact from device
     */
    fun deleteContactFromDevice(contactId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = repository.deleteContactFromDevice(contactId)
                onComplete(success)
                if (success) {
                    // Reload contacts after deletion
                    loadPhoneContacts()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    /**
     * Get section positions for fast scrolling
     */
    fun getSectionPositions(): Map<String, Int> {
        val items = _contactListItems.value ?: return emptyMap()
        val positions = mutableMapOf<String, Int>()
        
        items.forEachIndexed { index, item ->
            if (item is ContactListItem.Header) {
                positions[item.letter] = index
            }
        }
        
        return positions
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}