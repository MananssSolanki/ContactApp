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
        

    }
}