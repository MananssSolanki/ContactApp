package com.example.contactapp.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.contactapp.Model.CallLogListItem
import com.example.contactapp.Repository.CallLogRepository
import kotlinx.coroutines.launch

class CallLogViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = CallLogRepository(app.applicationContext)

    private val _callLogs = MutableLiveData<List<CallLogListItem>>()
    val callLogs: LiveData<List<CallLogListItem>> = _callLogs

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Load call logs
     */
    fun loadCallLogs() {
        viewModelScope.launch {
            try {
                _loading.value = true
                _errorMessage.value = null
                val logs = repository.getCallLogs()
                _callLogs.value = logs
                _loading.value = false
            } catch (e: Exception) {
                _loading.value = false
                _errorMessage.value = "Failed to load call logs: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    /**
     * Delete call log entry
     */
    fun deleteCallLog(callLogId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = repository.deleteCallLog(callLogId)
                onComplete(success)
                if (success) {
                    // Reload call logs after deletion
                    loadCallLogs()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    /**
     * Refresh call logs
     */
    fun refresh() {
        loadCallLogs()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
