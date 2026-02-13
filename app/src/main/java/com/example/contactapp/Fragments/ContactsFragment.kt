package com.example.contactapp.Fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.telephony.SmsManager
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
import com.example.contactapp.Adapter.ContactsAdapter
import com.example.contactapp.ViewModel.ContactsViewModel
import com.example.contactapp.databinding.FragmentContactBinding
import com.example.contactapp.Utils.SwipeCallback

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
        setupFastScroller()
        checkPermissions()

        binding.btnAddContact.setOnClickListener {
           try {
               startActivity(viewModel.getAddContactIntent())
           } catch (e: Exception) {
               Toast.makeText(context, "Cannot open contacts app", Toast.LENGTH_SHORT).show()
           }
        }
    }

    private val smsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(context, "SMS permission denied", Toast.LENGTH_SHORT).show()
            }
        }



    fun openSmsApp(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
        }
        startActivity(intent)
    }


    fun makeVideoCall(phoneNumber: String) {

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

        val uri = Uri.fromParts("tel", phoneNumber, null)

        val extras = Bundle().apply {
            putInt(
                TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE,
                VideoProfile.STATE_BIDIRECTIONAL
            )
        }

        try {
            telecomManager.placeCall(uri, extras)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Video call not supported on this device",
                Toast.LENGTH_SHORT
            ).show()
        }
    }



    private fun setupRecyclerView() {
        contactsAdapter = ContactsAdapter()

        contactsAdapter.setOnCallClickListener { contact ->
            makeCall(contact.phoneNumber)
        }

        contactsAdapter.setOnSmsClickListener {contact ->
            openSmsApp(contact.phoneNumber)
        }

        contactsAdapter.setOnVideoCallClickListener { contact ->
            makeVideoCall(contact.phoneNumber)
        }

        contactsAdapter.setOnInformationClickListener { contact ->
            val intent = Intent(requireContext() , ContactInformationActivity::class.java).apply {
                putExtra("CONTACT_DATA", contact)
            }
            startActivity(intent)
        }


        binding.rvPhoneContacts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = contactsAdapter
        }

        val swipeHandler = object : SwipeCallback(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val contact = contactsAdapter.currentList[position]

                if (direction == ItemTouchHelper.LEFT) {
                    // SMS
                    sendSMS(contact.phoneNumber)
                    contactsAdapter.notifyItemChanged(position) // Reset swipe
                } else if (direction == ItemTouchHelper.RIGHT) {
                    // Call
                    makeCall(contact.phoneNumber)
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

    private fun setupFastScroller() {
        binding.alphabetScrollbar.onSectionSelected = { section ->
            val position = contactsAdapter.currentList.indexOfFirst { contact ->
                val firstChar = contact.name.firstOrNull()?.uppercaseChar()
                if (section == "#") {
                    firstChar?.isDigit() == true
                } else {
                    firstChar.toString() == section
                }
            }
            if (position != -1) {
                (binding.rvPhoneContacts.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
            }
        }
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

    private val callPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}

    private fun makeCall(phoneNumber : String){
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ){
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }

        val telecomManager = requireContext().getSystemService(TelecomManager::class.java)

        val uri = Uri.fromParts("tel", phoneNumber ,null)

        try{
            telecomManager.placeCall(uri, Bundle())
        }catch (e : Exception){
            Toast.makeText(requireContext(), "Call failed : ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendSMS(number: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$number")
        }
        startActivity(intent)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}