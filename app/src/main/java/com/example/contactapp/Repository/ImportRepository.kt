package com.example.contactapp.Repository

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.contactapp.Model.PhoneEntry
import com.example.contactapp.Model.ContactEnhanced
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class ImportRepository(private val context: Context) {

    data class ImportAccount(
        val name: String,
        val type: String,
        val iconRes: Int? = null,
        val isSim: Boolean = false,
        val simSlot: Int = 0
    )

    suspend fun getAvailableAccounts(includeSim: Boolean = true): List<ImportAccount> = withContext(Dispatchers.IO) {
        val accounts = mutableListOf<ImportAccount>()
        
        // 1. Phone storage
        accounts.add(ImportAccount("Phone", "Local", isSim = false))
        
        // 2. SIM cards
        if (includeSim) {
            // Check for SIM 1
            if (hasSimContacts(1)) {
                accounts.add(ImportAccount("SIM 1", "SIM", isSim = true, simSlot = 1))
            }
            // Check for SIM 2
            if (hasSimContacts(2)) {
                accounts.add(ImportAccount("SIM 2", "SIM", isSim = true, simSlot = 2))
            }
        }
        
        // 3. Google and other accounts
        val am = AccountManager.get(context)
        val deviceAccounts = am.accounts
        for (acc in deviceAccounts) {
            accounts.add(ImportAccount(acc.name, acc.type))
        }
        
        accounts
    }

    private fun hasSimContacts(slot: Int): Boolean {
        val uri = getSimUri(slot)
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use {
                it.count > 0
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun getSimUri(slot: Int): Uri {
        return if (slot == 2) {
            Uri.parse("content://icc/adn/subId/1") // SubId logic varies, but usually 0 and 1
        } else {
            Uri.parse("content://icc/adn")
        }
    }

    suspend fun fetchSimContacts(slot: Int): List<ContactEnhanced> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<ContactEnhanced>()
        val uri = getSimUri(slot)
        
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                // SIM column names can vary: 'name'/'tag' and 'number'/'emails'/'number'
                val nameIndex = cursor.getColumnIndex("name").let { if (it == -1) cursor.getColumnIndex("tag") else it }
                val numberIndex = cursor.getColumnIndex("number").let { if (it == -1) cursor.getColumnIndex("emails") else it }
                val idIndex = cursor.getColumnIndex("_id")
                
                while (cursor.moveToNext()) {
                    val name = if (nameIndex != -1) cursor.getString(nameIndex) ?: "Unknown" else "Contact ${cursor.position}"
                    val number = if (numberIndex != -1) cursor.getString(numberIndex) ?: "" else ""
                    val id = if (idIndex != -1) cursor.getString(idIndex) ?: "sim_${slot}_${cursor.position}"
                             else "sim_${slot}_${cursor.position}"
                    
                    if (number.isNotBlank()) {
                        contacts.add(
                            ContactEnhanced(
                                contactId = id,
                                name = name,
                                phoneNumber = number,
                                phones = listOf(PhoneEntry(number, "Mobile"))
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        contacts
    }

}
