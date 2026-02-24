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
     * Fetch call logs from the system provider and group them by date sections.
     * @param phoneNumber Optional number to filter (for contact detail screen).
     */
    suspend fun getCallLogs(phoneNumber: String? = null): List<CallLogListItem> = withContext(Dispatchers.IO) {
        val callLogs = mutableListOf<AppCallLog>()

        try {
            val selection = if (phoneNumber != null) "${CallLog.Calls.NUMBER} LIKE ?" else null
            val selectionArgs = if (phoneNumber != null) arrayOf("%${phoneNumber.replace(Regex("[^0-9]"), "")}%") else null

            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls._ID,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.CACHED_PHOTO_URI
                ),
                selection,
                selectionArgs,
                "${CallLog.Calls.DATE} DESC"
            ) ?: return@withContext emptyList()

            cursor.use {
                val idIndex = it.getColumnIndexOrThrow(CallLog.Calls._ID)
                val numberIndex = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIndex = it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val dateIndex = it.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val durationIndex = it.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                val photoIndex = it.getColumnIndex(CallLog.Calls.CACHED_PHOTO_URI)

                while (it.moveToNext()) {
                    val id = it.getString(idIndex) ?: continue
                    val number = it.getString(numberIndex) ?: "Unknown"
                    val name = if (nameIndex != -1) it.getString(nameIndex)?.takeIf { n -> n.isNotBlank() } else null
                    val type = it.getInt(typeIndex)
                    val date = it.getLong(dateIndex)
                    val duration = it.getLong(durationIndex)
                    val photo = if (photoIndex != -1) it.getString(photoIndex) else null

                    val callType = when (type) {
                        CallLog.Calls.OUTGOING_TYPE -> AppCallLog.CallType.OUTGOING
                        CallLog.Calls.MISSED_TYPE -> AppCallLog.CallType.MISSED
                        else -> AppCallLog.CallType.INCOMING
                    }

                    callLogs.add(
                        AppCallLog(
                            id = id,
                            phoneNumber = number,
                            contactName = name,
                            callType = callType,
                            timestamp = date,
                            duration = duration,
                            dateSection = getDateSection(date),
                            photoUri = photo
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        groupByDateSections(callLogs)
    }

    /** Group a flat list of call logs into sectioned list with Header items. */
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

    private fun getDateSection(timestamp: Long): String {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val callDate = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            callDate >= today -> "Today"
            callDate >= yesterday -> "Yesterday"
            else -> SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    /**
     * Delete multiple call logs by their system IDs.
     * FIX: Was using bulk IN() query — now uses proper per-item deletion with ContentUris
     * to ensure system call log provider removes them reliably.
     */
    suspend fun deleteCallLogs(ids: List<String>): Boolean = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext true
        var allSuccess = true
        try {
            // Some ROMs need individual deletes via ContentUris for reliability
            ids.forEach { id ->
                val uri = android.content.ContentUris.withAppendedId(CallLog.Calls.CONTENT_URI, id.toLong())
                val deleted = context.contentResolver.delete(uri, null, null)
                if (deleted < 1) allSuccess = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: bulk delete
            try {
                val selection = "${CallLog.Calls._ID} IN (${ids.joinToString(",") { "?" }})"
                context.contentResolver.delete(CallLog.Calls.CONTENT_URI, selection, ids.toTypedArray())
            } catch (e2: Exception) {
                e2.printStackTrace()
                return@withContext false
            }
        }
        allSuccess
    }
}