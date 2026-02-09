package com.example.contactapp.ui.contacts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
import com.example.contactapp.utils.SwipeCallback


class ContactsFragment : Fragment() {

    private var _binding: FragmentContactBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactsViewModel by viewModels()
    private lateinit var contactsAdapter: ContactsAdapter

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val readContactsGranted = permissions[Manifest.permission.READ_CONTACTS] ?: false
            if (readContactsGranted) {
                viewModel.loadContacts()
            } else {
                Toast.makeText(context, "Read contacts permission is required", Toast.LENGTH_SHORT).show()
                binding.tvPermissionDenied.visibility = View.VISIBLE
                binding.tvEmptyState.text = "Permission denied"
                binding.tvEmptyState.visibility = View.VISIBLE
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
        super.onViewCreated(view, savedInstanceState)

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
            onContactClick = { contact ->
                // Handle contact click
            },
            onCallClick = { contact ->
                makePhoneCall(contact.phoneNumber)
            }
        )

        binding.rvPhoneContacts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = contactsAdapter
        }

        val swipeHandler = object : SwipeCallback(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val contact = contactsAdapter.currentList[position]

                if (direction == ItemTouchHelper.LEFT) {
                    // Call
                   makePhoneCall(contact.phoneNumber)
                   contactsAdapter.notifyItemChanged(position) // Reset swipe
                } else if (direction == ItemTouchHelper.RIGHT) {
                    // SMS
                    sendSMS(contact.phoneNumber)
                    contactsAdapter.notifyItemChanged(position) // Reset swipe
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.rvPhoneContacts)
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                // Implementing basic search filter if needed, or pass to ViewModel
                // For now, leaving it plain as ViewModel update for Search is separate constraint
                return true
            }
        })
    }

    private fun setupObservers() {
        viewModel.contacts.observe(viewLifecycleOwner) { contacts ->
            contactsAdapter.submitList(contacts)
            binding.tvEmptyState.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
            binding.tvPermissionDenied.visibility = View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun checkPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.loadContacts()
            }
            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.SEND_SMS
                    )
                )
            }
        }
    }

    private fun makePhoneCall(number: String) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
            startActivity(intent)
        } else {
             Toast.makeText(context, "Call permission required", Toast.LENGTH_SHORT).show()
             requestPermissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
        }
    }

    private fun sendSMS(number: String) {
         if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
             val intent = Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", number, null))
             startActivity(intent)
        } else {
             Toast.makeText(context, "SMS permission required", Toast.LENGTH_SHORT).show()
             requestPermissionLauncher.launch(arrayOf(Manifest.permission.SEND_SMS))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
