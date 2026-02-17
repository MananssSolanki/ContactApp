package com.example.contactapp.Activities

import android.Manifest
import android.accounts.AccountManager
import android.content.ContentProviderOperation
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText

import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.contactapp.R
import com.example.contactapp.databinding.ActivityAddContactBinding
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Calendar

class AddContactActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddContactBinding
    private var selectedAccountIndex = 0
    private var accountNames: MutableList<String> = mutableListOf()
    private var accountTypes: MutableList<String> = mutableListOf()
    private var photoBitmap: Bitmap? = null

    private var selectedGroupId: Long? = null
    private var selectedDate: String? = null
    private var selectedDateType: Int = ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.WRITE_CONTACTS] == true &&
                permissions[Manifest.permission.GET_ACCOUNTS] == true
            ) {
                loadAccounts()
            } else {
                Toast.makeText(this, "Permissions required to create contact", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    try {
                        photoBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                        binding.ivContactPhoto.setImageBitmap(photoBitmap)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        checkPermissions()
        setupSpinners()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.WRITE_CONTACTS,
                    Manifest.permission.GET_ACCOUNTS
                )
            )
        } else {
            loadAccounts()
        }
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { saveContact() }

        binding.ivContactPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        binding.btnExpandName.setOnClickListener {
            if (binding.layoutMoreNameFields.visibility == View.VISIBLE) {
                binding.layoutMoreNameFields.visibility = View.GONE
                binding.btnExpandName.animate().rotation(0f).start()
            } else {
                binding.layoutMoreNameFields.visibility = View.VISIBLE
                binding.btnExpandName.animate().rotation(180f).start()
            }
        }

        binding.tvViewMore.setOnClickListener {
            if (binding.layoutMoreFields.visibility == View.VISIBLE) {
                binding.layoutMoreFields.visibility = View.GONE
                binding.tvViewMore.text = "View more"
                binding.tvViewMore.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            } else {
                binding.layoutMoreFields.visibility = View.VISIBLE
                binding.tvViewMore.text = "View less"
            }
        }

        binding.layoutGroups.setOnClickListener { showGroupSelectionDialog() }

        // Dynamic Add Buttons
        binding.tvAddPhone.setOnClickListener { addPhoneRow() }
        binding.tvAddEmail.setOnClickListener { addEmailRow() }
        binding.tvAddAddress.setOnClickListener { addAddressRow() }
        binding.tvAddDate.setOnClickListener { addDateRow() }
        binding.tvAddWebsite.setOnClickListener { addWebsiteRow() }
        binding.tvAddMessenger.setOnClickListener { addMessengerRow() }
        binding.tvAddRelation.setOnClickListener { addRelationRow() }
    }

    private fun setupSpinners() {
         // Initial rows
         addPhoneRow()
         addEmailRow()
    }

    private fun addPhoneRow() {
        val view = layoutInflater.inflate(R.layout.layout_row_dynamic_entry, binding.llPhoneContainer, false)
        val etValue = view.findViewById<EditText>(R.id.et_value)
        val spinner = view.findViewById<android.widget.Spinner>(R.id.spinner_type)
        val ivRemove = view.findViewById<View>(R.id.iv_remove)

        etValue.hint = "Phone"
        etValue.inputType = android.text.InputType.TYPE_CLASS_PHONE

        val types = listOf("Mobile", "Work", "Home", "Main", "Fax", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        spinner.adapter = adapter

        ivRemove.setOnClickListener { binding.llPhoneContainer.removeView(view) }
        binding.llPhoneContainer.addView(view)
    }

    private fun addEmailRow() {
        val view = layoutInflater.inflate(R.layout.layout_row_dynamic_entry, binding.llEmailContainer, false)
        val etValue = view.findViewById<EditText>(R.id.et_value)
        val spinner = view.findViewById<android.widget.Spinner>(R.id.spinner_type)
        val ivRemove = view.findViewById<View>(R.id.iv_remove)

        etValue.hint = "Email"
        etValue.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        val types = listOf("Home", "Work", "Other", "Mobile")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        spinner.adapter = adapter

        ivRemove.setOnClickListener { binding.llEmailContainer.removeView(view) }
        binding.llEmailContainer.addView(view)
    }

    private fun addAddressRow() {
        val view = layoutInflater.inflate(R.layout.layout_row_dynamic_address, binding.llAddressContainer, false)
        val spinner = view.findViewById<android.widget.Spinner>(R.id.spinner_type)
        val ivRemove = view.findViewById<View>(R.id.iv_remove)

        val types = listOf("Home", "Work", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        spinner.adapter = adapter

        ivRemove.setOnClickListener { binding.llAddressContainer.removeView(view) }
        binding.llAddressContainer.addView(view)
    }

    private fun addDateRow() {
        val view = layoutInflater.inflate(R.layout.layout_row_dynamic_date, binding.llDateContainer, false)
        val spinner = view.findViewById<android.widget.Spinner>(R.id.spinner_type)
        val tvDate = view.findViewById<android.widget.TextView>(R.id.tv_date)
        val ivRemove = view.findViewById<View>(R.id.iv_remove)

        val types = listOf("Birthday", "Anniversary", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        spinner.adapter = adapter

        tvDate.setOnClickListener {
             val c = Calendar.getInstance()
            android.app.DatePickerDialog(this, { _, year, month, day ->
                val dateStr = String.format("%d-%02d-%02d", year, month + 1, day)
                tvDate.text = dateStr
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        ivRemove.setOnClickListener { binding.llDateContainer.removeView(view) }
        binding.llDateContainer.addView(view)
    }

    private fun addWebsiteRow() {
        val view = layoutInflater.inflate(R.layout.layout_row_dynamic_entry, binding.llWebsiteContainer, false)
        val etValue = view.findViewById<EditText>(R.id.et_value)
        val spinner = view.findViewById<android.widget.Spinner>(R.id.spinner_type)
        val ivRemove = view.findViewById<View>(R.id.iv_remove)

        etValue.hint = "Website"
        etValue.inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI

        val types = listOf("Homepage", "Blog", "Profile", "Home", "Work", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        spinner.adapter = adapter

        ivRemove.setOnClickListener { binding.llWebsiteContainer.removeView(view) }
        binding.llWebsiteContainer.addView(view)
    }

    private fun addMessengerRow() {
        val view = layoutInflater.inflate(R.layout.layout_row_dynamic_entry, binding.llMessengerContainer, false)
        val etValue = view.findViewById<EditText>(R.id.et_value)
        val spinner = view.findViewById<android.widget.Spinner>(R.id.spinner_type)
        val ivRemove = view.findViewById<View>(R.id.iv_remove)

        etValue.hint = "Messenger ID"

        val types = listOf("WhatsApp", "Facebook", "Hangouts", "Skype", "QQ", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        spinner.adapter = adapter

        ivRemove.setOnClickListener { binding.llMessengerContainer.removeView(view) }
        binding.llMessengerContainer.addView(view)
    }

    private fun addRelationRow() {
         val view = layoutInflater.inflate(R.layout.layout_row_dynamic_entry, binding.llRelationContainer, false)
        val etValue = view.findViewById<EditText>(R.id.et_value)
        val spinner = view.findViewById<android.widget.Spinner>(R.id.spinner_type)
        val ivRemove = view.findViewById<View>(R.id.iv_remove)

        etValue.hint = "Name"
        etValue.inputType = android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME

        val types = listOf("Assistant", "Brother", "Child", "Domestic Partner", "Father", "Friend", "Manager", "Mother", "Parent", "Partner", "Referred by", "Relative", "Sister", "Spouse")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        spinner.adapter = adapter

        ivRemove.setOnClickListener { binding.llRelationContainer.removeView(view) }
        binding.llRelationContainer.addView(view)
    }


    private fun loadAccounts() {
        accountNames.clear()
        accountTypes.clear()

        // Add "Phone" / Device-only option
        accountNames.add("Phone")
        accountTypes.add("vnd.sec.contact.phone")

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED) {
            val accounts = AccountManager.get(this).accounts
            for (account in accounts) {
                if (account.type.contains("google") ||
                    account.type.contains("samsung") ||
                    account.type.contains("xiaomi") ||
                    account.type.contains("whatsapp")) {
                        accountNames.add(account.name)
                        accountTypes.add(account.type)
                }
            }
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, accountNames)
        binding.spinnerAccounts.adapter = adapter

        binding.spinnerAccounts.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedAccountIndex = position
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showGroupSelectionDialog() {
        val groups = mutableListOf<String>()
        val groupIds = mutableListOf<Long>()

        // Query Groups
        val projection = arrayOf(ContactsContract.Groups._ID, ContactsContract.Groups.TITLE)
        val cursor = contentResolver.query(ContactsContract.Groups.CONTENT_URI, projection, null, null, null)
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val title = it.getString(1)
                if (title != null) {
                    groups.add(title)
                    groupIds.add(id)
                }
            }
        }
        groups.add("Create new group")

        AlertDialog.Builder(this)
            .setTitle("Select Group")
            .setItems(groups.toTypedArray()) { _, which ->
                if (which == groups.size - 1) {
                    showCreateGroupDialog()
                } else {
                    selectedGroupId = groupIds[which]
                    binding.tvGroupName.text = groups[which]
                }
            }
            .show()
    }

    private fun showCreateGroupDialog() {
        val input = EditText(this)
        input.hint = "Group Name"

        AlertDialog.Builder(this)
            .setTitle("Create Group")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val groupName = input.text.toString().trim()
                if (groupName.isNotEmpty()) {
                    createNewGroup(groupName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createNewGroup(groupName: String) {
        val ops = ArrayList<ContentProviderOperation>()
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Groups.CONTENT_URI)
            .withValue(ContactsContract.Groups.TITLE, groupName)
            .withValue(ContactsContract.Groups.GROUP_VISIBLE, 1)
            .build())

        try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            Toast.makeText(this, "Group created", Toast.LENGTH_SHORT).show()
             
             // Re-query to get ID
             val cursor = contentResolver.query(
                 ContactsContract.Groups.CONTENT_URI,
                 arrayOf(ContactsContract.Groups._ID),
                 "${ContactsContract.Groups.TITLE} = ?",
                 arrayOf(groupName),
                 null
             )
             cursor?.use {
                 if (it.moveToFirst()) {
                     selectedGroupId = it.getLong(0)
                     binding.tvGroupName.text = groupName
                 }
             }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to create group", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveContact() {
        val name = binding.etName.text.toString().trim()
        
        // Check dynamic fields for at least one entry
        var hasPhone = false
        for (i in 0 until binding.llPhoneContainer.childCount) {
             val view = binding.llPhoneContainer.getChildAt(i)
             val et = view.findViewById<EditText>(R.id.et_value)
             if (et.text.toString().trim().isNotEmpty()) hasPhone = true
        }
        var hasEmail = false
        for (i in 0 until binding.llEmailContainer.childCount) {
             val view = binding.llEmailContainer.getChildAt(i)
             val et = view.findViewById<EditText>(R.id.et_value)
             if (et.text.toString().trim().isNotEmpty()) hasEmail = true
        }

        if (name.isEmpty() && !hasPhone && !hasEmail) {
            Toast.makeText(this, "Please enter at least a Name, Phone, or Email", Toast.LENGTH_SHORT).show()
            return
        }

        val ops = ArrayList<ContentProviderOperation>()
        val rawContactInsertIndex = ops.size

        // 1. Insert RawContact
        val accountType = accountTypes.getOrElse(selectedAccountIndex) { null }
        val accountName = accountNames.getOrElse(selectedAccountIndex) { "Phone" }

        val op = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, if (accountName == "Phone") null else accountType)
            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, if (accountName == "Phone") null else accountName)
            .withValue(ContactsContract.RawContacts.STARRED, if (binding.cbFavorite.isChecked) 1 else 0)
        ops.add(op.build())

        // 2. Insert Name
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val nameBuilder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)

        if (name.isNotEmpty()) nameBuilder.withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
        if (firstName.isNotEmpty()) nameBuilder.withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
        if (lastName.isNotEmpty()) nameBuilder.withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, lastName)
        ops.add(nameBuilder.build())

        // 3. Insert Phones
        for (i in 0 until binding.llPhoneContainer.childCount) {
            val view = binding.llPhoneContainer.getChildAt(i)
            val phone = view.findViewById<EditText>(R.id.et_value).text.toString().trim()
            if (phone.isNotEmpty()) {
                val typeStr = view.findViewById<android.widget.Spinner>(R.id.spinner_type).selectedItem.toString()
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, getPhoneType(typeStr))
                    .build())
            }
        }

        // 4. Insert Emails
        for (i in 0 until binding.llEmailContainer.childCount) {
            val view = binding.llEmailContainer.getChildAt(i)
            val email = view.findViewById<EditText>(R.id.et_value).text.toString().trim()
            if (email.isNotEmpty()) {
                 val typeStr = view.findViewById<android.widget.Spinner>(R.id.spinner_type).selectedItem.toString()
                 ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, getEmailType(typeStr))
                    .build())
            }
        }

        // 5. Insert Photo
        photoBitmap?.let { bitmap ->
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, stream.toByteArray())
                .build())
        }

        // 6. Group
        selectedGroupId?.let { groupId ->
             ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, groupId)
                .build())
        }

        // 7. Address
        for (i in 0 until binding.llAddressContainer.childCount) {
            val view = binding.llAddressContainer.getChildAt(i)
            val street = view.findViewById<EditText>(R.id.et_street).text.toString().trim()
            val city = view.findViewById<EditText>(R.id.et_city).text.toString().trim()
            val postcode = view.findViewById<EditText>(R.id.et_postcode).text.toString().trim()
            
            if (street.isNotEmpty() || city.isNotEmpty() || postcode.isNotEmpty()) {
                 val typeStr = view.findViewById<android.widget.Spinner>(R.id.spinner_type).selectedItem.toString()
                 // Map typeStr to TYPE
                 val type = when(typeStr) { "Work" -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK ; "Other" -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER; else -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME }
                 
                 ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.STREET, street)
                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.CITY, city)
                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, postcode)
                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, type)
                    .build())
            }
        }

        // 8. Work Info (Static)
        val company = binding.etCompany.text.toString().trim()
        val title = binding.etJobTitle.text.toString().trim()
        if (company.isNotEmpty() || title.isNotEmpty()) {
             ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, company)
                .withValue(ContactsContract.CommonDataKinds.Organization.TITLE, title)
                .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
                .build())
        }

        // 9. Dates
        for (i in 0 until binding.llDateContainer.childCount) {
            val view = binding.llDateContainer.getChildAt(i)
            val date = view.findViewById<android.widget.TextView>(R.id.tv_date).text.toString()
            if (date != "Select Date" && date.isNotEmpty()) {
                 val typeStr = view.findViewById<android.widget.Spinner>(R.id.spinner_type).selectedItem.toString()
                 val type = when(typeStr) { "Anniversary" -> ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY; "Other" -> ContactsContract.CommonDataKinds.Event.TYPE_OTHER; else -> ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY }
                 
                 ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Event.START_DATE, date)
                    .withValue(ContactsContract.CommonDataKinds.Event.TYPE, type)
                    .build())
            }
        }

        // 10. Note
        val note = binding.etNote.text.toString().trim()
        if (note.isNotEmpty()) {
             ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Note.NOTE, note)
                .build())
        }

        // 11. Website
        for (i in 0 until binding.llWebsiteContainer.childCount) {
            val view = binding.llWebsiteContainer.getChildAt(i)
            val url = view.findViewById<EditText>(R.id.et_value).text.toString().trim()
            if (url.isNotEmpty()) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Website.URL, url)
                .withValue(ContactsContract.CommonDataKinds.Website.TYPE, ContactsContract.CommonDataKinds.Website.TYPE_HOMEPAGE)
                .build())
            }
        }
        
        // 12. Messenger
        for (i in 0 until binding.llMessengerContainer.childCount) {
            val view = binding.llMessengerContainer.getChildAt(i)
            val im = view.findViewById<EditText>(R.id.et_value).text.toString().trim()
            if (im.isNotEmpty()) {
                 val typeStr = view.findViewById<android.widget.Spinner>(R.id.spinner_type).selectedItem.toString()
                 ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Im.DATA, im)
                .withValue(ContactsContract.CommonDataKinds.Im.PROTOCOL, ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM)
                 .withValue(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL, typeStr)
                .build())
            }
        }

         // 13. Nickname (Static)
        val nickname = binding.etNickname.text.toString().trim()
        if (nickname.isNotEmpty()) {
             ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Nickname.NAME, nickname)
                .withValue(ContactsContract.CommonDataKinds.Nickname.TYPE, ContactsContract.CommonDataKinds.Nickname.TYPE_DEFAULT)
                .build())
        }
        
        // 14. Relations
         for (i in 0 until binding.llRelationContainer.childCount) {
            val view = binding.llRelationContainer.getChildAt(i)
            val name = view.findViewById<EditText>(R.id.et_value).text.toString().trim()
            if (name.isNotEmpty()) {
                 val typeStr = view.findViewById<android.widget.Spinner>(R.id.spinner_type).selectedItem.toString()
                  // Simplified type mapping or store as custom if needed, here just saving generic type
                 ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Relation.NAME, name)
                .withValue(ContactsContract.CommonDataKinds.Relation.TYPE, ContactsContract.CommonDataKinds.Relation.TYPE_CUSTOM)
                 .withValue(ContactsContract.CommonDataKinds.Relation.LABEL, typeStr)
                .build())
            }
        }

        try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            Toast.makeText(this, "Contact saved successfully", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving contact: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getPhoneType(type: String): Int {
        return when (type) {
            "Mobile" -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
            "Work" -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
            "Home" -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
            "Main" -> ContactsContract.CommonDataKinds.Phone.TYPE_MAIN
            "Fax" -> ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK
            else -> ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
        }
    }

     private fun getEmailType(type: String): Int {
        return when (type) {
            "Home" -> ContactsContract.CommonDataKinds.Email.TYPE_HOME
            "Work" -> ContactsContract.CommonDataKinds.Email.TYPE_WORK
            "Mobile" -> ContactsContract.CommonDataKinds.Email.TYPE_MOBILE
            else -> ContactsContract.CommonDataKinds.Email.TYPE_OTHER
        }
    }
}