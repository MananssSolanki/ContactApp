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

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /** FIX: Track whether we've loaded at least once to avoid redundant onResume reloads. */
    private var hasLoaded = false

    /**
     * Load call logs only if not already loaded.
     * Pass force=true to always reload (e.g. on swipe-to-refresh).
     */
    fun loadCallLogs(force: Boolean = false) {
        if (hasLoaded && !force) return
        viewModelScope.launch {
            try {
                _loading.value = true
                _errorMessage.value = null
                val logs = repository.getCallLogs()
                _callLogs.value = logs
                hasLoaded = true
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load call logs: ${e.message}"
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    /** Force a refresh (called from swipe-to-refresh). */
    fun refresh() = loadCallLogs(force = true)

    /**
     * Delete selected call logs by IDs.
     * FIX: Was completely commented out before. Now fully implemented.
     * After deletion, updates the LiveData list in-place without a full reload.
     */
    fun deleteCallLogs(ids: Set<String>, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = repository.deleteCallLogs(ids.toList())
                if (success) {
                    // Remove deleted items from current list without full reload
                    val current = _callLogs.value ?: emptyList()
                    val updated = current.filter { item ->
                        when (item) {
                            is CallLogListItem.Header -> true
                            is CallLogListItem.CallItem -> item.callLog.id !in ids
                        }
                    }
                    // Remove orphan headers (headers with no call items after them)
                    val cleaned = removeOrphanHeaders(updated)
                    _callLogs.value = cleaned
                }
                onComplete(success)
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Delete failed: ${e.message}"
                onComplete(false)
            }
        }
    }

    /** Remove date headers that have no call items following them. */
    private fun removeOrphanHeaders(list: List<CallLogListItem>): List<CallLogListItem> {
        val result = mutableListOf<CallLogListItem>()
        for (i in list.indices) {
            val item = list[i]
            if (item is CallLogListItem.Header) {
                // Only add header if the next item is a CallItem
                val next = list.getOrNull(i + 1)
                if (next is CallLogListItem.CallItem) result.add(item)
            } else {
                result.add(item)
            }
        }
        return result
    }

    fun clearError() {
        _errorMessage.value = null
    }
}