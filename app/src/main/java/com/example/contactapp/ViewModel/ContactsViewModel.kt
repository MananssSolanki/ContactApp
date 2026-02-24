package com.example.contactapp.ViewModel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.contactapp.Model.ContactEnhanced
import com.example.contactapp.Model.ContactListItemEnhanced
import com.example.contactapp.Repository.ContactsRepository
import com.example.contactapp.Utils.ContactCache
import kotlinx.coroutines.launch

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContactsRepository(application)
    private val cache = ContactCache.getInstance()

    private val _contactItems = MutableLiveData<List<ContactListItemEnhanced>>()
    val contactItems: LiveData<List<ContactListItemEnhanced>> = _contactItems

    private var allContacts: List<ContactEnhanced> = emptyList()

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * FIX: Load from cache if valid; only hit the content resolver when cache is stale or forced.
     * This prevents redundant reloads on every onResume.
     */
    fun loadContacts(force: Boolean = false) {
        // Use cache if valid and not forced
        if (!force && cache.isCacheValid()) {
            val cached = cache.getAllContacts()
            if (cached.isNotEmpty()) {
                val sorted = cached.sortedBy { it.name.lowercase() }
                allContacts = sorted
                _contactItems.value = buildSectionedList(sorted)
                return
            }
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val contacts = repository.getContacts()
                val sorted = contacts.sortedBy { it.name.lowercase() }
                allContacts = sorted
                cache.updateContacts(contacts) // Cache the raw list
                _contactItems.value = buildSectionedList(sorted)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Transforms a flat list of contacts into a sectioned list with headers.
     * Structure: Favorites → Groups → A-Z alphabetical
     */
    private fun buildSectionedList(contacts: List<ContactEnhanced>): List<ContactListItemEnhanced> {
        val list = mutableListOf<ContactListItemEnhanced>()

        // 1. Favorites
        val favorites = contacts.filter { it.isFavorite }
        if (favorites.isNotEmpty()) {
            list.add(ContactListItemEnhanced.Header("Favourites", isFavorite = true))
            favorites.forEach { list.add(ContactListItemEnhanced.ContactItem(it)) }
        }

        // 2. Groups
        list.add(ContactListItemEnhanced.Header("Groups"))
        list.add(ContactListItemEnhanced.GroupsItem)

        // 3. A-Z
        var currentLetter: Char? = null
        contacts.forEach { contact ->
            val firstChar = contact.name.firstOrNull()?.uppercaseChar() ?: '#'
            val groupChar = if (firstChar.isLetter()) firstChar else '#'

            if (groupChar != currentLetter) {
                currentLetter = groupChar
                list.add(ContactListItemEnhanced.Header(groupChar.toString()))
            }
            list.add(ContactListItemEnhanced.ContactItem(contact))
        }

        return list
    }

    /**
     * Filter contacts by search query.
     * When empty, restores full sectioned list.
     */
    fun searchContacts(query: String) {
        if (query.isBlank()) {
            _contactItems.value = buildSectionedList(allContacts)
        } else {
            val filtered = allContacts.filter { contact ->
                contact.name.contains(query, ignoreCase = true) ||
                        contact.phoneNumber.contains(query) ||
                        contact.additionalPhones.any { it.contains(query) }
            }.sortedBy { it.name.lowercase() }
            _contactItems.value = filtered.map { ContactListItemEnhanced.ContactItem(it) }
        }
    }

    /**
     * Toggle favorite and force-reload contacts (invalidates cache).
     */
    fun toggleFavorite(contactId: String, isCurrentlyFavorite: Boolean) {
        viewModelScope.launch {
            val success = repository.toggleFavorite(contactId, !isCurrentlyFavorite)
            if (success) {
                cache.invalidate()
                loadContacts(force = true)
            }
        }
    }

    /** Invalidate cache so next loadContacts() fetches fresh data. */
    fun invalidateCache() {
        cache.invalidate()
    }
//
//    fun getAddContactIntent(prefillNumber: String? = null): Intent =
//        repository.addContactIntent(prefillNumber)
//
//    fun getEditContactIntent(contactId: String): Intent =
//        repository.editContactIntent(contactId)
}