package com.example.contactapp.Utils

import com.example.contactapp.Model.ContactEnhanced
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory cache for contacts with TTL support.
 *
 * FIX: getAllContacts() previously returned an empty list when cache was expired.
 * Now it returns stale data and the ViewModel decides when to force-reload.
 * isCacheValid() is the single source of truth for freshness checking.
 */
class ContactCache private constructor() {

    private val contactsById = ConcurrentHashMap<String, ContactEnhanced>()
    private val orderedContacts = mutableListOf<ContactEnhanced>()
    private var lastUpdateTime = 0L
    private val cacheTTL = 5 * 60 * 1000L // 5 minutes

    companion object {
        @Volatile
        private var instance: ContactCache? = null

        fun getInstance(): ContactCache =
            instance ?: synchronized(this) {
                instance ?: ContactCache().also { instance = it }
            }
    }

    fun updateContacts(contacts: List<ContactEnhanced>) {
        synchronized(this) {
            contactsById.clear()
            orderedContacts.clear()
            contacts.forEach { contact ->
                contactsById[contact.contactId] = contact
                orderedContacts.add(contact)
            }
            lastUpdateTime = System.currentTimeMillis()
        }
    }

    /**
     * FIX: Returns all contacts regardless of cache validity.
     * Callers should check isCacheValid() to decide whether to reload.
     */
    fun getAllContacts(): List<ContactEnhanced> {
        return synchronized(this) { orderedContacts.toList() }
    }

    fun getContact(contactId: String): ContactEnhanced? = contactsById[contactId]

    fun updateContact(contact: ContactEnhanced) {
        synchronized(this) {
            contactsById[contact.contactId] = contact
            val idx = orderedContacts.indexOfFirst { it.contactId == contact.contactId }
            if (idx != -1) orderedContacts[idx] = contact else orderedContacts.add(contact)
        }
    }

    fun removeContact(contactId: String) {
        synchronized(this) {
            contactsById.remove(contactId)
            orderedContacts.removeAll { it.contactId == contactId }
        }
    }

    fun isCacheValid(): Boolean =
        lastUpdateTime > 0 && (System.currentTimeMillis() - lastUpdateTime) < cacheTTL

    fun invalidate() {
        synchronized(this) {
            contactsById.clear()
            orderedContacts.clear()
            lastUpdateTime = 0L
        }
    }

    fun size(): Int = orderedContacts.size
}