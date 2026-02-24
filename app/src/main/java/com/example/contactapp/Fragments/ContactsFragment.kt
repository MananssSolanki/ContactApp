package com.example.contactapp.Fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
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
import com.example.contactapp.Activities.ContactInformationActivity
import com.example.contactapp.Activities.AddContactActivity
import com.example.contactapp.Adapter.ContactsAdapter
import com.example.contactapp.Model.ContactListItemEnhanced
import com.example.contactapp.Utils.SwipeCallback
import com.example.contactapp.ViewModel.ContactsViewModel
import com.example.contactapp.databinding.FragmentContactBinding

class ContactsFragment : Fragment() {

    private var _binding: FragmentContactBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactsViewModel by viewModels()
    private lateinit var contactsAdapter: ContactsAdapter

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.READ_CONTACTS] == true) {
                viewModel.loadContacts(force = true)
            } else {
                binding.tvPermissionDenied.visibility = View.VISIBLE
                binding.tvEmptyState.visibility = View.VISIBLE
            }
        }

    /**
     * FIX: Launched when returning from ContactInformationActivity.
     * Only force-reloads if result was OK (contact was edited/deleted).
     */
    private val contactInfoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.invalidateCache()
                viewModel.loadContacts(force = true)
            }
        }

    /**
     * FIX: Launched when returning from AddContactActivity or system add-contact.
     * Invalidates cache and force-reloads so new contact appears immediately.
     */
    private val addContactLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK || result.resultCode == Activity.RESULT_FIRST_USER) {
                viewModel.invalidateCache()
                viewModel.loadContacts(force = true)
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
        setupFastScroller()
        checkPermissions()

        binding.btnAddContact.setOnClickListener {
            // Open in-app AddContactActivity instead of system contacts editor
            val intent = Intent(requireContext(), AddContactActivity::class.java)
            addContactLauncher.launch(intent)
        }
    }

    /**
     * FIX: onResume no longer unconditionally reloads.
     * ViewModel cache guard prevents duplicate loads.
     */
    override fun onResume() {
        super.onResume()
        // Let ViewModel decide based on cache validity
        val hasPermission = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.loadContacts() // cache guard inside ViewModel
        }
    }

    private fun setupRecyclerView() {
        contactsAdapter = ContactsAdapter().apply {
            setOnCallClickListener { makeCall(it.phoneNumber) }
            setOnSmsClickListener { openSmsApp(it.phoneNumber) }
            setOnVideoCallClickListener { makeVideoCall(it.phoneNumber) }
            setOnInformationClickListener { contact ->
                val intent = Intent(requireContext(), ContactInformationActivity::class.java).apply {
                    putExtra("CONTACT_DATA", contact)
                }
                contactInfoLauncher.launch(intent)
            }
            setOnEditClickListener { contact ->
                // Open in-app AddContactActivity in edit mode
                val intent = Intent(requireContext(), AddContactActivity::class.java).apply {
                    putExtra("CONTACT_DATA", contact)
                    putExtra("IS_EDIT_MODE", true)
                }
                addContactLauncher.launch(intent)
            }
        }

        binding.rvPhoneContacts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = contactsAdapter
        }

        /**
         * FIX: SwipeCallback draws LEFT=Call (green), RIGHT=SMS (blue).
         * Previously the fragment handler had them REVERSED.
         * Now: swipe LEFT → call, swipe RIGHT → SMS — matching the visual indicator.
         */
        val swipeHandler = object : SwipeCallback(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = contactsAdapter.currentList.getOrNull(position)
                if (item !is ContactListItemEnhanced.ContactItem) {
                    contactsAdapter.notifyItemChanged(position)
                    return
                }
                val contact = item.contact
                when (direction) {
                    ItemTouchHelper.LEFT -> makeCall(contact.phoneNumber)  // LEFT = Call (green)
                    ItemTouchHelper.RIGHT -> openSmsApp(contact.phoneNumber) // RIGHT = SMS (blue)
                }
                contactsAdapter.notifyItemChanged(position)
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvPhoneContacts)
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchContacts(newText ?: "")
                return true
            }
        })
    }

    private fun setupFastScroller() {
        binding.alphabetScrollbar.onSectionSelected = { section ->
            val position = when (section) {
                "★" -> contactsAdapter.currentList.indexOfFirst {
                    it is ContactListItemEnhanced.Header && it.isFavorite
                }
                "#" -> contactsAdapter.currentList.indexOfFirst {
                    it is ContactListItemEnhanced.Header && it.title == "#"
                }
                else -> contactsAdapter.currentList.indexOfFirst {
                    it is ContactListItemEnhanced.Header && it.title == section
                }
            }
            if (position != -1) {
                (binding.rvPhoneContacts.layoutManager as LinearLayoutManager)
                    .scrollToPositionWithOffset(position, 0)
            }
        }
    }

    private fun setupObservers() {
        viewModel.contactItems.observe(viewLifecycleOwner) { items ->
            contactsAdapter.submitList(items)
            binding.tvEmptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.loadContacts()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.SEND_SMS
                )
            )
        }
    }

    private val callPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) Toast.makeText(requireContext(), "Call permission denied", Toast.LENGTH_SHORT).show()
        }

    private fun makeCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            return
        }
        val telecomManager = requireContext().getSystemService(TelecomManager::class.java)
        val uri = Uri.fromParts("tel", phoneNumber, null)
        try {
            telecomManager.placeCall(uri, Bundle())
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Call failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSmsApp(phoneNumber: String) {
        startActivity(Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
        })
    }

    private fun makeVideoCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            return
        }
        val telecomManager = requireContext().getSystemService(TelecomManager::class.java)
        val uri = Uri.fromParts("tel", phoneNumber, null)
        val extras = Bundle().apply {
            putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, VideoProfile.STATE_BIDIRECTIONAL)
        }
        try {
            telecomManager.placeCall(uri, extras)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Video call failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}