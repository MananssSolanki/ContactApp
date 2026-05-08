package com.example.contactapp.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.contactapp.Model.ContactEnhanced
import com.example.contactapp.Model.ContactListItemEnhanced
import com.example.contactapp.Repository.ContactsRepository
import com.example.contactapp.Utils.ContactCache
import com.example.contactapp.Utils.RecycleBinManager
import kotlinx.coroutines.launch

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository       = ContactsRepository(application)
    private val cache            = ContactCache.getInstance()
    private val recycleBinManager = RecycleBinManager(application)

    private val _contactItems = MutableLiveData<List<ContactListItemEnhanced>>()
    val contactItems: LiveData<List<ContactListItemEnhanced>> = _contactItems

    // FIX: store only the filtered list so search never shows bin contacts
    private var displayedContacts: List<ContactEnhanced> = emptyList()

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Load contacts from cache if still valid; force-reload from system DB when asked.
     * Always filters out contacts that currently live in the Recycle Bin.
     */
    fun loadContacts(force: Boolean = false) {
        viewModelScope.launch {
            val raw: List<ContactEnhanced> = if (!force && cache.isCacheValid()) {
                cache.getAllContacts()
            } else {
                _isLoading.value = true
                try {
                    val fresh = repository.getContacts()
                    cache.updateContacts(fresh)
                    fresh
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                } finally {
                    _isLoading.value = false
                }
            }

            // Always subtract contacts that are sitting in the Recycle Bin (native)
            val deletedIds = recycleBinManager.getDeletedContacts().map { it.contactId }.toSet()
            val filtered   = raw
                .filter { it.contactId !in deletedIds }
                .sortedBy  { it.name.lowercase() }

            displayedContacts = filtered          // search operates on this
            _contactItems.value = buildSectionedList(filtered)
        }
    }

    /**
     * Sectioned list: Favourites → Groups placeholder → A-Z letters
     */
    private fun buildSectionedList(contacts: List<ContactEnhanced>): List<ContactListItemEnhanced> {
        val list = mutableListOf<ContactListItemEnhanced>()

        val favorites = contacts.filter { it.isFavorite }
        if (favorites.isNotEmpty()) {
            list.add(ContactListItemEnhanced.Header("Favourites", isFavorite = true))
            favorites.forEach { list.add(ContactListItemEnhanced.ContactItem(it)) }
        }

        list.add(ContactListItemEnhanced.Header("Groups"))
        list.add(ContactListItemEnhanced.GroupsItem)

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

    /** Live search — operates only on the already-filtered (non-bin) list. */
    fun searchContacts(query: String) {
        if (query.isBlank()) {
            _contactItems.value = buildSectionedList(displayedContacts)
        } else {
            val filtered = displayedContacts.filter { contact ->
                contact.name.contains(query, ignoreCase = true) ||
                        contact.phoneNumber.contains(query) ||
                        contact.additionalPhones.any { it.contains(query) }
            }.sortedBy { it.name.lowercase() }
            _contactItems.value = filtered.map { ContactListItemEnhanced.ContactItem(it) }
        }
    }

    fun toggleFavorite(contactId: String, isCurrentlyFavorite: Boolean) {
        viewModelScope.launch {
            if (repository.toggleFavorite(contactId, !isCurrentlyFavorite)) {
                cache.invalidate()
                loadContacts(force = true)
            }
        }
    }

    fun invalidateCache() { cache.invalidate() }

    /**
     * SOFT DELETE — called from ContactsFragment.
     *
     * Saves full contact data to Recycle Bin (Room DB) so it can be restored
     * or permanently deleted later.  The contact is intentionally left in the
     * Android system DB at this point — it is simply hidden by the filter in
     * loadContacts().  Actual system deletion happens in permanentlyDelete().
     */
    fun deleteSelectedContacts(contactIds: List<String>, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // System native deletion (sync adapters will mark DELETED=1)
                val isSuccess = repository.deleteContacts(contactIds)
                
                if (isSuccess) {
                    // Invalidate cache so the next load does not serve stale data (and honors the DELETED flag)
                    cache.invalidate()
                    loadContacts(force = true)
                }
                onResult(isSuccess)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            } finally {
                _isLoading.value = false
            }
        }
    }
}