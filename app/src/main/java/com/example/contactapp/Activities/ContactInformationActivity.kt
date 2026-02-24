package com.example.contactapp.Activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.contactapp.Adapter.CallLogAdapter
import com.example.contactapp.Model.ContactEnhanced
import com.example.contactapp.R
import com.example.contactapp.Repository.BlockedNumberRepository
import com.example.contactapp.Repository.CallLogRepository
import com.example.contactapp.Repository.ContactsRepository
import com.example.contactapp.databinding.ActivityContactInformationBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactInformationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactInformationBinding
    private var contact: ContactEnhanced? = null
    private lateinit var callLogRepository: CallLogRepository
    private lateinit var blockedNumberRepository: BlockedNumberRepository
    private lateinit var contactsRepository: ContactsRepository
    private lateinit var callLogAdapterShort: CallLogAdapter
    private var isFavorite = false
    private var isBlocked = false

    private val callPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                contact?.phoneNumber?.let { makeCallInternal(it) }
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val readCallLogPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                binding.layoutPermissionDenied.visibility = View.GONE
                contact?.phoneNumber?.let { loadCallLogs(it) }
            } else {
                binding.layoutPermissionDenied.visibility = View.VISIBLE
            }
        }

    // Launch AddContactActivity in edit mode and reload if saved
    private val editContactLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Signal to calling fragment to reload contacts
                setResult(RESULT_OK)
                
                // Reload contact info from repository to get latest data
                contact?.let { c ->
                    lifecycleScope.launch {
                        val updatedContact = contactsRepository.getContact(c.contactId)
                        updatedContact?.let {
                            contact = it
                            displayContactInfo(it)
                            updateFavoriteIcon()
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        callLogRepository = CallLogRepository(this)
        blockedNumberRepository = BlockedNumberRepository(this)
        contactsRepository = ContactsRepository(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.btnBack.setOnClickListener { finish() }

        lifecycleScope.launch {
            loadContact()
        }

        binding.btnPhone.setOnClickListener {
            contact?.phoneNumber?.let { phone ->
                handleCallWithBlockCheck(phone)
            }
        }

        binding.btnMessage.setOnClickListener {
            contact?.phoneNumber?.let { sendSMS(it) }
        }

        binding.btnGrantPermission.setOnClickListener {
            readCallLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
        }

        binding.btnViewMoreHistory.setOnClickListener {
            val intent = Intent(this, HistoryCallActivity::class.java).apply {
                putExtra("PHONE_NUMBER", contact?.phoneNumber)
            }
            startActivity(intent)
        }

        // ⭐ Favorite Toggle
        binding.btnFavorite.setOnClickListener {
            val c = contact ?: return@setOnClickListener
            lifecycleScope.launch {
                val success = contactsRepository.toggleFavorite(c.contactId, !isFavorite)
                if (success) {
                    isFavorite = !isFavorite
                    updateFavoriteIcon()
                    setResult(RESULT_OK) // Signal fragment to reload
                    Toast.makeText(
                        this@ContactInformationActivity,
                        if (isFavorite) "Added to favorites" else "Removed from favorites",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // 📤 Share Contact
        binding.btnShare.setOnClickListener {
            contact?.let { shareContact(it) }
        }

        // ✏️ Edit Contact
        binding.btnEdit.setOnClickListener {
            contact?.let { c ->
                val intent = Intent(this, AddContactActivity::class.java).apply {
                    putExtra("CONTACT_DATA", c)
                    putExtra("IS_EDIT_MODE", true)
                }
                editContactLauncher.launch(intent)
            }
        }

        // ⋮ More Options
        binding.btnMore.setOnClickListener { view ->
            showMoreOptionsMenu(view)
        }
    }

    private suspend fun loadContact() {
        val data = withContext(Dispatchers.Default) {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra("CONTACT_DATA", ContactEnhanced::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<ContactEnhanced>("CONTACT_DATA")
            }
        }

        data?.let {
            contact = it
            isFavorite = it.isFavorite
            // Check blocked status
            isBlocked = blockedNumberRepository.isBlocked(it.phoneNumber)
            displayContactInfo(it)
            updateFavoriteIcon()
        }
    }

    private fun displayContactInfo(contact: ContactEnhanced) {
        binding.tvContactName.text = contact.name
        binding.tvContactNumber.text = contact.phoneNumber

        if (contact.photoUri != null) {
            Glide.with(this)
                .load(Uri.parse(contact.photoUri))
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(binding.ivContactImage)
        } else {
            binding.ivContactImage.setImageResource(R.drawable.ic_launcher_foreground)
        }

        if (!contact.email.isNullOrEmpty()) {
            binding.layoutEmail.visibility = View.VISIBLE
            binding.tvContactEmail.text = contact.email
        } else {
            binding.layoutEmail.visibility = View.GONE
        }

        setupCallHistory(contact.phoneNumber)
    }

    private fun updateFavoriteIcon() {
        binding.btnFavorite.setImageResource(
            if (isFavorite) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )
    }

    // 📤 Share Contact Implementation
    private fun shareContact(contact: ContactEnhanced) {
        val shareText = "${contact.name}\n${contact.phoneNumber}"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Contact: ${contact.name}")
        }
        startActivity(Intent.createChooser(shareIntent, "Share Contact via"))
    }

    // ⋮ More Options Popup Menu
    private fun showMoreOptionsMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.menu_contact_info, popup.menu)

        // Change "Block Number" label based on current blocked state
        popup.menu.findItem(R.id.action_block)?.title =
            if (isBlocked) "Unblock Number" else "Block Number"

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_block -> {
                    handleBlockUnblock()
                    true
                }
                R.id.action_delete_history -> {
                    openDeleteHistory()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    // 🚫 Block / Unblock (Room blocked_numbers table)
    private fun handleBlockUnblock() {
        val phone = contact?.phoneNumber ?: return
        lifecycleScope.launch {
            val success = if (isBlocked) {
                blockedNumberRepository.unblockNumber(phone)
            } else {
                blockedNumberRepository.blockNumber(phone)
            }
            if (success) {
                isBlocked = !isBlocked
                Toast.makeText(
                    this@ContactInformationActivity,
                    if (isBlocked) "Number blocked successfully" else "Number unblocked",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this@ContactInformationActivity, "Operation failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🗑️ Delete From History → Open HistoryCallActivity in selection mode
    private fun openDeleteHistory() {
        val phone = contact?.phoneNumber ?: return
        val intent = Intent(this, HistoryCallActivity::class.java).apply {
            putExtra("PHONE_NUMBER", phone)
            putExtra("OPEN_IN_SELECTION_MODE", true)
        }
        startActivity(intent)
    }

    // Check blocked before calling
    private fun handleCallWithBlockCheck(phoneNumber: String) {
        lifecycleScope.launch {
            val blocked = blockedNumberRepository.isBlocked(phoneNumber)
            if (blocked) {
                Toast.makeText(
                    this@ContactInformationActivity,
                    "This number is blocked. Unblock from ⋮ menu.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                makeCall(phoneNumber)
            }
        }
    }

    private fun setupCallHistory(phoneNumber: String) {
        callLogAdapterShort = CallLogAdapter(
            onCallClick = { handleCallWithBlockCheck(it) },
            style = 0
        )
        binding.rvCallHistoryShort.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@ContactInformationActivity)
            adapter = callLogAdapterShort
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            binding.layoutPermissionDenied.visibility = View.GONE
            loadCallLogs(phoneNumber)
        } else {
            binding.layoutPermissionDenied.visibility = View.VISIBLE
            binding.rvCallHistoryShort.visibility = View.GONE
            binding.btnViewMoreHistory.visibility = View.GONE
        }
    }

    private fun loadCallLogs(phoneNumber: String) {
        lifecycleScope.launch {
            val logs = callLogRepository.getCallLogs(phoneNumber)
            val shortList = logs.take(10)
            callLogAdapterShort.submitList(shortList)
            binding.rvCallHistoryShort.visibility = if (shortList.isNotEmpty()) View.VISIBLE else View.GONE
            binding.btnViewMoreHistory.visibility = if (logs.size > 10) View.VISIBLE else View.GONE
        }
    }

    fun makeCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            return
        }
        makeCallInternal(phoneNumber)
    }

    fun sendSMS(number: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${number}")
        }
        startActivity(intent)
    }

    @RequiresPermission(Manifest.permission.CALL_PHONE)
    private fun makeCallInternal(phoneNumber: String) {
        val telecomManager = getSystemService(TelecomManager::class.java)
        val uri = Uri.fromParts("tel", phoneNumber, null)
        try {
            telecomManager.placeCall(uri, Bundle())
        } catch (e: Exception) {
            Toast.makeText(this, "Call failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
