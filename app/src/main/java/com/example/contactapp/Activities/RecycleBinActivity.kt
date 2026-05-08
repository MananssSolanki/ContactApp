package com.example.contactapp.Activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactapp.Adapter.RecycleBinAdapter
import com.example.contactapp.Model.DeletedContact
import com.example.contactapp.Utils.RecycleBinManager
import com.example.contactapp.databinding.ActivityRecycleBinBinding
import kotlinx.coroutines.launch

class RecycleBinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecycleBinBinding
    private lateinit var adapter: RecycleBinAdapter
    private lateinit var recycleBinManager: RecycleBinManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecycleBinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        recycleBinManager = RecycleBinManager(this)

        setupRecyclerView()
        setupButtons()
        loadDeletedContacts()
    }

    private fun setupRecyclerView() {
        adapter = RecycleBinAdapter(
            onRestoreClick = { contact -> restoreContact(contact) },
            onDeleteClick  = { contact -> confirmPermanentDelete(contact) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnEmptyBin.setOnClickListener {
            if (adapter.itemCount == 0) {
                Toast.makeText(this, "Recycle bin is already empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(this)
                .setTitle("Empty Recycle Bin")
                .setMessage("Permanently delete all ${adapter.itemCount} contacts? This cannot be undone.")
                .setPositiveButton("Empty Bin") { _, _ -> emptyBin() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Load
    // ─────────────────────────────────────────────────────────────────────────
    private fun loadDeletedContacts() {
        lifecycleScope.launch {
            val deleted = recycleBinManager.getDeletedContacts()
            adapter.submitList(deleted)
            updateEmptyState(deleted.isEmpty())
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.layoutEmpty.visibility        = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility       = if (isEmpty) View.GONE   else View.VISIBLE
        binding.layoutBottomActions.visibility = if (isEmpty) View.GONE   else View.VISIBLE
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Restore
    // ─────────────────────────────────────────────────────────────────────────
    private fun restoreContact(contact: DeletedContact) {
        lifecycleScope.launch {
            val success = recycleBinManager.restoreContact(contact)
            if (success) {
                Toast.makeText(this@RecycleBinActivity,
                    "${contact.name} restored", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)   // tells ContactsFragment to refresh
                loadDeletedContacts()
            } else {
                Toast.makeText(this@RecycleBinActivity,
                    "Restore failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Permanent delete (single contact)
    // ─────────────────────────────────────────────────────────────────────────
    private fun confirmPermanentDelete(contact: DeletedContact) {
        AlertDialog.Builder(this)
            .setTitle("Delete Permanently")
            .setMessage("\"${contact.name}\" will be permanently deleted from your device. This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    // Uses ContentUris.withAppendedId internally — see RecycleBinManager
                    val success = recycleBinManager.permanentlyDelete(contact)
                    // Reload list either way — the entry is always removed from Room DB
                    loadDeletedContacts()
                    setResult(RESULT_OK)
                    if (success) {
                        Toast.makeText(this@RecycleBinActivity,
                            "${contact.name} permanently deleted", Toast.LENGTH_SHORT).show()
                    } else {
                        // Contact was already gone from system; still cleaned up from bin
                        Toast.makeText(this@RecycleBinActivity,
                            "${contact.name} removed from bin", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Empty entire bin
    // ─────────────────────────────────────────────────────────────────────────
    private fun emptyBin() {
        lifecycleScope.launch {
            recycleBinManager.emptyBin()
            loadDeletedContacts()
            setResult(RESULT_OK)
            Toast.makeText(this@RecycleBinActivity,
                "Recycle bin emptied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}