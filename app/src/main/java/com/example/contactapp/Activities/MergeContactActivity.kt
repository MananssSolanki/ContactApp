package com.example.contactapp.Activities

import android.content.ContentProviderOperation
import android.os.Bundle
import android.provider.ContactsContract
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactapp.Adapter.MergeContactsAdapter
import com.example.contactapp.Model.ContactEnhanced
import com.example.contactapp.Model.MergeDuplicateGroup
import com.example.contactapp.R
import com.example.contactapp.databinding.ActivityMergeContactBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MergeContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMergeContactBinding
    private lateinit var adapter: MergeContactsAdapter
    private val selectedGroups = mutableSetOf<Int>()
    private var allGroups = listOf<MergeDuplicateGroup>()
    private var currentTab = "Number"
    private val groupsCache = mutableMapOf<String, List<MergeDuplicateGroup>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMergeContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        setupTabs()
        setupSelectAll()
        setupMergeButton()
        loadDuplicates("Number")
    }

    // ─── Tab setup ──────────────────────────────────────────────────────────

    private fun setupTabs() {
        binding.tabNumber.setOnClickListener { switchTab("Number") }
        binding.tabEmail.setOnClickListener { switchTab("Email") }
        binding.tabName.setOnClickListener { switchTab("Name") }
        updateTabUI("Number")
    }

    private fun switchTab(tab: String) {
        if (currentTab == tab) return
        currentTab = tab
        selectedGroups.clear()
        binding.checkboxSelectAll.setOnCheckedChangeListener(null)
        binding.checkboxSelectAll.isChecked = false
        binding.checkboxSelectAll.setOnCheckedChangeListener { _, isChecked -> onSelectAllChanged(isChecked) }
        updateTabUI(tab)

        if (groupsCache.containsKey(tab)) {
            displayGroups(groupsCache[tab] ?: emptyList())
        } else {
            loadDuplicates(tab)
        }
    }

    private fun updateTabUI(tab: String) {
        val activeBg = R.drawable.bg_samsung_tab_active
        val inactiveBg = R.drawable.bg_samsung_tab_inactive
        val activeTextColor = resources.getColor(R.color.samsung_tab_active_text, null)
        val inactiveTextColor = resources.getColor(R.color.samsung_tab_inactive_text, null)

        listOf(
            Pair(binding.tabNumber, "Number"),
            Pair(binding.tabEmail, "Email"),
            Pair(binding.tabName, "Name")
        ).forEach { (view, name) ->
            if (name == tab) {
                view.setBackgroundResource(activeBg)
                view.setTextColor(activeTextColor)
                view.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                view.setBackgroundResource(inactiveBg)
                view.setTextColor(inactiveTextColor)
                view.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }
    }

    // ─── Select All ─────────────────────────────────────────────────────────

    private fun setupSelectAll() {
        binding.checkboxSelectAll.setOnCheckedChangeListener { _, isChecked ->
            onSelectAllChanged(isChecked)
        }
    }

    private fun onSelectAllChanged(isChecked: Boolean) {
        if (isChecked) {
            selectedGroups.addAll(allGroups.indices)
        } else {
            selectedGroups.clear()
        }
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }

    // ─── Merge button ────────────────────────────────────────────────────────

    private fun setupMergeButton() {
        binding.btnMerge.setOnClickListener {
            if (selectedGroups.isEmpty()) {
                Toast.makeText(this, "Select at least one group to merge", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mergeSelectedContacts()
        }
    }

    // ─── Load duplicates ─────────────────────────────────────────────────────

    private fun loadDuplicates(type: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            val groups = withContext(Dispatchers.IO) { findDuplicates(type) }
            groupsCache[type] = groups
            displayGroups(groups)
        }
    }

    private fun displayGroups(groups: List<MergeDuplicateGroup>) {
        allGroups = groups
        binding.progressBar.visibility = View.GONE

        if (groups.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = "No duplicate contacts found by $currentTab"
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE

            adapter = MergeContactsAdapter(groups, selectedGroups) {
                // Update "select all" checkbox state when individual item toggled
                val allSelected = selectedGroups.size == groups.size
                binding.checkboxSelectAll.setOnCheckedChangeListener(null)
                binding.checkboxSelectAll.isChecked = allSelected
                binding.checkboxSelectAll.setOnCheckedChangeListener { _, isChecked -> onSelectAllChanged(isChecked) }
            }
            binding.recyclerView.layoutManager = LinearLayoutManager(this@MergeContactsActivity)
            binding.recyclerView.adapter = adapter
        }
    }

    // ─── Find duplicates from ContactsContract ───────────────────────────────

    private fun findDuplicates(type: String): List<MergeDuplicateGroup> {
        val cr = contentResolver
        val groups = mutableMapOf<String, MutableList<ContactEnhanced>>()

        when (type) {
            "Number" -> {
                val cursor = cr.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                    ), null, null, null
                ) ?: return emptyList()

                cursor.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getString(0) ?: continue
                        val name = c.getString(1) ?: "Unknown"
                        val rawNumber = c.getString(2) ?: continue
                        val photo = c.getString(3)

                        val digits = rawNumber.replace(Regex("[^0-9]"), "")
                        if (digits.length < 5) continue

                        // Group by last 10 digits to match numbers with/without country code
                        val groupKey = if (digits.length >= 10)
                            digits.substring(digits.length - 10)
                        else digits

                        val contact = ContactEnhanced(
                            contactId = id,
                            name = name,
                            phoneNumber = rawNumber,
                            photoUri = photo
                        )
                        groups.getOrPut(groupKey) { mutableListOf() }.add(contact)
                    }
                }
            }

            "Email" -> {
                val cursor = cr.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Email.ADDRESS,
                        ContactsContract.CommonDataKinds.Email.PHOTO_URI
                    ), null, null, null
                ) ?: return emptyList()

                cursor.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getString(0) ?: continue
                        val name = c.getString(1) ?: "Unknown"
                        val email = c.getString(2)?.lowercase()?.trim() ?: continue
                        if (email.isEmpty()) continue
                        val photo = c.getString(3)

                        val contact = ContactEnhanced(
                            contactId = id,
                            name = name,
                            phoneNumber = "",
                            email = email,
                            photoUri = photo
                        )
                        groups.getOrPut(email) { mutableListOf() }.add(contact)
                    }
                }
            }

            "Name" -> {
                val cursor = cr.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    arrayOf(
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                        ContactsContract.Contacts.PHOTO_URI
                    ), null, null, null
                ) ?: return emptyList()

                cursor.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getString(0) ?: continue
                        val name = c.getString(1)?.trim() ?: continue
                        if (name.isEmpty()) continue
                        val photo = c.getString(2)

                        // Fetch first phone number for this contact
                        val phoneCursor = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(id), null
                        )
                        val phone = phoneCursor?.use { pc ->
                            if (pc.moveToFirst()) pc.getString(0) ?: "" else ""
                        } ?: ""

                        val contact = ContactEnhanced(
                            contactId = id,
                            name = name,
                            phoneNumber = phone,
                            photoUri = photo
                        )
                        groups.getOrPut(name.lowercase().trim()) { mutableListOf() }.add(contact)
                    }
                }
            }
        }

        // Only return groups with more than one contact (duplicates)
        return groups
            .filter { it.value.size > 1 }
            .map { (key, contacts) ->
                val displayKey = when (type) {
                    "Number" -> contacts.find { it.phoneNumber.contains("+") }?.phoneNumber
                        ?: contacts.maxByOrNull { it.phoneNumber.length }?.phoneNumber
                        ?: key
                    "Email" -> contacts.firstOrNull()?.email ?: key
                    "Name" -> contacts.firstOrNull()?.name ?: key
                    else -> key
                }
                MergeDuplicateGroup(displayKey = displayKey, contacts = contacts)
            }
    }

    // ─── Perform merge via ContactsContract AggregationExceptions ────────────

    private fun mergeSelectedContacts() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnMerge.isEnabled = false

        lifecycleScope.launch {
            val mergedIndices = mutableListOf<Int>()

            val successCount = withContext(Dispatchers.IO) {
                var count = 0
                for (index in selectedGroups.toList()) {
                    val group = allGroups.getOrNull(index) ?: continue
                    if (group.contacts.size < 2) continue

                    // Collect raw contact IDs for this contact group
                    val rawIds = mutableListOf<String>()
                    group.contacts.forEach { contact ->
                        val rawCursor = contentResolver.query(
                            ContactsContract.RawContacts.CONTENT_URI,
                            arrayOf(ContactsContract.RawContacts._ID),
                            "${ContactsContract.RawContacts.CONTACT_ID} = ? AND ${ContactsContract.RawContacts.DELETED} = 0",
                            arrayOf(contact.contactId), null
                        )
                        rawCursor?.use { rc ->
                            while (rc.moveToNext()) {
                                rawIds.add(rc.getString(0))
                            }
                        }
                    }

                    if (rawIds.size < 2) continue
                    val primaryRawId = rawIds[0]

                    try {
                        val ops = ArrayList<ContentProviderOperation>()
                        for (i in 1 until rawIds.size) {
                            ops.add(
                                ContentProviderOperation
                                    .newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI)
                                    .withValue(
                                        ContactsContract.AggregationExceptions.TYPE,
                                        ContactsContract.AggregationExceptions.TYPE_KEEP_TOGETHER
                                    )
                                    .withValue(
                                        ContactsContract.AggregationExceptions.RAW_CONTACT_ID1,
                                        primaryRawId.toLong()
                                    )
                                    .withValue(
                                        ContactsContract.AggregationExceptions.RAW_CONTACT_ID2,
                                        rawIds[i].toLong()
                                    )
                                    .build()
                            )
                        }
                        if (ops.isNotEmpty()) {
                            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                            count++
                            mergedIndices.add(index)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                count
            }

            binding.progressBar.visibility = View.GONE
            binding.btnMerge.isEnabled = true

            if (successCount > 0) {
                // Remove merged groups from the list (in reverse order to keep indices valid)
                val mutableGroups = allGroups.toMutableList()
                mergedIndices.sortedDescending().forEach { idx ->
                    if (idx < mutableGroups.size) mutableGroups.removeAt(idx)
                }
                selectedGroups.clear()

                // Invalidate cache for current tab so next switch re-loads
                groupsCache.remove(currentTab)
                groupsCache[currentTab] = mutableGroups

                Toast.makeText(
                    this@MergeContactsActivity,
                    "Merged $successCount group(s) successfully",
                    Toast.LENGTH_SHORT
                ).show()

                displayGroups(mutableGroups)
            } else {
                Toast.makeText(
                    this@MergeContactsActivity,
                    "No groups were merged. Please check permissions.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}