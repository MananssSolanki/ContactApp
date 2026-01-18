package com.example.contactapp.Repository

import com.example.contactapp.Dao.ContactDao
import com.example.contactapp.RoomDatabase.Contact

class ContactRepository(private val dao : ContactDao) {
    val contact = dao.getAllContacts()

    suspend fun add(contact : Contact){
        dao.insert(contact)
    }

    suspend fun delete(contact: Contact){
        dao.delete(contact)
    }


}