package com.example.contactapp.Model

data class CallLog(
    val id: String,
    val phoneNumber: String,
    val contactName: String?,
    val callType: CallType,
    val timestamp: Long,
    val duration: Long, // in seconds
    val dateSection: String, // "Today", "Yesterday", or formatted date
    val photoUri: String? = null
) {
    enum class CallType {
        INCOMING,
        OUTGOING,
        MISSED
    }

    /** Format duration as M:SS (e.g. "2:05") */
    fun getFormattedDuration(): String {
        if (duration == 0L) return "0:00"
        val minutes = duration / 60
        val seconds = duration % 60
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }

    /** Format timestamp as time in 12-hour format (e.g. "10:30 AM") */
    fun getFormattedTime(): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val hour = calendar.get(java.util.Calendar.HOUR).let { if (it == 0) 12 else it }
        val minute = calendar.get(java.util.Calendar.MINUTE)
        val amPm = if (calendar.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM) "AM" else "PM"
        return "$hour:${minute.toString().padStart(2, '0')} $amPm"
    }

    /** Human-readable duration string for list display */
    fun getFormattedDurationLong(): String {
        if (duration == 0L) return ""
        val mins = duration / 60
        val secs = duration % 60
        return when {
            mins > 0 && secs > 0 -> "${mins}m ${secs}s"
            mins > 0 -> "${mins}m"
            else -> "${secs}s"
        }
    }
}

/** Sealed wrapper class to support both call log items and date section headers */
sealed class CallLogListItem {
    data class Header(val dateSection: String) : CallLogListItem()
    data class CallItem(val callLog: CallLog) : CallLogListItem()
}