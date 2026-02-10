package com.example.contactapp.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.contactapp.Model.Contact
import com.example.contactapp.Repository.ContactsRepository
import kotlinx.coroutines.launch

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContactsRepository(application)

    private val _contacts = MutableLiveData<List<Contact>>()
    val contacts: LiveData<List<Contact>> = _contacts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadContacts() {
        viewModelScope.launch {
            _isLoading.value = true
            val contactList = repository.getContacts()
            _contacts.value = contactList
            _isLoading.value = false
        }
    }

    fun getAddContactIntent() = repository.addContactIntent()
}