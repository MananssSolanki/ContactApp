package com.example.contactapp.Repository

import android.content.Context
import android.provider.ContactsContract
import com.example.contactapp.Dao.ContactDao
import com.example.contactapp.Model.PhoneContact
import com.example.contactapp.RoomDatabase.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactRepository(private val dao : ContactDao , private val context : Context) {
    val contact = dao.getAllContacts()

    suspend fun add(contact : Contact){
        dao.insert(contact)
    }

    suspend fun delete(contact: Contact){
        dao.delete(contact)
    }

    suspend fun getContacts() : List<PhoneContact> = withContext(Dispatchers.IO){
        val contact = mutableListOf<PhoneContact>()

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIndex =
                it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex =
                it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name =
                    if (nameIndex != -1) it.getString(nameIndex) else "Unknown"
                val number =
                    if (numberIndex != -1) it.getString(numberIndex) else "Unknown"

                contact.add(PhoneContact(name, number))
            }
        }
        contact
    }

}