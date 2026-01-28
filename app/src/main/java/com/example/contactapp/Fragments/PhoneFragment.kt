package com.example.contactapp.Fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactapp.Adapter.PhoneContactAdapter
import com.example.contactapp.Model.PhoneContact
import com.example.contactapp.databinding.FragmentPhoneBinding

class PhoneFragment : Fragment() {

    private lateinit var binding: FragmentPhoneBinding
    private val adapter = PhoneContactAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPhoneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.rvPhoneContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPhoneContacts.adapter = adapter
        
        loadContacts()
    }

    override fun onResume() {
        super.onResume()
        loadContacts()
    }

    private fun loadContacts() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            binding.tvPermissionDenied.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
            
            val contacts = ArrayList<PhoneContact>()
            val cursor = requireContext().contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            
            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                
                while (it.moveToNext()) {
                    val name = if (nameIndex != -1) it.getString(nameIndex) else "Unknown"
                    val number = if (numberIndex != -1) it.getString(numberIndex) else "Unknown"
                    contacts.add(PhoneContact(name, number))
                }
            }
            
            adapter.submitList(contacts)
            binding.progressBar.visibility = View.GONE
        } else {
            binding.tvPermissionDenied.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
            // Optional: You could request permission here, but MainActivity is handling it primarily.
        }
    }
}