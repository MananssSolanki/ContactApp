package com.example.contactapp.Model

data class CallLog(
    val id: String,
    val phoneNumber: String,
    val contactName: String?,
    val callType: CallType,
    val timestamp: Long,
    val duration: Long, // in seconds
    val dateSection: String // "Today", "Yesterday", or formatted date
) {
    enum class CallType {
        INCOMING,
        OUTGOING,
        MISSED
    }
    
    /**
     * Format duration as MM:SS
     */
    fun getFormattedDuration(): String {
        if (duration == 0L) return "0:00"
        
        val minutes = duration / 60
        val seconds = duration % 60
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }
    
    /**
     * Format timestamp as time (e.g., "10:30 AM")
     */
    fun getFormattedTime(): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        
        val hour = calendar.get(java.util.Calendar.HOUR)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        val amPm = if (calendar.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM) "AM" else "PM"
        
        return "${if (hour == 0) 12 else hour}:${minute.toString().padStart(2, '0')} $amPm"
    }
}

/**
 * Wrapper class to support both call log items and date headers
 */
sealed class CallLogListItem {
    data class Header(val dateSection: String) : CallLogListItem()
    data class CallItem(val callLog: CallLog) : CallLogListItem()
}
