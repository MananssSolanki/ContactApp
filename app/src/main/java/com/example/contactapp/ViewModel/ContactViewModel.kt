package com.example.contactapp.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.contactapp.Repository.ContactRepository
import com.example.contactapp.RoomDatabase.Contact
import com.example.contactapp.RoomDatabase.ContactDatabase
import kotlinx.coroutines.launch

class ContactViewModel(app : Application) : AndroidViewModel(app) {
    private val repository : ContactRepository
    val contact : LiveData<List<Contact>>

    init {
        val dao = ContactDatabase.getDatabase(app).contactDao()
        repository = ContactRepository(dao)
        contact = repository.contact
    }

    fun addContact(name : String, number: String){
        viewModelScope.launch {
            repository.add(Contact(name = name , number = number))
        }
    }

    fun deleteContact(contact : Contact){
        viewModelScope.launch {
            repository.delete(contact)
        }
    }
}