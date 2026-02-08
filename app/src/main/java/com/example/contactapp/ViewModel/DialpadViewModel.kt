package com.example.contactapp.ViewModel

import android.app.Application
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DialpadViewModel(app: Application) : AndroidViewModel(app) {

    private val _contactName = MutableLiveData<String?>()
    val contactName: LiveData<String?> = _contactName

    private val _isExistingContact = MutableLiveData<Boolean>(false)
    val isExistingContact: LiveData<Boolean> = _isExistingContact

    /**
     * Look up contact by phone number
     */
    fun lookupContact(phoneNumber: String) {
        if (phoneNumber.isEmpty()) {
            _contactName.value = null
            _isExistingContact.value = false
            return
        }

        viewModelScope.launch {
            val name = findContactByNumber(phoneNumber)
            _contactName.value = name
            _isExistingContact.value = name != null
        }
    }

    /**
     * Find contact name by phone number from device contacts
     */
    private suspend fun findContactByNumber(phoneNumber: String): String? = withContext(Dispatchers.IO) {
        try {
            val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI
            val lookupUri = android.net.Uri.withAppendedPath(uri, android.net.Uri.encode(phoneNumber))
            
            val cursor = getApplication<Application>().contentResolver.query(
                lookupUri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        return@withContext it.getString(nameIndex)
                    }
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Format phone number for display
     */
    fun formatPhoneNumber(number: String): String {
        // Basic formatting - can be enhanced with libphonenumber library
        return when {
            number.isEmpty() -> ""
            number.startsWith("+") -> number
            number.length > 10 -> {
                // Format: +1 (234) 567-8900
                val cleaned = number.replace(Regex("[^0-9]"), "")
                when (cleaned.length) {
                    11 -> "+${cleaned[0]} (${cleaned.substring(1, 4)}) ${cleaned.substring(4, 7)}-${cleaned.substring(7)}"
                    10 -> "(${cleaned.substring(0, 3)}) ${cleaned.substring(3, 6)}-${cleaned.substring(6)}"
                    else -> number
                }
            }
            else -> number
        }
    }

    /**
     * Validate if the number is callable
     */
    fun isValidPhoneNumber(number: String): Boolean {
        if (number.isEmpty()) return false
        val cleaned = number.replace(Regex("[^0-9+]"), "")
        return cleaned.length >= 10 || (cleaned.startsWith("+") && cleaned.length >= 11)
    }
}
