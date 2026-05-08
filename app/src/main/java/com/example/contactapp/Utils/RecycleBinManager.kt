package com.example.contactapp.Utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.example.contactapp.Model.DeletedContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecycleBinManager(private val context: Context) {

    // Helper URI that acts as a SyncAdapter to see/modify DELETED items
    private val rawContactsSyncUri: Uri by lazy {
        ContactsContract.RawContacts.CONTENT_URI.buildUpon()
            .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
            .build()
    }
    
    private val dataSyncUri: Uri by lazy {
        ContactsContract.Data.CONTENT_URI.buildUpon()
            .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
            .build()
    }

    /**
     * Retrieve all contacts currently marked DELETED=1 in the RawContacts table.
     */
    suspend fun getDeletedContacts(): List<DeletedContact> = withContext(Dispatchers.IO) {
        val deletedList = mutableListOf<DeletedContact>()

        // 1. Query RawContacts for DELETED = 1
        val selection = "${ContactsContract.RawContacts.DELETED} = 1"
        val projection = arrayOf(
            ContactsContract.RawContacts._ID,
            ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.RawContacts.ACCOUNT_NAME,
            ContactsContract.RawContacts.ACCOUNT_TYPE
        )

        val rawContactsMap = mutableMapOf<String, DeletedContactBuilder>()

        try {
            context.contentResolver.query(
                rawContactsSyncUri,
                projection,
                selection,
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts._ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY)
                val accNameIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)
                val accTypeIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)

                while (cursor.moveToNext()) {
                    val rawId = cursor.getString(idIdx)
                    val name = if (nameIdx != -1) cursor.getString(nameIdx) else null
                    val accName = if (accNameIdx != -1) cursor.getString(accNameIdx) else null
                    val accType = if (accTypeIdx != -1) cursor.getString(accTypeIdx) else null

                    rawContactsMap[rawId] = DeletedContactBuilder(
                        rawContactId = rawId,
                        name = name.takeIf { !it.isNullOrBlank() } ?: "Unknown",
                        accountName = accName,
                        accountType = accType
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }

        if (rawContactsMap.isEmpty()) return@withContext emptyList()

        // 2. Query Data table with SyncAdapter URI to get Phone/Email for those Raw IDs
        val rawIdsStr = rawContactsMap.keys.joinToString(",") { "?" }
        val dataSelection = "${ContactsContract.Data.RAW_CONTACT_ID} IN ($rawIdsStr)"
        val dataSelectionArgs = rawContactsMap.keys.toTypedArray()

        try {
            context.contentResolver.query(
                dataSyncUri,
                null,
                dataSelection,
                dataSelectionArgs,
                null
            )?.use { cursor ->
                val rawIdIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data.RAW_CONTACT_ID)
                val mimeTypeIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)

                while (cursor.moveToNext()) {
                    val rawId = cursor.getString(rawIdIdx)
                    val builder = rawContactsMap[rawId] ?: continue
                    val mimeType = cursor.getString(mimeTypeIdx)

                    when (mimeType) {
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (numIdx != -1) {
                                val num = cursor.getString(numIdx)
                                if (!num.isNullOrBlank() && builder.phone == null) builder.phone = num
                            }
                        }
                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                            val emailIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                            if (emailIdx != -1) {
                                val email = cursor.getString(emailIdx)
                                if (!email.isNullOrBlank() && builder.email == null) builder.email = email
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Build list
        rawContactsMap.values.forEach { b ->
            deletedList.add(
                DeletedContact(
                    id = b.rawContactId.toLongOrNull() ?: 0L,
                    contactId = b.rawContactId, // We use this as the raw_contact_id for restore/delete
                    name = b.name,
                    phoneNumber = b.phone ?: "No Number",
                    email = b.email,
                    photoUri = null,
                    accountName = b.accountName,
                    accountType = b.accountType
                )
            )
        }

        deletedList
    }

    /**
     * Restore a contact by updating DELETED = 0 on its RawContact row.
     */
    suspend fun restoreContact(contact: DeletedContact): Boolean = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(ContactsContract.RawContacts.DELETED, 0)
        }
        val selection = "${ContactsContract.RawContacts._ID} = ?"
        val selectionArgs = arrayOf(contact.contactId)

        try {
            val rows = context.contentResolver.update(rawContactsSyncUri, values, selection, selectionArgs)
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Permanently completely delete this RawContact using the SyncAdapter URI.
     */
    suspend fun permanentlyDelete(contact: DeletedContact): Boolean = withContext(Dispatchers.IO) {
        val selection = "${ContactsContract.RawContacts._ID} = ?"
        val selectionArgs = arrayOf(contact.contactId)

        try {
            val rows = context.contentResolver.delete(rawContactsSyncUri, selection, selectionArgs)
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Empty entire bin by deleting ALL rows where DELETED = 1.
     */
    suspend fun emptyBin(): Boolean = withContext(Dispatchers.IO) {
        val selection = "${ContactsContract.RawContacts.DELETED} = 1"
        try {
            context.contentResolver.delete(rawContactsSyncUri, selection, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private data class DeletedContactBuilder(
        val rawContactId: String,
        var name: String,
        var accountName: String?,
        var accountType: String?,
        var phone: String? = null,
        var email: String? = null
    )
}