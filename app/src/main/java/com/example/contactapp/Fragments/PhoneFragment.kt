package com.example.contactapp.Fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.contactapp.R
import com.example.contactapp.ViewModel.DialpadViewModel
import com.example.contactapp.databinding.FragmentPhoneBinding

class PhoneFragment : Fragment() {

    private lateinit var binding: FragmentPhoneBinding
    private lateinit var viewModel: DialpadViewModel
    private var currentNumber = StringBuilder()
    
    private val callPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                makeCall()
            } else {
                Toast.makeText(requireContext(), "Call permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPhoneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[DialpadViewModel::class.java]
        
        setupDialpad()
        setupActionButtons()
        observeViewModel()
    }

    private fun setupDialpad() {
        // Number buttons
        val numberButtons = listOf(
            binding.btn0 to "0",
            binding.btn1 to "1",
            binding.btn2 to "2",
            binding.btn3 to "3",
            binding.btn4 to "4",
            binding.btn5 to "5",
            binding.btn6 to "6",
            binding.btn7 to "7",
            binding.btn8 to "8",
            binding.btn9 to "9",
            binding.btnStar to "*",
            binding.btnHash to "#"
        )

        numberButtons.forEach { (button, digit) ->
            button.setOnClickListener {
                appendDigit(digit)
            }
        }

        // Long press on 0 to add +
        binding.btn0.setOnLongClickListener {
            if (currentNumber.isEmpty()) {
                appendDigit("+")
            }
            true
        }
    }

    private fun setupActionButtons() {
        // Backspace button
        binding.btnBackspace.setOnClickListener {
            if (currentNumber.isNotEmpty()) {
                currentNumber.deleteCharAt(currentNumber.length - 1)
                updateDisplay()
                lookupContact()
            }
        }

        // Long press backspace to clear all
        binding.btnBackspace.setOnLongClickListener {
            currentNumber.clear()
            updateDisplay()
            lookupContact()
            true
        }

        // Clear button
        binding.btnClear.setOnClickListener {
            currentNumber.clear()
            updateDisplay()
            lookupContact()
        }

        // Call button
        binding.btnCall.setOnClickListener {
            if (currentNumber.isNotEmpty()) {
                checkCallPermissionAndCall()
            } else {
                Toast.makeText(requireContext(), "Enter a phone number", Toast.LENGTH_SHORT).show()
            }
        }

        // Add to contacts button
        binding.btnAddContact.setOnClickListener {
            // TODO: Open add contact activity with pre-filled number
            Toast.makeText(requireContext(), "Add contact feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.contactName.observe(viewLifecycleOwner) { name ->
            if (name != null) {
                binding.tvContactName.text = name
                binding.tvContactName.visibility = View.VISIBLE
                binding.btnAddContact.visibility = View.GONE
            } else {
                binding.tvContactName.visibility = View.GONE
                // Show "Add to Contacts" only if number is valid
                if (currentNumber.isNotEmpty() && viewModel.isValidPhoneNumber(currentNumber.toString())) {
                    binding.btnAddContact.visibility = View.VISIBLE
                } else {
                    binding.btnAddContact.visibility = View.GONE
                }
            }
        }
    }

    private fun appendDigit(digit: String) {
        currentNumber.append(digit)
        updateDisplay()
        lookupContact()
    }

    private fun updateDisplay() {
        binding.etPhoneNumber.setText(currentNumber.toString())
        binding.etPhoneNumber.setSelection(currentNumber.length)
    }

    private fun lookupContact() {
        // Debounce the lookup to avoid excessive queries
        handler.removeCallbacks(lookupRunnable)
        handler.postDelayed(lookupRunnable, 300)
    }

    private val handler = Handler(Looper.getMainLooper())
    private val lookupRunnable = Runnable {
        viewModel.lookupContact(currentNumber.toString())
    }

    private fun checkCallPermissionAndCall() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            makeCall()
        } else {
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    private fun makeCall() {
        val number = currentNumber.toString()
        if (viewModel.isValidPhoneNumber(number)) {
            try {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$number")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to make call: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Invalid phone number", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(lookupRunnable)
    }
}