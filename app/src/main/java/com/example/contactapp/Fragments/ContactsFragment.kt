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
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.contactapp.Activities.AddContactActivity
import com.example.contactapp.Activities.ContactInformationActivity
import com.example.contactapp.Activities.ManageContactsActivity
import com.example.contactapp.Activities.RecycleBinActivity
import com.example.contactapp.Activities.SearchActivity
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

    private val contactInfoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.invalidateCache()
                viewModel.loadContacts(force = true)
            }
        }

    private val addContactLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK || result.resultCode == Activity.RESULT_FIRST_USER) {
                viewModel.invalidateCache()
                viewModel.loadContacts(force = true)
            }
        }

    private val recycleBinLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
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
        setupSearchAndMenu()
        setupFastScroller()
        checkPermissions()

        binding.btnAddContact.setOnClickListener {
            val intent = Intent(requireContext(), AddContactActivity::class.java)
            addContactLauncher.launch(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        val hasPermission = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.loadContacts()
        }
    }

    /**
     * Sets up the search icon (opens SearchActivity) and
     * the 3-dot more-options popup menu with:
     *   Edit | Manage contacts | Recycle bin | Contacts settings | Settings
     */
    private fun setupSearchAndMenu() {
        // Search icon tap → open SearchActivity
        binding.searchView.setOnClickListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }

        // More options (⋮) → popup menu
        binding.imgMoreOptions.setOnClickListener { anchor ->
            val popup = PopupMenu(requireContext(), anchor)
            popup.menuInflater.inflate(com.example.contactapp.R.menu.contact_more_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    com.example.contactapp.R.id.menu_edit -> {
                        // Enter multi-selection edit mode
                        Toast.makeText(requireContext(), "Select contacts to edit", Toast.LENGTH_SHORT).show()
                        true
                    }
                    com.example.contactapp.R.id.menu_manage_contacts -> {
                        startActivity(Intent(requireContext(), ManageContactsActivity::class.java))
                        true
                    }
                    com.example.contactapp.R.id.menu_recycle_bin -> {
                        recycleBinLauncher.launch(Intent(requireContext(), RecycleBinActivity::class.java))
                        true
                    }
                    com.example.contactapp.R.id.menu_contacts_settings -> {
                        // Open Android system contact settings
                        try {
                            startActivity(Intent(android.provider.Settings.ACTION_SYNC_SETTINGS))
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Settings not available", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    com.example.contactapp.R.id.menu_settings -> {
                        try {
                            startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${requireContext().packageName}")
                            })
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Settings not available", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    else -> false
                }
            }
            popup.show()
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
                val intent = Intent(requireContext(), AddContactActivity::class.java).apply {
                    putExtra("CONTACT_DATA", contact)
                    putExtra("IS_EDIT_MODE", true)
                }
                addContactLauncher.launch(intent)
            }
            setOnSelectionChangeListener { count ->
                updateSelectionUI(count)
            }
        }

        binding.selectionToolbar.visibility = View.GONE
        binding.bottomActionBar.visibility = View.GONE

        binding.btnCancelSelection.setOnClickListener { contactsAdapter.clearSelection() }
        binding.cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) contactsAdapter.selectAll() else contactsAdapter.clearSelection()
        }

        binding.btnDelete.setOnClickListener { deleteSelectedContacts() }
        binding.btnMessage.setOnClickListener { messageSelectedContacts() }
        binding.btnShare.setOnClickListener { shareSelectedContacts() }
        binding.btnMeet.setOnClickListener { meetSelectedContacts() }

        binding.rvPhoneContacts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = contactsAdapter
        }

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
                    ItemTouchHelper.LEFT -> makeCall(contact.phoneNumber)
                    ItemTouchHelper.RIGHT -> openSmsApp(contact.phoneNumber)
                }
                contactsAdapter.notifyItemChanged(position)
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvPhoneContacts)
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
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.fromParts("tel", phoneNumber, null)
            }
            startActivity(intent)
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
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.fromParts("tel", phoneNumber, null)
                putExtra(android.telecom.TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, android.telecom.VideoProfile.STATE_BIDIRECTIONAL)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Video call failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSelectionUI(count: Int) {
        if (count > 0) {
            binding.toolbar.visibility = View.GONE
            binding.selectionToolbar.visibility = View.VISIBLE
            binding.bottomActionBar.visibility = View.VISIBLE
            binding.tvSelectionCount.text = "$count selected"
            binding.cbSelectAll.isChecked = count == contactsAdapter.currentList
                .filterIsInstance<ContactListItemEnhanced.ContactItem>().size
        } else {
            binding.toolbar.visibility = View.VISIBLE
            binding.selectionToolbar.visibility = View.GONE
            binding.bottomActionBar.visibility = View.GONE
            binding.cbSelectAll.isChecked = false
        }
    }

    private val deleteContactsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val ids = contactsAdapter.selectedIds.toList()
            if (ids.isNotEmpty()) {
                executeDeleteContacts(ids)
            }
        } else {
            Toast.makeText(requireContext(), "Permission required to delete contacts", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteSelectedContacts() {
        val ids = contactsAdapter.selectedIds.toList()
        if (ids.isEmpty()) return

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Contacts")
            .setMessage("Are you sure you want to delete ${ids.size} contacts?")
            .setPositiveButton("Delete") { _, _ ->
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.WRITE_CONTACTS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    executeDeleteContacts(ids)
                } else {
                    deleteContactsPermissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun executeDeleteContacts(ids: List<String>) {
        viewModel.deleteSelectedContacts(ids) { success ->
            if (success) {
                Toast.makeText(context, "Deleted ${ids.size} contacts", Toast.LENGTH_SHORT).show()
                contactsAdapter.clearSelection()
            } else {
                Toast.makeText(context, "Failed to delete contacts", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun messageSelectedContacts() {
        val selectedContacts = contactsAdapter.currentList
            .filterIsInstance<ContactListItemEnhanced.ContactItem>()
            .filter { it.contact.contactId in contactsAdapter.selectedIds }
        if (selectedContacts.isEmpty()) return
        val phoneNumbers = selectedContacts.joinToString(";") { it.contact.phoneNumber }
        openSmsApp(phoneNumbers)
        contactsAdapter.clearSelection()
    }

    private fun shareSelectedContacts() {
        val selectedContacts = contactsAdapter.currentList
            .filterIsInstance<ContactListItemEnhanced.ContactItem>()
            .filter { it.contact.contactId in contactsAdapter.selectedIds }
        if (selectedContacts.isEmpty()) return
        val shareText = selectedContacts.joinToString("\n\n") {
            "${it.contact.name}: ${it.contact.phoneNumber}"
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Share Contacts"))
        contactsAdapter.clearSelection()
    }

    private fun meetSelectedContacts() {
        Toast.makeText(context, "Meet feature coming soon", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}