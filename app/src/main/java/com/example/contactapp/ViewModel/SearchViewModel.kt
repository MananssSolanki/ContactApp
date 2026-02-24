package com.example.contactapp.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.contactapp.Model.CallLogListItem
import com.example.contactapp.Model.ContactEnhanced
import com.example.contactapp.Model.ContactListItemEnhanced
import com.example.contactapp.Repository.CallLogRepository
import com.example.contactapp.Repository.ContactsRepository
import com.example.contactapp.Utils.SearchHistoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class SearchResultItem {
    data class Header(val title: String, val count: Int? = null) : SearchResultItem()
    data class HistoryItem(val query: String) : SearchResultItem()
    data class ContactItem(val contact: ContactEnhanced) : SearchResultItem()
    data class CallItem(val callLog: com.example.contactapp.Model.CallLog) : SearchResultItem()
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val contactsRepo = ContactsRepository(application)
    private val callLogRepo = CallLogRepository(application)
    private val historyManager = SearchHistoryManager(application)

    private val _searchItems = MutableLiveData<List<SearchResultItem>>()
    val searchItems: LiveData<List<SearchResultItem>> = _searchItems

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadHistory()
    }

    fun loadHistory() {
        val history = historyManager.getHistory()
        if (history.isEmpty()) {
            _searchItems.value = emptyList()
            return
        }
        val items = mutableListOf<SearchResultItem>()
        items.add(SearchResultItem.Header("Recent searches"))
        history.forEach { items.add(SearchResultItem.HistoryItem(it)) }
        _searchItems.value = items
    }

    fun search(query: String) {
        if (query.isBlank()) {
            loadHistory()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val contactsDeferred = async { contactsRepo.getContacts() }
            val callsDeferred = async { callLogRepo.getCallLogs() }

            val allContacts = contactsDeferred.await()
            val allCallsItems = callsDeferred.await()

            val filteredContacts = allContacts.filter {
                it.name.contains(query, ignoreCase = true) || 
                it.phoneNumber.contains(query) || 
                it.additionalPhones.any { p -> p.contains(query) }
            }.sortedBy { it.name.lowercase() }

            val filteredCalls = allCallsItems.filterIsInstance<CallLogListItem.CallItem>()
                .map { it.callLog }
                .filter { 
                    it.phoneNumber.contains(query) || 
                    (it.contactName?.contains(query, ignoreCase = true) ?: false)
                }

            val items = mutableListOf<SearchResultItem>()
            
            if (filteredContacts.isNotEmpty()) {
                items.add(SearchResultItem.Header("Contacts", filteredContacts.size))
                filteredContacts.forEach { items.add(SearchResultItem.ContactItem(it)) }
            }

            if (filteredCalls.isNotEmpty()) {
                items.add(SearchResultItem.Header("Recents", filteredCalls.size))
                filteredCalls.forEach { items.add(SearchResultItem.CallItem(it)) }
            }

            _searchItems.value = items
            _isLoading.value = false
        }
    }

    fun addToHistory(query: String) {
        historyManager.addSearch(query)
    }

    fun removeFromHistory(query: String) {
        historyManager.removeSearch(query)
        loadHistory()
    }
}
