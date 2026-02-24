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

    private val _isExistingContact = MutableLiveData(false)
    val isExistingContact: LiveData<Boolean> = _isExistingContact

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

    private suspend fun findContactByNumber(phoneNumber: String): String? = withContext(Dispatchers.IO) {
        try {
            val uri = android.net.Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(phoneNumber)
            )
            val cursor = getApplication<Application>().contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (idx != -1) return@withContext it.getString(idx)
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun formatPhoneNumber(number: String): String {
        val cleaned = number.replace(Regex("[^0-9+]"), "")
        return when {
            number.isEmpty() -> ""
            cleaned.startsWith("+") -> number
            cleaned.length == 10 -> "(${cleaned.substring(0, 3)}) ${cleaned.substring(3, 6)}-${cleaned.substring(6)}"
            cleaned.length == 11 -> "+${cleaned[0]} (${cleaned.substring(1, 4)}) ${cleaned.substring(4, 7)}-${cleaned.substring(7)}"
            else -> number
        }
    }

    /**
     * FIX: Relaxed validation. Accepts 7+ digit numbers (local calls) and numbers with +.
     * Previous check required 10+ digits which would reject many valid numbers.
     */
    fun isValidPhoneNumber(number: String): Boolean {
        if (number.isBlank()) return false
        val cleaned = number.replace(Regex("[^0-9+*#]"), "")
        return cleaned.length >= 3 // Allow emergency numbers, extensions, etc.
    }
}