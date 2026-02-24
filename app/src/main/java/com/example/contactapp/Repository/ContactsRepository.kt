package com.example.contactapp.Repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import com.example.contactapp.Model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsRepository(private val context: Context) {

    suspend fun getContacts(): List<ContactEnhanced> = withContext(Dispatchers.IO) {

        // Step 1 — base info
        data class Base(
            val id: String, val name: String, val photoUri: String?,
            val isFavorite: Boolean, val lastContacted: Long, val timesContacted: Int
        )
        val baseMap = LinkedHashMap<String, Base>()

        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.PHOTO_URI,
                ContactsContract.Contacts.STARRED,
                ContactsContract.Contacts.LAST_TIME_CONTACTED,
                ContactsContract.Contacts.TIMES_CONTACTED
            ),
            null, null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
        )?.use { c ->
            val iId    = c.getColumnIndex(ContactsContract.Contacts._ID)
            val iName  = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val iPhoto = c.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
            val iStar  = c.getColumnIndex(ContactsContract.Contacts.STARRED)
            val iLast  = c.getColumnIndex(ContactsContract.Contacts.LAST_TIME_CONTACTED)
            val iTimes = c.getColumnIndex(ContactsContract.Contacts.TIMES_CONTACTED)
            while (c.moveToNext()) {
                val id = c.getString(iId) ?: continue
                if (!baseMap.containsKey(id)) {
                    baseMap[id] = Base(
                        id, c.getString(iName) ?: "Unknown",
                        if (iPhoto != -1) c.getString(iPhoto) else null,
                        if (iStar  != -1) c.getInt(iStar) == 1 else false,
                        if (iLast  != -1) c.getLong(iLast) else 0L,
                        if (iTimes != -1) c.getInt(iTimes) else 0
                    )
                }
            }
        }

        if (baseMap.isEmpty()) return@withContext emptyList()

        // Step 2 — raw contact IDs
        val rawIdMap = HashMap<String, String>()
        context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts.CONTACT_ID, ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.DELETED}=0", null, null
        )?.use { c ->
            val iCid = c.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)
            val iRid = c.getColumnIndex(ContactsContract.RawContacts._ID)
            while (c.moveToNext()) {
                val cid = c.getString(iCid) ?: continue
                val rid = c.getString(iRid) ?: continue
                if (!rawIdMap.containsKey(cid)) rawIdMap[cid] = rid
            }
        }

        // Step 3 — all data rows in one query
        val phonesMap   = HashMap<String, MutableList<PhoneEntry>>()
        val emailsMap   = HashMap<String, MutableList<EmailEntry>>()
        val nameMap     = HashMap<String, Array<String?>>()   // [prefix,first,middle,last,suffix,phonetic]
        val nickMap     = HashMap<String, String>()
        val orgMap      = HashMap<String, Array<String?>>()   // [title,dept,company]
        val addrMap     = HashMap<String, MutableList<AddressEntry>>()
        val dateMap     = HashMap<String, MutableList<DateEntry>>()
        val relMap      = HashMap<String, MutableList<RelationshipEntry>>()
        val notesMap    = HashMap<String, String>()
        val webMap      = HashMap<String, MutableList<String>>()
        val imMap       = HashMap<String, MutableList<ImEntry>>()

        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            null, null, null, null
        )?.use { c ->
            val iCid  = c.getColumnIndex(ContactsContract.Data.CONTACT_ID)
            val iMime = c.getColumnIndex(ContactsContract.Data.MIMETYPE)
            while (c.moveToNext()) {
                val cid  = c.getString(iCid)  ?: continue
                val mime = c.getString(iMime) ?: continue
                if (!baseMap.containsKey(cid)) continue

                when (mime) {
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                        val num = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: continue
                        if (num.isBlank()) continue
                        val t   = c.getInt(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE))
                        val lbl = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)) ?: ""
                        phonesMap.getOrPut(cid) { mutableListOf() }.add(PhoneEntry(num, phoneTypeLabel(t, lbl)))
                    }
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                        val addr = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)) ?: continue
                        if (addr.isBlank()) continue
                        val t   = c.getInt(c.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE))
                        val lbl = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Email.LABEL)) ?: ""
                        emailsMap.getOrPut(cid) { mutableListOf() }.add(EmailEntry(addr, emailTypeLabel(t, lbl)))
                    }
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE -> {
                        if (!nameMap.containsKey(cid)) {
                            nameMap[cid] = arrayOf(
                                c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.PREFIX)),
                                c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)),
                                c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME)),
                                c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME)),
                                c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.SUFFIX)),
                                c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME))
                            )
                        }
                    }
                    ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE -> {
                        val n = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.NAME)) ?: continue
                        if (n.isNotBlank() && !nickMap.containsKey(cid)) nickMap[cid] = n
                    }
                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                        if (!orgMap.containsKey(cid)) {
                            orgMap[cid] = arrayOf(
                                c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE)),
                                c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Organization.DEPARTMENT)),
                                c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY))
                            )
                        }
                    }
                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                        val t   = c.getInt(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE))
                        val lbl = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.LABEL)) ?: ""
                        addrMap.getOrPut(cid) { mutableListOf() }.add(
                            AddressEntry(
                                street   = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET)) ?: "",
                                city     = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY)) ?: "",
                                state    = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION)) ?: "",
                                postcode = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE)) ?: "",
                                country  = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY)) ?: "",
                                typeLabel= addressTypeLabel(t, lbl)
                            )
                        )
                    }
                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE -> {
                        val start = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE)) ?: continue
                        val t     = c.getInt(c.getColumnIndex(ContactsContract.CommonDataKinds.Event.TYPE))
                        val label = when (t) {
                            ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY    -> "Birthday"
                            ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY -> "Anniversary"
                            else -> "Other"
                        }
                        val parts = start.replace("--","0-").split("-")
                        val year  = parts.getOrNull(0)?.toIntOrNull() ?: 0
                        val month = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        val day   = parts.getOrNull(2)?.toIntOrNull() ?: 0
                        dateMap.getOrPut(cid) { mutableListOf() }.add(DateEntry(year, month, day, label))
                    }
                    ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE -> {
                        val rName = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Relation.NAME)) ?: continue
                        val t     = c.getInt(c.getColumnIndex(ContactsContract.CommonDataKinds.Relation.TYPE))
                        val lbl   = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Relation.LABEL)) ?: ""
                        relMap.getOrPut(cid) { mutableListOf() }.add(RelationshipEntry(rName, relationTypeLabel(t, lbl)))
                    }
                    ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> {
                        val n = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE)) ?: continue
                        if (n.isNotBlank() && !notesMap.containsKey(cid)) notesMap[cid] = n
                    }
                    ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE -> {
                        val url = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Website.URL)) ?: continue
                        if (url.isNotBlank()) webMap.getOrPut(cid) { mutableListOf() }.add(url)
                    }
                    ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE -> {
                        val handle = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA)) ?: continue
                        if (handle.isBlank()) continue
                        val proto  = c.getInt(c.getColumnIndex(ContactsContract.CommonDataKinds.Im.PROTOCOL))
                        val custom = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL)) ?: ""
                        imMap.getOrPut(cid) { mutableListOf() }.add(ImEntry(handle, imProtocolLabel(proto, custom)))
                    }
                }
            }
        }

        // Step 4 — assemble
        val result = mutableListOf<ContactEnhanced>()
        for ((id, base) in baseMap) {
            val phones  = phonesMap[id] ?: emptyList()
            val emails  = emailsMap[id] ?: emptyList()
            val nameArr = nameMap[id]
            val orgArr  = orgMap[id]
            result.add(
                ContactEnhanced(
                    contactId         = id,
                    rawContactId      = rawIdMap[id],
                    name              = base.name,
                    namePrefix        = nameArr?.get(0),
                    firstName         = nameArr?.get(1),
                    middleName        = nameArr?.get(2),
                    lastName          = nameArr?.get(3),
                    nameSuffix        = nameArr?.get(4),
                    phoneticName      = nameArr?.get(5),
                    nickname          = nickMap[id],
                    phones            = phones,
                    emails            = emails,
                    photoUri          = base.photoUri,
                    jobTitle          = orgArr?.get(0),
                    department        = orgArr?.get(1),
                    company           = orgArr?.get(2),
                    addresses         = addrMap[id]  ?: emptyList(),
                    importantDates    = dateMap[id]  ?: emptyList(),
                    relationships     = relMap[id]   ?: emptyList(),
                    notes             = notesMap[id],
                    websites          = webMap[id]   ?: emptyList(),
                    imAccounts        = imMap[id]    ?: emptyList(),
                    isFavorite        = base.isFavorite,
                    lastContactedTime = base.lastContacted,
                    timesContacted    = base.timesContacted,
                    // Legacy compat
                    phoneNumber       = phones.firstOrNull()?.number ?: "",
                    email             = emails.firstOrNull()?.address,
                    additionalPhones  = phones.drop(1).map { it.number },
                    additionalEmails  = emails.drop(1).map { it.address }
                )
            )
        }
        return@withContext result
    }

    suspend fun getContact(contactId: String): ContactEnhanced? = withContext(Dispatchers.IO) {
        // Step 1 — base info
        data class Base(
            val id: String, val name: String, val photoUri: String?,
            val isFavorite: Boolean, val lastContacted: Long, val timesContacted: Int
        )
        var base: Base? = null

        context.contentResolver.query(
            Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId),
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.PHOTO_URI,
                ContactsContract.Contacts.STARRED,
                ContactsContract.Contacts.LAST_TIME_CONTACTED,
                ContactsContract.Contacts.TIMES_CONTACTED
            ),
            null, null, null
        )?.use { c ->
            if (c.moveToFirst()) {
                val iId = c.getColumnIndex(ContactsContract.Contacts._ID)
                val iName = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                val iPhoto = c.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
                val iStar = c.getColumnIndex(ContactsContract.Contacts.STARRED)
                val iLast = c.getColumnIndex(ContactsContract.Contacts.LAST_TIME_CONTACTED)
                val iTimes = c.getColumnIndex(ContactsContract.Contacts.TIMES_CONTACTED)

                base = Base(
                    c.getString(iId) ?: return@use,
                    c.getString(iName) ?: "Unknown",
                    if (iPhoto != -1) c.getString(iPhoto) else null,
                    if (iStar != -1) c.getInt(iStar) == 1 else false,
                    if (iLast != -1) c.getLong(iLast) else 0L,
                    if (iTimes != -1) c.getInt(iTimes) else 0
                )
            }
        }

        val b = base ?: return@withContext null

        // Step 2 — raw contact ID
        var rawContactId: String? = null
        context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.CONTACT_ID}=? AND ${ContactsContract.RawContacts.DELETED}=0",
            arrayOf(contactId), null
        )?.use { c ->
            if (c.moveToFirst()) {
                rawContactId = c.getString(0)
            }
        }

        // Step 3 — data rows
        val phones = mutableListOf<PhoneEntry>()
        val emails = mutableListOf<EmailEntry>()
        var nameArr: Array<String?>? = null
        var nickname: String? = null
        var orgArr: Array<String?>? = null
        val addresses = mutableListOf<AddressEntry>()
        val dates = mutableListOf<DateEntry>()
        val relationships = mutableListOf<RelationshipEntry>()
        var notes: String? = null
        val websites = mutableListOf<String>()
        val imAccounts = mutableListOf<ImEntry>()

        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            null,
            "${ContactsContract.Data.CONTACT_ID}=?",
            arrayOf(contactId),
            null
        )?.use { c ->
            val iMime = c.getColumnIndex(ContactsContract.Data.MIMETYPE)
            while (c.moveToNext()) {
                val mime = c.getString(iMime) ?: continue
                when (mime) {
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                        val num = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: continue
                        if (num.isBlank()) continue
                        val t = c.getInt(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE))
                        val lbl = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)) ?: ""
                        phones.add(PhoneEntry(num, phoneTypeLabel(t, lbl)))
                    }
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                        val addr = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)) ?: continue
                        if (addr.isBlank()) continue
                        val t = c.getInt(c.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE))
                        val lbl = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Email.LABEL)) ?: ""
                        emails.add(EmailEntry(addr, emailTypeLabel(t, lbl)))
                    }
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE -> {
                        nameArr = arrayOf(
                            c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.PREFIX)),
                            c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)),
                            c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME)),
                            c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME)),
                            c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.SUFFIX)),
                            c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME))
                        )
                    }
                    ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE -> {
                        nickname = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.NAME))
                    }
                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                        orgArr = arrayOf(
                            c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE)),
                            c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Organization.DEPARTMENT)),
                            c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY))
                        )
                    }
                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                        val t = c.getInt(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE))
                        val lbl = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.LABEL)) ?: ""
                        addresses.add(
                            AddressEntry(
                                street = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET)) ?: "",
                                city = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY)) ?: "",
                                state = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION)) ?: "",
                                postcode = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE)) ?: "",
                                country = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY)) ?: "",
                                typeLabel = addressTypeLabel(t, lbl)
                            )
                        )
                    }
                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE -> {
                        val start = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE)) ?: continue
                        val t = c.getInt(c.getColumnIndex(ContactsContract.CommonDataKinds.Event.TYPE))
                        val label = when (t) {
                            ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY -> "Birthday"
                            ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY -> "Anniversary"
                            else -> "Other"
                        }
                        val parts = start.replace("--", "0-").split("-")
                        val year = parts.getOrNull(0)?.toIntOrNull() ?: 0
                        val month = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        val day = parts.getOrNull(2)?.toIntOrNull() ?: 0
                        dates.add(DateEntry(year, month, day, label))
                    }
                    ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE -> {
                        val rName = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Relation.NAME)) ?: continue
                        val t = c.getInt(c.getColumnIndex(ContactsContract.CommonDataKinds.Relation.TYPE))
                        val lbl = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Relation.LABEL)) ?: ""
                        relationships.add(RelationshipEntry(rName, relationTypeLabel(t, lbl)))
                    }
                    ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> {
                        notes = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE))
                    }
                    ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE -> {
                        val url = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Website.URL)) ?: continue
                        if (url.isNotBlank()) websites.add(url)
                    }
                    ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE -> {
                        val handle = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA)) ?: continue
                        if (handle.isBlank()) continue
                        val proto = c.getInt(c.getColumnIndex(ContactsContract.CommonDataKinds.Im.PROTOCOL))
                        val custom = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL)) ?: ""
                        imAccounts.add(ImEntry(handle, imProtocolLabel(proto, custom)))
                    }
                }
            }
        }

        // Assembly
        ContactEnhanced(
            contactId = b.id,
            rawContactId = rawContactId,
            name = b.name,
            namePrefix = nameArr?.get(0),
            firstName = nameArr?.get(1),
            middleName = nameArr?.get(2),
            lastName = nameArr?.get(3),
            nameSuffix = nameArr?.get(4),
            phoneticName = nameArr?.get(5),
            nickname = nickname,
            phones = phones,
            emails = emails,
            photoUri = b.photoUri,
            jobTitle = orgArr?.get(0),
            department = orgArr?.get(1),
            company = orgArr?.get(2),
            addresses = addresses,
            importantDates = dates,
            relationships = relationships,
            notes = notes,
            websites = websites,
            imAccounts = imAccounts,
            isFavorite = b.isFavorite,
            lastContactedTime = b.lastContacted,
            timesContacted = b.timesContacted,
            // Legacy compat
            phoneNumber = phones.firstOrNull()?.number ?: "",
            email = emails.firstOrNull()?.address,
            additionalPhones = phones.drop(1).map { it.number },
            additionalEmails = emails.drop(1).map { it.address }
        )
    }


    suspend fun toggleFavorite(contactId: String, setFavorite: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.update(
                Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId),
                ContentValues().apply { put(ContactsContract.Contacts.STARRED, if (setFavorite) 1 else 0) },
                null, null
            ) > 0
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    suspend fun deleteCallHistory(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.delete(
                android.provider.CallLog.Calls.CONTENT_URI,
                "${android.provider.CallLog.Calls.NUMBER}=?",
                arrayOf(phoneNumber)
            ) >= 0
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    fun addContactIntent(): Intent = Intent(Intent.ACTION_INSERT).apply {
        type = ContactsContract.Contacts.CONTENT_TYPE
    }

    // ── Type label helpers ────────────────────────────────────────────────────

    fun phoneTypeLabel(type: Int, custom: String) = when (type) {
        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE   -> "Mobile"
        ContactsContract.CommonDataKinds.Phone.TYPE_HOME     -> "Home"
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK     -> "Work"
        ContactsContract.CommonDataKinds.Phone.TYPE_MAIN     -> "Main"
        ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> "Work Fax"
        ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME -> "Home Fax"
        ContactsContract.CommonDataKinds.Phone.TYPE_PAGER    -> "Pager"
        ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM   -> custom.ifBlank { "Custom" }
        else -> "Other"
    }

    fun emailTypeLabel(type: Int, custom: String) = when (type) {
        ContactsContract.CommonDataKinds.Email.TYPE_HOME   -> "Home"
        ContactsContract.CommonDataKinds.Email.TYPE_WORK   -> "Work"
        ContactsContract.CommonDataKinds.Email.TYPE_OTHER  -> "Other"
        ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM -> custom.ifBlank { "Custom" }
        else -> "Home"
    }

    fun addressTypeLabel(type: Int, custom: String) = when (type) {
        ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME   -> "Home"
        ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK   -> "Work"
        ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER  -> "Other"
        ContactsContract.CommonDataKinds.StructuredPostal.TYPE_CUSTOM -> custom.ifBlank { "Custom" }
        else -> "Home"
    }

    fun relationTypeLabel(type: Int, custom: String) = when (type) {
        ContactsContract.CommonDataKinds.Relation.TYPE_PARENT   -> "Parent"
        ContactsContract.CommonDataKinds.Relation.TYPE_MOTHER   -> "Mother"
        ContactsContract.CommonDataKinds.Relation.TYPE_FATHER   -> "Father"
        ContactsContract.CommonDataKinds.Relation.TYPE_BROTHER  -> "Brother"
        ContactsContract.CommonDataKinds.Relation.TYPE_SISTER   -> "Sister"
        ContactsContract.CommonDataKinds.Relation.TYPE_SPOUSE   -> "Spouse"
        ContactsContract.CommonDataKinds.Relation.TYPE_CHILD    -> "Child"
        ContactsContract.CommonDataKinds.Relation.TYPE_FRIEND   -> "Friend"
        ContactsContract.CommonDataKinds.Relation.TYPE_RELATIVE -> "Relative"
        ContactsContract.CommonDataKinds.Relation.TYPE_CUSTOM   -> custom.ifBlank { "Custom" }
        else -> "Other"
    }

    fun imProtocolLabel(proto: Int, custom: String) = when (proto) {
        ContactsContract.CommonDataKinds.Im.PROTOCOL_GOOGLE_TALK -> "Hangouts"
        ContactsContract.CommonDataKinds.Im.PROTOCOL_QQ          -> "QQ"
        ContactsContract.CommonDataKinds.Im.PROTOCOL_SKYPE        -> "Skype"
        ContactsContract.CommonDataKinds.Im.PROTOCOL_YAHOO        -> "Yahoo"
        ContactsContract.CommonDataKinds.Im.PROTOCOL_AIM          -> "AIM"
        ContactsContract.CommonDataKinds.Im.PROTOCOL_MSN          -> "MSN"
        ContactsContract.CommonDataKinds.Im.PROTOCOL_ICQ          -> "ICQ"
        ContactsContract.CommonDataKinds.Im.PROTOCOL_JABBER       -> "Jabber"
        ContactsContract.CommonDataKinds.Im.PROTOCOL_NETMEETING   -> "NetMeeting"
        ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM       -> custom.ifBlank { "Custom" }
        else -> "Other"
    }
}