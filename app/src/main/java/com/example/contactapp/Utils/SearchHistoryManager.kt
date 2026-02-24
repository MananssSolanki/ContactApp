package com.example.contactapp.Utils

import android.content.Context
import android.content.SharedPreferences

class SearchHistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_HISTORY = "recent_searches"
        private const val MAX_HISTORY = 10
    }

    fun getHistory(): List<String> {
        val historyString = prefs.getString(KEY_HISTORY, "") ?: ""
        return if (historyString.isEmpty()) emptyList() else historyString.split("|")
    }

    fun addSearch(query: String) {
        if (query.isBlank()) return
        val history = getHistory().toMutableList()
        // Remove duplicate if exists
        history.remove(query)
        // Add to top
        history.add(0, query)
        // Trim
        val trimmed = history.take(MAX_HISTORY)
        prefs.edit().putString(KEY_HISTORY, trimmed.joinToString("|")).apply()
    }

    fun removeSearch(query: String) {
        val history = getHistory().toMutableList()
        if (history.remove(query)) {
            prefs.edit().putString(KEY_HISTORY, history.joinToString("|")).apply()
        }
    }
}
