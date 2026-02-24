package com.example.contactapp.Repository

import android.content.Context
import com.example.contactapp.database.AppDatabase
import com.example.contactapp.database.BlockedNumberEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BlockedNumberRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).blockedNumberDao()

    /**
     * Block a phone number by inserting into Room blocked_numbers table.
     * Avoids duplicates via IGNORE conflict strategy.
     * @return true if inserted or already blocked, false on error.
     */
    suspend fun blockNumber(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val normalized = normalizeNumber(phoneNumber)
            if (normalized.isBlank()) return@withContext false
            val rowId = dao.insert(BlockedNumberEntity(phoneNumber = normalized))
            // rowId == -1 means duplicate (IGNORE), we still consider it success
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Unblock a phone number by removing from Room table.
     */
    suspend fun unblockNumber(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val normalized = normalizeNumber(phoneNumber)
            dao.deleteByPhoneNumber(normalized) > 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if number exists in blocked_numbers table.
     */
    suspend fun isBlocked(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val normalized = normalizeNumber(phoneNumber)
            dao.isBlocked(normalized)
        } catch (e: Exception) {
            false
        }
    }

    /** App always supports blocking (Room table). */
    fun canBlockNumbers(): Boolean = true

    private fun normalizeNumber(phone: String): String =
        phone.replace(Regex("[^0-9+]"), "").ifBlank { phone }
}
