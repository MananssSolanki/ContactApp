package com.example.contactapp.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.contactapp.Model.PhoneContact
import com.example.contactapp.Repository.ContactRepository
import com.example.contactapp.RoomDatabase.Contact
import com.example.contactapp.RoomDatabase.ContactDatabase
import kotlinx.coroutines.launch

class ContactViewModel(app : Application) : AndroidViewModel(app) {
    private val repository : ContactRepository
    val contact : LiveData<List<Contact>>

    private val _phoneContacts = MutableLiveData<List<PhoneContact>>()
    val phoneContacts : LiveData<List<PhoneContact>> = _phoneContacts

    private val _loading = MutableLiveData<Boolean>()
    val loading : LiveData<Boolean> = _loading

    init {
        val dao = ContactDatabase.getDatabase(app).contactDao()
        repository = ContactRepository(dao , app.applicationContext)
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

    fun loadPhoneContacts(){
        viewModelScope.launch {
            _loading.value = true
            _phoneContacts.value = repository.getContacts()
            _loading.value = false
        }
    }
}