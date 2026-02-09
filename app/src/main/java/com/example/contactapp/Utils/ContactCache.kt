package com.example.contactapp.Utils

import com.example.contactapp.Model.ContactEnhanced
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory cache for contacts with TTL support
 * Optimizes performance for large contact lists
 */
class ContactCache private constructor() {
    
    private val contactsCache = ConcurrentHashMap<String, ContactEnhanced>()
    private val allContactsCache = mutableListOf<ContactEnhanced>()
    private var lastUpdateTime = 0L
    private val cacheTTL = 5 * 60 * 1000L // 5 minutes
    
    companion object {
        @Volatile
        private var instance: ContactCache? = null
        
        fun getInstance(): ContactCache {
            return instance ?: synchronized(this) {
                instance ?: ContactCache().also { instance = it }
            }
        }
    }
    
    /**
     * Update the entire contacts cache
     */
    fun updateContacts(contacts: List<ContactEnhanced>) {
        synchronized(this) {
            contactsCache.clear()
            allContactsCache.clear()
            
            contacts.forEach { contact ->
                contactsCache[contact.contactId] = contact
                allContactsCache.add(contact)
            }
            
            lastUpdateTime = System.currentTimeMillis()
        }
    }
    
    /**
     * Get all cached contacts
     */
    fun getAllContacts(): List<ContactEnhanced> {
        return if (isCacheValid()) {
            synchronized(this) {
                allContactsCache.toList()
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * Get contact by ID
     */
    fun getContact(contactId: String): ContactEnhanced? {
        return if (isCacheValid()) {
            contactsCache[contactId]
        } else {
            null
        }
    }
    
    /**
     * Update a single contact in cache
     */
    fun updateContact(contact: ContactEnhanced) {
        synchronized(this) {
            contactsCache[contact.contactId] = contact
            val index = allContactsCache.indexOfFirst { it.contactId == contact.contactId }
            if (index != -1) {
                allContactsCache[index] = contact
            } else {
                allContactsCache.add(contact)
            }
        }
    }
    
    /**
     * Remove contact from cache
     */
    fun removeContact(contactId: String) {
        synchronized(this) {
            contactsCache.remove(contactId)
            allContactsCache.removeAll { it.contactId == contactId }
        }
    }
    
    /**
     * Check if cache is still valid
     */
    fun isCacheValid(): Boolean {
        return (System.currentTimeMillis() - lastUpdateTime) < cacheTTL
    }
    
    /**
     * Invalidate cache
     */
    fun invalidate() {
        synchronized(this) {
            contactsCache.clear()
            allContactsCache.clear()
            lastUpdateTime = 0L
        }
    }
    
    /**
     * Get cache size
     */
    fun size(): Int = allContactsCache.size
}
