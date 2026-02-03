package com.example.contactapp.Fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactapp.Adapter.PhoneContactAdapter
import com.example.contactapp.R
import com.example.contactapp.ViewModel.ContactViewModel
import com.example.contactapp.databinding.FragmentContactBinding

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ContactFragment : Fragment() {

    private lateinit var binding : FragmentContactBinding

    private val adapter = PhoneContactAdapter()
    private lateinit var viewModel: ContactViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentContactBinding.inflate(inflater, container , false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(requireActivity())[ContactViewModel::class.java]


        binding.rvPhoneContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPhoneContacts.adapter = adapter

        observeData()
        checkPermissionAndLoad()

    }

    fun observeData(){
        viewModel.phoneContacts.observe(viewLifecycleOwner){
            adapter.submitList(it)
        }

        viewModel.loading.observe(viewLifecycleOwner){
            binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }
    }

    fun checkPermissionAndLoad(){
        if (ContextCompat.checkSelfPermission(requireContext() , Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED){
            binding.tvPermissionDenied.visibility = View.GONE
            viewModel.loadPhoneContacts()
        }else{
            binding.tvPermissionDenied.visibility = View.VISIBLE
        }
    }

}