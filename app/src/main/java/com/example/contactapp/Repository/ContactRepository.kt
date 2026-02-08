package com.example.contactapp.Repository

import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import com.example.contactapp.Dao.ContactDao
import com.example.contactapp.Model.ContactListItem
import com.example.contactapp.Model.PhoneContact
import com.example.contactapp.RoomDatabase.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactRepository(private val dao: ContactDao, private val context: Context) {
    val contact = dao.getAllContacts()

    suspend fun add(contact: Contact) {
        dao.insert(contact)
    }

    suspend fun delete(contact: Contact) {
        dao.delete(contact)
    }

    /**
     * Get all contacts with complete details
     * Photo URIs are loaded in background to prevent memory overhead
     */
    suspend fun getContacts(): List<PhoneContact> = withContext(Dispatchers.IO) {
        val contactsMap = mutableMapOf<String, PhoneContact>()

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

            while (it.moveToNext()) {
                val contactId = if (idIndex != -1) it.getString(idIndex) else ""
                val name = if (nameIndex != -1) it.getString(nameIndex) ?: "Unknown" else "Unknown"
                val number = if (numberIndex != -1) it.getString(numberIndex) ?: "" else ""
                val photoUri = if (photoIndex != -1) it.getString(photoIndex) else null

                // Group multiple numbers for the same contact
                if (contactsMap.containsKey(contactId)) {
                    val existing = contactsMap[contactId]!!
                    contactsMap[contactId] = existing.copy(
                        additionalPhones = existing.additionalPhones + number
                    )
                } else {
                    contactsMap[contactId] = PhoneContact(
                        contactId = contactId,
                        name = name,
                        phoneNumber = number,
                        photoUri = photoUri
                    )
                }
            }
        }

        contactsMap.values.toList()
    }

    /**
     * Get contacts sorted alphabetically with section headers
     * Special characters and non-English names grouped under "#"
     */
    suspend fun getContactsSortedAlphabetically(): List<ContactListItem> = withContext(Dispatchers.IO) {
        val contacts = getContacts()
        val sortedContacts = contacts.sortedBy { it.name.uppercase() }
        val result = mutableListOf<ContactListItem>()
        
        var currentSection = ""
        sortedContacts.forEach { contact ->
            val section = contact.getSectionCharacter()
            if (section != currentSection) {
                currentSection = section
                result.add(ContactListItem.Header(section))
            }
            result.add(ContactListItem.ContactItem(contact))
        }
        
        result
    }

    /**
     * Search contacts by name or phone number
     */
    suspend fun searchContacts(query: String): List<PhoneContact> = withContext(Dispatchers.IO) {
        if (query.isEmpty()) {
            return@withContext getContacts()
        }

        val contacts = getContacts()
        contacts.filter { contact ->
            contact.name.contains(query, ignoreCase = true) ||
            contact.phoneNumber.contains(query) ||
            contact.additionalPhones.any { it.contains(query) }
        }
    }

    /**
     * Add a new contact to device contacts database
     */
    suspend fun addContactToDevice(contact: PhoneContact): Boolean = withContext(Dispatchers.IO) {
        try {
            val ops = ArrayList<ContentProviderOperation>()

            // Create new raw contact
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
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

            // Add phone number
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.phoneNumber)
                    .withValue(
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    )
                    .build()
            )

            // Add email if provided
            if (!contact.email.isNullOrEmpty()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Email.DATA, contact.email)
                        .withValue(
                            ContactsContract.CommonDataKinds.Email.TYPE,
                            ContactsContract.CommonDataKinds.Email.TYPE_HOME
                        )
                        .build()
                )
            }

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Delete contact from device
     */
    suspend fun deleteContactFromDevice(contactId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = ContactsContract.RawContacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                .build()

            val deleted = context.contentResolver.delete(
                uri,
                "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                arrayOf(contactId)
            )
            deleted > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Get contact details by phone number
     */
    suspend fun getContactByNumber(phoneNumber: String): PhoneContact? = withContext(Dispatchers.IO) {
        val contacts = getContacts()
        contacts.find { 
            it.phoneNumber == phoneNumber || it.additionalPhones.contains(phoneNumber) 
        }
    }
}