package com.example.contactapp.Fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactapp.Model.ContactListItem
import com.example.contactapp.Adapter.PhoneContactAdapter
import com.example.contactapp.R
import com.example.contactapp.ViewModel.ContactViewModel
import com.example.contactapp.databinding.FragmentContactBinding

class ContactFragment : Fragment() {

    private lateinit var binding: FragmentContactBinding
    private lateinit var viewModel: ContactViewModel
    private lateinit var adapter: PhoneContactAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[ContactViewModel::class.java]

        setupRecyclerView()
        setupSearchView()
        setupAlphabeticScrollbar()
        setupAddContactButton()
        observeData()
        checkPermissionAndLoad()
    }

    private fun setupRecyclerView() {
        adapter = PhoneContactAdapter(
            onContactClick = { contactItem ->
                // TODO: Open contact details
                Toast.makeText(
                    requireContext(),
                    "Contact: ${contactItem.contact.name}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onCallClick = { phoneNumber ->
                makeCall(phoneNumber)
            }
        )

        binding.rvPhoneContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPhoneContacts.adapter = adapter
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchContacts(newText ?: "")
                return true
            }
        })
    }

    private fun setupAlphabeticScrollbar() {
        // Create alphabet letters
        val alphabet = ('A'..'Z').toList() + '#'
        
        alphabet.forEach { letter ->
            val textView = TextView(requireContext()).apply {
                text = letter.toString()
                textSize = 10f
                setTextColor(Color.parseColor("#00796B"))
                setPadding(4, 2, 4, 2)
                
                setOnClickListener {
                    scrollToSection(letter.toString())
                }
            }
            binding.alphabetScrollbar.addView(textView)
        }
    }

    private fun scrollToSection(letter: String) {
        val positions = viewModel.getSectionPositions()
        val position = positions[letter]
        if (position != null) {
            binding.rvPhoneContacts.scrollToPosition(position)
        }
    }

    private fun setupAddContactButton() {
        binding.btnAddContact.setOnClickListener {
            // TODO: Open add contact activity
            Toast.makeText(
                requireContext(),
                "Add contact feature coming soon",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun observeData() {
        viewModel.contactListItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            
            // Show empty state if no contacts
            if (items.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvPhoneContacts.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvPhoneContacts.visibility = View.VISIBLE
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun checkPermissionAndLoad() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            binding.tvPermissionDenied.visibility = View.GONE
            viewModel.loadPhoneContacts()
        } else {
            binding.tvPermissionDenied.visibility = View.VISIBLE
            binding.rvPhoneContacts.visibility = View.GONE
        }
    }

    private fun makeCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Failed to make call: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                requireContext(),
                "Call permission required",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload contacts when fragment resumes
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.loadPhoneContacts()
        }
    }
}