package com.example.contactapp.Activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.contactapp.databinding.ActivityManageContactBinding

class ManageContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageContactBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage contacts"

        binding.layoutMergeContacts.setOnClickListener {
            startActivity(Intent(this, MergeContactsActivity::class.java))
        }

        binding.layoutImportContacts.setOnClickListener {
            startActivity(Intent(this, ImportContactsActivity::class.java))
        }

//        binding.layoutExportContacts.setOnClickListener {
//            startActivity(Intent(this, ExportContactsActivity::class.java))
//        }
//
//        binding.layoutMoveContacts.setOnClickListener {
//            startActivity(Intent(this, MoveContactsActivity::class.java))
//        }
//
//        binding.layoutSyncContacts.setOnClickListener {
//            startActivity(Intent(this, SyncContactsActivity::class.java))
//        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}