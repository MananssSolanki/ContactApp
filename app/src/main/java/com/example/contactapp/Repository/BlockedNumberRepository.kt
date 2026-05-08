package com.example.contactapp.Repository

import android.content.Context
import android.provider.BlockedNumberContract
import com.example.contactapp.database.AppDatabase
import com.example.contactapp.database.BlockedNumberEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BlockedNumberRepository(private val context: Context) {

    /**
     * Block a phone number by inserting into the Android System BlockedNumberContract.
     */
    suspend fun blockNumber(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val normalized = normalizeNumber(phoneNumber)
            if (normalized.isBlank()) return@withContext false

            if (BlockedNumberContract.canCurrentUserBlockNumbers(context)) {
                val values = android.content.ContentValues().apply {
                    put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, normalized)
                }
                val uri = context.contentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values)
                uri != null
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Unblock a phone number by removing from the Android System BlockedNumberContract.
     */
    suspend fun unblockNumber(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val normalized = normalizeNumber(phoneNumber)
            if (normalized.isBlank()) return@withContext false

            if (BlockedNumberContract.canCurrentUserBlockNumbers(context)) {
                val uri = BlockedNumberContract.BlockedNumbers.CONTENT_URI
                val selection = "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?"
                val deleted = context.contentResolver.delete(uri, selection, arrayOf(normalized))
                deleted > 0
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Check if number exists in the Android System BlockedNumberContract.
     */
    suspend fun isBlocked(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val normalized = normalizeNumber(phoneNumber)
            if (normalized.isBlank()) return@withContext false

            if (BlockedNumberContract.canCurrentUserBlockNumbers(context)) {
                val cursor = context.contentResolver.query(
                    BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                    arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ID),
                    "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?",
                    arrayOf(normalized),
                    null
                )
                val blocked = cursor?.moveToFirst() == true
                cursor?.close()
                blocked
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Only supports blocking if it's the default dialer / has permission. */
    fun canBlockNumbers(): Boolean {
        return BlockedNumberContract.canCurrentUserBlockNumbers(context)
    }

    private fun normalizeNumber(phone: String): String {
        val digits = phone.replace(Regex("[^0-9+]"), "").trim()
        return digits.ifBlank { phone.trim() }
    }
}