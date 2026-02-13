package com.example.contactapp.Repository

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import com.example.contactapp.Model.ContactEnhanced
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsRepository(private val context: Context) {

    suspend fun getContacts(): List<ContactEnhanced> = withContext(Dispatchers.IO) {
        val contactsList = mutableListOf<ContactEnhanced>()
        val contentResolver: ContentResolver = context.contentResolver

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.LAST_TIME_CONTACTED,
            ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED,
            ContactsContract.CommonDataKinds.Phone.STARRED
        )

        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            val lastContactedIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LAST_TIME_CONTACTED)
            val timesContactedIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED)
            val starredIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)

            val uniqueContactIds = HashSet<String>()

            while (it.moveToNext()) {
                val id = it.getString(idIndex)

                if (uniqueContactIds.contains(id)) continue

                val name = it.getString(nameIndex) ?: "Unknown"
                val number = it.getString(numberIndex) ?: ""
                val photoUriString = it.getString(photoIndex)
                val lastContacted = if (lastContactedIndex != -1) it.getLong(lastContactedIndex) else 0L
                val timesContacted = if (timesContactedIndex != -1) it.getInt(timesContactedIndex) else 0
                val isFavorite = if (starredIndex != -1) it.getInt(starredIndex) == 1 else false


                uniqueContactIds.add(id)
                contactsList.add(
                    ContactEnhanced(
                        contactId = id,
                        name = name,
                        phoneNumber = number,
                        photoUri = photoUriString,
                        lastContactedTime = lastContacted,
                        timesContacted = timesContacted,
                        isFavorite = isFavorite
                    )
                )
            }
        }
        return@withContext contactsList
    }

    fun addContactIntent(): Intent {
        return Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
        }
    }
}