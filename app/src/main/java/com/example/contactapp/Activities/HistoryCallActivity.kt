package com.example.contactapp.Activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.contactapp.Adapter.CallLogAdapter
import com.example.contactapp.R
import com.example.contactapp.Repository.CallLogRepository
import kotlinx.coroutines.launch

class HistoryCallActivity : AppCompatActivity() {

    private lateinit var callLogRepository: CallLogRepository
    private lateinit var callLogAdapter: CallLogAdapter
    private var phoneNumber: String? = null

    private val callPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                // Permission granted, can now make calls
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

        val btnBack = findViewById<ImageView>(R.id.btn_back)
        btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.rv_call_history)
        callLogAdapter = CallLogAdapter(onCallClick = { number ->
            makeCall(number)
        })

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = callLogAdapter
    }

    private fun loadCallLogs() {
        val progressBar = findViewById<View>(R.id.progressBar)
        val tvEmpty = findViewById<TextView>(R.id.tv_empty)
        val recyclerView = findViewById<RecyclerView>(R.id.rv_call_history)

        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val logs = callLogRepository.getCallLogs(phoneNumber)
            
            progressBar.visibility = View.GONE
            
            if (logs.isNotEmpty()) {
                callLogAdapter.submitList(logs)
                recyclerView.visibility = View.VISIBLE
                tvEmpty.visibility = View.GONE
            } else {
                recyclerView.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun makeCall(phoneNumber: String) {
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
