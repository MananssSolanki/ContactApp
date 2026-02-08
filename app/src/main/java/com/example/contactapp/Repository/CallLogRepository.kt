package com.example.contactapp.Repository

import android.content.Context
import android.provider.CallLog
import com.example.contactapp.Model.CallLog as AppCallLog
import com.example.contactapp.Model.CallLogListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CallLogRepository(private val context: Context) {

    /**
     * Fetch call logs from system and group by date sections
     */
    suspend fun getCallLogs(): List<CallLogListItem> = withContext(Dispatchers.IO) {
        val callLogs = mutableListOf<AppCallLog>()

        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls._ID,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION
                ),
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(CallLog.Calls._ID)
                val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)

                while (it.moveToNext()) {
                    val id = if (idIndex != -1) it.getString(idIndex) else ""
                    val number = if (numberIndex != -1) it.getString(numberIndex) ?: "Unknown" else "Unknown"
                    val name = if (nameIndex != -1) it.getString(nameIndex) else null
                    val type = if (typeIndex != -1) it.getInt(typeIndex) else CallLog.Calls.INCOMING_TYPE
                    val date = if (dateIndex != -1) it.getLong(dateIndex) else 0L
                    val duration = if (durationIndex != -1) it.getLong(durationIndex) else 0L

                    val callType = when (type) {
                        CallLog.Calls.OUTGOING_TYPE -> AppCallLog.CallType.OUTGOING
                        CallLog.Calls.MISSED_TYPE -> AppCallLog.CallType.MISSED
                        else -> AppCallLog.CallType.INCOMING
                    }

                    val dateSection = getDateSection(date)

                    callLogs.add(
                        AppCallLog(
                            id = id,
                            phoneNumber = number,
                            contactName = name,
                            callType = callType,
                            timestamp = date,
                            duration = duration,
                            dateSection = dateSection
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Group by date sections
        groupByDateSections(callLogs)
    }

    /**
     * Group call logs by date sections with headers
     */
    private fun groupByDateSections(callLogs: List<AppCallLog>): List<CallLogListItem> {
        val result = mutableListOf<CallLogListItem>()
        var currentSection = ""

        callLogs.forEach { callLog ->
            if (callLog.dateSection != currentSection) {
                currentSection = callLog.dateSection
                result.add(CallLogListItem.Header(currentSection))
            }
            result.add(CallLogListItem.CallItem(callLog))
        }

        return result
    }

    /**
     * Determine date section (Today, Yesterday, or formatted date)
     */
    private fun getDateSection(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        val today = calendar.clone() as Calendar
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val yesterday = today.clone() as Calendar
        yesterday.add(Calendar.DAY_OF_YEAR, -1)

        val callDate = Calendar.getInstance()
        callDate.timeInMillis = timestamp

        return when {
            callDate.timeInMillis >= today.timeInMillis -> "Today"
            callDate.timeInMillis >= yesterday.timeInMillis -> "Yesterday"
            else -> {
                val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                dateFormat.format(Date(timestamp))
            }
        }
    }

    /**
     * Delete call log entry
     */
    suspend fun deleteCallLog(callLogId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val deleted = context.contentResolver.delete(
                CallLog.Calls.CONTENT_URI,
                "${CallLog.Calls._ID} = ?",
                arrayOf(callLogId)
            )
            deleted > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
