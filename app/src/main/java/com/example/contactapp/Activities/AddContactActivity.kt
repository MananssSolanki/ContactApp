package com.example.contactapp.Activities

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ContentProviderOperation
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.contactapp.Model.*
import com.example.contactapp.R
import com.example.contactapp.databinding.ActivityAddContactBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import android.accounts.Account
import android.accounts.AccountManager
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import com.bumptech.glide.Glide
import java.io.ByteArrayOutputStream

class AddContactActivity : AppCompatActivity() {

    private lateinit var b: ActivityAddContactBinding
    private var existingContact: ContactEnhanced? = null
    private var selectedImageUri: Uri? = null
    private var selectedImageBytes: ByteArray? = null

    private val pickMedia = registerForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            selectedImageBytes = uriToBytes(uri)
            Glide.with(this).load(uri).into(b.ivAvatar)
        }
    }

    // Track every dynamic row view
    private val phoneRows        = mutableListOf<View>()
    private val emailRows        = mutableListOf<View>()
    private val addressRows      = mutableListOf<View>()
    private val dateRows         = mutableListOf<View>()
    private val relationRows     = mutableListOf<View>()
    private val websiteRows      = mutableListOf<View>()
    private val imRows           = mutableListOf<View>()

    // Mutable date holder stored in row.tag
    private data class SelDate(var year: Int = 0, var month: Int = 0, var day: Int = 0)

    private data class AccountData(val name: String, val type: String, val email: String)
    private var selectedAccount: AccountData? = null
    private var availableAccounts = mutableListOf<AccountData>()

    private val phoneTypes    = arrayOf("Mobile","Home","Work","Main","Work Fax","Home Fax","Pager","Other")
    private val emailTypes    = arrayOf("Home","Work","Other")
    private val addrTypes     = arrayOf("Home","Work","Other")
    private val dateTypes     = arrayOf("Birthday","Anniversary","Other")
    private val relTypes      = arrayOf("Parent","Mother","Father","Brother","Sister","Spouse","Child","Friend","Relative","Other")
    private val imProtos      = arrayOf("Hangouts","QQ","Skype","Yahoo","AIM","MSN","ICQ","Jabber","NetMeeting")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAddContactBinding.inflate(layoutInflater)
        setContentView(b.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        existingContact = intent.getParcelableExtra("CONTACT_DATA")

        if (existingContact != null) {
            supportActionBar?.title = "Edit Contact"
            b.btnSave.text = "Update"
            prefill(existingContact!!)
        } else {
            supportActionBar?.title = "New Contact"
            b.btnSave.text = "Save"
            addPhoneRow()
            addEmailRow()
        }

        // Name expand/collapse
        b.layoutNameExpand.setOnClickListener {
            val show = b.expandedNameFields.visibility != View.VISIBLE
            b.expandedNameFields.visibility = if (show) View.VISIBLE else View.GONE
            b.ivNameArrow.rotation = if (show) 180f else 0f
        }

        b.btnAddPhone.setOnClickListener        { addPhoneRow() }
        b.btnAddEmail.setOnClickListener        { addEmailRow() }
        b.btnAddAddress.setOnClickListener      { addAddressRow() }
        b.btnAddDate.setOnClickListener         { addDateRow() }
        b.btnAddRelationship.setOnClickListener { addRelationRow() }
        b.btnAddWebsite.setOnClickListener      { addWebsiteRow() }
        b.btnAddIm.setOnClickListener           { addImRow() }

        b.btnSave.setOnClickListener   { onSave() }
        b.btnCancel.setOnClickListener { finish() }

        loadAccounts()
        b.layoutAccount.setOnClickListener { showAccountSelectionDialog() }

        b.ivAvatar.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        }
    }

    private fun loadAccounts() {
        val am = AccountManager.get(this)
        val accounts = am.accounts
        availableAccounts.clear()
        
        // Add "Phone" / "Local" option
        availableAccounts.add(AccountData("Phone", "Local", "Device only"))
        
        for (acc in accounts) {
            availableAccounts.add(AccountData(acc.name, acc.type, acc.name))
        }
        
        // Default selection: Phone if no others, or the first real account
        if (availableAccounts.size > 1) {
            updateSelectedAccount(availableAccounts[1])
        } else {
            updateSelectedAccount(availableAccounts[0])
        }
    }

    private fun showAccountSelectionDialog() {
        val names = availableAccounts.map { "${it.name} (${it.type})" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Save contact to")
            .setItems(names) { _, i ->
                updateSelectedAccount(availableAccounts[i])
            }
            .show()
    }

    private fun updateSelectedAccount(acc: AccountData) {
        selectedAccount = acc
        b.tvAccountName.text = acc.name
        b.tvAccountEmail.text = acc.email
        // Simple heuristic for icon
        val iconRes = when {
            acc.type.contains("google", ignoreCase = true) -> R.drawable.ic_launcher_background // Should use a google icon if available
            acc.type == "Local" -> android.R.drawable.ic_menu_call
            else -> android.R.drawable.ic_menu_myplaces
        }
        b.ivAccountIcon.setImageResource(iconRes)
    }

    // ── Pre-fill all fields in edit mode ─────────────────────────────────────

    private fun prefill(c: ContactEnhanced) {
        b.etDisplayName.setText(c.name)
        b.etNamePrefix.setText(c.namePrefix   ?: "")
        b.etFirstName.setText(c.firstName     ?: "")
        b.etMiddleName.setText(c.middleName   ?: "")
        b.etLastName.setText(c.lastName       ?: "")
        b.etNameSuffix.setText(c.nameSuffix   ?: "")
        b.etPhoneticName.setText(c.phoneticName ?: "")
        b.etNickname.setText(c.nickname       ?: "")
        b.etJobTitle.setText(c.jobTitle       ?: "")
        b.etDepartment.setText(c.department   ?: "")
        b.etCompany.setText(c.company         ?: "")
        b.etNotes.setText(c.notes             ?: "")

        if (c.photoUri != null) {
            Glide.with(this)
                .load(Uri.parse(c.photoUri))
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(b.ivAvatar)
        } else {
            b.ivAvatar.setImageResource(R.drawable.ic_launcher_foreground)
        }

        if (c.phones.isEmpty()) addPhoneRow() else c.phones.forEach { addPhoneRow(it.number, it.typeLabel) }
        if (c.emails.isEmpty()) addEmailRow() else c.emails.forEach { addEmailRow(it.address, it.typeLabel) }
        c.addresses.forEach      { addAddressRow(it) }
        c.importantDates.forEach { addDateRow(it) }
        c.relationships.forEach  { addRelationRow(it.name, it.typeLabel) }
        c.websites.forEach       { addWebsiteRow(it) }
        c.imAccounts.forEach     { addImRow(it.account, it.protocol) }

        // Fetch account info for existing contact
        lifecycleScope.launch(Dispatchers.IO) {
            val raw = getRawInfo(c.contactId)
            withContext(Dispatchers.Main) {
                raw?.let { updateSelectedAccount(it) }
                // Disable account selection in edit mode
                b.layoutAccount.isClickable = false
                b.ivAccountArrow.visibility = View.GONE
            }
        }
    }

    private fun getRawInfo(contactId: String): AccountData? = try {
        contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts.ACCOUNT_NAME, ContactsContract.RawContacts.ACCOUNT_TYPE),
            "${ContactsContract.RawContacts.CONTACT_ID}=? AND ${ContactsContract.RawContacts.DELETED}=0",
            arrayOf(contactId), null
        )?.use { 
            if (it.moveToFirst()) {
                val name = it.getString(0) ?: "Phone"
                val type = it.getString(1) ?: "Local"
                AccountData(name, type, if (type == "Local") "Device only" else name)
            } else null 
        }
    } catch (e: Exception) { e.printStackTrace(); null }

    // ── Row builders ─────────────────────────────────────────────────────────

    private fun addPhoneRow(number: String = "", type: String = "Mobile") {
        val row = inflate(R.layout.row_phone)
        row.find<EditText>(R.id.etPhone).setText(number)
        row.find<TextView>(R.id.tvPhoneType).also { tv ->
            tv.text = type
            tv.setOnClickListener { pick(phoneTypes, tv) }
        }
        row.find<ImageButton>(R.id.btnRemovePhone).setOnClickListener { remove(row, b.phoneContainer, phoneRows) }
        b.phoneContainer.addView(row); phoneRows.add(row)
    }

    private fun addEmailRow(address: String = "", type: String = "Home") {
        val row = inflate(R.layout.row_email)
        row.find<EditText>(R.id.etEmail).setText(address)
        row.find<TextView>(R.id.tvEmailType).also { tv ->
            tv.text = type
            tv.setOnClickListener { pick(emailTypes, tv) }
        }
        row.find<ImageButton>(R.id.btnRemoveEmail).setOnClickListener { remove(row, b.emailContainer, emailRows) }
        b.emailContainer.addView(row); emailRows.add(row)
    }

    private fun addAddressRow(entry: AddressEntry? = null) {
        val row = inflate(R.layout.row_address)
        entry?.let {
            row.find<TextView>(R.id.tvAddressType).text = it.typeLabel
            row.find<EditText>(R.id.etStreet).setText(it.street)
            row.find<EditText>(R.id.etCity).setText(it.city)
            row.find<EditText>(R.id.etState).setText(it.state)
            row.find<EditText>(R.id.etPostcode).setText(it.postcode)
            row.find<EditText>(R.id.etCountry).setText(it.country)
        }
        row.find<TextView>(R.id.tvAddressType).setOnClickListener { pick(addrTypes, row.find(R.id.tvAddressType)) }
        row.find<ImageButton>(R.id.btnRemoveAddress).setOnClickListener { remove(row, b.addressContainer, addressRows) }
        b.addressContainer.addView(row); addressRows.add(row)
    }

    private fun addDateRow(entry: DateEntry? = null) {
        val row = inflate(R.layout.row_date)
        val sel = SelDate(entry?.year ?: 0, entry?.month ?: 0, entry?.day ?: 0)
        row.tag = sel

        val tvType  = row.find<TextView>(R.id.tvDateType)
        val btnDate = row.find<Button>(R.id.btnPickDate)

        entry?.let {
            tvType.text = it.typeLabel
            if (it.day > 0 && it.month > 0) btnDate.text = fmtDate(it.year, it.month, it.day)
        }

        tvType.setOnClickListener { pick(dateTypes, tvType) }
        btnDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                sel.year = y; sel.month = m + 1; sel.day = d
                btnDate.text = fmtDate(y, m + 1, d)
            },
                if (sel.year > 0) sel.year else cal.get(Calendar.YEAR),
                if (sel.month > 0) sel.month - 1 else cal.get(Calendar.MONTH),
                if (sel.day > 0) sel.day else cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        row.find<ImageButton>(R.id.btnRemoveDate).setOnClickListener { remove(row, b.dateContainer, dateRows) }
        b.dateContainer.addView(row); dateRows.add(row)
    }

    private fun addRelationRow(name: String = "", type: String = "Parent") {
        val row = inflate(R.layout.row_relationship)
        row.find<EditText>(R.id.etRelationshipName).setText(name)
        row.find<TextView>(R.id.tvRelationshipType).also { tv ->
            tv.text = type
            tv.setOnClickListener { pick(relTypes, tv) }
        }
        row.find<ImageButton>(R.id.btnRemoveRelationship).setOnClickListener { remove(row, b.relationshipContainer, relationRows) }
        b.relationshipContainer.addView(row); relationRows.add(row)
    }

    private fun addWebsiteRow(url: String = "") {
        val row = inflate(R.layout.row_website)
        row.find<EditText>(R.id.etWebsite).setText(url)
        row.find<ImageButton>(R.id.btnRemoveWebsite).setOnClickListener { remove(row, b.websiteContainer, websiteRows) }
        b.websiteContainer.addView(row); websiteRows.add(row)
    }

    private fun addImRow(account: String = "", proto: String = "Hangouts") {
        val row = inflate(R.layout.row_im)
        row.find<EditText>(R.id.etImAccount).setText(account)
        row.find<TextView>(R.id.tvImProtocol).also { tv ->
            tv.text = proto
            tv.setOnClickListener { pick(imProtos, tv) }
        }
        row.find<ImageButton>(R.id.btnRemoveIm).setOnClickListener { remove(row, b.imContainer, imRows) }
        b.imContainer.addView(row); imRows.add(row)
    }

    // ── Collect and save ──────────────────────────────────────────────────────

    private fun onSave() {
        // Display name
        var displayName = b.etDisplayName.text.toString().trim()
        if (displayName.isEmpty()) {
            displayName = listOfNotNull(
                b.etFirstName.text.toString().trim().ifEmpty { null },
                b.etMiddleName.text.toString().trim().ifEmpty { null },
                b.etLastName.text.toString().trim().ifEmpty { null }
            ).joinToString(" ")
        }
        if (displayName.isEmpty()) {
            b.etDisplayName.error = "Name is required"; b.etDisplayName.requestFocus(); return
        }

        // Phones (required)
        val phones = phoneRows.mapNotNull { row ->
            val n = row.find<EditText>(R.id.etPhone).text.toString().trim()
            val t = row.find<TextView>(R.id.tvPhoneType).text.toString()
            if (n.isNotEmpty()) PhoneEntry(n, t) else null
        }
        if (phones.isEmpty()) {
            Toast.makeText(this, "At least one phone number is required", Toast.LENGTH_SHORT).show(); return
        }

        // Emails
        val emails = emailRows.mapNotNull { row ->
            val a = row.find<EditText>(R.id.etEmail).text.toString().trim()
            val t = row.find<TextView>(R.id.tvEmailType).text.toString()
            if (a.isNotEmpty()) EmailEntry(a, t) else null
        }

        // Addresses
        val addresses = addressRows.mapNotNull { row ->
            val street  = row.find<EditText>(R.id.etStreet).text.toString().trim()
            val city    = row.find<EditText>(R.id.etCity).text.toString().trim()
            val state   = row.find<EditText>(R.id.etState).text.toString().trim()
            val post    = row.find<EditText>(R.id.etPostcode).text.toString().trim()
            val country = row.find<EditText>(R.id.etCountry).text.toString().trim()
            val type    = row.find<TextView>(R.id.tvAddressType).text.toString()
            if (street.isNotEmpty() || city.isNotEmpty() || country.isNotEmpty())
                AddressEntry(street, city, state, post, country, type) else null
        }

        // Dates
        val dates = dateRows.mapNotNull { row ->
            val sel  = row.tag as? SelDate ?: return@mapNotNull null
            val type = row.find<TextView>(R.id.tvDateType).text.toString()
            if (sel.month > 0 && sel.day > 0) DateEntry(sel.year, sel.month, sel.day, type) else null
        }

        // Relationships
        val rels = relationRows.mapNotNull { row ->
            val n = row.find<EditText>(R.id.etRelationshipName).text.toString().trim()
            val t = row.find<TextView>(R.id.tvRelationshipType).text.toString()
            if (n.isNotEmpty()) RelationshipEntry(n, t) else null
        }

        // Websites
        val websites = websiteRows.mapNotNull { row ->
            row.find<EditText>(R.id.etWebsite).text.toString().trim().ifEmpty { null }
        }

        // IM
        val ims = imRows.mapNotNull { row ->
            val a = row.find<EditText>(R.id.etImAccount).text.toString().trim()
            val p = row.find<TextView>(R.id.tvImProtocol).text.toString()
            if (a.isNotEmpty()) ImEntry(a, p) else null
        }

        val namePrefix   = b.etNamePrefix.text.toString().trim()
        val firstName    = b.etFirstName.text.toString().trim()
        val middleName   = b.etMiddleName.text.toString().trim()
        val lastName     = b.etLastName.text.toString().trim()
        val nameSuffix   = b.etNameSuffix.text.toString().trim()
        val phoneticName = b.etPhoneticName.text.toString().trim()
        val nickname     = b.etNickname.text.toString().trim()
        val jobTitle     = b.etJobTitle.text.toString().trim()
        val department   = b.etDepartment.text.toString().trim()
        val company      = b.etCompany.text.toString().trim()
        val notes        = b.etNotes.text.toString().trim()

        b.btnSave.isEnabled = false
        b.progressSave.visibility = View.VISIBLE

        val contact = existingContact
        if (contact != null) {
            doUpdate(contact.contactId, displayName, namePrefix, firstName, middleName, lastName,
                nameSuffix, phoneticName, nickname, phones, emails, jobTitle, department, company,
                addresses, dates, rels, notes, websites, ims)
        } else {
            doInsert(displayName, namePrefix, firstName, middleName, lastName, nameSuffix,
                phoneticName, nickname, phones, emails, jobTitle, department, company,
                addresses, dates, rels, notes, websites, ims)
        }
    }

    // ── INSERT ────────────────────────────────────────────────────────────────

    private fun doInsert(
        displayName: String, namePrefix: String, firstName: String, middleName: String,
        lastName: String, nameSuffix: String, phoneticName: String, nickname: String,
        phones: List<PhoneEntry>, emails: List<EmailEntry>,
        jobTitle: String, department: String, company: String,
        addresses: List<AddressEntry>, dates: List<DateEntry>,
        rels: List<RelationshipEntry>, notes: String,
        websites: List<String>, ims: List<ImEntry>
    ) {
        lifecycleScope.launch {
            val err = withContext(Dispatchers.IO) {
                try {
                    val ops = ArrayList<ContentProviderOperation>()
                    val ri = 0 // rawIdx

                    // Raw contact
                    ops += op { ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, selectedAccount?.type.takeIf { it != "Local" })
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, selectedAccount?.name.takeIf { it != "Phone" && it != "Local" })
                        .build() }

                    // Structured name
                    ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, ri)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.PREFIX, namePrefix)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, middleName)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, lastName)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.SUFFIX, nameSuffix)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME, phoneticName)
                        .build()

                    // Nickname
                    if (nickname.isNotEmpty()) ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, ri)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Nickname.NAME, nickname).build()

                    // Phones
                    for (p in phones) ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, ri)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, p.number)
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneTypeInt(p.typeLabel))
                        .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, p.typeLabel).build()

                    // Photo
                    if (selectedImageBytes != null) {
                        ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, ri)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, selectedImageBytes)
                            .build()
                    }

                    // Emails
                    for (e in emails) ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, ri)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, e.address)
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, emailTypeInt(e.typeLabel))
                        .withValue(ContactsContract.CommonDataKinds.Email.LABEL, e.typeLabel).build()

                    // Organization
                    if (jobTitle.isNotEmpty() || department.isNotEmpty() || company.isNotEmpty())
                        ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, ri)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Organization.TITLE, jobTitle)
                            .withValue(ContactsContract.CommonDataKinds.Organization.DEPARTMENT, department)
                            .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, company).build()

                    // Addresses
                    for (a in addresses) ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, ri)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, addrTypeInt(a.typeLabel))
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.STREET, a.street)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.CITY, a.city)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.REGION, a.state)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, a.postcode)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, a.country).build()

                    // Dates
                    for (d in dates) ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, ri)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Event.TYPE, dateTypeInt(d.typeLabel))
                        .withValue(ContactsContract.CommonDataKinds.Event.START_DATE, "%04d-%02d-%02d".format(d.year, d.month, d.day)).build()

                    // Relationships
                    for (r in rels) ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, ri)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Relation.NAME, r.name)
                        .withValue(ContactsContract.CommonDataKinds.Relation.TYPE, relTypeInt(r.typeLabel)).build()

                    // Notes
                    if (notes.isNotEmpty()) ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, ri)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Note.NOTE, notes).build()

                    // Websites
                    for (w in websites) ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, ri)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Website.URL, w).build()

                    // IM
                    for (im in ims) ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, ri)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Im.DATA, im.account)
                        .withValue(ContactsContract.CommonDataKinds.Im.PROTOCOL, imProtoInt(im.protocol)).build()

                    contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                    null
                } catch (e: Exception) { e.printStackTrace(); e.message ?: "Unknown error" }
            }
            done(err)
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    private fun doUpdate(
        contactId: String,
        displayName: String, namePrefix: String, firstName: String, middleName: String,
        lastName: String, nameSuffix: String, phoneticName: String, nickname: String,
        phones: List<PhoneEntry>, emails: List<EmailEntry>,
        jobTitle: String, department: String, company: String,
        addresses: List<AddressEntry>, dates: List<DateEntry>,
        rels: List<RelationshipEntry>, notes: String,
        websites: List<String>, ims: List<ImEntry>
    ) {
        lifecycleScope.launch {
            val err = withContext(Dispatchers.IO) {
                try {
                    val rawId = getRawId(contactId) ?: return@withContext "Could not find raw contact ID"
                    val ops   = ArrayList<ContentProviderOperation>()

                    fun delMime(mime: String) {
                        ops += ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                            .withSelection(
                                "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                                arrayOf(rawId, mime)
                            ).build()
                    }

                    fun ins(mime: String, block: ContentProviderOperation.Builder.() -> Unit) {
                        ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                            .withValue(ContactsContract.Data.MIMETYPE, mime)
                            .apply(block).build()
                    }

                    // Structured name
                    delMime(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    ins(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE) {
                        withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                        withValue(ContactsContract.CommonDataKinds.StructuredName.PREFIX, namePrefix)
                        withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
                        withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, middleName)
                        withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, lastName)
                        withValue(ContactsContract.CommonDataKinds.StructuredName.SUFFIX, nameSuffix)
                        withValue(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME, phoneticName)
                    }

                    // Nickname
                    delMime(ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
                    if (nickname.isNotEmpty()) ins(ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE) {
                        withValue(ContactsContract.CommonDataKinds.Nickname.NAME, nickname)
                    }

                    // Phones
                    delMime(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    for (p in phones) ins(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE) {
                        withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, p.number)
                        withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneTypeInt(p.typeLabel))
                        withValue(ContactsContract.CommonDataKinds.Phone.LABEL, p.typeLabel)
                    }

                    // Emails
                    delMime(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    for (e in emails) ins(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE) {
                        withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, e.address)
                        withValue(ContactsContract.CommonDataKinds.Email.TYPE, emailTypeInt(e.typeLabel))
                        withValue(ContactsContract.CommonDataKinds.Email.LABEL, e.typeLabel)
                    }

                    // Photo
                    if (selectedImageBytes != null) {
                        delMime(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                        ins(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE) {
                            withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, selectedImageBytes)
                        }
                    }

                    // Organization
                    delMime(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                    if (jobTitle.isNotEmpty() || department.isNotEmpty() || company.isNotEmpty())
                        ins(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE) {
                            withValue(ContactsContract.CommonDataKinds.Organization.TITLE, jobTitle)
                            withValue(ContactsContract.CommonDataKinds.Organization.DEPARTMENT, department)
                            withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, company)
                        }

                    // Addresses
                    delMime(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                    for (a in addresses) ins(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE) {
                        withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, addrTypeInt(a.typeLabel))
                        withValue(ContactsContract.CommonDataKinds.StructuredPostal.STREET, a.street)
                        withValue(ContactsContract.CommonDataKinds.StructuredPostal.CITY, a.city)
                        withValue(ContactsContract.CommonDataKinds.StructuredPostal.REGION, a.state)
                        withValue(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, a.postcode)
                        withValue(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, a.country)
                    }

                    // Dates
                    delMime(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                    for (d in dates) ins(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE) {
                        withValue(ContactsContract.CommonDataKinds.Event.TYPE, dateTypeInt(d.typeLabel))
                        withValue(ContactsContract.CommonDataKinds.Event.START_DATE, "%04d-%02d-%02d".format(d.year, d.month, d.day))
                    }

                    // Relationships
                    delMime(ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE)
                    for (r in rels) ins(ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE) {
                        withValue(ContactsContract.CommonDataKinds.Relation.NAME, r.name)
                        withValue(ContactsContract.CommonDataKinds.Relation.TYPE, relTypeInt(r.typeLabel))
                    }

                    // Notes
                    delMime(ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                    if (notes.isNotEmpty()) ins(ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE) {
                        withValue(ContactsContract.CommonDataKinds.Note.NOTE, notes)
                    }

                    // Websites
                    delMime(ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                    for (w in websites) ins(ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE) {
                        withValue(ContactsContract.CommonDataKinds.Website.URL, w)
                    }

                    // IM
                    delMime(ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE)
                    for (im in ims) ins(ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE) {
                        withValue(ContactsContract.CommonDataKinds.Im.DATA, im.account)
                        withValue(ContactsContract.CommonDataKinds.Im.PROTOCOL, imProtoInt(im.protocol))
                    }

                    contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                    null
                } catch (e: Exception) { e.printStackTrace(); e.message ?: "Unknown error" }
            }
            done(err)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun done(error: String?) {
        b.btnSave.isEnabled = true
        b.progressSave.visibility = View.GONE
        if (error == null) {
            Toast.makeText(this, if (existingContact != null) "Contact updated" else "Contact saved", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_OK)
            finish()
        } else {
            Toast.makeText(this, "Failed: $error", Toast.LENGTH_LONG).show()
        }
    }

    private fun getRawId(contactId: String): String? = try {
        contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.CONTACT_ID}=? AND ${ContactsContract.RawContacts.DELETED}=0",
            arrayOf(contactId), null
        )?.use { if (it.moveToFirst()) it.getString(0) else null }
    } catch (e: Exception) { e.printStackTrace(); null }

    private fun uriToBytes(uri: Uri): ByteArray? = try {
        contentResolver.openInputStream(uri)?.use { 
            val buffer = ByteArrayOutputStream()
            it.copyTo(buffer)
            buffer.toByteArray()
        }
    } catch (e: Exception) { e.printStackTrace(); null }


    private fun inflate(res: Int): View = LayoutInflater.from(this).inflate(res, null, false)
    private fun remove(v: View, c: LinearLayout, l: MutableList<View>) { c.removeView(v); l.remove(v) }
    private fun pick(opts: Array<String>, tv: TextView) = AlertDialog.Builder(this).setItems(opts) { _, i -> tv.text = opts[i] }.show()
    private fun fmtDate(y: Int, m: Int, d: Int): String {
        val months = arrayOf("","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        return if (y > 0) "${months.getOrElse(m){"?"}} $d, $y" else "${months.getOrElse(m){"?"}} $d"
    }

    private operator fun ArrayList<ContentProviderOperation>.plusAssign(op: ContentProviderOperation) { add(op) }
    private fun op(block: () -> ContentProviderOperation) = block()
    private fun <T : View> View.find(id: Int): T = findViewById(id)

    // ── Type → Int ────────────────────────────────────────────────────────────
    private fun phoneTypeInt(l: String) = when(l) {
        "Mobile" -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
        "Home"   -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
        "Work"   -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
        "Main"   -> ContactsContract.CommonDataKinds.Phone.TYPE_MAIN
        "Work Fax"->ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK
        "Home Fax"->ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME
        "Pager"  -> ContactsContract.CommonDataKinds.Phone.TYPE_PAGER
        else     -> ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
    }
    private fun emailTypeInt(l: String) = when(l) {
        "Work"  -> ContactsContract.CommonDataKinds.Email.TYPE_WORK
        "Other" -> ContactsContract.CommonDataKinds.Email.TYPE_OTHER
        else    -> ContactsContract.CommonDataKinds.Email.TYPE_HOME
    }
    private fun addrTypeInt(l: String) = when(l) {
        "Work"  -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK
        "Other" -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER
        else    -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME
    }
    private fun dateTypeInt(l: String) = when(l) {
        "Anniversary" -> ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY
        "Other"       -> ContactsContract.CommonDataKinds.Event.TYPE_OTHER
        else          -> ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY
    }
    private fun relTypeInt(l: String) = when(l) {
        "Mother"   -> ContactsContract.CommonDataKinds.Relation.TYPE_MOTHER
        "Father"   -> ContactsContract.CommonDataKinds.Relation.TYPE_FATHER
        "Brother"  -> ContactsContract.CommonDataKinds.Relation.TYPE_BROTHER
        "Sister"   -> ContactsContract.CommonDataKinds.Relation.TYPE_SISTER
        "Spouse"   -> ContactsContract.CommonDataKinds.Relation.TYPE_SPOUSE
        "Child"    -> ContactsContract.CommonDataKinds.Relation.TYPE_CHILD
        "Friend"   -> ContactsContract.CommonDataKinds.Relation.TYPE_FRIEND
        "Relative" -> ContactsContract.CommonDataKinds.Relation.TYPE_RELATIVE
        else       -> ContactsContract.CommonDataKinds.Relation.TYPE_PARENT
    }
    private fun imProtoInt(l: String) = when(l) {
        "Hangouts"   -> ContactsContract.CommonDataKinds.Im.PROTOCOL_GOOGLE_TALK
        "QQ"         -> ContactsContract.CommonDataKinds.Im.PROTOCOL_QQ
        "Skype"      -> ContactsContract.CommonDataKinds.Im.PROTOCOL_SKYPE
        "Yahoo"      -> ContactsContract.CommonDataKinds.Im.PROTOCOL_YAHOO
        "AIM"        -> ContactsContract.CommonDataKinds.Im.PROTOCOL_AIM
        "MSN"        -> ContactsContract.CommonDataKinds.Im.PROTOCOL_MSN
        "ICQ"        -> ContactsContract.CommonDataKinds.Im.PROTOCOL_ICQ
        "Jabber"     -> ContactsContract.CommonDataKinds.Im.PROTOCOL_JABBER
        "NetMeeting" -> ContactsContract.CommonDataKinds.Im.PROTOCOL_NETMEETING
        else         -> ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}