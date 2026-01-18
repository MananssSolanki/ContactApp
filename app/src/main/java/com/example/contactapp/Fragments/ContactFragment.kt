package com.example.contactapp.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.contactapp.Adapter.ContactAdapter
import com.example.contactapp.R
import com.example.contactapp.ViewModel.ContactViewModel
import com.example.contactapp.databinding.FragmentContactBinding

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ContactFragment : Fragment() {

    private lateinit var binding : FragmentContactBinding
    private lateinit var viewModel: ContactViewModel
    private val adapter = ContactAdapter()


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

        binding.recyclerContacts.adapter = adapter

        viewModel.contact.observe(viewLifecycleOwner){
            adapter.submitList(it)
        }

        binding.btnAddContact.setOnClickListener {

        }


    }

}