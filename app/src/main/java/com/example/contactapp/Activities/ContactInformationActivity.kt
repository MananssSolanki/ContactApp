package com.example.contactapp.Activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactapp.Adapter.CallLogAdapter
import com.example.contactapp.Model.CallLogListItem
import com.example.contactapp.Repository.CallLogRepository
import com.example.contactapp.Model.ContactEnhanced
import com.example.contactapp.R
import com.example.contactapp.databinding.ActivityContactInformationBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactInformationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactInformationBinding
    private var contact: ContactEnhanced? = null
    private lateinit var callLogRepository: CallLogRepository
    private lateinit var callLogAdapterShort: CallLogAdapter

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        callLogRepository = CallLogRepository(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.btnBack.setOnClickListener { finish() }

        lifecycleScope.launch {
            loadContact()
        }

        binding.btnPhone.setOnClickListener {
            contact?.phoneNumber?.let { makeCall(it) }
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
            displayContactInfo(it)
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

        binding.btnMessage.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${contact.phoneNumber}")
            }
            startActivity(intent)
        }

        setupCallHistory(contact.phoneNumber)
    }

    private fun setupCallHistory(phoneNumber: String) {
        // Setup adapters
        callLogAdapterShort = CallLogAdapter(
            onCallClick = { makeCall(it) },
            style = 0 // New/History style
        )
        binding.rvCallHistoryShort.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@ContactInformationActivity)
            adapter = callLogAdapterShort
        }

        // Check permission
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
            
            // Update short list (max 10 items)
            // Note: Use take(10) but keep headers logic if possible, or just take first 10 items including headers
            val shortList = logs.take(10)
            callLogAdapterShort.submitList(shortList)

            binding.rvCallHistoryShort.visibility = if (shortList.isNotEmpty()) View.VISIBLE else View.GONE
            
            // Show "View More" if there are more items
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

    fun sendSMS(number : String){
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
