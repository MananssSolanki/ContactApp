package com.example.contactapp.Activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.contactapp.Model.ContactEnhanced
import com.example.contactapp.R
import com.example.contactapp.databinding.ActivityContactInformationBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactInformationActivity : AppCompatActivity() {

    private lateinit var binding : ActivityContactInformationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.btnBack.setOnClickListener {
            finish()
        }

        lifecycleScope.launch {
            loadContact()
        }

    }

    private suspend fun loadContact() {

        // Simulate background work (safe if later you fetch from DB/API)
        val contact = withContext(Dispatchers.IO) {
            intent.getParcelableExtra<ContactEnhanced>("CONTACT_DATA")
        }

        contact?.let {
            displayContactInfo(it)
        }
    }

    private fun displayContactInfo(contact: ContactEnhanced) {

        binding.tvContactName.text = contact.name
        binding.tvContactNumber.text = contact.phoneNumber

        // Glide already async internally
        if (contact.photoUri != null) {
            Glide.with(this)
                .load(Uri.parse(contact.photoUri))
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(binding.ivContactImage)
        } else {
            binding.ivContactImage.setImageResource(R.drawable.ic_launcher_foreground)
        }

        // Email
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
    }
}