package com.example.contactapp.Activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactapp.Adapter.SearchAdapter
import com.example.contactapp.ViewModel.SearchViewModel
import com.example.contactapp.databinding.ActivitySearchBinding

class SearchActivity : AppCompatActivity() {

    private lateinit var b: ActivitySearchBinding
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var adapter: SearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Force white theme for this activity as requested
        b.root.setBackgroundColor(android.graphics.Color.WHITE)

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        adapter = SearchAdapter().apply {
            setOnHistoryClickListener { 
                b.etSearch.setText(it)
                b.etSearch.setSelection(it.length)
                viewModel.search(it)
            }
            setOnHistoryRemoveClickListener { viewModel.removeFromHistory(it) }
            setOnContactClickListener { contact ->
                viewModel.addToHistory(b.etSearch.text.toString())
            }
            
            setOnCallClickListener { contact ->
                makeCall(contact.phoneNumber)
            }

            setOnSmsClickListener { openSmsApp(it.phoneNumber) }
            setOnVideoCallClickListener { makeVideoCall(it.phoneNumber) }
            
            setOnInformationClickListener { contact ->
                val intent = Intent(this@SearchActivity, ContactInformationActivity::class.java).apply {
                    putExtra("CONTACT_DATA", contact)
                }
                startActivity(intent)
            }

            setOnCallLogClickListener { cl ->
                viewModel.addToHistory(b.etSearch.text.toString())
            }
        }

        b.rvSearchResults.layoutManager = LinearLayoutManager(this)
        b.rvSearchResults.adapter = adapter

        b.btnBack.setOnClickListener { finish() }
        b.btnClear.setOnClickListener { b.etSearch.text.clear() }

        b.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s.toString()
                b.btnClear.visibility = if (q.isNotEmpty()) View.VISIBLE else View.GONE
                viewModel.search(q)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        b.etSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val q = b.etSearch.text.toString()
                if (q.isNotBlank()) viewModel.addToHistory(q)
                true
            } else false
        }
        
        b.etSearch.requestFocus()
    }

    private val callPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) Toast.makeText(this@SearchActivity, "Call permission denied", Toast.LENGTH_SHORT).show()
        }
    private fun makeCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(this@SearchActivity ,  Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            return
        }
        val telecomManager = this@SearchActivity.getSystemService(TelecomManager::class.java)
        val uri = Uri.fromParts("tel", phoneNumber, null)
        try {
            telecomManager.placeCall(uri, Bundle())
        } catch (e: Exception) {
            Toast.makeText(this@SearchActivity, "Call failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        viewModel.searchItems.observe(this) { 
            adapter.submitList(it) 
        }
        viewModel.isLoading.observe(this) {
            b.progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }
    }

    private fun openSmsApp(phoneNumber: String) {
        startActivity(Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
        })
    }

    private fun makeVideoCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(this@SearchActivity, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            return
        }
        val telecomManager = this@SearchActivity.getSystemService(TelecomManager::class.java)
        val uri = Uri.fromParts("tel", phoneNumber, null)
        val extras = Bundle().apply {
            putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, VideoProfile.STATE_BIDIRECTIONAL)
        }
        try {
            telecomManager.placeCall(uri, extras)
        } catch (e: Exception) {
            Toast.makeText(this@SearchActivity, "Video call failed", Toast.LENGTH_SHORT).show()
        }
    }
}
