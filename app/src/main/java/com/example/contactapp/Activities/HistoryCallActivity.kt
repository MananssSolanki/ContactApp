package com.example.contactapp.Activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.contactapp.Adapter.CallLogAdapter
import com.example.contactapp.R
import com.example.contactapp.Repository.BlockedNumberRepository
import com.example.contactapp.Repository.CallLogRepository
import kotlinx.coroutines.launch

class HistoryCallActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var callLogRepository: CallLogRepository
    private lateinit var blockedNumberRepository: BlockedNumberRepository
    private lateinit var callLogAdapter: CallLogAdapter
    private var phoneNumber: String? = null

    // UI Elements
    private lateinit var toolbar: View
    private lateinit var selectionToolbar: LinearLayout
    private lateinit var tvSelectionCount: TextView
    private lateinit var cbSelectAll: CheckBox

    private val callPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                // Permission granted
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history_call)

        phoneNumber = intent.getStringExtra("PHONE_NUMBER")

        initView()
        setupRecyclerView()
        loadCallLogs()
    }

    private fun initView() {
        callLogRepository = CallLogRepository(this)
        blockedNumberRepository = BlockedNumberRepository(this)
        
        toolbar = findViewById(R.id.toolbar)
        selectionToolbar = findViewById(R.id.ll_selection_toolbar)
        tvSelectionCount = findViewById(R.id.tv_selection_count)
        cbSelectAll = findViewById(R.id.cb_select_all)

        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
        
        findViewById<ImageView>(R.id.btn_cancel_selection).setOnClickListener {
            callLogAdapter.clearSelection()
            updateUI(false)
        }

        findViewById<ImageView>(R.id.btn_delete_selected).setOnClickListener {
            deleteSelectedLogs()
        }

        cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) callLogAdapter.selectAll() else callLogAdapter.clearSelection()
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rv_call_history)
        callLogAdapter = CallLogAdapter(
            onCallClick = { makeCall(it) },
            onSelectionChanged = { count ->
                tvSelectionCount.text = "$count selected"
                updateUI(count > 0 || callLogAdapter.isSelectionMode)
            },
            style = 0 // History style
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = callLogAdapter
    }

    private fun updateUI(isSelectionMode: Boolean) {
        if (isSelectionMode) {
            toolbar.visibility = View.GONE
            selectionToolbar.visibility = View.VISIBLE
        } else {
            toolbar.visibility = View.VISIBLE
            selectionToolbar.visibility = View.GONE
            cbSelectAll.isChecked = false
        }
    }

    private fun loadCallLogs() {
        val progressBar = findViewById<View>(R.id.progressBar)
        val tvEmpty = findViewById<TextView>(R.id.tv_empty)
        val openInSelectionMode = intent.getBooleanExtra("OPEN_IN_SELECTION_MODE", false)

        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val logs = callLogRepository.getCallLogs(phoneNumber)
            progressBar.visibility = View.GONE

            if (logs.isNotEmpty()) {
                callLogAdapter.submitList(logs)
                recyclerView.visibility = View.VISIBLE
                tvEmpty.visibility = View.GONE
                if (openInSelectionMode) {
                    callLogAdapter.isSelectionMode = true
                    updateUI(true)
                }
            } else {
                recyclerView.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun deleteSelectedLogs() {
        val ids = callLogAdapter.selectedIds.toList()
        if (ids.isEmpty()) {
            Toast.makeText(this, "Select items to delete", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val success = callLogRepository.deleteCallLogs(ids)
            if (success) {
                Toast.makeText(this@HistoryCallActivity, "Deleted ${ids.size} items", Toast.LENGTH_SHORT).show()
                callLogAdapter.removeItemsByIdAndExitSelection(ids.toSet())
                updateUI(false)
            } else {
                Toast.makeText(this@HistoryCallActivity, "Failed to delete items", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun makeCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            return
        }
        lifecycleScope.launch {
            val blocked = blockedNumberRepository.isBlocked(phoneNumber)
            if (blocked) {
                Toast.makeText(this@HistoryCallActivity, "This number is blocked.", Toast.LENGTH_SHORT).show()
            } else {
                makeCallInternal(phoneNumber)
            }
        }
    }

    private fun makeCallInternal(phoneNumber: String) {
        val telecomManager = getSystemService(TelecomManager::class.java)
        val uri = Uri.fromParts("tel", phoneNumber, null)
        try {
            telecomManager.placeCall(uri, Bundle())
        } catch (e: Exception) {
            Toast.makeText(this, "Call failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (callLogAdapter.isSelectionMode) {
            callLogAdapter.clearSelection()
            updateUI(false)
        } else {
            super.onBackPressed()
        }
    }
}
