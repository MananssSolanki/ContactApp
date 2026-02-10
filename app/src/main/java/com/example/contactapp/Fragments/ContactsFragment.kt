package com.example.contactapp.Fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.contactapp.databinding.FragmentContactBinding
import com.example.contactapp.Adapter.ContactsAdapter
import com.example.contactapp.ViewModel.ContactsViewModel
import com.example.contactapp.utils.SwipeCallback

class ContactsFragment : Fragment() {

    private var _binding: FragmentContactBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactsViewModel by viewModels()
    private lateinit var contactsAdapter: ContactsAdapter

    // Multi-permission launcher
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    // Call permission launcher
    private val callPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(context, "Call permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupObservers()
        setupSearchView()
        checkPermissions()

        binding.btnAddContact.setOnClickListener {
            try {
                startActivity(viewModel.getAddContactIntent())
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot open contacts app", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        contactsAdapter = ContactsAdapter(
            onContactClick = {},
            onCallClick = { makeCall(it.phoneNumber) }
        )

        binding.rvPhoneContacts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = contactsAdapter
        }

        val swipeHandler = object : SwipeCallback(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val contact = contactsAdapter.currentList[position]

                when (direction) {
                    ItemTouchHelper.LEFT -> makeCall(contact.phoneNumber)
                    ItemTouchHelper.RIGHT -> sendSMS(contact.phoneNumber)
                }

                contactsAdapter.notifyItemChanged(position)
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvPhoneContacts)
    }

    private fun setupObservers() {
        viewModel.contacts.observe(viewLifecycleOwner) {
            contactsAdapter.submitList(it)
            binding.tvEmptyState.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?) = true
        })
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS
        )

        permissionLauncher.launch(permissions)

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.loadContacts()
        }
    }

    private fun makeCall(phone: String) {

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            return
        }

        val telecomManager =
            requireContext().getSystemService(TelecomManager::class.java)

        val uri = Uri.fromParts("tel", phone, null)

        try {
            telecomManager.placeCall(uri, Bundle())
        } catch (e: Exception) {
            Toast.makeText(context, "Call failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendSMS(number: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$number"))
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
